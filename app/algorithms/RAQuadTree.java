package algorithms;

import model.Point;
import model.Query;
import util.*;
import util.render.*;

import java.io.*;
import java.util.*;

import static util.Mercator.*;

public class RAQuadTree implements IAlgorithm {

    public class QuadTree {
        public Point sample;
        public int count; // count of subtree
        public double[] errors; // errors between this sample and four children's samples for all zoom levels

        public QuadTree() {
            this.sample = null;
            this.count = 0;
            this.errors = new double[Constants.MAX_ZOOM + 1];
        }

        // children
        public QuadTree northWest;
        public QuadTree northEast;
        public QuadTree southWest;
        public QuadTree southEast;

        /**
         * Pre-order traverse the quadtree and write each node to one line in the buffered writer
         *
         * each node format:
         *   level (int), cx (double), cy (double), halfDimension (double), count (int),
         *   sample.x (double), sample.y (double), errors[0] (double), errors[1] (double), ...
         *
         * @param bufferedWriter
         * @param _cX
         * @param _cY
         * @param _halfDimension
         * @param _level
         */
        public void writeToFile(BufferedWriter bufferedWriter, double _cX, double _cY, double _halfDimension, int _level) throws IOException {
            // write current node
            bufferedWriter.write(String.valueOf(this.count));
            bufferedWriter.write(",");
            if (this.sample != null) {
                bufferedWriter.write(String.valueOf(this.sample.getX()));
                bufferedWriter.write(",");
                bufferedWriter.write(String.valueOf(this.sample.getY()));
            }
            else {
                bufferedWriter.write(",");
            }
            for (int zoom = 0; zoom <= Constants.MAX_ZOOM; zoom ++) {
                bufferedWriter.write(",");
                bufferedWriter.write(String.valueOf(this.errors[zoom]));
            }
            bufferedWriter.newLine();

            // leaf node write an empty line for each child
            if (this.northWest == null) {
                // nw
                bufferedWriter.write("");
                bufferedWriter.newLine();
                // ne
                bufferedWriter.write("");
                bufferedWriter.newLine();
                // sw
                bufferedWriter.write("");
                bufferedWriter.newLine();
                // se
                bufferedWriter.write("");
                bufferedWriter.newLine();
                return;
            }
            else {
                // recursively write the children
                double halfDimension = _halfDimension / 2;
                this.northWest.writeToFile(bufferedWriter, _cX - halfDimension, _cY - halfDimension, halfDimension, _level + 1);
                this.northEast.writeToFile(bufferedWriter, _cX + halfDimension, _cY - halfDimension, halfDimension, _level + 1);
                this.southWest.writeToFile(bufferedWriter, _cX - halfDimension, _cY + halfDimension, halfDimension, _level + 1);
                this.southEast.writeToFile(bufferedWriter, _cX + halfDimension, _cY + halfDimension, halfDimension, _level + 1);
            }
        }

