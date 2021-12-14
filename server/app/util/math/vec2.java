package util.math;

public class vec2 {
    public double x;
    public double y;

    public vec2(double _x, double _y) {
        x = _x;
        y = _y;
    }

    public static vec2 multiply(vec2 l, double r) {
        l.x *= r;
        l.y *= r;
        return l;
    }

    public static double length(vec2 _xy) {
        return Math.sqrt(Math.pow(_xy.x, 2) + Math.pow(_xy.y, 2));
    }
}
