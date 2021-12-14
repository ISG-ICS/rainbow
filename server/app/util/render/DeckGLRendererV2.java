package util.render;

import model.Point;
import util.UnsignedByte;
import util.math.*;

import java.util.HashMap;
import java.util.Map;

public class DeckGLRendererV2 {

    /** ==== WebGL Constants ==== */
    static double TILE_SIZE = 512.0;
    static double PI = Math.PI;
    static double WORLD_SCALE = TILE_SIZE / (PI * 2.0);
    static double SMOOTH_EDGE_RADIUS = 0.5;

    class pixel {
        int i;
        int j;

        pixel(int _i, int _j) {
            i = _i;
            j = _j;
        }

        @Override
        public String toString() {
            return "pixel{" +
                    "i=" + i +
                    ", j=" + j +
                    '}';
        }
    }

    /** ==== Project Uniforms ==== */

    private vec4 project_uCenter() {
        return new vec4(0, 0, 0, 0);
    }

    private double project_uScale() {
        return 8;
    }

    private mat4 project_uModelMatrix() {
        double[] modelMatrix = {
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        };
        return new mat4(modelMatrix);
    }

    private mat4 project_uViewProjectionMatrix() {
        double[] viewProjectionMatrix = {
                0.0125, 0, 0, 0,
                0, 0.0245398773006135, 0, 0,
                0, 0, -0.009336137064897717, -0.0081799591002045,
                -1.4875, -7.785276073619633, 1.497879858657244, 1.5
        };
        return new mat4(viewProjectionMatrix);
    }

    private vec3 project_uCommonUnitsPerMeter() {
        return new vec3(1.6576909442350456E-05, 1.6576909442350456E-05, 1.6576909442350456E-05);
    }

    private boolean project_uWrapLongitude() {
        return false;
    }

    private double project_uAntimeridian() {
        return -276.328125;
    }

    /** ==== DeckGLRendererV2 ==== */
    static byte[] COLOR = {
            UnsignedByte.toByte(0),
            UnsignedByte.toByte(0),
            UnsignedByte.toByte(255)
    }; // blue

    static byte[] BG_COLOR = {
            UnsignedByte.toByte(255),
            UnsignedByte.toByte(255),
            UnsignedByte.toByte(255)
    }; // white

    int radiusInPixels;
    double opacity;

    //view port parameters
    int width;
    int height;
    double latitude;
    double longitude;
    int zoom;
    double pitch;
    double bearing;
    boolean orthographic;

    // web mercator viewport
    WebMercatorViewport viewport;

    public DeckGLRendererV2(int radiusInPixels, double opacity) {
        this.radiusInPixels = radiusInPixels;
        this.opacity = opacity;
        System.out.println("[DeckGLRendererV2] initializing with { radiusInPixels: " + radiusInPixels + ", opacity: " + opacity + "}.");

        // view port parameters
        this.width = 1920;
        this.height = 978;
        this.latitude = 39.50404070558415;
        this.longitude = -96.328125;
        this.zoom = 3;
        this.pitch = 0;
        this.bearing = 0;
        this.orthographic = false;

        Map<String, Object> viewportOpts = new HashMap<>();
        viewportOpts.put("width", this.width);
        viewportOpts.put("height", this.height);
        viewportOpts.put("latitude", this.latitude);
        viewportOpts.put("longitude", this.longitude);
        viewportOpts.put("zoom", this.zoom);
        viewportOpts.put("pitch", this.pitch);
        viewportOpts.put("bearing", this.bearing);
        viewportOpts.put("orthographic", this.orthographic);

        viewport = new WebMercatorViewport(viewportOpts);
    }

    /**
     * Create a rendering of given resolution with background color
     *
     *  - Use 1-D array to simulate a 3-D array
     *    suppose 3-D array has dimension lengths: resX * resY * 3
     *    [i][j][k] = i * resY * 3 + j * 3 + k
     *
     * @param resX
     * @param resY
     * @return
     */
    public byte[] createRendering(int resX, int resY) {
        byte[] rendering = new byte[resX * resY * 3];
        // init rendering with BG_COLOR
        for (int i = 0; i < resX; i++) {
            for (int j = 0; j < resY; j++) {
                rendering[i * resY * 3 + j * 3 + 0] = BG_COLOR[0]; // R
                rendering[i * resY * 3 + j * 3 + 1] = BG_COLOR[1]; // G
                rendering[i * resY * 3 + j * 3 + 2] = BG_COLOR[2]; // B
            }
        }
        return rendering;
    }

