package util;

public class Constants {

    // Map
    public static int MIN_ZOOM = 0;
    public static int MAX_ZOOM = 18;

    // Data
    public static double MIN_LONGITUDE;
    public static double MIN_LATITUDE;
    public static double MAX_LONGITUDE;
    public static double MAX_LATITUDE;
    public static double MIN_X;
    public static double MIN_Y;
    public static double MAX_X;
    public static double MAX_Y;

    // Database
    public static String DB_URL;
    public static String DB_USERNAME;
    public static String DB_PASSWORD;
    public static String DB_TABLENAME;

    // Serialization
    public static String DATASET_NAME;

    // Message
    public static int DOUBLE_BYTES = 8;
    public static int INT_BYTES = 4;
    // ---- header ----
    //  progress  totalTime  treeTime   aggTime  msgType   binary data payload
    // | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 4 BYTES | ...
    public static int HEADER_SIZE = INT_BYTES + 3 * DOUBLE_BYTES + INT_BYTES;
    public static int MSG_TYPE = 0;

    public static int RADIUS_IN_PIXELS = 1;

    public static int TILE_RESOLUTION = 1;

    public static String SAMPLING_METHOD = "stratified";

    public static double STOP_CRITERIA = 100;

    // For new RAQuadTree
    public static int NODE_SAMPLE_SIZE = 1;
    public static int NODE_RESOLUTION = 1;
    public static int DEFAULT_SAMPLE_SIZE = 100000; // 100K

    public static String RENDERING_FUNCTION = "snap";
    public static String ERROR_FUNCTION = "L1";
}
