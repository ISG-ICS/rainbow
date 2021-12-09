package util;

import java.util.*;

public class MyTimer {
    public static Map<String, List<Double>> progressTimer = new HashMap<>();
    public static Map<String, Double> temporaryTimer = new HashMap<>();
    private static Stack<Long> startTimes = new Stack<>();
    private static Queue<Double> durations = new LinkedList<>();

    public static void startTimer() {
        long startTime = System.nanoTime();
        startTimes.push(startTime);
    }

    public static void stopTimer() {
        long endTime = System.nanoTime();
        long startTime = startTimes.pop();
        durations.add((double) (endTime - startTime) / 1000000000.0);
    }

    public static double durationSeconds() {
        return durations.poll();
    }
}
