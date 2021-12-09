package util;

import java.math.BigInteger;

import static util.Mercator.*;

public class MemoryTest {
    public static void main(String[] args) {
        MyMemory.printMemory();

        // double bytes
        System.out.println(Double.BYTES);
        // int bytes
        System.out.println(Integer.BYTES);

        System.out.println("lngX(-180) = " + lngX(-180));
        System.out.println("lngX(180) = "  + lngX(180));
        System.out.println("latY(-90) = " + latY(-90));
        System.out.println("latY(90) = " + latY(90));

        int j = 0;
        int j1 = j / 8;
        System.out.println("j1 = " + j1);

        // Creating BigInteger object
        BigInteger bigInt = new BigInteger("1223699864552382464");

        // create a byte array
        byte b1[];
        b1 = bigInt.toByteArray();

        // print result
        System.out.print("ByteArray of BigInteger "
                + bigInt + " is");

        for (int i = 0; i < b1.length; i++) {
            System.out.format(" "
                            + "0x%02X",
                    b1[i]);
        }
    }
}
