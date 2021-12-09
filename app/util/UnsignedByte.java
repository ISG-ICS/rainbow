package util;

public class UnsignedByte {
    public static int toInt(byte from) {
        return 0xFF & (int) from;
    }

    public static byte toByte(int from) {
        return (byte) from;
    }
}
