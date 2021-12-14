package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {
    public String key; // key of the algorithm
    public int zoom;
    public double[] bbox; //x0, y0, x1, y1
    public String algorithm; // "KDTreeExplorer" / "DataAggregator" / "QuadTreeAggregator" / "QuadTreeExplorerAggregator"
    public int resX; // frontend resolution x
    public int resY; // frontend resolution y
    public String aggregator; // for DataAggregator and QuadTreeAggregators: "gl-pixel" / "leaflet" / "deck-gl"
    public int sampleSize; // target sample size, <=0 - disabled
    public int samplePercentage; // target sample percentage (1 ~ 100), 0 - disabled
}
