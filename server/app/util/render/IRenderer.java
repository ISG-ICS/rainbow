package util.render;

import model.Point;
import util.UnsignedByte;

import java.util.List;

public interface IRenderer {

    byte[] COLOR = {
            UnsignedByte.toByte(0),
            UnsignedByte.toByte(0),
            UnsignedByte.toByte(255)
    }; // blue
    byte[] BG_COLOR = {
            UnsignedByte.toByte(221),
            UnsignedByte.toByte(221),
            UnsignedByte.toByte(221)
    }; // gray

    byte[] createRendering(int _resolution);

    boolean render(byte[] rendering, double _cX, double _cY, double _halfDimension, int _resolution, Point point);

    boolean render(List<Pixel> rendering, double _cX, double _cY, double _halfDimension, int _resolution, Point point);

    int realResolution(int _resolution);
}
