package util.render;

import java.util.List;

public interface IErrorMetric {
    double error(byte[] _rendering1, byte[] _rendering2, int _resolution);

    double error(List<Pixel> _rendering1, List<Pixel> _rendering2, int _resolution);
}
