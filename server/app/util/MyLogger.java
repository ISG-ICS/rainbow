package util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MyLogger {
    private static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void info(Class c, String message) {
        System.out.println("[" + sdf.format(new Date()) + "][" + c.getName() + "] " + message);
    }

    public static void error(Class c, String message) {
        System.err.println("[" + sdf.format(new Date()) + "][" + c.getName() + "] " + message);
    }
}
