package util;

import model.I2DPoint;

import java.util.List;

public interface I2DIndex<PointType> {

    void insert(PointType point);

    void load(PointType[] points);

    void delete(PointType point);

    List<PointType> within(I2DPoint center, double radius);

    List<PointType> range(I2DPoint leftBottom, I2DPoint rightTop);

    int range(I2DPoint leftBottom, I2DPoint rightTop, I2DIndexNodeHandler nodeHandler);

    void print();
}
