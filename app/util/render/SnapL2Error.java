package util.render;

import javafx.util.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SnapL2Error implements IErrorMetric {

    /**
     * Compute L1 error between two snap renderings
     *
     * a rendering
     * - Use 1-D array to simulate a 2-D bitmap (0 or 1)
     *   suppose 2-D bitmap has dimension lengths: side * side
     *   [i][j] = i * side + j
     *
     * @param _rendering1
     * @param _rendering2
     * @param _resolution
     * @return
     */
    @Override
    public double error(byte[] _rendering1, byte[] _rendering2, int _resolution) {
        int side = _resolution;
        // compute L1 error between pixels with 0 / 1 bit
        double error = 0.0;
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                byte bit1 = _rendering1[i * side + j];
                byte bit2 = _rendering2[i * side + j];
                error += (bit1 - bit2) * (bit1 - bit2);
            }
        }
        return error;
    }

    /**
     * Compute L1 error between two snap renderings
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
        // compute L1 error between pixels with 0 / 1 bit
        double error = 0.0;
        for (Pixel pixel: shorter) {
            byte bit1 = 1;
            Pair<Integer, Integer> pixelIndex = new Pair<>(pixel.i, pixel.j);

            byte bit2 = 0;
            if (longerMap.containsKey(pixelIndex)) {
                // remove the matched pixel from longer map
                longerMap.remove(pixelIndex);
                bit2 = 1;
            }

            error += (bit1 - bit2) * (bit1 - bit2);
        }

        // loop the rest of the longer map that has not matched pixel in shorter list
        byte bit1 = 1;
        byte bit2 = 0;
        error += (bit1 - bit2) * (bit1 - bit2) * longerMap.size();

        return error;
    }
}
