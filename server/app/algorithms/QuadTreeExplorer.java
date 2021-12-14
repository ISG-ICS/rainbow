package algorithms;

import javafx.util.Pair;
import model.Point;
import model.Query;
import util.BinaryMessageBuilder;
import util.Constants;
import util.MyTimer;
import util.render.DeckGLRenderer;
import util.render.IRenderer;
import util.MyMemory;

import java.util.*;

import static util.Mercator.*;

public class QuadTreeExplorer implements IAlgorithm {

    public static double highestLevelNodeDimension;
    // resolution of each node (similar to a tile in map systems), e.g. 512
    public static int oneNodeResolution;

    public static IRenderer renderer;

    public class QuadTree {
        // Store count of the sub-tree
        public int count;
        public byte[] rendering;
        public List<Point> samples;

        // children
        public QuadTree northWest;
        public QuadTree northEast;
        public QuadTree southWest;
        public QuadTree southEast;

        public QuadTree() {
            this.count = 0;
        }

        public boolean containsPoint(double cX, double cY, double halfDimension, Point point) {
            if (point.getX() >= (cX - halfDimension)
                    && point.getY() >= (cY - halfDimension)
                    && point.getX() < (cX + halfDimension)
                    && point.getY() < (cY + halfDimension)) {
                return true;
            }
            else {
                return false;
            }
        }

        public boolean intersectsBBox(double c1X, double c1Y, double halfDimension1,
                                      double c2X, double c2Y, double halfWidth2, double halfHeight2) {
            // bbox 1
            double left = c1X - halfDimension1;
            double right = c1X + halfDimension1;
            double bottom = c1Y + halfDimension1;
            double top = c1Y - halfDimension1;
            // bbox 2
            double minX = c2X - halfWidth2;
            double maxX = c2X + halfWidth2;
            double minY = c2Y - halfHeight2;
            double maxY = c2Y + halfHeight2;

            // right to the right
            if (minX > right) return false;
            // left to the left
            if (maxX < left) return false;
            // above the bottom
            if (minY > bottom) return false;
            // below the top
            if (maxY < top) return false;

            return true;
        }

        public boolean insert(double cX, double cY, double halfDimension, Point point, IRenderer aggregator, int level) {
            // Ignore objects that do not belong in this quad tree
            if (!containsPoint(cX, cY, halfDimension, point)) {
                return false;
            }
            // If this node is leaf and empty, put this point on this node
            if (this.samples == null && this.northWest == null) {
                this.samples = new ArrayList<>();
                this.samples.add(point);
                this.rendering = aggregator.createRendering(oneNodeResolution);
                aggregator.render(this.rendering, cX, cY, halfDimension, oneNodeResolution, point);
                this.count = 1;
                return true;
            }
            // Else, add count into this node
            this.count ++;

            // if boundary is smaller than highestLevelNodeDimension, stop splitting, and make current node a leaf node.
            if (halfDimension * 2 / oneNodeResolution < highestLevelNodeDimension) {
                this.samples.add(point);
                return true;
            }

            // Otherwise, subdivide
            if (this.northWest == null) {
                this.subdivide();
                // insert current node's point into corresponding quadrant
                this.insertNorthWest(cX, cY, halfDimension, this.samples.get(0), aggregator, level + 1);
                this.insertNorthEast(cX, cY, halfDimension, this.samples.get(0), aggregator, level + 1);
                this.insertSouthWest(cX, cY, halfDimension, this.samples.get(0), aggregator, level + 1);
                this.insertSouthEast(cX, cY, halfDimension, this.samples.get(0), aggregator, level + 1);
            }

            // update the rendering of this node
            boolean isDifferent = aggregator.render(this.rendering, cX, cY, halfDimension, oneNodeResolution, point);
            // if new rendering is different, store this point within samples
            // (only start storing samples from level 10)
            if (level > 2 && isDifferent) this.samples.add(point);

            // insert new point into corresponding quadrant
            if (insertNorthWest(cX, cY, halfDimension, point, aggregator, level + 1)) return true;
            if (insertNorthEast(cX, cY, halfDimension, point, aggregator, level + 1)) return true;
            if (insertSouthWest(cX, cY, halfDimension, point, aggregator, level + 1)) return true;
            if (insertSouthEast(cX, cY, halfDimension, point, aggregator, level + 1)) return true;

            return false;
        }

