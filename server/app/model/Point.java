package model;

public class Point implements I2DPoint {
    protected double x;
    protected double y;

    public Point() {
        x = 0.0;
        y = 0.0;
    }

    public Point(double _x, double _y) {
        x = _x;
        y = _y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public void setX(double _x) {
        x = _x;
    }

    public void setY(double _y) {
        y = _y;
    }

    public boolean equalsTo(I2DPoint p2) {
        if (x == p2.getX() && y == p2.getY()) return true;
        else return false;
    }

    public double distanceTo(I2DPoint p2) {
        return Math.sqrt(Math.pow(x - p2.getX(), 2) + Math.pow(y - p2.getY(), 2));
    }


    public boolean leftBelow(I2DPoint p2) {
        if (x < p2.getX() && y < p2.getY()) return true;
        else return false;
    }

    public boolean rightAbove(I2DPoint p2) {
        if (x > p2.getX() && y > p2.getY()) return true;
        else return false;
    }

    public I2DPoint clone() {
        I2DPoint copy = new Point(x, y);
        return copy;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(x);
        sb.append(",");
        sb.append(y);
        sb.append(")");
        return sb.toString();
    }
}
