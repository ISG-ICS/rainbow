package util.math;

public class vec4 {
    public double x;
    public double y;
    public double z;
    public double w;

    public vec4() {
    }

    public vec4(double _x, double _y, double _z, double _w) {
        x = _x;
        y = _y;
        z = _z;
        w = _w;
    }

    public vec4(vec3 _xyz, double _w) {
        x = _xyz.x;
        y = _xyz.y;
        z = _xyz.z;
        w = _w;
    }

    public vec4(vec2 _xy, double _z, double _w) {
        x = _xy.x;
        y = _xy.y;
        z = _z;
        w = _w;
    }

    public vec3 xyz() {
        return new vec3(x, y, z);
    }

    public vec2 xy() {
        return new vec2(x, y);
    }

    @Override
    public String toString() {
        return "vec4{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                ", w=" + w +
                '}';
    }

    public static vec4 add(vec4 l, vec4 r) {
        l.x += r.x;
        l.y += r.y;
        l.z += r.z;
        l.w += r.w;
        return l;
    }

    /**
     * Transforms the vec4 with a mat4.
     *
     * @param a - the vector to transform
     * @param m - matrix to transform with
     * @return
     */
    public static vec4 transformMat4(vec4 a, mat4 m) {
        double x = a.x;
        double y = a.y;
        double z = a.z;
        double w = a.w;

        vec4 out = new vec4();

        out.x = m.matrix[0] * x + m.matrix[4] * y + m.matrix[8] * z + m.matrix[12] * w;
        out.y = m.matrix[1] * x + m.matrix[5] * y + m.matrix[9] * z + m.matrix[13] * w;
        out.z = m.matrix[2] * x + m.matrix[6] * y + m.matrix[10] * z + m.matrix[14] * w;
        out.w = m.matrix[3] * x + m.matrix[7] * y + m.matrix[11] * z + m.matrix[15] * w;
        return out;
    }

    /**
     * Scales a vec4 by a scalar number
     *
     * @param a - the vector to scale
     * @param b - amount to scale the vector by
     * @return
     */
    public static vec4 scale(vec4 a, double b) {
        vec4 out = new vec4();
        out.x = a.x * b;
        out.y = a.y * b;
        out.z = a.z * b;
        out.w = a.w * b;
        return out;
    }
}
