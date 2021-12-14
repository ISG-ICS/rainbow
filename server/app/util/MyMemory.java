package util;

import java.util.ArrayList;
import java.util.List;

public class MyMemory {

    static final int mb = 1024*1024;

    public static List<Integer> progressUsedMemory = new ArrayList<>();
    public static List<Integer> porgressTotalMemory = new ArrayList<>();

    public static void printMemory() {

        //Getting the runtime reference from system
        Runtime runtime = Runtime.getRuntime();

        System.out.println("##### Heap utilization statistics [MB] #####");

        //Print used memory
        System.out.println("Used Memory:"
                + (runtime.totalMemory() - runtime.freeMemory()) / mb);

        //Print free memory
        System.out.println("Free Memory:"
                + runtime.freeMemory() / mb);

        //Print total available memory
        System.out.println("Total Memory:" + runtime.totalMemory() / mb);

        //Print Maximum available memory
        System.out.println("Max Memory:" + runtime.maxMemory() / mb);
    }

    public static int getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (int)((runtime.totalMemory() - runtime.freeMemory()) / mb);
    }

    public static int getTotalMemory() {
        Runtime runtime = Runtime.getRuntime();
        return (int)(runtime.totalMemory() / mb);
    }
}
