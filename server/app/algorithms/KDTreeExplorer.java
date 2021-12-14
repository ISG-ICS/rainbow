package algorithms;

import model.Point;
import model.Query;
import util.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static util.Mercator.*;

public class KDTreeExplorer implements IAlgorithm {
    I2DIndex index;
    int totalNumberOfPoints = 0;

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> timing;
    //-Timing-//

    public KDTreeExplorer() {
        this.index = new KDTree<>();

        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("total", 0.0);
        }

        MyMemory.printMemory();
    }

    public void load(List<Point> points) {
        System.out.println("[KDTree Explorer] loading " + points.size() + " points ... ...");
        this.totalNumberOfPoints += points.size();

        MyTimer.startTimer();
        for (Point point: points) {
            this.index.insert(lngLatToXY(point));
        }
        MyTimer.stopTimer();
        double loadTime = MyTimer.durationSeconds();

        if (keepTiming) timing.put("total", timing.get("total") + loadTime);
        System.out.println("[KDTree Explorer] loading is done!");
        System.out.println("[KDTree Explorer] loading time: " + loadTime + " seconds.");
        if (keepTiming) this.printTiming();

        MyMemory.printMemory();
    }

    @Override
    public void finishLoad() {

    }

    /**
     * Get list of points references for given visible region
     *
     * @param lng0
     * @param lat0
     * @param lng1
     * @param lat1
     * @return
     */
    private List<Point> getPoints(double lng0, double lat0, double lng1, double lat1) {
        double minLng = ((lng0 + 180) % 360 + 360) % 360 - 180;
        double minLat = Math.max(-90, Math.min(90, lat0));
        double maxLng = lng1 == 180 ? 180 : ((lng1 + 180) % 360 + 360) % 360 - 180;
        double maxLat = Math.max(-90, Math.min(90, lat1));

        if (lng1 - lng0 >= 360) {
            minLng = -180;
            maxLng = 180;
        } else if (minLng > maxLng) {
            List<Point> easternHem = this.getPoints(minLng, minLat, 180, maxLat);
            List<Point> westernHem = this.getPoints(-180, minLat, maxLng, maxLat);
            return concat(easternHem, westernHem);
        }

        Point leftBottom = new Point(lngX(minLng), latY(maxLat));
        Point rightTop = new Point(lngX(maxLng), latY(minLat));
        List<Point> points = this.index.range(leftBottom, rightTop);
        return points;
    }

    private List<Point> concat(List<Point> a, List<Point> b) {
        a.addAll(b);
        return a;
    }

    public byte[] answerQuery(Query query) {
        double lng0 = query.bbox[0];
        double lat0 = query.bbox[1];
        double lng1 = query.bbox[2];
        double lat1 = query.bbox[3];
        int zoom = query.zoom;
        int resX = query.resX;
        int resY = query.resY;
        int sampleSize = query.sampleSize;

        MyTimer.startTimer();
        System.out.println("[KDTree Explorer] is answering query: \n" +
                "Q = { \n" +
                "    range: [" + lng0 + ", " + lat0 + "] ~ [" + lng1 + ", " + lat1 + "], \n" +
                "    resolution: [" + resX + " x " + resY + "], \n" +
                "    zoom: " + zoom + ",\n " +
                "    sampleSize: " + sampleSize + " \n" +
                " }");

        // get all data points
        MyTimer.startTimer();
        List<Point> allPoints = getPoints(lng0, lat0, lng1, lat1);
        MyTimer.stopTimer();
        double treeTime = MyTimer.durationSeconds();
        MyTimer.temporaryTimer.put("treeTime", treeTime);
        System.out.println("[KDTree Explorer] tree search got " + allPoints.size() + " data points.");
        System.out.println("[KDTree Explorer] tree search time: " + treeTime + " seconds.");

        // if sampleSize > 0 (sampleSize given by the user), shuffle the result list of points
        if (sampleSize > 0) {
            Collections.shuffle(allPoints);
        }

        // build binary result message
        MyTimer.startTimer();
        BinaryMessageBuilder messageBuilder = new BinaryMessageBuilder();
        double lng, lat;
        int resultSize = 0;
        for (Point point : allPoints) {
            lng = xLng(point.getX());
            lat = yLat(point.getY());
            messageBuilder.add(lng, lat);
            resultSize ++;
            // if sampleSize > 0 (sampleSize given by the user), return the top sampleSize points as result
            if (sampleSize > 0 && resultSize >= sampleSize)
                break;
        }
        MyTimer.stopTimer();
        double buildBinaryTime = MyTimer.durationSeconds();
        MyTimer.temporaryTimer.put("aggregateTime", buildBinaryTime);
        System.out.println("[KDTree Explorer] build binary result with " + resultSize + " points.");
        System.out.println("[KDTree Explorer] build binary result time: " + buildBinaryTime + " seconds.");

        MyTimer.stopTimer();
        System.out.println("[KDTree Explorer] answer query total time: " + MyTimer.durationSeconds() + " seconds.");
        return messageBuilder.getBuffer();
    }

    @Override
    public boolean readFromFile(String fileName) {
        return false;
    }

    @Override
    public boolean writeToFile(String fileName) {
        return false;
    }

    private void printTiming() {
        System.out.println("[Total Time] " + timing.get("total") + " seconds.");
    }
}