    /**
     * Render a new point onto the given rendering
     *
     * @param rendering - Use 1-D array to simulate a 3-D array
     *                    suppose 3-D array has dimension lengths: resX * resY * 3
     *                    [i][j][k] = i * resY * 3 + j * 3 + k
     * @param resX
     * @param resY
     * @param lng
     * @param lat
     * @return boolean - if render the point on given rendering does not change the result, return false; else return true;
     */
    public boolean render(byte[] rendering, int resX, int resY, double lng, double lat) {

        //-DEBUG-//
        // compare the shader's project_position_to_clipspace with viewport's project result
//        vec3 lnglat = new vec3(point.getX(), point.getY(), 0);
//        vec4 shader_clipspace_position = project_position_to_clipspace(lnglat);
//        vec3 shader_screen_pixel = clipspaceToScreen(resX, resY, shader_clipspace_position);
//        vec3 vp_screen_pixel = this.viewport.project(lnglat, true);
//        double epsilon = 1e-4;
//        boolean succeed = true;
//        if (Math.abs(shader_screen_pixel.x - vp_screen_pixel.x) > epsilon) {
//            succeed = false;
//        }
//        if (Math.abs(shader_screen_pixel.y - vp_screen_pixel.y) > epsilon) {
//            succeed = false;
//        }
//        if (Math.abs(shader_screen_pixel.z - vp_screen_pixel.z) > epsilon) {
//            succeed = false;
//        }
//        if (!succeed) {
//            System.err.println("[error][render] point " + lnglat);
//            System.err.println("[error][render] shader_clipspace_position = " + shader_clipspace_position);
//            System.err.println("[error][render] shader_screen_pixel = " + shader_screen_pixel);
//            System.err.println("[error][render] vp_screen_pixel = " + vp_screen_pixel);
//        }
        //-DEBUG-//

        boolean isDifferent = false;

        // 1) get outer radius in pixels
        double outerRadiusPixels = this.radiusInPixels;

        // 2) find circumscribed square corners offset corresponding to the point position in common space
        vec3 southwestOffset = new vec3(-1, -1, 0);
        vec3 northwestOffset = new vec3(-1, 1, 0);
        vec3 northeastOffset = new vec3(1, 1, 0);
        vec3 southeastOffset = new vec3(1, -1, 0);
        double halfCircumscribedSquareSideLength = project_pixel_size(outerRadiusPixels);
        southwestOffset = vec3.multiply(southwestOffset, halfCircumscribedSquareSideLength);
        northwestOffset = vec3.multiply(northwestOffset, halfCircumscribedSquareSideLength);
        northeastOffset = vec3.multiply(northeastOffset, halfCircumscribedSquareSideLength);
        southeastOffset = vec3.multiply(southeastOffset, halfCircumscribedSquareSideLength);

        //-DEBUG-//
//        System.out.println("----> (offsets) <----");
//        System.out.println("swo = " + southwestOffset);
//        System.out.println("nwo = " + northwestOffset);
//        System.out.println("neo = " + northeastOffset);
//        System.out.println("seo = " + southeastOffset);
        //-DEBUG-//

        // 3) find clip-space positions of the circumscribed square corners
        vec3 instancePositions = new vec3(lng, lat, 0);

        //-DEBUG-//
//        System.out.println("----> (instance positions) <----");
//        System.out.println("instancePositions = " + instancePositions);
        //-DEBUG-//

        vec4 southwest = project_position_to_clipspace(instancePositions, southwestOffset);
        vec4 northwest = project_position_to_clipspace(instancePositions, northwestOffset);
        vec4 northeast = project_position_to_clipspace(instancePositions, northeastOffset);
        vec4 southeast = project_position_to_clipspace(instancePositions, southeastOffset);

        //-DEBUG-//
//        System.out.println("----> (projected_position_in_clipspace) <----");
//        System.out.println("sw = " + southwest);
//        System.out.println("nw = " + northwest);
//        System.out.println("ne = " + northeast);
//        System.out.println("se = " + southeast);
        //-DEBUG-//

        // 4) fill color's alpha
        double fillColorAlpha = 1.0 * opacity;

        // 5) rasterize the circumscribed square corners into rendering pixel indexes
        pixel leftbottom = rasterize(southwest, resX, resY);
        pixel lefttop = rasterize(northwest, resX, resY);
        pixel righttop = rasterize(northeast, resX, resY);
        pixel rightbottom = rasterize(southeast, resX, resY);

        //- DEBUG -//
//        System.out.println("----> (rasterized pixel index) <----");
//        System.out.println("lb = " + leftbottom);
//        System.out.println("lt = " + lefttop);
//        System.out.println("rt = " + righttop);
//        System.out.println("rb = " + rightbottom);
        //- DEBUG -//

        // 6) loop all pixels within the circumscribed square (including corners) and calculate the color of each pixel
        // TODO - we currently assume the rasterized circumscribed square is regular:
        //        lefttop.i == leftbottom.i, righttop.i == rightbottom.i
        //        lefttop.j == righttop.j, leftbottom.j == rightbottom.j
        // traverse pixels within the square
        double distToCenter;
        double inCircle;
        double alpha;
        int or, og, ob;
        int r, g, b;
        double lbX = southwest.x / southwest.w;
        double lbY = southwest.y / southwest.w;
        double rtX = northeast.x / northeast.w;
        double rtY = northeast.y / northeast.w;
        for (int i = leftbottom.i; i <= righttop.i; i ++) {
            for (int j = leftbottom.j; j <= righttop.j; j ++) {
                // get the interpolated unitPosition corresponding to center
                // leftbottom (-1.0, -1.0) -> righttop (1.0, 1.0)
                vec2 unitPosition = new vec2(0.0, 0.0);
                vec2 pixelClipCoord = unrasterize(new pixel(i, j), resX, resY);
                // TODO - use interpolation formula with w involved
//                double tX = (pixelClipCoord.x - lbX) / (rtX - lbX);
//                double tY = (pixelClipCoord.y - lbY) / (rtY - lbY);
//                unitPosition.x = (1.0 - tX) * southwest.x / southwest.w + tX * northeast.x / northeast.w;
//                unitPosition.x = unitPosition.x / ((1.0 - tX) / southwest.w + tX / northeast.w);
//                unitPosition.y = (1.0 - tY) * southwest.y / southwest.w + tY * northeast.y / northeast.w;
//                unitPosition.y = unitPosition.y / ((1.0 - tY) / southwest.w + tY / northeast.w);
                double tX = (pixelClipCoord.x - lbX) / (rtX - lbX);
                double tY = (pixelClipCoord.y - lbY) / (rtY - lbY);
                unitPosition.x = -1.0 * (1 - tX) + 1.0 * tX;
                unitPosition.y = -1.0 * (1 - tY) + 1.0 * tY;
                distToCenter = vec2.length(unitPosition) * outerRadiusPixels;
                inCircle = smoothEdge(distToCenter, outerRadiusPixels);
                //-DEBUG-//
                //System.out.println("pixel [" + i + ", " + j + "] distToCenter = " + distToCenter + ", inCircle = " + inCircle);
                //-DEBUG-//
                alpha = fillColorAlpha * inCircle;
                or = UnsignedByte.toInt(rendering[i * resY * 3 + j * 3 + 0]);
                og = UnsignedByte.toInt(rendering[i * resY * 3 + j * 3 + 1]);
                ob = UnsignedByte.toInt(rendering[i * resY * 3 + j * 3 + 2]);
                // apply blend function DST_COLOR = SRC_COLOR * SRC_ALPHA + DST_COLOR * (1 - SRC_ALPHA)
                r = (int) (UnsignedByte.toInt(COLOR[0]) * alpha + or * (1.0 - alpha)); // R
                g = (int) (UnsignedByte.toInt(COLOR[1]) * alpha + og * (1.0 - alpha)); // G
                b = (int) (UnsignedByte.toInt(COLOR[2]) * alpha + ob * (1.0 - alpha)); // B
                if (or != r) {
                    isDifferent = true;
                    rendering[i * resY * 3 + j * 3 + 0] = UnsignedByte.toByte(r);
                }
                if (og != g) {
                    isDifferent = true;
                    rendering[i * resY * 3 + j * 3 + 1] = UnsignedByte.toByte(g);
                }
                if (ob != b) {
                    isDifferent = true;
                    rendering[i * resY * 3 + j * 3 + 2] = UnsignedByte.toByte(b);
                }
            }
        }

        return isDifferent;
    }