        /**
         * Pre-order traverse the quadtree and read each node from one line in the buffered reader
         *
         * each node format:
         *   level (int), cx (double), cy (double), halfDimension (double), count (int),
         *   sample.x (double), sample.y (double), errors[0] (double), errors[1] (double), ...
         *
         * @param bufferedReader
         * @param _cX
         * @param _cY
         * @param _halfDimension
         * @param _level
         */
        public QuadTree readFromFile(BufferedReader bufferedReader, double _cX, double _cY, double _halfDimension, int _level) throws IOException {
            String line = bufferedReader.readLine();

            // end of file
            if (line == null) return null;

            // null for this node
            if (line.isEmpty()) {
                return null;
            }

            try {
                // read current node
                QuadTree node = new QuadTree();
                String[] attributes = line.split(",");
                int i = 0;
                node.count = Integer.valueOf(attributes[i++]);

                if (attributes[i].isEmpty()) {
                    node.sample = null;
                    i += 2;
                } else {
                    double x = Double.valueOf(attributes[i++]);
                    double y = Double.valueOf(attributes[i++]);
                    node.sample = new Point(x, y);
                }

                for (int zoom = 0; zoom <= Constants.MAX_ZOOM; zoom++) {
                    node.errors[zoom] = Double.valueOf(attributes[i++]);
                }

                // recursively read the children
                double halfDimension = _halfDimension / 2;
                node.northWest = this.readFromFile(bufferedReader, _cX - halfDimension, _cY - halfDimension, halfDimension, _level + 1);
                node.northEast = this.readFromFile(bufferedReader, _cX + halfDimension, _cY - halfDimension, halfDimension, _level + 1);
                node.southWest = this.readFromFile(bufferedReader, _cX - halfDimension, _cY + halfDimension, halfDimension, _level + 1);
                node.southEast = this.readFromFile(bufferedReader, _cX + halfDimension, _cY + halfDimension, halfDimension, _level + 1);
                return node;
            }
            catch (Exception e) {
                e.printStackTrace();
                System.out.println(line);
            }
            return null;
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

        public boolean insert(double cX, double cY, double halfDimension, Point point, int level) {
            // Ignore objects that do not belong in this quad tree
            if (!containsPoint(cX, cY, halfDimension, point)) {
                return false;
            }
            // If this node is leaf and empty, put this point on this node
            if (this.sample == null && this.northWest == null) {
                this.sample = point;
                this.count = 1;
                return true;
            }
            // Else, add count into this node
            this.count ++;

            // if boundary is smaller than highestLevelNodeDimension,
            // stop splitting, and make current node a leaf node.
            if (halfDimension * 2 < highestLevelNodeDimension) {
                // at this moment, this node must already have a sample
                return false; // skip this point
            }

            // Otherwise, subdivide
            if (this.northWest == null) {
                this.subdivide();
                // descend current node's point into corresponding quadrant
                this.insertNorthWest(cX, cY, halfDimension, this.sample, level + 1);
                this.insertNorthEast(cX, cY, halfDimension, this.sample, level + 1);
                this.insertSouthWest(cX, cY, halfDimension, this.sample, level + 1);
                this.insertSouthEast(cX, cY, halfDimension, this.sample, level + 1);
                this.sample = null;
            }

            // insert new point into corresponding quadrant
            if (insertNorthWest(cX, cY, halfDimension, point, level + 1)) return true;
            if (insertNorthEast(cX, cY, halfDimension, point, level + 1)) return true;
            if (insertSouthWest(cX, cY, halfDimension, point, level + 1)) return true;
            if (insertSouthEast(cX, cY, halfDimension, point, level + 1)) return true;

            return false;
        }

        boolean insertNorthWest(double _cX, double _cY, double _halfDimension, Point point, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX - halfDimension;
            double cY = _cY - halfDimension;
            return this.northWest.insert(cX, cY, halfDimension, point, level);
        }

        boolean insertNorthEast(double _cX, double _cY, double _halfDimension, Point point, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX + halfDimension;
            double cY = _cY - halfDimension;
            return this.northEast.insert(cX, cY, halfDimension, point, level);
        }

        boolean insertSouthWest(double _cX, double _cY, double _halfDimension, Point point, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX - halfDimension;
            double cY = _cY + halfDimension;
            return this.southWest.insert(cX, cY, halfDimension, point, level);
        }

        boolean insertSouthEast(double _cX, double _cY, double _halfDimension, Point point, int level) {
            double halfDimension = _halfDimension / 2;
            double cX = _cX + halfDimension;
            double cY = _cY + halfDimension;
            return this.southEast.insert(cX, cY, halfDimension, point, level);
        }

        void subdivide() {
            this.northWest = new QuadTree();
            this.northEast = new QuadTree();
            this.southWest = new QuadTree();
            this.southEast = new QuadTree();
            nodesCount += 4;
        }

        class QEntry {
            int level;
            double ncX;
            double ncY;
            double nhalfDimension;
            QuadTree node;
            double benefit; // the benefit value if the take the best move

            QEntry(int _level, double _ncX, double _ncY, double _nhalfDimension, QuadTree _node, double _benefit) {
                level = _level;
                ncX = _ncX;
                ncY = _ncY;
                nhalfDimension = _nhalfDimension;
                node = _node;
                benefit = _benefit;
            }
        }

        /**
         * breadth first search
         *
         * explore nodes with higher estimated benefit first
         * - benefit = gain of quality / cost of sample size
         *
         * @param _ncX
         * @param _ncY
         * @param _nhalfDimension
         * @param _rcX
         * @param _rcY
         * @param _rhalfWidth
         * @param _rhalfHeight
         * @param _zoom - zoom level of current query
         * @param _targetSampleSize
         * @return
         */
        public List<Point> bfs(double _ncX, double _ncY, double _nhalfDimension,
                               double _rcX, double _rcY, double _rhalfWidth, double _rhalfHeight,
                               int _zoom, int _targetSampleSize) {

            List<Point> result = new ArrayList<>();

            // explore larger estimatedProfit node first
            PriorityQueue<QEntry> queue = new PriorityQueue<>(new Comparator<QEntry>() {
                @Override
                public int compare(QEntry o1, QEntry o2) {
                    if (o2.benefit > o1.benefit)
                        return 1;
                    else if (o2.benefit < o1.benefit)
                        return -1;
                    else
                        return 0;
                }
            });

            double rootBenefit = computeBenefit(_zoom, 0, this);
            QEntry rootEntry = new QEntry(0, _ncX, _ncY, _nhalfDimension, this, rootBenefit);
            // add root node
            queue.add(rootEntry);
            int availableSampleSize = _targetSampleSize - Constants.NODE_SAMPLE_SIZE;

            while (queue.size() > 0) {

                // pick the largest benefit node
                QEntry entry = queue.poll();
                int level = entry.level;
                double ncX = entry.ncX;
                double ncY = entry.ncY;
                double nhalfDimension = entry.nhalfDimension;
                QuadTree node = entry.node;
                double benefit = entry.benefit;
                int sampleSize = node.sample == null? 0: Constants.NODE_SAMPLE_SIZE;

                // if the largest estimated benefit is 0 or enough samples, entering collecting samples mode
                if (benefit <= 0.0 || availableSampleSize <= 0) {
                    //-DEBUG-//
//                    System.out.println("[queue] level = " + level);
//                    System.out.println("[queue] benefit = " + benefit);
//                    System.out.println("[queue] sample size = " + sampleSize);
                    //-DEBUG-//
                    if (node.sample != null) {
                        numberOfNodesStoppedAtLevels[level] ++;
                        result.add(node.sample);
                    }
                    continue;
                }

                // otherwise, expand this node
                double cX, cY;
                double halfDimension = nhalfDimension / 2;
                availableSampleSize += sampleSize;

                // northwest
                cX = ncX - halfDimension;
                cY = ncY - halfDimension;
                // ignore this node if the range does not intersect with it
                if (intersectsBBox(cX, cY, halfDimension, _rcX, _rcY, _rhalfWidth, _rhalfHeight)) {
                    double benefitNW = computeBenefit(_zoom, level + 1, node.northWest);
                    QEntry entryNW = new QEntry(level + 1, cX, cY, halfDimension, node.northWest, benefitNW);
                    queue.add(entryNW);
                    if (node.northWest.sample != null) {
                        availableSampleSize -= Constants.NODE_SAMPLE_SIZE;
                    }
                }

                // northeast
                cX = ncX + halfDimension;
                cY = ncY - halfDimension;
                // ignore this node if the range does not intersect with it
                if (intersectsBBox(cX, cY, halfDimension, _rcX, _rcY, _rhalfWidth, _rhalfHeight)) {
                    double benefitNE = computeBenefit(_zoom, level + 1, node.northEast);
                    QEntry entryNE = new QEntry(level + 1, cX, cY, halfDimension, node.northEast, benefitNE);
                    queue.add(entryNE);
                    if (node.northEast.sample != null) {
                        availableSampleSize -= Constants.NODE_SAMPLE_SIZE;
                    }
                }

                // southwest
                cX = ncX - halfDimension;
                cY = ncY + halfDimension;
                // ignore this node if the range does not intersect with it
                if (intersectsBBox(cX, cY, halfDimension, _rcX, _rcY, _rhalfWidth, _rhalfHeight)) {
                    double benefitSW = computeBenefit(_zoom, level + 1, node.southWest);
                    QEntry entrySW = new QEntry(level + 1, cX, cY, halfDimension, node.southWest, benefitSW);
                    queue.add(entrySW);
                    if (node.southWest.sample != null) {
                        availableSampleSize -= Constants.NODE_SAMPLE_SIZE;
                    }
                }

                // southeast
                cX = ncX + halfDimension;
                cY = ncY + halfDimension;
                // ignore this node if the range does not intersect with it
                if (intersectsBBox(cX, cY, halfDimension, _rcX, _rcY, _rhalfWidth, _rhalfHeight)) {
                    double benefitSE = computeBenefit(_zoom, level + 1, node.southEast);
                    QEntry entrySE = new QEntry(level + 1, cX, cY, halfDimension, node.southEast, benefitSE);
                    queue.add(entrySE);
                    if (node.southEast.sample != null) {
                        availableSampleSize -= Constants.NODE_SAMPLE_SIZE;
                    }
                }
            }

            //-DEBUG-//
            System.out.println("[availableSampleSize] = " + availableSampleSize);

            return result;
        }

        /**
         * Post-order traverse the Quadtree,
         * select the best sample for each node
         *
         * V1 - select the best from only its 4 children
         *    - store errors between sample on node and samples on children for all resolutions
         */
        public void selectSamples(double _cX, double _cY, double _halfDimension, int _level) {
            // leaf node already has the best sample
            if (this.northWest == null) {
                return;
            }

            double halfDimension = _halfDimension / 2;

            // select best samples for all four children first
            this.northWest.selectSamples(_cX - halfDimension, _cY - halfDimension, halfDimension, _level + 1);
            this.northEast.selectSamples(_cX + halfDimension, _cY - halfDimension, halfDimension, _level + 1);
            this.southWest.selectSamples(_cX - halfDimension, _cY + halfDimension, halfDimension, _level + 1);
            this.southEast.selectSamples(_cX + halfDimension, _cY + halfDimension, halfDimension, _level + 1);

            // render the four best samples on four children as the ground truth
            byte[] rendering0 = renderer.createRendering(Constants.NODE_RESOLUTION);
            if (this.northWest.sample != null) {
                renderer.render(rendering0, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.northWest.sample);
            }
            if (this.northEast.sample != null) {
                renderer.render(rendering0, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.northEast.sample);
            }
            if (this.southWest.sample != null) {
                renderer.render(rendering0, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.southWest.sample);
            }
            if (this.southEast.sample != null) {
                renderer.render(rendering0, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.southEast.sample);
            }

            // render each candidate of the four children individually and select the minimum error one
            double minError = Double.MAX_VALUE;
            Point bestSample = null;
            if (this.northWest.sample != null) {
                byte[] renderingNW = renderer.createRendering(Constants.NODE_RESOLUTION);
                renderer.render(renderingNW, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.northWest.sample);
                double error = errorMetric.error(rendering0, renderingNW, renderer.realResolution(Constants.NODE_RESOLUTION));
                if (error < minError) {
                    minError = error;
                    bestSample = this.northWest.sample;
                }
            }
            if (this.northEast.sample != null) {
                byte[] renderingNE = renderer.createRendering(Constants.NODE_RESOLUTION);
                renderer.render(renderingNE, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.northEast.sample);
                double error = errorMetric.error(rendering0, renderingNE, renderer.realResolution(Constants.NODE_RESOLUTION));
                if (error < minError) {
                    minError = error;
                    bestSample = this.northEast.sample;
                }
            }
            if (this.southWest.sample != null) {
                byte[] renderingSW = renderer.createRendering(Constants.NODE_RESOLUTION);
                renderer.render(renderingSW, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.southWest.sample);
                double error = errorMetric.error(rendering0, renderingSW, renderer.realResolution(Constants.NODE_RESOLUTION));
                if (error < minError) {
                    minError = error;
                    bestSample = this.southWest.sample;
                }
            }
            if (this.southEast.sample != null) {
                byte[] renderingSE = renderer.createRendering(Constants.NODE_RESOLUTION);
                renderer.render(renderingSE, _cX, _cY, _halfDimension, Constants.NODE_RESOLUTION, this.southEast.sample);
                double error = errorMetric.error(rendering0, renderingSE, renderer.realResolution(Constants.NODE_RESOLUTION));
                if (error < minError) {
                    minError = error;
                    bestSample = this.southEast.sample;
                }
            }
            // best sample stored on this node
            this.sample = bestSample;

            // for all zoom levels (resolutions),
            // compute and store the errors between best sample and all four children's best samples
            for (int zoom = 0; zoom <= Constants.MAX_ZOOM; zoom ++) {
                double pixelScale = 1.0 / 256 / Math.pow(2, zoom);
                this.errors[zoom] = computeErrorAgainstChildren(this, _cX, _cY, _halfDimension, pixelScale);
            }
        }


    }

