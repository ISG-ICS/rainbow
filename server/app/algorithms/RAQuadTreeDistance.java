package algorithms;

import model.Point;
import model.Query;
import util.*;

import java.io.*;
import java.util.*;

import static util.Mercator.*;

/**
 * RA-QuadTree algorithm
 *   Special implementation for average euclidean distance error metric
 */
public class RAQuadTreeDistance implements IAlgorithm {

    public class QuadTree {
        public Point sample; // always centroid for this special error metric
        public int count; // count of subtree
        public double error; // average distance between this centroid and the four children

        public QuadTree() {
            this.sample = null;
            this.count = 0;
            this.error = 0.0;
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
            if (this.sample != null) {
                bufferedWriter.write(String.valueOf(this.sample.getX()));
                bufferedWriter.write(",");
                bufferedWriter.write(String.valueOf(this.sample.getY()));
            }
            else {
                bufferedWriter.write(",");
            }
            bufferedWriter.write(",");
            bufferedWriter.write(String.valueOf(this.error));
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
                if (attributes[i].isEmpty()) {
                    node.sample = null;
                    i += 2;
                } else {
                    double x = Double.valueOf(attributes[i++]);
                    double y = Double.valueOf(attributes[i++]);
                    node.sample = new Point(x, y);
                }

                node.error = Double.valueOf(attributes[i++]);

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
         * compute a centroid as the best fake sample from its 4 children
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

            // sum the four centroids weighted coordinates (count is the weight) from four children
            double sumX = 0.0;
            double sumY = 0.0;
            double sumCount = 0.0;
            if (this.northWest.sample != null) {
                sumX += this.northWest.count * this.northWest.sample.getX();
                sumY += this.northWest.count * this.northWest.sample.getY();
                sumCount += this.northWest.count;
            }
            if (this.northEast.sample != null) {
                sumX += this.northEast.count * this.northEast.sample.getX();
                sumY += this.northEast.count * this.northEast.sample.getY();
                sumCount += this.northEast.count;
            }
            if (this.southWest.sample != null) {
                sumX += this.southWest.count * this.southWest.sample.getX();
                sumY += this.southWest.count * this.southWest.sample.getY();
                sumCount += this.southWest.count;
            }
            if (this.southEast.sample != null) {
                sumX += this.southEast.count * this.southEast.sample.getX();
                sumY += this.southEast.count * this.southEast.sample.getY();
                sumCount += this.southEast.count;
            }

            // best sample stored on this node is the centroid of the 4 children
            this.sample = new Point(sumX / sumCount, sumY / sumCount);

            // sum the weighted distances between this centroid and the four children
            double sumDistance = 0.0;
            if (this.northWest.sample != null) {
                sumDistance += this.northWest.count * this.northWest.sample.distanceTo(this.sample);
            }
            if (this.northEast.sample != null) {
                sumDistance += this.northEast.count * this.northEast.sample.distanceTo(this.sample);
            }
            if (this.southWest.sample != null) {
                sumDistance += this.southWest.count * this.southWest.sample.distanceTo(this.sample);
            }
            if (this.southEast.sample != null) {
                sumDistance += this.southEast.count * this.southEast.sample.distanceTo(this.sample);
            }

            // error of this node is the average weighted distance between this centroid and the four children
            this.error = sumDistance / sumCount;
        }

    }

    public static double highestLevelNodeDimension;

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

    public RAQuadTreeDistance() {
        this.quadTree = new QuadTree();

        // zoom level 0 is fixed with dimension 1.0 / 256 (because one tile of the base map is 256px x 256px)
        highestLevelNodeDimension = 1.0 / 256 / Math.pow(2, Constants.MAX_ZOOM);

        System.out.println("[RA-QuadTree-Distance] rendering function = NULL");
        System.out.println("[RA-QuadTree-Distance] error function = AVG Euclidean Distance");

        // initialize the timing map
        if (keepTiming) {
            timing = new HashMap<>();
            timing.put("insert", 0.0);
            timing.put("selectSamples", 0.0);
            timing.put("writeToFile", 0.0);
        }

        /** For query stats */
        numberOfNodesStoppedAtLevels = new int[Constants.MAX_ZOOM + 9 + 1];

        /** For query time analysis */
        times = new HashMap<>();

        MyMemory.printMemory();
    }