    private pixel rasterize(vec4 _clipCoord, int _resX, int _resY) {
        double x = ((_clipCoord.x / _clipCoord.w + 1.0) / 2.0) * _resX;
        double y = ((_clipCoord.y / _clipCoord.w + 1.0) / 2.0) * _resY;
        return new pixel((int) x, (int) y);
    }

    private vec2 unrasterize(pixel p, int _resX, int _resY) {
        double clipX = (2 * ((double)p.i + 0.5) - (double)_resX) / (double)_resX;
        double clipY = (2 * ((double)p.j + 0.5) - (double)_resY) / (double)_resY;
        return new vec2(clipX, clipY);
    }

    private vec2 project_mercator_(vec2 lnglat) {
        double x = lnglat.x;
        if (project_uWrapLongitude()) {
            x = mod(x - project_uAntimeridian(), 360.0) + project_uAntimeridian();
        }
        return new vec2(
                radians(x) + PI, // = lngX(x) * 2 * PI
                PI + Math.log(Math.tan(PI * 0.25 + radians(lnglat.y) * 0.5)) // != latY(y)
        );
    }

    private double project_size(double meters) {
        return meters * project_uCommonUnitsPerMeter().z;
    }

    private vec4 project_position(vec4 position) {
        vec4 position_world = mat4.multiply(project_uModelMatrix(), position);
        //-DEBUG-//
        //System.out.println("[DEBUG][DeckGLRendererV2] position_world = " + position_world);
        return new vec4(
                vec2.multiply(project_mercator_(position_world.xy()) , WORLD_SCALE),
                project_size(position_world.z),
                position_world.w
        );
    }

