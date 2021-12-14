package util.render;

import util.math.*;

import java.util.Map;

public class WebMercatorViewport {

    static double TILE_SIZE = 512.0;
    static double PI = Math.PI;
    static double PI_4 = PI / 4;
    static double DEGREES_TO_RADIANS = PI / 180;
    static double EARTH_CIRCUMFERENCE = 40.03e6;

    public static int DEFAULT_WIDTH = 1;
    public static int DEFAULT_HEIGHT = 1;
    public static double DEFAULT_ALTITUDE = 1.5;

    public static double DEFAULT_LONGITUDE = 0;
    public static double DEFAULT_LATITUDE = 0;
    public static int DEFAULT_ZOOM = 11;

    public static double DEFAULT_PITCH = 0;
    public static double DEFAULT_BEARING = 0;

    public static boolean DEFAULT_ORTHOGRAPHIC = false;
    public static double DEFAULT_NEAR_Z_MULTIPLIER = 0.1;
    public static double DEFAULT_FAR_Z_MULTIPLIER = 1.01;

    int width;
    int height;
    double altitude;

    double longitude;
    double latitude;
    int zoom;
    double pitch;
    double bearing;
    double scale;

    boolean orthographic;
    double nearZMultiplier;
    double farZMultiplier;

    // view matrix
    mat4 viewMatrixUncentered;
    mat4 viewMatrix;
    vec3 center;

    // projection parameters
    double fov, aspect, focalDistance, near, far, fovyRadians;

    // projection matrix
    mat4 projectionMatrix;

    // view projection matrix
    mat4 viewProjectionMatrix;

    // pixel projection matrix
    mat4 pixelProjectionMatrix;

    // distance scales
    DistanceScales distanceScales;

    public vec4 transformVector(mat4 matrix, vec4 vector) {
        vec4 result = vec4.transformMat4(vector, matrix);
        result = vec4.scale(result, 1 / result.w);
        return result;
    }

    /**
     * Project xy (longitude, latitude) to world position using Web-Mercator-Projection
     *
     * @param lnglat
     * @return
     */
    public vec2 lngLatToWorldPosition(vec2 lnglat) {
        if (lnglat.x < -180 || lnglat.x > 180) {
            return null;
        }
        if (lnglat.y < -90 || lnglat.y > 90) {
            return null;
        }
        double lambda2 = lnglat.x * DEGREES_TO_RADIANS;
        double phi2 = lnglat.y * DEGREES_TO_RADIANS;
        double x = (TILE_SIZE * (lambda2 + PI)) / (2 * PI);
        double y = (TILE_SIZE * (PI + Math.log(Math.tan(PI_4 + phi2 * 0.5)))) / (2 * PI);
        return new vec2(x, y);
    }

    /**
     * Project xyz (longitude, latitude, altitude) to world position
     *
     * @param xyz
     * @return
     */
    public vec3 lngLatToWorldPosition(vec3 xyz) {
        vec2 xy = lngLatToWorldPosition(xyz.xy());
        double z = xyz.z * this.distanceScales.unitsPerMeter.z;
        return new vec3(xy.x, xy.y, z);
    }

    /**
     * Project xyz (longitude, latitude, altitude) to clip space position
     *
     * @param xyz
     * @return
     */
    public vec4 lngLatToClipspacePosition(vec3 xyz) {
        vec3 wordPosition = lngLatToWorldPosition(xyz);
        vec4 commonPosition = new vec4(wordPosition, 1.0);
        return vec4.transformMat4(commonPosition, this.viewProjectionMatrix);
    }

    /**
     * Project world position to a screen pixel using given pixelProjectionMatrix
     *
     * @param position
     * @param pixelProjectionMatrix
     * @return
     */
    public vec4 worldPositionToScreenPixel(vec3 position, mat4 pixelProjectionMatrix) {
        return transformVector(pixelProjectionMatrix, new vec4(position, 1));
    }

