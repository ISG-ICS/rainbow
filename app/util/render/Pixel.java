package util.render;

public class Pixel {
    int i;
    int j;
    byte r;
    byte g;
    byte b;

    public Pixel(int _i, int _j, byte _r, byte _g, byte _b) {
        i = _i;
        j = _j;
        r = _r;
        g = _g;
        b = _b;
    }

    public Pixel(int _i, int _j) {
        i = _i;
        j = _j;
    }
}