    private vec3 project_position(vec3 position) {
        vec4 projected_position = project_position(new vec4(position, 1.0));
        //-DEBUG-//
        //System.out.println("[DEBUG][DeckGLRendererV2] projected_position = " + projected_position);
        return projected_position.xyz();
    }

    private vec4 project_common_position_to_clipspace(vec4 position) {
        return vec4.add(vec4.transformMat4(position, project_uViewProjectionMatrix()), project_uCenter());
    }

    private vec4 project_position_to_clipspace(vec3 position, vec3 offset) {
        vec3 projectedPosition = project_position(position);
        vec4 commonPosition = new vec4(vec3.add(projectedPosition, offset), 1.0);
        //-DEBUG-//
        //System.out.println("[DEBUG][DeckGLRendererV2] commonPosition = " + commonPosition);
        return project_common_position_to_clipspace(commonPosition);
    }

    private vec4 project_position_to_clipspace(vec3 position) {
        vec3 projectedPosition = project_position(position);
        return project_common_position_to_clipspace(new vec4(projectedPosition, 1.0));
    }

    private double project_pixel_size(double pixels) {
        return pixels / project_uScale();
    }

    private double smoothEdge(double edge, double x) {
        return smoothStep(edge - SMOOTH_EDGE_RADIUS, edge + SMOOTH_EDGE_RADIUS, x);
    }

    private vec3 clipspaceToScreen(int _resX, int _resY, vec4 coords) {
        return new vec3(((coords.x / coords.w + 1) / 2) * _resX,
                ((1 - coords.y / coords.w) / 2) * _resY,
                coords.z / coords.w);
    }

