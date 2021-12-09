package util;

import model.I2DPoint;
import model.Point;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class OptKDTree<PointType extends I2DPoint> implements I2DIndex<PointType> {

    class Node extends Point {
        public short duplicates;
        public boolean align; // true-x, false-y
        public int depth;
        public Node left = null;
        public Node right = null;

        public Node(PointType point, boolean align, int depth) {
            this.x = point.getX();
            this.y = point.getY();
            this.align = align;
            this.depth = depth;
            this.duplicates = 0;
        }
    }

    private Node root;
    private int height = 0;
    private int size = 0;

    public OptKDTree() {
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
            // duplicate
            if (currentNode.equalsTo(point)) {
                currentNode.duplicates ++;
                return;
            }
            else {
                // check x
                if (align) {
                    if (point.getX() < currentNode.getX()) {
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
                    if (point.getY() < currentNode.getY()) {
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
        return;
    }

    public List<PointType> range(I2DPoint leftBottom, I2DPoint rightTop) {
        return null;
    }

    public List<PointType> within(I2DPoint center, double radius) {
        return null;
    }

    public int range(I2DPoint leftBottom, I2DPoint rightTop, I2DIndexNodeHandler nodeHandler) {
        if (root == null) {
            return 0 ;
        }

        int counter = 0;

        Queue<Node> queue = new LinkedList<>();
        queue.add(root);
        while (queue.size() > 0) {
            Node currentNode = queue.poll();
            boolean align = currentNode.align;
            // if current node within range, call nodeHandler to handle it, and put both children to queue
            if (currentNode.rightAbove(leftBottom) && currentNode.leftBelow(rightTop)) {

                nodeHandler.handleNode(currentNode.getX(), currentNode.getY(), currentNode.duplicates);
                counter ++;

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
                    if (rightTop.getX() < currentNode.getX()) {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                    }
                    // currentNode is to the left of left edge of rectangle, only check right child
                    else if (leftBottom.getX() > currentNode.getX()) {
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
                    if (rightTop.getY() < currentNode.getY()) {
                        if (currentNode.left != null) {
                            queue.add(currentNode.left);
                        }
                    }
                    // currentNode is below the bottom edge of rectangle, only check right child
                    else if (leftBottom.getY() > currentNode.getY()) {
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
        return counter;
    }

    public void print() {
        System.out.println("=================== KDTree ===================");
        System.out.println();
    }

    public int size() {
        return this.size;
    }
}