    public boolean readFromFile(String fileName) {
        System.out.println("[RA-QuadTree-Distance] read from file " + fileName + " ... ...");

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(fileName));
            //--time--//
            long startTime = System.nanoTime();
            this.quadTree = quadTree.readFromFile(bufferedReader, 0.5, 0.5, 0.5, 0);
            bufferedReader.close();
            //--time--//
            long endTime = System.nanoTime();
            System.out.println("[RA-QuadTree-Distance] read from file " + fileName + " done! Time: " + ((double) (endTime - startTime) / 1000000000.0) + " seconds.");
            finish = true;
            System.gc();
            System.runFinalization();
            MyMemory.printMemory();
            return true;
        } catch (IOException e) {
            System.out.println("[RA-QuadTree-Distance] read from file " + fileName + " failed!");
            e.printStackTrace();
        }
        return false;
    }

    public boolean writeToFile(String fileName) {
        System.out.println("[RA-QuadTree-Distance] write to file " + fileName + " ... ...");

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
            double writeToFileTime = (double) (endTime - startTime) / 1000000000.0;
            if (keepTiming) timing.put("writeToFile", writeToFileTime);
            System.out.println("[RA-QuadTree-Distance] write to file " + fileName + " done! Time: " + writeToFileTime + " seconds.");
            System.out.println("==== Data writing to file finished ====");
            this.printTiming();
            System.gc();
            System.runFinalization();
            MyMemory.printMemory();
            return true;
        }
        catch (IOException e) {
            System.out.println("[RA-QuadTree-Distance] write to file " + fileName + " failed!");
            e.printStackTrace();
        }

        return false;
    }

    public void load(List<Point> points) {
        System.out.println("[RA-QuadTree-Distance] loading " + points.size() + " points ... ...");

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
        System.out.println("[RA-QuadTree-Distance] inserted " + count + " points and skipped " + skip + " points.");
        System.out.println("[RA-QuadTree-Distance] insertion time: " + insertTime + " seconds.");
        if (keepTiming) timing.put("insert", timing.get("insert") + insertTime);
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
        if (keepTiming) timing.put("selectSamples", selectSamplesTime);
        System.out.println("[RA-QuadTree-Distance] select best sample for each node is done!");
        System.out.println("[RA-QuadTree-Distance] sample selection time: " + selectSamplesTime + " seconds.");
        System.out.println("==== Data loading finished ====");
        this.printTiming();
        MyMemory.printMemory();
    }

    public static double computeBenefit(int _zoom, int _level, QuadTree _node) {
        computeBenefitTimes ++;

        //--time--//
        long startTime = System.nanoTime();

        // for leaf node, it can not be expanded at all.
        if (_node.northWest == null) return 0.0;

        // for levels < zoom level 0 (2^8 = 256, zoom level 0 has 256px resolution), always expand.
        if (_level < 8) return Double.MAX_VALUE;

        // for nodes with resolution larger than query resolution, always yield other nodes
        if (_level >= _zoom + 8) return 0.0;

        // resolution of this node represent in query
        int resolution = (int) Math.max(Math.pow(2, _zoom + 8 - _level), 1);
        // current error = average distance of this node against 4 children x number of pixels this node has
        double error = _node.error * resolution * resolution;

        // resolution of child node represent in query
        int childResolution = (int) Math.max(resolution / 2, 1);
        // total error of children
        double sumErrorChildren = _node.northWest.error + _node.northEast.error + _node.southWest.error + _node.southEast.error;
        sumErrorChildren = sumErrorChildren * childResolution * childResolution;

        double gain = error - (sumErrorChildren / 4.0);
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
            // System.out.println("[RA-QuadTree-Distance] has not finished loading data, will not answer this query!");
            // MyTimer.temporaryTimer.put("treeTime", 0.0);
            // MyTimer.temporaryTimer.put("aggregateTime", 0.0);
            // BinaryMessageBuilder messageBuilder = new BinaryMessageBuilder();
            // double lng = xLng(0.5);
            // double lat = yLat(0.5);
            // messageBuilder.add(lng, lat);
            // return messageBuilder.getBuffer();
            System.out.println("[RA-QuadTree-Distance] has not finished loading data, select samples temporarily for progressive results!");
            MyTimer.startTimer();
            this.quadTree.selectSamples(0.5, 0.5, 0.5, 0);
            MyTimer.stopTimer();
            double selectSamplesTime = MyTimer.durationSeconds();
            System.out.println("[RA-QuadTree-Distance] sample selection time: " + selectSamplesTime + " seconds.");
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
        System.out.println("[RA-QuadTree-Distance] is answering query: \n" +
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

        System.out.println("[RA-QuadTree-Distance] starting range search on QuadTree with: \n" +
                "bbox = [(" + iX0 + ", " + iY0 + "), (" + iX1 + ", " + iY1 + ")] ; \n" +
                "range = [(" + rcX + ", " + rcY + "), " + rhalfWidth + ", " + rhalfHeight + "] ; \n" +
                "pixelScale = " + pixelScale + ";");

        /** For query stats*/
        for (int i = 0; i <= Constants.MAX_ZOOM + 9; i ++) numberOfNodesStoppedAtLevels[i] = 0;
        computeBenefitTimes = 0;

        /** For query time analysis */
        times.put("computeBenefit", 0.0);

        MyTimer.startTimer();
        System.out.println("[RA-QuadTree-Distance] is doing a best first search with sampleSize = " + sampleSize + ".");
        List<Point> points = this.quadTree.bfs(0.5, 0.5, 0.5,
                rcX, rcY, rhalfWidth, rhalfHeight, zoom, sampleSize);
        MyTimer.stopTimer();
        double treeTime = MyTimer.durationSeconds();

        MyTimer.temporaryTimer.put("treeTime", treeTime);
        System.out.println("[RA-QuadTree-Distance] tree search got " + points.size() + " data points.");
        System.out.println("[RA-QuadTree-Distance] tree search time: " + treeTime + " seconds.");
        System.out.println("[RA-QuadTree-Distance]     - compute benefit time: " + times.get("computeBenefit") + " seconds.");
        System.out.println("[RA-QuadTree-Distance]     - compute benefit was called: " + computeBenefitTimes + " times.");

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

        System.out.println("[RA-QuadTree-Distance] build binary result with  " + resultSize + " points.");
        System.out.println("[RA-QuadTree-Distance] build binary result time: " + buildBinaryTime + " seconds.");

        MyTimer.stopTimer();
        System.out.println("[RA-QuadTree-Distance] answer query total time: " + MyTimer.durationSeconds() + " seconds.");
        System.out.println("[RA-QuadTree-Distance] ---- # of nodes stopping at each level ----");
        for (int i = 0; i <= Constants.MAX_ZOOM + 9; i ++) {
            System.out.println("Level " + i + ": " + numberOfNodesStoppedAtLevels[i]);
        }

        return messageBuilder.getBuffer();
    }

    private void printTiming() {
        System.out.println("========== Building RA-QuadTree-Distance Timings ==========");
        System.out.println("insert time,    selectSamples time,    writeToFile time");
        System.out.println(timing.get("insert") + ",    " + timing.get("selectSamples") + ",    " + timing.get("writeToFile"));
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