        boolean insertNorthWest(double _cX, double _cY, double _halfDimension, Point point, IRenderer aggregator, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX - halfDimension;
            double cY = _cY - halfDimension;
            return this.northWest.insert(cX, cY, halfDimension, point, aggregator, level);
        }

        boolean insertNorthEast(double _cX, double _cY, double _halfDimension, Point point, IRenderer aggregator, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX + halfDimension;
            double cY = _cY - halfDimension;
            return this.northEast.insert(cX, cY, halfDimension, point, aggregator, level);
        }

        boolean insertSouthWest(double _cX, double _cY, double _halfDimension, Point point, IRenderer aggregator, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX - halfDimension;
            double cY = _cY + halfDimension;
            return this.southWest.insert(cX, cY, halfDimension, point, aggregator, level);
        }

        boolean insertSouthEast(double _cX, double _cY, double _halfDimension, Point point, IRenderer aggregator, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX + halfDimension;
            double cY = _cY + halfDimension;
            return this.southEast.insert(cX, cY, halfDimension, point, aggregator, level);
        }

        void subdivide() {
            this.northWest = new QuadTree();
            this.northEast = new QuadTree();
            this.southWest = new QuadTree();
            this.southEast = new QuadTree();
            nodesCount += 4;
        }

        public List<Point> range(double ncX, double ncY, double nhalfDimension,
                                 double rcX, double rcY, double rhalfWidth, double rhalfHeight,
                                 double rPixelScale, int level) {
            List<Point> pointsInRange = new ArrayList<>();

            // Automatically abort if the range does not intersect this quad
            if (!intersectsBBox(ncX, ncY, nhalfDimension, rcX, rcY, rhalfWidth, rhalfHeight))
                return pointsInRange; // empty list

            // Terminate here, if there are no children
            if (this.northWest == null) {
                highestLevelForQuery = Math.max(highestLevelForQuery, level);
                highestPixelScale = Math.max(highestPixelScale, (nhalfDimension * 2 / oneNodeResolution));
                if (this.samples != null) {
                    pointsInRange.addAll(this.samples);
                }
                return pointsInRange;
            }

            // Terminate here, if this node's pixel scale is already smaller than the range query's pixel scale
            if ((nhalfDimension * 2 / oneNodeResolution) <= rPixelScale) {
                lowestLevelForQuery = Math.min(lowestLevelForQuery, level);
                lowestPixelScale = Math.min(lowestPixelScale, (nhalfDimension * 2 / oneNodeResolution));
                // add this node's samples
                pointsInRange.addAll(this.samples);
                return pointsInRange;
            }

            // Otherwise, add the points from the children
            double cX, cY;
            double halfDimension;
            halfDimension = nhalfDimension / 2;
            // northwest
            cX = ncX - halfDimension;
            cY = ncY - halfDimension;
            pointsInRange.addAll(this.northWest.range(cX, cY, halfDimension,
                    rcX, rcY, rhalfWidth, rhalfHeight, rPixelScale, level + 1));

            // northeast
            cX = ncX + halfDimension;
            cY = ncY - halfDimension;
            pointsInRange.addAll(this.northEast.range(cX, cY, halfDimension,
                    rcX, rcY, rhalfWidth, rhalfHeight, rPixelScale, level + 1));

            // southwest
            cX = ncX - halfDimension;
            cY = ncY + halfDimension;
            pointsInRange.addAll(this.southWest.range(cX, cY, halfDimension,
                    rcX, rcY, rhalfWidth, rhalfHeight, rPixelScale, level + 1));

            // southeast
            cX = ncX + halfDimension;
            cY = ncY + halfDimension;
            pointsInRange.addAll(this.southEast.range(cX, cY, halfDimension,
                    rcX, rcY, rhalfWidth, rhalfHeight, rPixelScale, level + 1));

            return pointsInRange;
        }

