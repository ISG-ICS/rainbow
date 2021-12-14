package util;

import model.I2DPoint;

import java.util.*;

public class KDTree<PointType extends I2DPoint> implements I2DIndex<PointType> {

    class Node {
        private PointType point;
        private List<PointType> duplicates;
        boolean align; // true-x, false-y
        int depth;
        Node left = null;
        Node right = null;
        boolean deleted = false;

        public Node(PointType point, boolean align, int depth) {
            this.point = point;
            this.align = align;
            this.depth = depth;
            this.duplicates = new LinkedList<>();
        }

        public PointType getPoint() {
            return this.point;
        }

        public List<PointType> getDuplicates() {
            return this.duplicates;
        }

        public void addDuplicate(PointType point) {
            this.duplicates.add(point);
        }
    }

    private Node root;
    private int height = 0;
    private int size = 0;

    public KDTree() {
        this.root = null;
    }

    public void insert(PointType point) {
        size ++;

        // empty tree
        if (root == null) {
            root = new Node(point, true, 0);
            height = 1;
            return;
        }

        // root always align with x
        boolean align = true;
        Node currentNode = root;
        Node parentNode = currentNode;
        boolean left = true;
        // find the position to insert
        while (currentNode != null) {
            I2DPoint currentPoint = currentNode.getPoint();
            // duplicate
            if (currentPoint.equalsTo(point)) {
                if (currentNode.deleted) {
                    currentNode.deleted = false;
                    currentNode.point = point;
                }
                else {
                    currentNode.addDuplicate(point);
                }
                return;
            }
            else {
                // check x
                if (align) {
                    if (point.getX() < currentPoint.getX()) {
                        parentNode = currentNode;
                        currentNode = currentNode.left;
                        left = true;
                    } else {
                        parentNode = currentNode;
                        currentNode = currentNode.right;
                        left = false;
                    }
                }
                // check y
                else {
                    if (point.getY() < currentPoint.getY()) {
                        parentNode = currentNode;
                        currentNode = currentNode.left;
                        left = true;
                    } else {
                        parentNode = currentNode;
                        currentNode = currentNode.right;
                        left = false;
                    }
                }
            }
            align = !align;
        }
        // parentNode clusters to the parent of new node
        currentNode = new Node(point, align, parentNode.depth + 1);
        if (currentNode.depth + 1 > height) {
            height = currentNode.depth + 1;
        }
        if (left) {
            parentNode.left = currentNode;
        }
        else {
            parentNode.right = currentNode;
        }
    }

    public void load(PointType[] points) {
        for (int i = 0; i < points.length; i ++) {
            this.insert(points[i]);
        }
    }

    public void delete(PointType point) {
        // root always align with x
        boolean align = true;
        Node currentNode = root;

        // find the point
        while (currentNode != null) {
            PointType currentPoint = currentNode.getPoint();
            // hit
            if (currentPoint.equalsTo(point)) {
                // if still has duplicates, delete one from duplicates
                if (currentNode.duplicates.isEmpty()) {
                    currentNode.duplicates.remove(currentNode.duplicates.size() - 1);
                }
                // else make it a tombstone here
                else {
                    currentNode.deleted = true;
                }
                size --;
                return;
            }
            else {
                // check x
                if (align) {
                    if (point.getX() < currentPoint.getX()) {
                        currentNode = currentNode.left;
                    } else {
                        currentNode = currentNode.right;
                    }
                }
                // check y
                else {
                    if (point.getY() < currentPoint.getY()) {
                        currentNode = currentNode.left;
                    } else {
                        currentNode = currentNode.right;
                    }
                }
            }
            align = !align;
        }
        // didn't find the point
    }

    public List<PointType> within(I2DPoint center, double radius) {
        List<PointType> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            Node currentNode = queue.poll();
            boolean align = currentNode.align;
            PointType currentPoint = currentNode.getPoint();
            // if current node within range, put it into result, and put both children to queue
            if (currentPoint.distanceTo(center) <= radius) {
                if (!currentNode.deleted) {
                    result.add(currentPoint);
                }
                // also add duplicates inside current node
                for (PointType duplicate: currentNode.getDuplicates()) {
                    result.add(duplicate);
                }
                if (currentNode.left != null) {
                    queue.add(currentNode.left);
                }
                if (currentNode.right != null) {
                    queue.add(currentNode.right);
                }
            }
            // else current node outside range
            else {
                // check x
                if (align) {
                    // but if NOT (currentNode.x + r) < center.x, left child still needs to be checked
                    if (currentPoint.getX() + radius >= center.getX()) {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                    }
                    // but if NOT center.x < (currentNode.x - r), right child still needs to be checked
                    if (center.getX() >= currentPoint.getX() - radius) {
                        if (currentNode.right != null) {
                            queue.add(currentNode.right);
                        }
                    }
                }
                // check y
                else {
                    // but if NOT (currentNode.y + r) < center.y, left child still needs to be checked
                    if (currentPoint.getY() + radius >= center.getY()) {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                    }
                    // but if NOT center.y < (currentNode.y - r), right child still needs to be checked
                    if (center.getY() >= currentPoint.getY() - radius) {
                        if (currentNode.right != null) {
                            queue.add(currentNode.right);
                        }
                    }
                }
            }
        }
        return result;
    }

    public List<PointType> range(I2DPoint leftBottom, I2DPoint rightTop) {
        List<PointType> result = new ArrayList<>();
        if (root == null) {
            return result;
        }
        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            Node currentNode = queue.poll();
            boolean align = currentNode.align;
            PointType currentPoint = currentNode.getPoint();
            // if current node within range, put it into result, and put both children to queue
            if (currentPoint.rightAbove(leftBottom) && currentPoint.leftBelow(rightTop)) {
                if (!currentNode.deleted) {
                    result.add(currentPoint);
                }
                // also add duplicates inside current node
                for (PointType duplicate: currentNode.getDuplicates()) {
                    result.add(duplicate);
                }
                if (currentNode.left != null) {
                    queue.add(currentNode.left);
                }
                if (currentNode.right != null) {
                    queue.add(currentNode.right);
                }
            }
            // else current node outside range
            else {
                // check x
                if (align) {
                    // currentNode is to the right of right edge of rectangle, only check left child
                    if (rightTop.getX() < currentPoint.getX()) {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                    }
                    // currentNode is to the left of left edge of rectangle, only check right child
                    else if (leftBottom.getX() > currentPoint.getX()) {
                        if (currentNode.right != null) {
                            queue.add(currentNode.right);
                        }
                    }
                    // currentNode.x is between leftBottom and rightTop, both children need to be explored
                    else {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                        if (currentNode.right != null) {
                            queue.add(currentNode.right);
                        }
                    }
                }
                // check y
                else {
                    // currentNode is above the top edge of rectangle, only check left child
                    if (rightTop.getY() < currentPoint.getY()) {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                    }
                    // currentNode is below the bottom edge of rectangle, only check right child
                    else if (leftBottom.getY() > currentPoint.getY()) {
                        if (currentNode.right != null) {
                            queue.add(currentNode.right);
                        }
                    }
                    // currentNode.y is between leftBottom and rightTop, both children need to be explored
                    else {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                        if (currentNode.right != null) {
                            queue.add(currentNode.right);
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public int range(I2DPoint leftBottom, I2DPoint rightTop, I2DIndexNodeHandler nodeHandler) {
        return 0 ;
    }

    public void print() {
        System.out.println("=================== KDTree ===================");
        System.out.println();
    }

    public int size() {
        return this.size;
    }
}
