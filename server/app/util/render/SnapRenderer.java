package util.render;

import javafx.util.Pair;
import model.Point;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapRenderer implements IRenderer {

    /**
     * Create a snap rendering with all bits 0
     *
     *  - Use 1-D array to simulate a 2-D bitmap (0 or 1)
     *    suppose 2-D bitmap has dimension lengths: side * side
     *    [i][j] = i * side + j
     *
     * @param _resolution
     * @return
     */
    @Override
    public byte[] createRendering(int _resolution) {
        int side = _resolution;
        byte[] rendering = new byte[side * side];
        // init rendering with bit 0
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                rendering[i * side + j] = 0;
            }
        }
        return rendering;
    }

    /**
     * Render a new point onto the given snap rendering
     *
     * @param rendering - Use 1-D array to simulate a 2-D bitmap (0 or 1)
     *                    suppose 2-D bitmap has dimension lengths: side * side
     *                    [i][j] = i * side + j
     * @param _cX
     * @param _cY
     * @param _halfDimension
     * @param _resolution
     * @param point
     * @return boolean - if render the point on given rendering does not change the result, return false; else return true;
     */
    @Override
    public boolean render(byte[] rendering, double _cX, double _cY, double _halfDimension, int _resolution, Point point) {
        int side = _resolution;
        boolean isDifferent = false;
        double pixelLength = 2 * _halfDimension / (double)_resolution;
        // boundary of the rendering
        double left = _cX - _halfDimension; // may be overflow to negative
        double top = _cY - _halfDimension; // may be overflow to negative
        // pixel index of the point
        int i = (int)((point.getX() - left) / pixelLength);
        int j = (int)((point.getY() - top) / pixelLength);
        byte obit = rendering[i * side + j];
        if (obit == 0) {
            isDifferent = true;
            rendering[i * side + j] = 1;
        }

        return isDifferent;
    }

    /**
     * Render a new point onto the given snap rendering
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
        // boundary of the rendering
        double left = _cX - _halfDimension; // may be overflow to negative
        double top = _cY - _halfDimension; // may be overflow to negative
        // pixel index of the point
        int i = (int)((point.getX() - left) / pixelLength);
        int j = (int)((point.getY() - top) / pixelLength);
        Pair<Integer, Integer> pixelIndex = new Pair<>(i, j);
        if (!renderingMap.containsKey(pixelIndex)) {
            isDifferent = true;
            rendering.add(new Pixel(i, j));
        }

        return isDifferent;
    }

    @Override
    public int realResolution(int _resolution) {
        return _resolution;
    }
}
