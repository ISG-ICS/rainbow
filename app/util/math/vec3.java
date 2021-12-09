package util.math;

public class vec3 {
    public double x;
    public double y;
    public double z;

    public vec3(double _x, double _y, double _z) {
        x = _x;
        y = _y;
        z = _z;
    }

    public vec3(vec2 _xy, double _z) {
        x = _xy.x;
        y = _xy.y;
        z = _z;
    }

    public vec2 xy() {
        return new vec2(x, y);
    }

    @Override
    public String toString() {
        return "vec3{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public static vec3 add(vec3 l , vec3 r) {
        l.x += r.x;
        l.y += r.y;
        l.z += r.z;
        return l;
    }

    public static vec3 negate(vec3 v) {
        return new vec3(-v.x, -v.y, -v.z);
    }

    public static vec3 multiply(vec3 l, double r) {
        l.x *= r;
        l.y *= r;
        l.z *= r;
        return l;
    }
}