    /**
     * Projects xyz (longitude, latitude, altitude) to pixel coordinates in window
     * using viewport projection parameters
     * - [longitude, latitude] to [x, y]
     * - [longitude, latitude, Z] => [x, y, z]
     * Note: By default, returns top-left coordinates for canvas/SVG type render
     *
     * @param xyz
     * @param topLeft
     * @return
     */
    public vec3 lngLatToScreenPixel(vec3 xyz, boolean topLeft) {
        vec3 worldPosition = lngLatToWorldPosition(xyz);
        vec4 coord = worldPositionToScreenPixel(worldPosition, pixelProjectionMatrix);
        double x = coord.x;
        double y = topLeft? coord.y: this.height - coord.y;
        double z = coord.z;
        return new vec3(x, y, z);
    }

    public WebMercatorViewport(Map<String, Object> opts) {

        width = (int) opts.getOrDefault("width", DEFAULT_WIDTH);
        height = (int) opts.getOrDefault("height", DEFAULT_HEIGHT);
        altitude = (double) opts.getOrDefault("altitude", DEFAULT_ALTITUDE);
        altitude = Math.max(0.75, altitude);

        longitude = (double) opts.getOrDefault("longitude", DEFAULT_LONGITUDE);
        latitude = (double) opts.getOrDefault("latitude", DEFAULT_LATITUDE);
        zoom = (int) opts.getOrDefault("zoom", DEFAULT_ZOOM);
        pitch = (double) opts.getOrDefault("pitch", DEFAULT_PITCH);
        bearing = (double) opts.getOrDefault("bearing", DEFAULT_BEARING);
        scale = Math.pow(2, zoom);

        nearZMultiplier = (double) opts.getOrDefault("nearZMultiplier", DEFAULT_NEAR_Z_MULTIPLIER);
        farZMultiplier = (double) opts.getOrDefault("farZMultiplier", DEFAULT_FAR_Z_MULTIPLIER);
        orthographic = (boolean) opts.getOrDefault("orthographic", DEFAULT_ORTHOGRAPHIC);

        ProjectionParameters pp = getProjectionParameters(width, height, altitude, pitch, nearZMultiplier, farZMultiplier);
        fov = pp.fov;
        aspect = pp.aspect;
        focalDistance = pp.focalDistance;
        near = pp.near;
        far = pp.far;
        fovyRadians = fov;

        this.viewMatrixUncentered = getViewMatrix(height, altitude, pitch, bearing, scale);

        initViewMatrix();
        initProjectionMatrix();
        initViewProjectionMatrix();
        initPixelProjectionMatrix();
        initDistanceScales(this.latitude, this.longitude);
    }

    public mat4 getViewMatrix() {
        return this.viewMatrix;
    }

    public mat4 getProjectionMatrix() {
        return this.projectionMatrix;
    }

    public mat4 getViewProjectionMatrix() {
        return this.viewProjectionMatrix;
    }

    public mat4 getPixelProjectionMatrix() {
        return this.pixelProjectionMatrix;
    }

    class ProjectionParameters {
        public double fov, aspect, focalDistance, near, far;

        public ProjectionParameters(double _fov, double _aspect, double _focalDistance, double _near, double _far) {
            fov = _fov;
            aspect = _aspect;
            focalDistance = _focalDistance;
            near = _near;
            far = _far;
        }
    }

    private ProjectionParameters getProjectionParameters(int width, int height, double altitude, double pitch, double nearZMultiplier, double farZMultiplier) {
        altitude = Math.max(0.75, altitude);
        double pitchRadians = pitch * DEGREES_TO_RADIANS;
        double halfFov = Math.atan(0.5 / altitude);
        double topHalfSurfaceDistance = Math.sin(halfFov) * altitude / Math.sin(Math.PI / 2 - pitchRadians - halfFov);
        double farZ = Math.cos(Math.PI / 2 - pitchRadians) * topHalfSurfaceDistance + altitude;

        return new ProjectionParameters(2 * halfFov, (double)width / (double)height, altitude, nearZMultiplier, farZ * farZMultiplier);
    }

