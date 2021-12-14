package util;

import static util.Mercator.*;

public class BitmapNodeHandler implements I2DIndexNodeHandler {

    boolean[][] bitmap;
    int resX;
    int resY;
    double x0;
    double y1;
    double x1;
    double y0;
    double deltaX;
    double deltaY;

    public BitmapNodeHandler(int _resX, int _resY, double _lng0, double _lat0, double _lng1, double _lat1) {
        resX = _resX;
        resY = _resY;
        bitmap = new boolean[resX][resY];
        x0 = lngX(_lng0);
        y1 = latY(_lat0);
        x1 = lngX(_lng1);
        y0 = latY(_lat1);
        deltaX = x1 - x0;
        deltaY = y1 - y0;
    }

    public boolean[][] getBitmap() {
        return bitmap;
    }

    @Override
    public void handleNode(double x, double y, short duplicates) {
        // find pixel index of this point based on resolution resX * resY
        int i = (int) Math.floor((x - x0) * resX / deltaX);
        int j = (int) Math.floor((y - y0) * resY / deltaY);

        // skip point outside given screen view
        if (i < 0 || i >= resX || j < 0 || j >= resY) {
            return;
        }

        // set the bit to be true
        bitmap[i][j] = true;
    }
}