    /** ==== OpenGL functions ====*/
    private double radians(double degrees) {
        return PI * degrees / 180;
    }

    private double mod(double x, double y) {
        return x - y * Math.floor(x / y);
    }

    private double smoothStep(double edge0, double edge1, double x) {
        if (x <= edge0) return 0.0;
        if (x >= edge1) return 1.0;
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3 - 2 * t );
    }

    private double clamp(double x, double minVal, double maxVal) {
        return Math.min(Math.max(x, minVal), maxVal);
    }
    /** ==== OpenGL functions ====*/

    public void test_project_position() {
        vec3 lnglat = new vec3(-118.26517425999998, 34.04450895999999, 0.0);
        vec3 expected_projected_position = new vec3(this.viewport.lngLatToWorldPosition(lnglat.xy()), 0.0);
        vec3 projected_position = project_position(lnglat);
        double epsilon = 1e-4;
        boolean succeed = true;
        if (Math.abs(expected_projected_position.x - projected_position.x) > epsilon) {
            succeed = false;
        }
        if (Math.abs(expected_projected_position.y - projected_position.y) > epsilon) {
            succeed = false;
        }
        if (succeed) {
            System.out.println("[test_project_position] succeeded!");
            System.out.println("[test_project_position] lnglat = " + lnglat);
            System.out.println("[test_project_position] expected_projected_position = " + expected_projected_position);
            System.out.println("[test_project_position] projected_position = " + projected_position);
        }
        else {
            System.err.println("[test_project_position] failed! ");
            System.err.println("[test_project_position] lnglat = " + lnglat);
            System.err.println("[test_project_position] expected_projected_position = " + expected_projected_position);
            System.err.println("[test_project_position] projected_position = " + projected_position);
        }
    }

    public void test_project_common_position_to_clipspace() {
        vec3 lnglat = new vec3(-118.26517425999998, 34.04450895999999, 0.0);
        vec3 projected_position = project_position(lnglat);
        vec4 common_position = new vec4(projected_position.x, projected_position.y, projected_position.z, 1.0);
        vec4 clipspace_position = project_common_position_to_clipspace(common_position);
        System.out.println("[test_project_common_position_to_clipspace] clipspace_position = " + clipspace_position);
        vec3 screen_pixel = clipspaceToScreen(1920, 978, clipspace_position);
        vec3 expected_screen_pixel = new vec3(710.4051284195557,566.6119216363259,0.9985865724381626);
        double epsilon = 1e-4;
        boolean succeed = true;
        if (Math.abs(expected_screen_pixel.x - screen_pixel.x) > epsilon) {
            succeed = false;
        }
        if (Math.abs(expected_screen_pixel.y - screen_pixel.y) > epsilon) {
            succeed = false;
        }
        if (succeed) {
            System.out.println("[test_project_common_position_to_clipspace] succeeded!");
            System.out.println("[test_project_common_position_to_clipspace] lnglat = " + lnglat);
            System.out.println("[test_project_common_position_to_clipspace] expected_screen_pixel = " + expected_screen_pixel);
            System.out.println("[test_project_common_position_to_clipspace] screen_pixel = " + screen_pixel);
        }
        else {
            System.err.println("[test_project_common_position_to_clipspace] failed! ");
            System.err.println("[test_project_common_position_to_clipspace] lnglat = " + lnglat);
            System.err.println("[test_project_common_position_to_clipspace] expected_screen_pixel = " + expected_screen_pixel);
            System.err.println("[test_project_common_position_to_clipspace] screen_pixel = " + screen_pixel);
        }
    }

    public void test_web_mercator_vewiport_project() {
        vec3 lnglat = new vec3(-118.26517425999998, 34.04450895999999, 0.0);
        vec3 screen_pixel = this.viewport.lngLatToScreenPixel(lnglat, true);
        vec3 expected_screen_pixel = new vec3(710.4051284195557,566.6119216363259,0.9985865724381626);
        double epsilon = 1e-4;
        boolean succeed = true;
        if (Math.abs(expected_screen_pixel.x - screen_pixel.x) > epsilon) {
            succeed = false;
        }
        if (Math.abs(expected_screen_pixel.y - screen_pixel.y) > epsilon) {
            succeed = false;
        }
        if (succeed) {
            System.out.println("[test_web_mercator_vewiport_project] succeeded!");
            System.out.println("[test_web_mercator_vewiport_project] lnglat = " + lnglat);
            System.out.println("[test_web_mercator_vewiport_project] expected_screen_pixel = " + expected_screen_pixel);
            System.out.println("[test_web_mercator_vewiport_project] screen_pixel = " + screen_pixel);
        }
        else {
            System.err.println("[test_web_mercator_vewiport_project] failed! ");
            System.err.println("[test_web_mercator_vewiport_project] lnglat = " + lnglat);
            System.err.println("[test_web_mercator_vewiport_project] expected_screen_pixel = " + expected_screen_pixel);
            System.err.println("[test_web_mercator_vewiport_project] screen_pixel = " + screen_pixel);
        }
    }

    public void test_different_viewports_effects_on_clipspace_positions() {
        vec3 lnglat = new vec3(-118.26517425999998, 34.04450895999999, 0.0);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] lnglat = " + lnglat);

        // view port #1 parameters
        int width1 = 1920;
        int height1 = 978;
        double latitude1 = 39.50404070558415;
        double longitude1 = -96.328125;
        int zoom1 = 3;

        Map<String, Object> viewportOpts = new HashMap<>();
        viewportOpts.put("width", width1);
        viewportOpts.put("height", height1);
        viewportOpts.put("latitude", latitude1);
        viewportOpts.put("longitude", longitude1);
        viewportOpts.put("zoom", zoom1);
        viewportOpts.put("pitch", 0.0);
        viewportOpts.put("bearing", 0.0);
        viewportOpts.put("orthographic", false);

        WebMercatorViewport viewport1 = new WebMercatorViewport(viewportOpts);

        vec3 center1 = new vec3(longitude1, latitude1, 0.0);
        vec4 clipspaceCenter1 = viewport1.lngLatToClipspacePosition(center1);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] viewport#1 center position = " + clipspaceCenter1);
        vec4 clipspacePosition1 = viewport1.lngLatToClipspacePosition(lnglat);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] viewport#1 clipspace position = " + clipspacePosition1);

        // view port #2 parameters
        double latitude2 = 32.10118973232094;
        double longitude2 = -109.248046875;

        viewportOpts.put("latitude", latitude2);
        viewportOpts.put("longitude", longitude2);

        WebMercatorViewport viewport2 = new WebMercatorViewport(viewportOpts);
        vec3 center2 = new vec3(longitude2, latitude2, 0.0);
        vec4 clipspaceCenter2 = viewport2.lngLatToClipspacePosition(center2);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] viewport#2 center position = " + clipspaceCenter2);
        vec4 clipspacePosition2 = viewport2.lngLatToClipspacePosition(lnglat);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] viewport#2 clipspace position = " + clipspacePosition2);

        // view port #3 parameters
        int width2 = 1394;
        int height2 = 834;

        viewportOpts.put("width", width2);
        viewportOpts.put("height", height2);
        viewportOpts.put("latitude", latitude1);
        viewportOpts.put("longitude", longitude1);

        WebMercatorViewport viewport3 = new WebMercatorViewport(viewportOpts);
        vec4 clipspacePosition3 = viewport3.lngLatToClipspacePosition(lnglat);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] viewport#3 clipspace position = " + clipspacePosition3);

        // view port #4 parameters
        int zoom4 = 5;

        viewportOpts.put("width", width1);
        viewportOpts.put("height", height1);
        viewportOpts.put("latitude", latitude1);
        viewportOpts.put("longitude", longitude1);
        viewportOpts.put("zoom", zoom4);

        WebMercatorViewport viewport4 = new WebMercatorViewport(viewportOpts);
        vec4 clipspacePosition4 = viewport4.lngLatToClipspacePosition(lnglat);
        System.out.println("[test_different_viewports_effects_on_clipspace_positions] viewport#4 clipspace position = " + clipspacePosition4);
    }
}
