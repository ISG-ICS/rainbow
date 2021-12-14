package util.render;

import javafx.util.Pair;
import util.UnsignedByte;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class L2Error implements IErrorMetric {

    /**
     * Compute L2 error between two renderings
     *
     * a rendering
     * - Use 1-D array to simulate a 3-D array
     *   suppose 3-D array has dimension lengths: side * side * 3
     *   [i][j][k] = i * side * 3 + j * 3 + k
     *
     * @param _rendering1
     * @param _rendering2
     * @param _resolution
     * @return
     */
    @Override
    public double error(byte[] _rendering1, byte[] _rendering2, int _resolution) {
        int side = _resolution;
        // compute squared error between pixels with gray scaling
        double error = 0.0;
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                int r1 = UnsignedByte.toInt(_rendering1[i * side * 3 + j * 3 + 0]);
                int g1 = UnsignedByte.toInt(_rendering1[i * side * 3 + j * 3 + 1]);
                int b1 = UnsignedByte.toInt(_rendering1[i * side * 3 + j * 3 + 2]);
                // gray scaling formula = (0.3 * R) + (0.59 * G) + (0.11 * B)
                int gray1 = (int) ((0.3 * r1) + (0.59 * g1) + (0.11 * b1));
                int r2 = UnsignedByte.toInt(_rendering2[i * side * 3 + j * 3 + 0]);
                int g2 = UnsignedByte.toInt(_rendering2[i * side * 3 + j * 3 + 1]);
                int b2 = UnsignedByte.toInt(_rendering2[i * side * 3 + j * 3 + 2]);
                int gray2 = (int) ((0.3 * r2) + (0.59 * g2) + (0.11 * b2));
                error += (gray1 - gray2) * (gray1 - gray2);
            }
        }
        return error;
    }

    /**
     * Compute L2 error between two renderings
     *
     * a rendering
     * - a list of pixels [i, j, r, g, b]
     *
     * @param _rendering1
     * @param _rendering2
     * @param _resolution
     * @return
     */
    @Override
    public double error(List<Pixel> _rendering1, List<Pixel> _rendering2, int _resolution) {

        // find the longer list to build a hash map
        List<Pixel> longer;
        List<Pixel> shorter;
        if (_rendering1.size() > _rendering2.size()) {
            longer = _rendering1;
            shorter = _rendering2;
        }
        else {
            longer = _rendering2;
            shorter = _rendering1;
        }

        Map<Pair<Integer, Integer>, Pixel> longerMap = new HashMap<>();
        for (Pixel pixel: longer) {
            longerMap.put(new Pair<>(pixel.i, pixel.j), pixel);
        }

        // loop the shorter list,
        // compute squared error between pixels with gray scaling
        double error = 0.0;
        for (Pixel pixel: shorter) {
            Pair<Integer, Integer> pixelIndex = new Pair<>(pixel.i, pixel.j);
            int r1 = UnsignedByte.toInt(pixel.r);
            int g1 = UnsignedByte.toInt(pixel.g);
            int b1 = UnsignedByte.toInt(pixel.b);
            // gray scaling formula = (0.3 * R) + (0.59 * G) + (0.11 * B)
            int gray1 = (int) ((0.3 * r1) + (0.59 * g1) + (0.11 * b1));

            int r2, g2, b2;
            if (longerMap.containsKey(pixelIndex)) {
                // remove the matched pixel from longer map
                Pixel pixel2 = longerMap.remove(pixelIndex);
                r2 = UnsignedByte.toInt(pixel2.r);
                g2 = UnsignedByte.toInt(pixel2.g);
                b2 = UnsignedByte.toInt(pixel2.b);
            }
            else {
                r2 = UnsignedByte.toInt(IRenderer.BG_COLOR[0]);
                g2 = UnsignedByte.toInt(IRenderer.BG_COLOR[1]);
                b2 = UnsignedByte.toInt(IRenderer.BG_COLOR[2]);
            }
            int gray2 = (int) ((0.3 * r2) + (0.59 * g2) + (0.11 * b2));
            error += (gray1 - gray2) * (gray1 - gray2);
        }

        // loop the rest of the longer map that has not matched pixel in shorter list
        for (Pixel pixel: longerMap.values()) {
            int r1 = UnsignedByte.toInt(pixel.r);
            int g1 = UnsignedByte.toInt(pixel.g);
            int b1 = UnsignedByte.toInt(pixel.b);
            // gray scaling formula = (0.3 * R) + (0.59 * G) + (0.11 * B)
            int gray1 = (int) ((0.3 * r1) + (0.59 * g1) + (0.11 * b1));

            int r2 = UnsignedByte.toInt(IRenderer.BG_COLOR[0]);
            int g2 = UnsignedByte.toInt(IRenderer.BG_COLOR[1]);
            int b2 = UnsignedByte.toInt(IRenderer.BG_COLOR[2]);
            int gray2 = (int) ((0.3 * r2) + (0.59 * g2) + (0.11 * b2));
            error += (gray1 - gray2) * (gray1 - gray2);
        }

        return error;
    }
}
