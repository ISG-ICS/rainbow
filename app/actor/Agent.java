package actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.util.ByteString;
import algorithms.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.Config;
import model.*;
import play.libs.Json;
import util.*;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static util.Constants.DOUBLE_BYTES;
import static util.Constants.INT_BYTES;

public class Agent extends AbstractActor {

    public static Props props(ActorRef out, Config config) {
        return Props.create(Agent.class, out, config);
    }

    // states of this agent
    private ActorRef out;
    private Config config;
    private PostgreSQL postgreSQL;
    private List<Point> batch;

    /**
     * map of Algorithm instances
     * key - key
     * value - handle to Algorithm instance
     */
    private Map<String, IAlgorithm> algorithms;
    /**
     * map of Algorithms visiting counters
     * key - key
     * value - counter, number of visiting times of the algorithm
     */
    private Map<String, Integer> algorithmsHits;
    /**
     * Maximum number of Algorithm instances being kept in memory
     */
    private final int MAX_ALGORITHMS = 30;
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private Date start;
    private Date end;
    private int intervalDays;


    @Inject
    public Agent(ActorRef out, Config config) {
        this.out = out;
        this.config = config;
        this.algorithms = new HashMap<>();
        this.algorithmsHits = new HashMap<>();

        try {
            this.start = sdf.parse(this.config.getString("progressive.start"));
            this.end = sdf.parse(this.config.getString("progressive.end"));
        } catch(ParseException e) {
            e.printStackTrace();
        }
        this.intervalDays = this.config.getInt("progressive.interval");


        // initialize constants
        Constants.MIN_ZOOM = this.config.getInt("map.min_zoom");
        Constants.MAX_ZOOM = this.config.getInt("map.max_zoom");
        Constants.MIN_LONGITUDE = this.config.getDouble("data.minLng");
        Constants.MIN_LATITUDE = this.config.getDouble("data.minLat");
        Constants.MAX_LONGITUDE = this.config.getDouble("data.maxLng");
        Constants.MAX_LATITUDE = this.config.getDouble("data.maxLat");

        /**
         * Note: Mercator project a globe coordinate [lng, lat] onto a [0 ~ 1] coordinate on a continuous plane
         *              ^ 90                   0 ----------------> 1
         *              |                      |
         *              |                      |
         * -180 --------+--------> 180  ==>    |
         *              |                      |
         *              |                      |
         *              |-90                   V 1
         */
        Constants.MIN_X = Mercator.lngX(Constants.MIN_LONGITUDE);
        Constants.MIN_Y = Mercator.latY(Constants.MAX_LATITUDE); // latitude -> y is reversed than geo coordinates
        Constants.MAX_X = Mercator.lngX(Constants.MAX_LONGITUDE);
        Constants.MAX_Y = Mercator.latY(Constants.MIN_LATITUDE); // latitude -> y is reversed than geo coordinates

        Constants.DB_URL = this.config.getString("db.url");
        Constants.DB_USERNAME = this.config.getString("db.username");
        Constants.DB_PASSWORD = this.config.getString("db.password");
        Constants.DB_TABLENAME = this.config.getString("db.tablename");

        Constants.DATASET_NAME = this.config.getString("dataset.name");

        Constants.MSG_TYPE = this.config.getInt("message.type");

        Constants.TILE_RESOLUTION = this.config.getInt("tile.resolution");

        Constants.SAMPLING_METHOD = this.config.getString("sampling.method");

        Constants.STOP_CRITERIA = this.config.getDouble("stop.criteria");

        Constants.RENDERING_FUNCTION = this.config.getString("rendering.function");
        Constants.ERROR_FUNCTION = this.config.getString("error.function");
    }