    public static double highestLevelNodeDimension;

    public static IRenderer renderer;

    public static IErrorMetric errorMetric;

    QuadTree quadTree;
    int totalNumberOfPoints = 0;
    int totalStoredNumberOfPoints = 0;
    static long nodesCount = 0; // count quad-tree nodes
    boolean finish = false; // loading data finish flag

    /** For query stats */
    static int[] numberOfNodesStoppedAtLevels; // for current query, count how many nodes stopped at a certain level
    static int computeBenefitTimes; // for current query, count how many times compute the benefit

    /** For query time analysis */
    static Map<String, Double> times; // for current query, store times for different parts

    //-Timing-//
    static final boolean keepTiming = true;
    Map<String, Double> timing;
    //-Timing-//

    public RAQuadTree() {
        this.quadTree = new QuadTree();

        // zoom level 0 is fixed with dimension 1.0 / 256 (because one tile of the base map is 256px x 256px)
        highestLevelNodeDimension = 1.0 / 256 / Math.pow(2, Constants.MAX_ZOOM);

        switch (Constants.RENDERING_FUNCTION.toLowerCase()) {
            case "deckgl":
                System.out.println("[RA-QuadTree] rendering function = Deck.GL");
                renderer =  new DeckGLRenderer(Constants.RADIUS_IN_PIXELS);
                switch (Constants.ERROR_FUNCTION.toLowerCase()) {
                    case "l2":
                        System.out.println("[RA-QuadTree] error function = L2");
                        errorMetric = new L2Error();
                        break;
                    case "l1":
                    default:
                        System.out.println("[RA-QuadTree] error function = L1");
                        errorMetric = new L1Error();
                }
                break;
            case "snap":
            default:
                System.out.println("[RA-QuadTree] rendering function = Snap");
                renderer = new SnapRenderer();
                switch (Constants.ERROR_FUNCTION.toLowerCase()) {
                    case "l2":
                        System.out.println("[RA-QuadTree] error function = Snap L2");
                        errorMetric = new SnapL2Error();
                        break;
                    case "l1":
                    default:
                        System.out.println("[RA-QuadTree] error function = Snap L1");
                        errorMetric = new SnapL1Error();
                }
        }

        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("total", 0.0);
        }