        public void print() {
            System.out.println("=================== QuadTreeExplorer ===================");
            Queue<Pair<Integer, QuadTree>> queue = new LinkedList<>();
            queue.add(new Pair<>(0, this));
            int currentLevel = -1;
            while (queue.size() > 0) {
                Pair<Integer, QuadTree> currentEntry = queue.poll();
                int level = currentEntry.getKey();
                QuadTree currentNode = currentEntry.getValue();
                if (level > currentLevel) {
                    System.out.println();
                    System.out.print("[" + level + "] ");
                    currentLevel = level;
                }
                System.out.print(currentNode.samples == null? "0": currentNode.samples.size());
                System.out.print(", ");
                if (currentNode.northWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northWest));
                }
                if (currentNode.northEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northEast));
                }
                if (currentNode.southWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southWest));
                }
                if (currentNode.southEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southEast));
                }
            }
            System.out.println();
        }

        public void statistics() {
            System.out.println("=================== QuadTreeExplorer Statistics ===================");
            System.out.println("level,    # samples,    # nodes,    # samples/node,    min # samples,    max # samples");
            Queue<Pair<Integer, QuadTree>> queue = new LinkedList<>();
            queue.add(new Pair<>(0, this));
            int currentLevel = -1;
            int totalNumberOfSamples = 0;
            int totalNumberOfNodes = 0;
            int totalMinNumberOfSamples = Integer.MAX_VALUE;
            int totalMaxNumberOfSamples = 0;
            int numberOfSamples = 0;
            int numberOfNodes = 0;
            int minNumberOfSamples = Integer.MAX_VALUE;
            int maxNumberOfSamples = 0;
            while (queue.size() > 0) {
                Pair<Integer, QuadTree> currentEntry = queue.poll();
                int level = currentEntry.getKey();
                QuadTree currentNode = currentEntry.getValue();
                int currentNumberOfSamples = currentNode.samples == null? 0: currentNode.samples.size();
                numberOfSamples += currentNumberOfSamples;
                numberOfNodes += 1;
                minNumberOfSamples = Math.min(currentNumberOfSamples, minNumberOfSamples);
                maxNumberOfSamples = Math.max(currentNumberOfSamples, maxNumberOfSamples);
                if (level > currentLevel) {
                    System.out.println(level + ",    " + numberOfSamples + ",    " + numberOfNodes + ",    " + (numberOfSamples/numberOfNodes) + ",    " + minNumberOfSamples + ",    " + maxNumberOfSamples);
                    currentLevel = level;
                    totalNumberOfSamples += numberOfSamples;
                    totalNumberOfNodes += numberOfNodes;
                    totalMinNumberOfSamples = Math.min(totalMinNumberOfSamples, minNumberOfSamples);
                    totalMaxNumberOfSamples = Math.max(totalMaxNumberOfSamples, maxNumberOfSamples);
                    numberOfSamples = 0;
                    numberOfNodes = 0;
                    minNumberOfSamples = Integer.MAX_VALUE;
                    maxNumberOfSamples = 0;
                }
                if (currentNode.northWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northWest));
                }
                if (currentNode.northEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northEast));
                }
                if (currentNode.southWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southWest));
                }
                if (currentNode.southEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southEast));
                }
            }
            System.out.println("-------------------------- Summary -------------------------");
            System.out.println("total # samples,    total # nodes,    total # samples/node,    total min # samples,    total max # samples");
            System.out.println(totalNumberOfSamples + ",    " + totalNumberOfNodes + ",    " + (totalNumberOfSamples/totalNumberOfNodes) + ",    " + totalMinNumberOfSamples + " ,   " + totalMaxNumberOfSamples);
        }

        public void histograms(int someLevel) {
            int[] histogramForSamplesOnIntermediateNodes = new int[101]; // 0 ~ 99, >=100
            int[] histogramForRawPointsOnLeafNodes = new int[101]; // 0 ~ 99, >=100
            int[] histogramForSamplesOnIntermediateNodesAtLevel = new int[101]; // 0 ~ 99, >=100
            int[] histogramForRawPointsOnIntermediateNodesAtLevel = new int[101]; // 0 ~ 999, >=1000

            Queue<Pair<Integer, QuadTree>> queue = new LinkedList<>();
            queue.add(new Pair<>(0, this));
            while (queue.size() > 0) {
                Pair<Integer, QuadTree> currentEntry = queue.poll();
                int level = currentEntry.getKey();
                QuadTree currentNode = currentEntry.getValue();
                int currentNumberOfSamples = currentNode.samples == null? 0: currentNode.samples.size();
                if (currentNode.northWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northWest));
                }
                if (currentNode.northEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.northEast));
                }
                if (currentNode.southWest != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southWest));
                }
                if (currentNode.southEast != null) {
                    queue.add(new Pair<>(level + 1, currentNode.southEast));
                }
                if (currentNode.northWest != null && level > 10){
                    if (currentNumberOfSamples > 99) histogramForSamplesOnIntermediateNodes[100] += 1;
                    else histogramForSamplesOnIntermediateNodes[currentNumberOfSamples] += 1;
                    if (level == someLevel) {
                        if (currentNumberOfSamples > 99) histogramForSamplesOnIntermediateNodesAtLevel[100] += 1;
                        else histogramForSamplesOnIntermediateNodesAtLevel[currentNumberOfSamples] += 1;
                        if (currentNode.count > 990) histogramForRawPointsOnIntermediateNodesAtLevel[100] += 1;
                        else histogramForRawPointsOnIntermediateNodesAtLevel[currentNode.count/10] += 1;
                    }
                }
                else if (currentNode.northWest == null) {
                    if (currentNumberOfSamples > 99) histogramForRawPointsOnLeafNodes[100] += 1;
                    else histogramForRawPointsOnLeafNodes[currentNumberOfSamples] += 1;
                }
            }

            System.out.println("=================== QuadTreeExplorer Histogram for Samples on Intermediate Nodes ===================");
            System.out.println("# of samples on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println(i + ",    " + histogramForSamplesOnIntermediateNodes[i]);
            }
            System.out.println(">=100,    " + histogramForSamplesOnIntermediateNodes[100]);

            System.out.println("=================== QuadTreeExplorer Histogram for Raw Points on Leaf Nodes ===================");
            System.out.println("# of raw points on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println(i + ",    " + histogramForRawPointsOnLeafNodes[i]);
            }
            System.out.println(">=100,    " + histogramForRawPointsOnLeafNodes[100]);

            System.out.println("=================== QuadTreeExplorer Histogram for Samples on Intermediate Nodes at level " + someLevel + " ===================");
            System.out.println("# of samples on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println(i + ",    " + histogramForSamplesOnIntermediateNodesAtLevel[i]);
            }
            System.out.println(">=100,    " + histogramForSamplesOnIntermediateNodesAtLevel[100]);

            System.out.println("=================== QuadTreeExplorer Histogram for Raw Points on Intermediate Nodes at level " + someLevel + " ===================");
            System.out.println("# of raw points on node,    # of nodes");
            for (int i = 0; i < 100; i ++) {
                System.out.println((0 + i*10) + "~" + (9 + i*10) + ",    " + histogramForRawPointsOnIntermediateNodesAtLevel[i]);
            }
            System.out.println(">=1000,    " + histogramForRawPointsOnIntermediateNodesAtLevel[100]);
        }
    }

    QuadTree quadTree;
    int totalNumberOfPoints = 0;
    int totalStoredNumberOfPoints = 0;
    static long nodesCount = 0; // count quad-tree nodes
    static int lowestLevelForQuery = Integer.MAX_VALUE; // the lowest level of range searching for a query
    static double lowestPixelScale = Double.MAX_VALUE; // the lowest pixel scale of range searching for a query
    static int highestLevelForQuery = 0; // the highest level of range searching for a query
    static double highestPixelScale = 0.0; // the highest pixel scale of range searching for a query

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> timing;
    //-Timing-//

    public QuadTreeExplorer() {
        this.quadTree = new QuadTree();

        oneNodeResolution = Constants.TILE_RESOLUTION;

        // zoom level 0 is fixed with dimension 1.0
        highestLevelNodeDimension = 1.0 / Math.pow(2, Constants.MAX_ZOOM);

        renderer = new DeckGLRenderer(Constants.RADIUS_IN_PIXELS);

        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("total", 0.0);
        }

        MyMemory.printMemory();
    }

    public void load(List<Point> points) {
        System.out.println("[QuadTree Explorer] loading " + points.size() + " points ... ...");

        MyTimer.startTimer();
        this.totalNumberOfPoints += points.size();
        int count = 0;
        int skip = 0;
        for (Point point: points) {
            if (this.quadTree.insert(0.5, 0.5, 0.5, lngLatToXY(point), renderer, 0))
                count ++;
            else
                skip ++;
        }
        this.totalStoredNumberOfPoints += count;
        MyTimer.stopTimer();
        double loadTime = MyTimer.durationSeconds();

        System.out.println("[QuadTree Explorer] inserted " + count + " points and skipped " + skip + " points.");
        if (keepTiming) timing.put("total", timing.get("total") + loadTime);
        System.out.println("[QuadTree Explorer] loading is done!");
        System.out.println("[QuadTree Explorer] loading time: " + loadTime + " seconds.");
        if (keepTiming) this.printTiming();

        MyMemory.printMemory();

        //-DEBUG-//
        System.out.println("==== Until now ====");
        System.out.println("General QuadTree has processed " + this.totalNumberOfPoints + " points.");
        System.out.println("General QuadTree has stored " + this.totalStoredNumberOfPoints + " points.");
        System.out.println("General QuadTree has skipped " + skip + " points.");
        System.out.println("General QuadTree has generated " + nodesCount + " nodes.");
        this.quadTree.statistics();
        //this.quadTree.histograms(12);
        //-DEBUG-//
    }

    @Override
    public void finishLoad() {

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
        System.out.println("[QuadTree Explorer] is answering query: \n" +
                "Q = { \n" +
                "    range: [" + lng0 + ", " + lat0 + "] ~ [" + lng1 + ", " + lat1 + "], \n" +
                "    resolution: [" + resX + " x " + resY + "], \n" +
                "    zoom: " + zoom + ",\n " +
                "    sampleSize: " + sampleSize + " \n" +
                " }");

        double iX0 = lngX(lng0);
        double iY0 = latY(lat0);
        double iX1 = lngX(lng1);
        double iY1 = latY(lat1);
        double pixelScale = 1.0 / 256 / Math.pow(2, zoom);
        double rcX = (iX0 + iX1) / 2;
        double rcY = (iY0 + iY1) / 2;
        double rhalfWidth = (iX1 - iX0) / 2;
        double rhalfHeight = (iY0 - iY1) / 2;

        System.out.println("[QuadTree Explorer] starting range search on QuadTree with: \n" +
                "range = [(" + rcX + ", " + rcY + "), " + rhalfWidth + ", " + rhalfHeight + "] ; \n" +
                "pixelScale = " + pixelScale + ";");

        lowestLevelForQuery = Integer.MAX_VALUE;
        lowestPixelScale = Double.MAX_VALUE;
        highestLevelForQuery = 0;
        highestPixelScale = 0.0;

        MyTimer.startTimer();
        List<Point> points = this.quadTree.range(0.5, 0.5, 0.5,
                rcX, rcY, rhalfWidth, rhalfHeight, pixelScale, 0);
        MyTimer.stopTimer();
        double treeTime = MyTimer.durationSeconds();

        MyTimer.temporaryTimer.put("treeTime", treeTime);
        System.out.println("[QuadTree Explorer] tree search got " + points.size() + " data points.");
        System.out.println("[QuadTree Explorer] tree search time: " + treeTime + " seconds.");

        // if sampleSize > 0 (sampleSize given by the user), shuffle the result list of points
        if (sampleSize > 0) {
            Collections.shuffle(points);
        }

        // build binary result message
        MyTimer.startTimer();
        BinaryMessageBuilder messageBuilder = new BinaryMessageBuilder();
        double lng, lat;
        int resultSize = 0;
        for (Point point : points) {
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

        System.out.println("[QuadTree Explorer] build binary result with  " + resultSize + " points.");
        System.out.println("[QuadTree Explorer] build binary result time: " + buildBinaryTime + " seconds.");

        MyTimer.stopTimer();
        System.out.println("[QuadTree Explorer] answer query total time: " + MyTimer.durationSeconds() + " seconds.");
        System.out.println("[QuadTree Explorer] lowest level for this query: " + lowestLevelForQuery);
        System.out.println("[QuadTree Explorer] highest level for this query: " + highestLevelForQuery);
        System.out.println("[QuadTree Explorer] lowest pixelScale for this query: " + lowestPixelScale);
        System.out.println("[QuadTree Explorer] highest pixelScale for this query: " + highestPixelScale);
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