    public static Props getProps() {
        return Props.create(Agent.class);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JsonNode.class, request -> {
                    MyLogger.info(this.getClass(), "received request: " + request);
                    if (this.out == null) {
                        this.out = sender();
                    }
                    handleRequest(request);
                })
                .matchAny(object -> MyLogger.error(this.getClass(), "Received unknown message: " + object.getClass()))
                .build();
    }

    private void buildGeoJsonArrayOfPoint(List<Point> points, ArrayNode geoJsonArray) {
        for (int i = 0; i < points.size(); i ++) {
            ObjectNode feature = JsonNodeFactory.instance.objectNode();
            feature.put("type", "Feature");

            ObjectNode properties = JsonNodeFactory.instance.objectNode();
            feature.set("properties", properties);

            ObjectNode geometry = JsonNodeFactory.instance.objectNode();
            ArrayNode coordinates = geometry.putArray("coordinates");
            coordinates.add(points.get(i).getX());
            coordinates.add(points.get(i).getY());
            geometry.put("type", "Point");
            feature.set("geometry", geometry);

            geoJsonArray.add(feature);
        }
    }

    private void buildDataArrayOfPoint(List<Point> points, ArrayNode dataArray) {
        for (int i = 0; i < points.size(); i ++) {
            ArrayNode pointTuple = JsonNodeFactory.instance.arrayNode();
            pointTuple.add(points.get(i).getY());
            pointTuple.add(points.get(i).getX());
            dataArray.add(pointTuple);
        }
    }

    /**
     * handle query request
     *  - if given cluster key does NOT exists,
     *      do the loadData and clusterData first,
     *  - query the cluster
     *
     * @param _request
     */
    private void handleQuery(Request _request) {

        if (_request.query == null) {
            // TODO - exception
        }
        Query query = _request.query;

        if (query.key == null) {
            // TODO - exception
        }
        String clusterKey = query.key;

        // if given cluster key does NOT exists, do the loadData and clusterData first,
        if (!algorithms.containsKey(clusterKey)) {
            // first check if we can load file to algorithm
            boolean success = loadFileToAlgorithm(query);
            if (success) {
                answerQuery(query, 100);
            }
            // otherwise, we can only do progressive data loading from DB
            else {
                handleQueryProgressively(_request);
            }
        }
        // otherwise, answer the query directly
        else {
            answerQuery(query, 100);
        }
    }

    private void answerQuery(Query query, int progress) {
        MyTimer.temporaryTimer.clear();
        MyTimer.temporaryTimer.put("treeTIme", 0.0);
        MyTimer.temporaryTimer.put("aggregateTime", 0.0);
        MyTimer.startTimer();

        // Add hit to querying super cluster
        algorithmsHits.put(query.key, algorithmsHits.get(query.key) + 1);

        // query the algorithm
        IAlgorithm algorithm = algorithms.get(query.key);
        byte[] binaryData = algorithm.answerQuery(query);

        MyTimer.stopTimer();
        double totalTime = MyTimer.durationSeconds();
        double treeTime = MyTimer.temporaryTimer.get("treeTime");
        double aggregateTime = MyTimer.temporaryTimer.get("aggregateTime");

        buildBinaryHeader(binaryData, progress, totalTime, treeTime, aggregateTime);

        respond(binaryData);
    }

    private void finishLoad(Query query) {
        // query the algorithm
        IAlgorithm algorithm = algorithms.get(query.key);
        algorithm.finishLoad();
    }

    private void buildBinaryHeader(byte[] binaryData, int progress, double totalTime, double treeTime, double aggregateTime) {
        // construct final response
        //  progress  totalTime  treeTime   aggTime  msgType   binary data payload
        // | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 4 BYTES | ...
        // header 1: progress
        int j = 0;
        binaryData[j+0] = (byte)((progress >> 24) & 0xff);
        binaryData[j+1] = (byte)((progress >> 16) & 0xff);
        binaryData[j+2] = (byte)((progress >>  8) & 0xff);
        binaryData[j+3] = (byte)((progress >>  0) & 0xff);
        // header 2: totalTime
        j = j + INT_BYTES;
        long totalTimeL = Double.doubleToRawLongBits(totalTime);
        binaryData[j+0] = (byte) ((totalTimeL >> 56) & 0xff);
        binaryData[j+1] = (byte) ((totalTimeL >> 48) & 0xff);
        binaryData[j+2] = (byte) ((totalTimeL >> 40) & 0xff);
        binaryData[j+3] = (byte) ((totalTimeL >> 32) & 0xff);
        binaryData[j+4] = (byte) ((totalTimeL >> 24) & 0xff);
        binaryData[j+5] = (byte) ((totalTimeL >> 16) & 0xff);
        binaryData[j+6] = (byte) ((totalTimeL >>  8) & 0xff);
        binaryData[j+7] = (byte) ((totalTimeL >>  0) & 0xff);
        j = j + DOUBLE_BYTES;
        long treeTimeL = Double.doubleToRawLongBits(treeTime);
        binaryData[j+0] = (byte) ((treeTimeL >> 56) & 0xff);
        binaryData[j+1] = (byte) ((treeTimeL >> 48) & 0xff);
        binaryData[j+2] = (byte) ((treeTimeL >> 40) & 0xff);
        binaryData[j+3] = (byte) ((treeTimeL >> 32) & 0xff);
        binaryData[j+4] = (byte) ((treeTimeL >> 24) & 0xff);
        binaryData[j+5] = (byte) ((treeTimeL >> 16) & 0xff);
        binaryData[j+6] = (byte) ((treeTimeL >>  8) & 0xff);
        binaryData[j+7] = (byte) ((treeTimeL >>  0) & 0xff);
        j = j + DOUBLE_BYTES;
        long aggregateTimeL = Double.doubleToRawLongBits(aggregateTime);
        binaryData[j+0] = (byte) ((aggregateTimeL >> 56) & 0xff);
        binaryData[j+1] = (byte) ((aggregateTimeL >> 48) & 0xff);
        binaryData[j+2] = (byte) ((aggregateTimeL >> 40) & 0xff);
        binaryData[j+3] = (byte) ((aggregateTimeL >> 32) & 0xff);
        binaryData[j+4] = (byte) ((aggregateTimeL >> 24) & 0xff);
        binaryData[j+5] = (byte) ((aggregateTimeL >> 16) & 0xff);
        binaryData[j+6] = (byte) ((aggregateTimeL >>  8) & 0xff);
        binaryData[j+7] = (byte) ((aggregateTimeL >>  0) & 0xff);
    }

    private void handleQueryProgressively(Request _request) {

        // for experiments analysis
        MyTimer.progressTimer.clear();
        MyTimer.progressTimer.put("clusterTime",  new ArrayList<>());
        MyTimer.progressTimer.put("treeTime", new ArrayList<>());
        MyMemory.progressUsedMemory.clear();
        MyMemory.porgressTotalMemory.clear();

        Query query = _request.query;
        if (_request.keyword == null) {
            // TODO - exception
        }
        this.batch = null;

        // initialize query slicing parameters
        Date currentStart = new Date(this.start.getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentStart);
        calendar.add(Calendar.DATE, this.intervalDays);
        Date currentEnd = calendar.getTime();
        long totalDays = (this.end.getTime() - this.start.getTime()) / (24 * 3600 * 1000);

        // start query slicing cycles
        while (currentStart.before(this.end)) {
            long progress = (currentEnd.getTime() - this.start.getTime()) / (24 * 3600 * 1000);
            progress = 100 * progress / totalDays;

            // (1) fetch a batch of data from database
            boolean success = fetchBatchFromDB(_request.keyword, currentStart, currentEnd);
            if (!success) {
                // TODO - exception
            }

            // (2) load the batch into algorithm
            MyTimer.startTimer();
            success = loadBatchToAlgorithm(query);
            MyTimer.stopTimer();
            MyTimer.progressTimer.get("clusterTime").add(MyTimer.durationSeconds());
            MyMemory.progressUsedMemory.add(MyMemory.getUsedMemory());
            MyMemory.porgressTotalMemory.add(MyMemory.getTotalMemory());
            if (!success) {
                // TODO - exception
            }

            // (3) answer current query with partial data loaded
            MyTimer.startTimer();
            answerQuery(query, (int) progress);
            MyTimer.stopTimer();
            MyTimer.progressTimer.get("treeTime").add(MyTimer.durationSeconds());

            // prepare query slicing parameters for next cycle
            currentStart = currentEnd;
            calendar = Calendar.getInstance();
            calendar.setTime(currentStart);
            calendar.add(Calendar.DATE, this.intervalDays);
            currentEnd = calendar.getTime();
        }

        // notify algorithm that data loading is done.
        finishLoad(query);

        // save algorithm to file.
        saveAlgorithmToFile(query);

        // for experiments analysis
        System.out.println("========== Experiment Analysis ==========");
        System.out.println("Progressive Query: ");
        System.out.println("keyword: " + _request.keyword);
        System.out.println("algorithm: " + _request.query.algorithm);
        System.out.println("clustering time for each batch: ");
        for (double time: MyTimer.progressTimer.get("clusterTime")) {
            System.out.println(time);
        }
        System.out.println("Tree-cut time for each batch: ");
        for (double time: MyTimer.progressTimer.get("treeTime")) {
            System.out.println(time);
        }
        System.out.println("memory usage until each batch (MB): ");
        for (int i = 0; i < MyMemory.progressUsedMemory.size(); i ++) {
            System.out.println(MyMemory.progressUsedMemory.get(i) + ",  " + MyMemory.porgressTotalMemory.get(i));
        }
        System.out.println("========== =================== ==========");
    }

    private void handleCmds(Request _request) {
        Command[] cmds = _request.cmds;

        if (cmds.length == 0) {
            respond(buildCmdResponse(_request, "null", "cmds is empty", "done"));
        }
        else {
            // run commands one by one, if any command fails, stop
            for (int i = 0; i < cmds.length; i ++) {
                Command cmd = cmds[i];
                boolean success = handleCmd(cmd, _request);
                if (!success) {
                    break;
                }
            }
        }
    }

    /**
     * fetch batch for given keyword and time range from database
     *
     * @param keyword
     * @param start
     * @param end
     * @return
     */
    private boolean fetchBatchFromDB(String keyword, Date start, Date end) {
        if (postgreSQL == null) {
            postgreSQL = new PostgreSQL();
        }
        List<Point> batchPoints;
        if (keyword.equals("%")) {
            batchPoints = postgreSQL.queryPointsForTime(start, end);
        }
        else {
            batchPoints = postgreSQL.queryPointsForKeywordAndTime(keyword, start, end);
        }
        if (batchPoints == null) {
            return false;
        }

        batch = batchPoints;

        return true;
    }

    /**
     * load current batch into the algorithm
     *
     * @param query - Query
     * @return
     */
    private boolean loadBatchToAlgorithm(Query query) {

        if (this.batch == null || this.batch.isEmpty()) {
            return false;
        }
        else {
            IAlgorithm algorithm = getAlgorithm(query);
            algorithm.load(this.batch);
        }

        return true;
    }

    private boolean loadFileToAlgorithm(Query query) {
        String fileName = Constants.DATASET_NAME + "-" + query.key + ".raqt";
        IAlgorithm algorithm = getAlgorithm(query);
        return algorithm.readFromFile(fileName);
    }

    private boolean saveAlgorithmToFile(Query query) {
        String fileName = Constants.DATASET_NAME + "-" + query.key + ".raqt";
        IAlgorithm algorithm = getAlgorithm(query);
        return algorithm.writeToFile(fileName);
    }

    private IAlgorithm getAlgorithm(Query query) {
        IAlgorithm algorithm;
        if (algorithms.containsKey(query.key)) {
            algorithm = algorithms.get(query.key);
        }
        else {
            // if too many cached clusters, replace the least used one
            if (algorithms.size() > MAX_ALGORITHMS) {
                String leastUsedAlgorithmKey = null;
                int leastHit = Integer.MAX_VALUE;
                for (Map.Entry<String, Integer> map: this.algorithmsHits.entrySet()) {
                    if (map.getValue() < leastHit) {
                        leastHit = map.getValue();
                        leastUsedAlgorithmKey = map.getKey();
                    }
                }
                if (leastUsedAlgorithmKey != null) {
                    algorithms.remove(leastUsedAlgorithmKey);
                }
            }

            switch (query.algorithm) {
                case "KDTreeExplorer":
                    algorithm = new KDTreeExplorer();
                    break;
                case "QuadTreeExplorer":
                    algorithm = new QuadTreeExplorer();
                    break;
                case "RAQuadTree":
                    algorithm = new RAQuadTree();
                    break;
                case "RAQuadTreeDistance":
                    algorithm = new RAQuadTreeDistance();
                    break;
                default:
                    return null;
            }
            algorithms.put(query.key, algorithm);
            algorithmsHits.put(query.key, 0);
        }
        return algorithm;
    }

    private boolean handleCmd(Command _cmd, Request _request) {
        switch (_cmd.action) {
            case "load": // deprecated
                respond(buildCmdResponse(_request, _cmd.action, "load data failed", "interface deprecated."));
                return false;
            case "cluster": // deprecated
                respond(buildCmdResponse(_request, _cmd.action, "cluster failed", "interface deprecated."));
                return false;
            default:
                respond(buildCmdResponse(_request, _cmd.action, "action is unknown", "error"));
                return false;
        }
    }

    private JsonNode buildCmdResponse(Request _request, String _cursor, String _msg, String _status) {
        JsonNode response = Json.toJson(_request);
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("cursor", _cursor);
        result.put("message", _msg);
        ((ObjectNode) response).put("status", _status);
        ((ObjectNode) response).set("result", result);
        return response;
    }

    private void handleTransfer(Request _request) {
        if (_request.keyword == null) {
            // TODO - exception
        }
        if (postgreSQL == null) {
            postgreSQL = new PostgreSQL();
        }
        batch = postgreSQL.queryPointsForKeyword(_request.keyword);
        if (batch == null) {
            // TODO - exception
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("type", "FeatureCollection");
        ArrayNode features = result.putArray("features");
        buildGeoJsonArrayOfPoint(batch, features);
        respond(result);
    }

    private void handleProgressTransfer(Request _request) {
        if (_request.keyword == null) {
            // TODO - exception
        }
        this.batch = null;

        // initialize query slicing parameters
        Date currentStart = new Date(this.start.getTime());
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentStart);
        calendar.add(Calendar.DATE, this.intervalDays);
        Date currentEnd = calendar.getTime();
        long totalDays = (this.end.getTime() - this.start.getTime()) / (24 * 3600 * 1000);

        // start query slicing cycles
        while (currentStart.before(this.end)) {

            long progress = (currentEnd.getTime() - this.start.getTime()) / (24 * 3600 * 1000);
            progress = 100 * progress / totalDays;

            // query delta data, pointTuples only keep delta data
            boolean success = fetchBatchFromDB(_request.keyword, currentStart, currentEnd);
            if (!success) {
                // TODO - exception
            }

            // construct the response Json and return
            JsonNode response = Json.toJson(_request);
            ObjectNode result = JsonNodeFactory.instance.objectNode();
            ArrayNode data = result.putArray("data");
            buildDataArrayOfPoint(batch, data);
            ((ObjectNode) response).put("progress", progress);
            ((ObjectNode) response).set("result", result);
            respond(response);

            // prepare query slicing parameters for next cycle
            currentStart = currentEnd;
            calendar = Calendar.getInstance();
            calendar.setTime(currentStart);
            calendar.add(Calendar.DATE, this.intervalDays);
            currentEnd = calendar.getTime();
        }
    }

    private void handleAnalysis(Request _request) {
        if (_request.analysis.objective == null) {
            // TODO - exception
        }
        switch (_request.analysis.objective) {
            case "distance": // deprecated
                JsonNode response = Json.toJson(_request);
                ObjectNode result = JsonNodeFactory.instance.objectNode();
                result.put("distance", 0.0);
                ((ObjectNode) response).put("status", "done");
                ((ObjectNode) response).set("result", result);
                respond(response);
                break;
            case "rand-index": // deprecated
                response = Json.toJson(_request);
                result = JsonNodeFactory.instance.objectNode();
                result.put("randIndex", 0.0);
                ((ObjectNode) response).put("status", "done");
                ((ObjectNode) response).set("result", result);
                respond(response);
                break;
        }

    }

    private void handleRequest(JsonNode _request) {
        Request request = Json.fromJson(_request, Request.class);

        switch (request.type) {
            case "query":
                MyLogger.info(this.getClass(), "request is a Query");
                handleQuery(request);
                break;
            case "cmd":
                MyLogger.info(this.getClass(), "request is a Command");
                handleCmds(request);
                break;
            case "transfer":
                MyLogger.info(this.getClass(), "request is a Transfer");
                handleTransfer(request);
                break;
            case "progress-transfer":
                MyLogger.info(this.getClass(), "request is a Progress-Transfer");
                handleProgressTransfer(request);
                break;
            case "analysis":
                MyLogger.info(this.getClass(), "request is a Analysis");
                handleAnalysis(request);
                break;
            default:
                MyLogger.info(this.getClass(), "request type is unknown");
                JsonNode response = Json.toJson(_request);
                ((ObjectNode) response).put("status", "unknown");
                ((ObjectNode) response).put("message", "this request type is unknown");
                respond(response);
        }
    }

    private void respond(JsonNode _response) {
        MyLogger.info(this.getClass(), "responding in JSON format.");
        out.tell(_response, self());
    }

    private void respond(byte[] _response) {
        ByteString response = ByteString.fromArray(_response);
        MyLogger.info(this.getClass(), "responding in Binary format.");
        out.tell(response, self());
    }
}