        /** For query stats */
        numberOfNodesStoppedAtLevels = new int[Constants.MAX_ZOOM + 9 + 1];

        /** For query time analysis */
        times = new HashMap<>();

        MyMemory.printMemory();
    }

    public boolean readFromFile(String fileName) {
        System.out.println("[RA-QuadTree] read from file " + fileName + " ... ...");

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
            //--time--//
            long startTime = System.nanoTime();
            this.quadTree = quadTree.readFromFile(bufferedReader, 0.5, 0.5, 0.5, 0);
            bufferedReader.close();
            //--time--//
            long endTime = System.nanoTime();
            System.out.println("[RA-QuadTree] read from file " + fileName + " done! Time: " + ((double) (endTime - startTime) / 1000000000.0) + " seconds.");
            finish = true;
            return true;
        } catch (IOException e) {
            System.out.println("[RA-QuadTree] read from file " + fileName + " failed!");
            e.printStackTrace();
        }
        return false;
    }

    public boolean writeToFile(String fileName) {
        System.out.println("[RA-QuadTree] write to file " + fileName + " ... ...");

        try {
            File file = new File(fileName);
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(fileOutputStream));
            //--time--//
            long startTime = System.nanoTime();
            quadTree.writeToFile(bufferedWriter, 0.5, 0.5, 0.5, 0);
            bufferedWriter.close();
            fileOutputStream.close();
            //--time--//
            long endTime = System.nanoTime();
            System.out.println("[RA-QuadTree] write to file " + fileName + " done! Time: " + ((double) (endTime - startTime) / 1000000000.0) + " seconds.");
            return true;
        }
        catch (IOException e) {
            System.out.println("[RA-QuadTree] write to file " + fileName + " failed!");
            e.printStackTrace();
        }

        return false;
    }

    public void load(List<Point> points) {
        System.out.println("[RA-QuadTree] loading " + points.size() + " points ... ...");

        MyTimer.startTimer();
        this.totalNumberOfPoints += points.size();
        int count = 0;
        int skip = 0;
        MyTimer.startTimer();
        for (Point point: points) {
            if (this.quadTree.insert(0.5, 0.5, 0.5, lngLatToXY(point), 0))
                count ++;
            else
                skip ++;
        }
        MyTimer.stopTimer();
        double insertTime = MyTimer.durationSeconds();
        this.totalStoredNumberOfPoints += count;
        System.out.println("[RA-QuadTree] inserted " + count + " points and skipped " + skip + " points.");
        System.out.println("[RA-QuadTree] insertion time: " + insertTime + " seconds.");

        MyTimer.stopTimer();
        double loadTime = MyTimer.durationSeconds();

        if (keepTiming) timing.put("total", timing.get("total") + loadTime);
        System.out.println("[RA-QuadTree] loading is done!");
        System.out.println("[RA-QuadTree] loading time: " + loadTime + " seconds.");
        if (keepTiming) this.printTiming();

        MyMemory.printMemory();

        //-DEBUG-//
        System.out.println("==== Until now ====");
        System.out.println("RA-QuadTree has processed " + this.totalNumberOfPoints + " points.");
        System.out.println("RA-QuadTree has stored " + this.totalStoredNumberOfPoints + " points.");
        System.out.println("RA-QuadTree has skipped " + (this.totalNumberOfPoints - this.totalStoredNumberOfPoints) + " points.");
        System.out.println("RA-QuadTree has generated " + nodesCount + " nodes.");
        //-DEBUG-//
    }

    @Override
    public void finishLoad() {
        this.finish = true;
        // select best sample for each node in the QuadTree
        MyTimer.startTimer();
        this.quadTree.selectSamples(0.5, 0.5, 0.5, 0);
        MyTimer.stopTimer();
        double selectSamplesTime = MyTimer.durationSeconds();
        System.out.println("==== Data loading finished ====");
        System.out.println("[RA-QuadTree] select best sample for each node is done!");
        System.out.println("[RA-QuadTree] sample selection time: " + selectSamplesTime + " seconds.");
    }

    public static double computeErrorAgainstChildren(QuadTree _node, double _ncX, double _ncY, double _nhalfDimension,
                                                     double _rPixelScale) {
        // if already leaf, benefit is 0.0, no need to expand it
        if (_node.northWest == null) return 0.0;

        // get the resolution for given _node as piece of the result
        int resolution = (int) Math.round(2 * _nhalfDimension / _rPixelScale);

        // TODO - verify for DeckGLRenderer
        if (resolution == 0) return 0.0;

        double error;

        if (resolution > 4 * Constants.NODE_SAMPLE_SIZE) {
            // render the point on node
            // for pixel list rendering, background is always an empty list
            List<Pixel> rendering1 = new ArrayList<>();
            if (_node.sample != null) {
                renderer.render(rendering1, _ncX, _ncY, _nhalfDimension, resolution, _node.sample);
            }
            // render the 4 children points
            // for pixel list rendering, background is always an empty list
            List<Pixel> rendering2 = new ArrayList<>();
            if (_node.northWest.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.northWest.sample);
            }
            if (_node.northEast.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.northEast.sample);
            }
            if (_node.southWest.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.southWest.sample);
            }
            if (_node.southEast.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.southEast.sample);
            }
            error = errorMetric.error(rendering1, rendering2, renderer.realResolution(resolution));
        }
        // otherwise, use byte array rendering
        else {
            // render the point on node
            byte[] rendering1 = renderer.createRendering(resolution);
            if (_node.sample != null) {
                renderer.render(rendering1, _ncX, _ncY, _nhalfDimension, resolution, _node.sample);
            }
            // render the 4 children points
            byte[] rendering2 = renderer.createRendering(resolution);
            if (_node.northWest.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.northWest.sample);
            }
            if (_node.northEast.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.northEast.sample);
            }
            if (_node.southWest.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.southWest.sample);
            }
            if (_node.southEast.sample != null) {
                renderer.render(rendering2, _ncX, _ncY, _nhalfDimension, resolution, _node.southEast.sample);
            }
            error = errorMetric.error(rendering1, rendering2, renderer.realResolution(resolution));
        }
        return error;
    }

    public static double computeBenefit(int _zoom, int _level, QuadTree _node) {
        computeBenefitTimes ++;

        //--time--//
        long startTime = System.nanoTime();

        // for leaf node, it can not be expanded at all.
        if (_node.northWest == null) return 0.0;

        // for levels < zoom level 0 (2^8 = 256, zoom level 0 has 256px resolution), always expand.
        if (_level < 8) return Double.MAX_VALUE;

        double error = _node.errors[_zoom];

        double gain = error * Math.log(_node.count);
        int sampleSize = (_node.sample == null? 0: 1);
        int sampleSizeOfChildren = 0;
        sampleSizeOfChildren += (_node.northWest.sample == null? 0: 1);
        sampleSizeOfChildren += (_node.northEast.sample == null? 0: 1);
        sampleSizeOfChildren += (_node.southWest.sample == null? 0: 1);
        sampleSizeOfChildren += (_node.southEast.sample == null? 0: 1);
        int cost = sampleSizeOfChildren - sampleSize;

        //--time--//
        long endTime = System.nanoTime();
        times.put("computeBenefit", times.get("computeBenefit") + ((double) (endTime - startTime) / 1000000000.0));

        if (cost == 0) {
            return Double.MAX_VALUE;
        }
        else {
            return gain / (double) cost;
        }
    }

    public byte[] answerQuery(Query query) {

        if (!this.finish) {
            // System.out.println("[RA-QuadTree] has not finished loading data, will not answer this query!");
            // MyTimer.temporaryTimer.put("treeTime", 0.0);
            // MyTimer.temporaryTimer.put("aggregateTime", 0.0);
            // BinaryMessageBuilder messageBuilder = new BinaryMessageBuilder();
            // double lng = xLng(0.5);
            // double lat = yLat(0.5);
            // messageBuilder.add(lng, lat);
            // return messageBuilder.getBuffer();
            System.out.println("[RA-QuadTree] has not finished loading data, select samples temporarily for progressive results!");
            MyTimer.startTimer();
            this.quadTree.selectSamples(0.5, 0.5, 0.5, 0);
            MyTimer.stopTimer();
            double selectSamplesTime = MyTimer.durationSeconds();
            System.out.println("[RA-QuadTree] sample selection time: " + selectSamplesTime + " seconds.");
        }

        double lng0 = query.bbox[0];
        double lat0 = query.bbox[1];
        double lng1 = query.bbox[2];
        double lat1 = query.bbox[3];
        int resX = query.resX;
        int resY = query.resY;
        int zoom = query.zoom;
        int sampleSize = query.sampleSize <= 0? Constants.DEFAULT_SAMPLE_SIZE: query.sampleSize;

        MyTimer.startTimer();
        System.out.println("[RA-QuadTree] is answering query: \n" +
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

        System.out.println("[RA-QuadTree] starting range search on QuadTree with: \n" +
                "bbox = [(" + iX0 + ", " + iY0 + "), (" + iX1 + ", " + iY1 + ")] ; \n" +
                "range = [(" + rcX + ", " + rcY + "), " + rhalfWidth + ", " + rhalfHeight + "] ; \n" +
                "pixelScale = " + pixelScale + ";");

        /** For query stats*/
        for (int i = 0; i <= Constants.MAX_ZOOM + 9; i ++) numberOfNodesStoppedAtLevels[i] = 0;
        computeBenefitTimes = 0;

        /** For query time analysis */
        times.put("computeBenefit", 0.0);

        MyTimer.startTimer();
        System.out.println("[RA-QuadTree] is doing a best first search with sampleSize = " + sampleSize + ".");
        List<Point> points = this.quadTree.bfs(0.5, 0.5, 0.5,
                rcX, rcY, rhalfWidth, rhalfHeight, zoom, sampleSize);
        MyTimer.stopTimer();
        double treeTime = MyTimer.durationSeconds();

        MyTimer.temporaryTimer.put("treeTime", treeTime);
        System.out.println("[RA-QuadTree] tree search got " + points.size() + " data points.");
        System.out.println("[RA-QuadTree] tree search time: " + treeTime + " seconds.");
        System.out.println("[RA-QuadTree]     - compute benefit time: " + times.get("computeBenefit") + " seconds.");
        System.out.println("[RA-QuadTree]     - compute benefit was called: " + computeBenefitTimes + " times.");

        // build binary result message
        MyTimer.startTimer();
        BinaryMessageBuilder messageBuilder = new BinaryMessageBuilder();
        double lng, lat;
        int resultSize = 0;
        for (Point point : points) {
            lng = xLng(point.getX());
            lat = yLat(point.getY());
            messageBuilder.add(lng, lat);
            resultSize++;
        }
        MyTimer.stopTimer();
        double buildBinaryTime = MyTimer.durationSeconds();
        MyTimer.temporaryTimer.put("aggregateTime", buildBinaryTime);

        System.out.println("[RA-QuadTree] build binary result with  " + resultSize + " points.");
        System.out.println("[RA-QuadTree] build binary result time: " + buildBinaryTime + " seconds.");

        MyTimer.stopTimer();
        System.out.println("[RA-QuadTree] answer query total time: " + MyTimer.durationSeconds() + " seconds.");
        System.out.println("[RA-QuadTree] ---- # of nodes stopping at each level ----");
        for (int i = 0; i <= Constants.MAX_ZOOM + 9; i ++) {
            System.out.println("Level " + i + ": " + numberOfNodesStoppedAtLevels[i]);
        }

        return messageBuilder.getBuffer();
    }

    private void printTiming() {
        System.out.println("[Total Time] " + timing.get("total") + " seconds.");
    }

    public static void printRenderingGray(String name, byte[] _rendering, int _resolution, boolean _expansion) {
        int side = _expansion? _resolution + 2 * (Constants.RADIUS_IN_PIXELS + 1): _resolution;
        System.out.println("========== " + name + "==========");
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                int r = UnsignedByte.toInt(_rendering[i * side * 3 + j * 3 + 0]);
                int g = UnsignedByte.toInt(_rendering[i * side * 3 + j * 3 + 1]);
                int b = UnsignedByte.toInt(_rendering[i * side * 3 + j * 3 + 2]);
                // gray scaling formula = (0.3 * R) + (0.59 * G) + (0.11 * B)
                int gray = (int) ((0.3 * r) + (0.59 * g) + (0.11 * b));
                if (j > 0) System.out.print(" ");
                System.out.print(gray);
            }
            System.out.println();
        }
    }
}
