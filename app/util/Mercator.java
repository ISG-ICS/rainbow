package util;

import model.Point;

public class Mercator {

    /**
     * Note: Mercator project a globe coordinate [lng, lat] onto a [0 ~ 1] coordinate on a continuous plane
     *              ^ 90                   0 ----------------> 1
     *              |                      |
     *              |                      |
     * -180 --------+--------> 180  ==>    |
     *              |                      |
     *              |                      |
     *              |-90                   V 1
     */

    // longitude to spherical mercator in [0..1] range
    public static double lngX(double lng) {
        return lng / 360 + 0.5;
    }

    // latitude to spherical mercator in [0..1] range
    public static double latY(double lat) {
        double sin = Math.sin(lat * Math.PI / 180);
        double y = (0.5 - 0.25 * Math.log((1 + sin) / (1 - sin)) / Math.PI);
        return y < 0 ? 0 : y > 1 ? 1 : y;
    }

    // spherical mercator to longitude
    public static double xLng(double x) {
        return (x - 0.5) * 360;
    }

    // spherical mercator to latitude
    public static double yLat(double y) {
        double y2 = (180 - y * 360) * Math.PI / 180;
        return 360 * Math.atan(Math.exp(y2)) / Math.PI - 90;
    }

    public static Point lngLatToXY(Point point) {
        point.setX(lngX(point.getX()));
        point.setY(latY(point.getY()));
        return point;
    }
}