    private mat4 getViewMatrix(int height, double altitude, double pitch, double bearing,  double scale) {
        mat4 vm = mat4.createMat4();
        vm = mat4.translate(vm, new vec3(0, 0, -altitude));
        vm = mat4.rotateX(vm, -pitch * DEGREES_TO_RADIANS);
        vm = mat4.rotateZ(vm, bearing * DEGREES_TO_RADIANS);
        scale /= height;
        vm = mat4.scale(vm, new vec3(scale, scale, scale));

        return vm;
    }

    private vec3 getCenterInWorld(double longitude, double latitude) {

        // Make a centered version of the matrix for projection modes without an offset
        vec2 center2d = lngLatToWorldPosition(new vec2(longitude, latitude));
        vec3 center = new vec3(center2d.x, center2d.y, 0);

        return center;
    }

    private void initViewMatrix() {
        // Determine camera center
        center = getCenterInWorld(longitude, latitude);

        // Make a centered version of the matrix for projection modes without an offset
        viewMatrix = mat4.multiply(viewMatrixUncentered, mat4.createMat4());
        // And center it
        viewMatrix = mat4.translate(viewMatrix, vec3.negate(center));
    }

    private mat4 createProjectionMatrix(boolean orthographic, double fovyRadians, double aspect, double near, double far) {
        if (orthographic) {
            // TODO - orghographic
            return null;
        }
        else {
            return mat4.perspective(fovyRadians, aspect, near, far);
        }
    }

    private void initProjectionMatrix() {
        projectionMatrix = createProjectionMatrix(orthographic, fovyRadians, aspect, near, far);
    }

    private void initViewProjectionMatrix() {
        mat4 vpm = mat4.createMat4();
        vpm = mat4.multiply(this.projectionMatrix, vpm);
        vpm = mat4.multiply(this.viewMatrix, vpm);
        this.viewProjectionMatrix = vpm;
    }

    private void initPixelProjectionMatrix() {
        mat4 _viewportMatrix = mat4.createMat4(); // matrix from NDC to viewport.
        mat4 _pixelProjectionMatrix; // matrix from world space to viewport.
        _viewportMatrix = mat4.scale(_viewportMatrix, new vec3(this.width / 2, -this.height / 2, 1));
        _viewportMatrix = mat4.translate(_viewportMatrix, new vec3(1, -1, 0));
        _pixelProjectionMatrix = mat4.multiply(viewProjectionMatrix, _viewportMatrix);
        pixelProjectionMatrix = _pixelProjectionMatrix;
    }

    class DistanceScales {
        vec3 unitsPerMeter;
        vec3 metersPerUnit;
        vec3 unitsPerDegree;
        vec3 degreesPerUnit;
    }

    private void initDistanceScales(double latitude, double longitude) {
        distanceScales = new DistanceScales();
        double worldSize = TILE_SIZE;
        double latCosine = Math.cos(latitude * DEGREES_TO_RADIANS);
        double unitsPerDegreeX = worldSize / 360;
        double unitsPerDegreeY = unitsPerDegreeX / latCosine;
        double altUnitsPerMeter = worldSize / EARTH_CIRCUMFERENCE / latCosine;
        distanceScales.unitsPerMeter = new vec3(altUnitsPerMeter, altUnitsPerMeter, altUnitsPerMeter);
        distanceScales.metersPerUnit = new vec3(1 / altUnitsPerMeter, 1 / altUnitsPerMeter, 1 / altUnitsPerMeter);
        distanceScales.unitsPerDegree = new vec3(unitsPerDegreeX, unitsPerDegreeY, altUnitsPerMeter);
        distanceScales.degreesPerUnit = new vec3(1 / unitsPerDegreeX, 1 / unitsPerDegreeY, 1 / altUnitsPerMeter);
    }
}
