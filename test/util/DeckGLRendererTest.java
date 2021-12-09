package util;

import model.Point;
import util.render.DeckGLRenderer;

public class DeckGLRendererTest {
    public static void main(String[] args) {
        DeckGLRenderer renderer = new DeckGLRenderer(1);

        int resolution = 64;

        byte[] rendering = renderer.createRendering(resolution);

        Point point = new Point(0.18738888888888888, 0.3833203873543357);
        double ncX = 0.1796875;
        double ncY = 0.3828125;
        double halfDimension = 0.0078125;

        renderer.render(rendering, ncX, ncY, halfDimension, resolution, point);

        printRenderingGray("test", rendering, resolution);
    }

    public static void printRenderingGray(String name, byte[] _rendering, int _resolution) {
        int side = _resolution;
        System.out.println("========== " + name + "==========");
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                int r = UnsignedByte.toInt(_rendering[i * side * 3 + j * 3 + 0]);
                int g = UnsignedByte.toInt(_rendering[i * side * 3 + j * 3 + 1]);
                int b = UnsignedByte.toInt(_rendering[i * side * 3 + j * 3 + 2]);
                // gray scaling formula = (0.3 * R) + (0.59 * G) + (0.11 * B)
                int gray = (int) ((0.3 * r) + (0.59 * g) + (0.11 * b));
                if (j > 0) System.out.print(" ");
                System.out.print(gray);
            }
            System.out.println();
        }
    }
}
