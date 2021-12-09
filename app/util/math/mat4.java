package util.math;

import java.util.Arrays;

public class mat4 {
    public double [] matrix;

    public mat4(double[] _matrix) {
        matrix = _matrix;
    }

    @Override
    public String toString() {
        return "mat4{" +
                "matrix=" + Arrays.toString(matrix) +
                '}';
    }

    public static mat4 createMat4() {
        double[] matrix = {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
        return new mat4(matrix);
    }

    public static vec4 multiply(mat4 l, vec4 r) {
        vec4 result = new vec4();
        result.x = l.matrix[0] * r.x + l.matrix[1] * r.y + l.matrix[2] * r.z + l.matrix[3] * r.w;
        result.y = l.matrix[4] * r.x + l.matrix[5] * r.y + l.matrix[6] * r.z + l.matrix[7] * r.w;
        result.z = l.matrix[8] * r.x + l.matrix[9] * r.y + l.matrix[10] * r.z + l.matrix[11] * r.w;
        result.w = l.matrix[12] * r.x + l.matrix[13] * r.y + l.matrix[14] * r.z + l.matrix[15] * r.w;
        return result;
    }

    public static mat4 multiply(mat4 l, mat4 r) {
        mat4 result = new mat4(new double[16]);
        result.matrix[0] = l.matrix[0] * r.matrix[0] + l.matrix[1] * r.matrix[4] + l.matrix[2] * r.matrix[8] + l.matrix[3] * r.matrix[12];
        result.matrix[1] = l.matrix[0] * r.matrix[1] + l.matrix[1] * r.matrix[5] + l.matrix[2] * r.matrix[9] + l.matrix[3] * r.matrix[13];
        result.matrix[2] = l.matrix[0] * r.matrix[2] + l.matrix[1] * r.matrix[6] + l.matrix[2] * r.matrix[10] + l.matrix[3] * r.matrix[14];
        result.matrix[3] = l.matrix[0] * r.matrix[3] + l.matrix[1] * r.matrix[7] + l.matrix[2] * r.matrix[11] + l.matrix[3] * r.matrix[15];

        result.matrix[4] = l.matrix[4] * r.matrix[0] + l.matrix[5] * r.matrix[4] + l.matrix[6] * r.matrix[8] + l.matrix[7] * r.matrix[12];
        result.matrix[5] = l.matrix[4] * r.matrix[1] + l.matrix[5] * r.matrix[5] + l.matrix[6] * r.matrix[9] + l.matrix[7] * r.matrix[13];
        result.matrix[6] = l.matrix[4] * r.matrix[2] + l.matrix[5] * r.matrix[6] + l.matrix[6] * r.matrix[10] + l.matrix[7] * r.matrix[14];
        result.matrix[7] = l.matrix[4] * r.matrix[3] + l.matrix[5] * r.matrix[7] + l.matrix[6] * r.matrix[11] + l.matrix[7] * r.matrix[15];

        result.matrix[8] = l.matrix[8] * r.matrix[0] + l.matrix[9] * r.matrix[4] + l.matrix[10] * r.matrix[8] + l.matrix[11] * r.matrix[12];
        result.matrix[9] = l.matrix[8] * r.matrix[1] + l.matrix[9] * r.matrix[5] + l.matrix[10] * r.matrix[9] + l.matrix[11] * r.matrix[13];
        result.matrix[10] = l.matrix[8] * r.matrix[2] + l.matrix[9] * r.matrix[6] + l.matrix[10] * r.matrix[10] + l.matrix[11] * r.matrix[14];
        result.matrix[11] = l.matrix[8] * r.matrix[3] + l.matrix[9] * r.matrix[7] + l.matrix[10] * r.matrix[11] + l.matrix[11] * r.matrix[15];

        result.matrix[12] = l.matrix[12] * r.matrix[0] + l.matrix[13] * r.matrix[4] + l.matrix[14] * r.matrix[8] + l.matrix[15] * r.matrix[12];
        result.matrix[13] = l.matrix[12] * r.matrix[1] + l.matrix[13] * r.matrix[5] + l.matrix[14] * r.matrix[9] + l.matrix[15] * r.matrix[13];
        result.matrix[14] = l.matrix[12] * r.matrix[2] + l.matrix[13] * r.matrix[6] + l.matrix[14] * r.matrix[10] + l.matrix[15] * r.matrix[14];
        result.matrix[15] = l.matrix[12] * r.matrix[3] + l.matrix[13] * r.matrix[7] + l.matrix[14] * r.matrix[11] + l.matrix[15] * r.matrix[15];

        return result;
    }

    /**
     * Translate a mat4 by the given vector
     * @param a - the matrix to translate
     * @param v - vector to translate by
     * @return
     */
    public static mat4 translate(mat4 a, vec3 v) {
        double  x = v.x,
                y = v.y,
                z = v.z;
        double a00, a01, a02, a03;
        double a10, a11, a12, a13;
        double a20, a21, a22, a23;

        mat4 out = new mat4(new double[16]);
        a00 = a.matrix[0];
        a01 = a.matrix[1];
        a02 = a.matrix[2];
        a03 = a.matrix[3];
        a10 = a.matrix[4];
        a11 = a.matrix[5];
        a12 = a.matrix[6];
        a13 = a.matrix[7];
        a20 = a.matrix[8];
        a21 = a.matrix[9];
        a22 = a.matrix[10];
        a23 = a.matrix[11];
        out.matrix[0] = a00;
        out.matrix[1] = a01;
        out.matrix[2] = a02;
        out.matrix[3] = a03;
        out.matrix[4] = a10;
        out.matrix[5] = a11;
        out.matrix[6] = a12;
        out.matrix[7] = a13;
        out.matrix[8] = a20;
        out.matrix[9] = a21;
        out.matrix[10] = a22;
        out.matrix[11] = a23;
        out.matrix[12] = a00 * x + a10 * y + a20 * z + a.matrix[12];
        out.matrix[13] = a01 * x + a11 * y + a21 * z + a.matrix[13];
        out.matrix[14] = a02 * x + a12 * y + a22 * z + a.matrix[14];
        out.matrix[15] = a03 * x + a13 * y + a23 * z + a.matrix[15];

        return out;
    }

    /**
     * Rotates a matrix by the given angle around the X axis
     *
     * @param a - the matrix to rotate
     * @param rad - the angle to rotate the matrix by
     * @return
     */
    public static mat4 rotateX(mat4 a, double rad) {
        double s = Math.sin(rad);
        double c = Math.cos(rad);
        double a10 = a.matrix[4];
        double a11 = a.matrix[5];
        double a12 = a.matrix[6];
        double a13 = a.matrix[7];
        double a20 = a.matrix[8];
        double a21 = a.matrix[9];
        double a22 = a.matrix[10];
        double a23 = a.matrix[11];

        mat4 out = new mat4(new double[16]);

        out.matrix[0] = a.matrix[0];
        out.matrix[1] = a.matrix[1];
        out.matrix[2] = a.matrix[2];
        out.matrix[3] = a.matrix[3];
        out.matrix[12] = a.matrix[12];
        out.matrix[13] = a.matrix[13];
        out.matrix[14] = a.matrix[14];
        out.matrix[15] = a.matrix[15];
        out.matrix[4] = a10 * c + a20 * s;
        out.matrix[5] = a11 * c + a21 * s;
        out.matrix[6] = a12 * c + a22 * s;
        out.matrix[7] = a13 * c + a23 * s;
        out.matrix[8] = a20 * c - a10 * s;
        out.matrix[9] = a21 * c - a11 * s;
        out.matrix[10] = a22 * c - a12 * s;
        out.matrix[11] = a23 * c - a13 * s;

        return out;
    }

    /**
     * Rotates a matrix by the given angle around the Z axis
     *
     * @param a - the matrix to rotate
     * @param rad - the angle to rotate the matrix by
     * @return
     */
    public static mat4 rotateZ(mat4 a, double rad) {
        double s = Math.sin(rad);
        double c = Math.cos(rad);
        double a00 = a.matrix[0];
        double a01 = a.matrix[1];
        double a02 = a.matrix[2];
        double a03 = a.matrix[3];
        double a10 = a.matrix[4];
        double a11 = a.matrix[5];
        double a12 = a.matrix[6];
        double a13 = a.matrix[7];

        mat4 out = new mat4(new double[16]);

        out.matrix[8] = a.matrix[8];
        out.matrix[9] = a.matrix[9];
        out.matrix[10] = a.matrix[10];
        out.matrix[11] = a.matrix[11];
        out.matrix[12] = a.matrix[12];
        out.matrix[13] = a.matrix[13];
        out.matrix[14] = a.matrix[14];
        out.matrix[15] = a.matrix[15];

        out.matrix[0] = a00 * c + a10 * s;
        out.matrix[1] = a01 * c + a11 * s;
        out.matrix[2] = a02 * c + a12 * s;
        out.matrix[3] = a03 * c + a13 * s;
        out.matrix[4] = a10 * c - a00 * s;
        out.matrix[5] = a11 * c - a01 * s;
        out.matrix[6] = a12 * c - a02 * s;
        out.matrix[7] = a13 * c - a03 * s;
        return out;
    }

    /**
     * Scales the mat4 by the dimensions in the given vec3 not using vectorization
     *
     * @param a - the matrix to scale
     * @param v - the vec3 to scale the matrix by
     * @return
     */
    public static mat4 scale(mat4 a, vec3 v) {
        double x = v.x, y = v.y, z = v.z;

        mat4 out = new mat4(new double[16]);

        out.matrix[0] = a.matrix[0] * x;
        out.matrix[1] = a.matrix[1] * x;
        out.matrix[2] = a.matrix[2] * x;
        out.matrix[3] = a.matrix[3] * x;
        out.matrix[4] = a.matrix[4] * y;
        out.matrix[5] = a.matrix[5] * y;
        out.matrix[6] = a.matrix[6] * y;
        out.matrix[7] = a.matrix[7] * y;
        out.matrix[8] = a.matrix[8] * z;
        out.matrix[9] = a.matrix[9] * z;
        out.matrix[10] = a.matrix[10] * z;
        out.matrix[11] = a.matrix[11] * z;
        out.matrix[12] = a.matrix[12];
        out.matrix[13] = a.matrix[13];
        out.matrix[14] = a.matrix[14];
        out.matrix[15] = a.matrix[15];

        return out;
    }

    /**
     * Generates a perspective projection matrix with the given bounds.
     * Passing null/undefined/no value for far will generate infinite projection matrix.
     *
     * @param fovy - Vertical field of view in radians
     * @param aspect - Aspect ratio. typically viewport width/height
     * @param near - Near bound of the frustum
     * @param far - Far bound of the frustum, can be null or Infinity
     * @return - frustum matrix
     */
    public static mat4 perspective(double fovy, double aspect, double near, double far) {
        double f = 1.0 / Math.tan(fovy / 2), nf;

        mat4 out = new mat4(new double[16]);

        out.matrix[0] = f / aspect;
        out.matrix[1] = 0;
        out.matrix[2] = 0;
        out.matrix[3] = 0;
        out.matrix[4] = 0;
        out.matrix[5] = f;
        out.matrix[6] = 0;
        out.matrix[7] = 0;
        out.matrix[8] = 0;
        out.matrix[9] = 0;
        out.matrix[11] = -1;
        out.matrix[12] = 0;
        out.matrix[13] = 0;
        out.matrix[15] = 0;

        nf = 1 / (near - far);
        out.matrix[10] = (far + near) * nf;
        out.matrix[14] = 2 * far * near * nf;

        return out;
    }
}
