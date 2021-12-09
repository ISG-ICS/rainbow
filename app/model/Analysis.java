package model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Analysis {
    /**
     * distance [key] [zoom] [point1_id] [point2_id]
     *  - key: String
     *  - zoom: int
     *  - point1_id: int
     *  - point2_id: int
     *
     * randindex [clusterKey1] [clusterKey2] [zoom]
     *  - clusterKey1: String
     *  - clusterKey2: String
     *  - zoom: int
     */
    public String objective;
    public String[] arguments;
//    public String cluster; // key of the cluster
//    public int zoom;
//    public int p1; // point1 id
//    public int p2; // point2 id
}
