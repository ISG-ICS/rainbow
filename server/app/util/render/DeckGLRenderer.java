package util.render;

import javafx.util.Pair;
import model.Point;
import util.UnsignedByte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeckGLRenderer implements IRenderer {

    public static double SMOOTH_EDGE_RADIUS = 0.5;

    int radiusInPixels;

    public DeckGLRenderer(int radiusInPixels) {
        this.radiusInPixels = radiusInPixels;
        System.out.println("[DeckGLAggregator] initializing with { radiusInPixels: " + radiusInPixels + "}.");
    }

    /**
     * Create a rendering with background color
     *
     *  - Use 1-D array to simulate a 3-D array
     *    suppose 3-D array has dimension lengths: side * side * 3
     *    [i][j][k] = i * side * 3 + j * 3 + k
     *
     * @param _resolution
     * @return
     */
    @Override
    public byte[] createRendering(int _resolution) {
        int side = _resolution + 2 * (radiusInPixels + 1);
        byte[] rendering = new byte[side * side * 3];
        // init rendering with BG_COLOR
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                rendering[i * side * 3 + j * 3 + 0] = BG_COLOR[0]; // R
                rendering[i * side * 3 + j * 3 + 1] = BG_COLOR[1]; // G
                rendering[i * side * 3 + j * 3 + 2] = BG_COLOR[2]; // B
            }
        }
        return rendering;
    }

    /**
     * Render a new point onto the given rendering
     *
     * @param rendering - Use 1-D array to simulate a 3-D array
     *                    suppose 3-D array has dimension lengths: side * side * 3
     *                    [i][j][k] = i * side * 3 + j * 3 + k
     * @param _cX
     * @param _cY
     * @param _halfDimension
     * @param _resolution
     * @param point
     * @return boolean - if render the point on given rendering does not change the result, return false; else return true;
     */
    @Override
    public boolean render(byte[] rendering, double _cX, double _cY, double _halfDimension, int _resolution, Point point) {
        int side = _resolution + 2 * (radiusInPixels + 1);
        boolean isDifferent = false;
        double pixelLength = 2 * _halfDimension / (double)_resolution;
        double px, py;
        double distanceToCenter;
        double inCircle;
        double alpha;
        int or, og, ob;
        int r, g, b;
        // render the point on the background as a new rendering
        // boundary of the rendering
        double left = _cX - _halfDimension - (radiusInPixels + 1) * pixelLength; // may be overflow to negative
        double top = _cY - _halfDimension - (radiusInPixels + 1) * pixelLength; // may be overflow to negative
        // (1) find the circumscribed square of the point
        double csLeft = point.getX() - radiusInPixels * pixelLength;
        double csRight = point.getX() + radiusInPixels * pixelLength;
        double csTop = point.getY() - radiusInPixels * pixelLength;
        double csBottom = point.getY() + radiusInPixels * pixelLength;
        // (2) pixel indexes of the circumscribed square
        int csLeftPixel = (int)((csLeft - left) / pixelLength);
        int csRightPixel = (int)((csRight - left) / pixelLength);
        int csTopPixel = (int)((csTop - top) / pixelLength);
        int csBottomPixel = (int)((csBottom - top) / pixelLength);
        // (3) traverse the pixels within the circumscribed square
        for (int i = csLeftPixel; i <= csRightPixel; i ++) {
            for (int j = csTopPixel; j <= csBottomPixel; j ++) {
                px = left + (i + 0.5) * pixelLength;
                py = top + (j + 0.5) * pixelLength;
                // current pixel to center point's distance in units of # of pixels
                distanceToCenter = (Math.sqrt(Math.pow(px - point.getX(), 2) + Math.pow(py - point.getY(), 2))) / pixelLength;
                // how dense this pixel color
                inCircle = smoothEdge(distanceToCenter, radiusInPixels);
                alpha = 1.0 * inCircle;
                or = UnsignedByte.toInt(rendering[i * side * 3 + j * 3 + 0]);
                og = UnsignedByte.toInt(rendering[i * side * 3 + j * 3 + 1]);
                ob = UnsignedByte.toInt(rendering[i * side * 3 + j * 3 + 2]);
                // apply blend function DST_COLOR = SRC_COLOR * SRC_ALPHA + DST_COLOR * (1 - SRC_ALPHA)
                r = (int) (UnsignedByte.toInt(COLOR[0]) * alpha + or * (1.0 - alpha)); // R
                g = (int) (UnsignedByte.toInt(COLOR[1]) * alpha + og * (1.0 - alpha)); // G
                b = (int) (UnsignedByte.toInt(COLOR[2]) * alpha + ob * (1.0 - alpha)); // B
                if (or != r) {
                    isDifferent = true;
                    rendering[i * side * 3 + j * 3 + 0] = UnsignedByte.toByte(r);
                }
                if (og != g) {
                    isDifferent = true;
                    rendering[i * side * 3 + j * 3 + 1] = UnsignedByte.toByte(g);
                }
                if (ob != b) {
                    isDifferent = true;
                    rendering[i * side * 3 + j * 3 + 2] = UnsignedByte.toByte(b);
                }
            }
        }

        return isDifferent;
    }

    /**
     * Render a new point onto the given rendering
     *
     * @param rendering - a list of pixels [i, j, r, g, b]
     * @param _cX
     * @param _cY
     * @param _halfDimension
     * @param _resolution
     * @param point
     * @return boolean - if render the point on given rendering does not change the result, return false; else return true;
     */
    @Override
    public boolean render(List<Pixel> rendering, double _cX, double _cY, double _halfDimension, int _resolution, Point point) {

        // build hash map for rendering pixels
        Map<Pair<Integer, Integer>, Pixel> renderingMap = new HashMap<>();
        for (Pixel pixel: rendering) {
            renderingMap.put(new Pair<>(pixel.i, pixel.j), pixel);
        }

        boolean isDifferent = false;
        double pixelLength = 2 * _halfDimension / (double)_resolution;
        double px, py;
        double distanceToCenter;
        double inCircle;
        double alpha;
        int or, og, ob;
        int r, g, b;
        // render the point on the background as a new rendering
        // boundary of the rendering
        double left = _cX - _halfDimension - (radiusInPixels + 1) * pixelLength; // may be overflow to negative
        double top = _cY - _halfDimension - (radiusInPixels + 1) * pixelLength; // may be overflow to negative
        // (1) find the circumscribed square of the point
        double csLeft = point.getX() - radiusInPixels * pixelLength;
        double csRight = point.getX() + radiusInPixels * pixelLength;
        double csTop = point.getY() - radiusInPixels * pixelLength;
        double csBottom = point.getY() + radiusInPixels * pixelLength;
        // (2) pixel indexes of the circumscribed square
        int csLeftPixel = (int)((csLeft - left) / pixelLength);
        int csRightPixel = (int)((csRight - left) / pixelLength);
        int csTopPixel = (int)((csTop - top) / pixelLength);
        int csBottomPixel = (int)((csBottom - top) / pixelLength);
        // (3) traverse the pixels within the circumscribed square
        for (int i = csLeftPixel; i <= csRightPixel; i ++) {
            for (int j = csTopPixel; j <= csBottomPixel; j ++) {
                px = left + (i + 0.5) * pixelLength;
                py = top + (j + 0.5) * pixelLength;
                // current pixel to center point's distance in units of # of pixels
                distanceToCenter = (Math.sqrt(Math.pow(px - point.getX(), 2) + Math.pow(py - point.getY(), 2))) / pixelLength;
                // how dense this pixel color
                inCircle = smoothEdge(distanceToCenter, radiusInPixels);
                alpha = 1.0 * inCircle;
                // get the original pixel color in rendering
                Pair<Integer, Integer> pixelIndex = new Pair<>(i, j);
                boolean existingPixel = false;
                if (renderingMap.containsKey(pixelIndex)) existingPixel = true;
                or = existingPixel? UnsignedByte.toInt(renderingMap.get(pixelIndex).r) : UnsignedByte.toInt(BG_COLOR[0]);
                og = existingPixel? UnsignedByte.toInt(renderingMap.get(pixelIndex).g) : UnsignedByte.toInt(BG_COLOR[1]);
                ob = existingPixel? UnsignedByte.toInt(renderingMap.get(pixelIndex).b) : UnsignedByte.toInt(BG_COLOR[2]);
                // apply blend function DST_COLOR = SRC_COLOR * SRC_ALPHA + DST_COLOR * (1 - SRC_ALPHA)
                r = (int) (UnsignedByte.toInt(COLOR[0]) * alpha + or * (1.0 - alpha)); // R
                g = (int) (UnsignedByte.toInt(COLOR[1]) * alpha + og * (1.0 - alpha)); // G
                b = (int) (UnsignedByte.toInt(COLOR[2]) * alpha + ob * (1.0 - alpha)); // B
                // existing pixel, apply the new color
                if (existingPixel) {
                    if (or != r) {
                        isDifferent = true;
                        renderingMap.get(pixelIndex).r = UnsignedByte.toByte(r);
                    }
                    if (og != g) {
                        isDifferent = true;
                        renderingMap.get(pixelIndex).g = UnsignedByte.toByte(g);
                    }
                    if (ob != b) {
                        isDifferent = true;
                        renderingMap.get(pixelIndex).b = UnsignedByte.toByte(b);
                    }
                }
                // otherwise, create a new pixel and add to the rendering
                else {
                    isDifferent = true;
                    rendering.add(new Pixel(i, j, UnsignedByte.toByte(r), UnsignedByte.toByte(g), UnsignedByte.toByte(b)));
                }
            }
        }

        return isDifferent;
    }

    @Override
    public int realResolution(int _resolution) {
        return _resolution + 2 * (radiusInPixels + 1);
    }

    private double smoothEdge(double edge, double x) {
        return smoothStep(edge - SMOOTH_EDGE_RADIUS, edge + SMOOTH_EDGE_RADIUS, x);
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
}
