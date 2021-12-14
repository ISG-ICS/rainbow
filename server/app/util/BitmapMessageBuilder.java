package util;

import static util.Constants.DOUBLE_BYTES;
import static util.Constants.INT_BYTES;

public class BitmapMessageBuilder {

    /**
     * ---- header ----
     *  progress  totalTime  treeTime   aggTime  msgType
     * | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 4 BYTES |
     * ---- bitmap header ----
     *   resX      resY      lng0      lat0      lng1      lat1
     * | 4 BYTES | 4 BYTES | 8 BYTES | 8 BYTES | 8 BYTES | 8 BYTES |
     * ---- bitmap data payload ----
     *     i=0, j=0~_resY          i=1, j=0~_resY        ...
     * | ceil(resY / 8) BYTES | ceil(resY / 8) BYTES | ...
     */
    byte[] buffer;

    int resX, resY;
    double lng0, lat0, lng1, lat1;

    int bitmapHeaderSize;
    int bitmapOneLineSize;
    int bitmapNumberOfLines;

    public BitmapMessageBuilder(int _resX, int _resY, double _lng0, double _lat0, double _lng1, double _lat1) {
        resX = _resX;
        resY = _resY;
        lng0 = _lng0;
        lat0 = _lat0;
        lng1 = _lng1;
        lat1 = _lat1;

        bitmapHeaderSize = INT_BYTES * 2 + DOUBLE_BYTES * 4;
        bitmapOneLineSize = (int) Math.ceil(resY / 8.0);
        bitmapNumberOfLines = resX;

        buffer = new byte[Constants.HEADER_SIZE + bitmapHeaderSize + bitmapOneLineSize * bitmapNumberOfLines];

        // tag msgType in the header
        // bitmap message (1)
        int msgType = 1;
        // offset of msgType position in the header
        int j = INT_BYTES + 3 * DOUBLE_BYTES;
        buffer[j+0] = (byte)((msgType >> 24) & 0xff);
        buffer[j+1] = (byte)((msgType >> 16) & 0xff);
        buffer[j+2] = (byte)((msgType >>  8) & 0xff);
        buffer[j+3] = (byte)((msgType >>  0) & 0xff);

        // write the bitmap header
        // resX
        j = Constants.HEADER_SIZE;
        buffer[j+0] = (byte)((resX >> 24) & 0xff);
        buffer[j+1] = (byte)((resX >> 16) & 0xff);
        buffer[j+2] = (byte)((resX >>  8) & 0xff);
        buffer[j+3] = (byte)((resX >>  0) & 0xff);
        // resY
        j = j + INT_BYTES;
        buffer[j+0] = (byte)((resY >> 24) & 0xff);
        buffer[j+1] = (byte)((resY >> 16) & 0xff);
        buffer[j+2] = (byte)((resY >>  8) & 0xff);
        buffer[j+3] = (byte)((resY >>  0) & 0xff);
        // lng0
        j = j + INT_BYTES;
        long lng0L = Double.doubleToRawLongBits(lng0);
        buffer[j+0] = (byte) ((lng0L >> 56) & 0xff);
        buffer[j+1] = (byte) ((lng0L >> 48) & 0xff);
        buffer[j+2] = (byte) ((lng0L >> 40) & 0xff);
        buffer[j+3] = (byte) ((lng0L >> 32) & 0xff);
        buffer[j+4] = (byte) ((lng0L >> 24) & 0xff);
        buffer[j+5] = (byte) ((lng0L >> 16) & 0xff);
        buffer[j+6] = (byte) ((lng0L >>  8) & 0xff);
        buffer[j+7] = (byte) ((lng0L >>  0) & 0xff);
        // lat0
        j = j + DOUBLE_BYTES;
        long lat0L = Double.doubleToRawLongBits(lat0);
        buffer[j+0] = (byte) ((lat0L >> 56) & 0xff);
        buffer[j+1] = (byte) ((lat0L >> 48) & 0xff);
        buffer[j+2] = (byte) ((lat0L >> 40) & 0xff);
        buffer[j+3] = (byte) ((lat0L >> 32) & 0xff);
        buffer[j+4] = (byte) ((lat0L >> 24) & 0xff);
        buffer[j+5] = (byte) ((lat0L >> 16) & 0xff);
        buffer[j+6] = (byte) ((lat0L >>  8) & 0xff);
        buffer[j+7] = (byte) ((lat0L >>  0) & 0xff);
        // lng1
        j = j + DOUBLE_BYTES;
        long lng1L = Double.doubleToRawLongBits(lng1);
        buffer[j+0] = (byte) ((lng1L >> 56) & 0xff);
        buffer[j+1] = (byte) ((lng1L >> 48) & 0xff);
        buffer[j+2] = (byte) ((lng1L >> 40) & 0xff);
        buffer[j+3] = (byte) ((lng1L >> 32) & 0xff);
        buffer[j+4] = (byte) ((lng1L >> 24) & 0xff);
        buffer[j+5] = (byte) ((lng1L >> 16) & 0xff);
        buffer[j+6] = (byte) ((lng1L >>  8) & 0xff);
        buffer[j+7] = (byte) ((lng1L >>  0) & 0xff);
        // lat1
        j = j + DOUBLE_BYTES;
        long lat1L = Double.doubleToRawLongBits(lat1);
        buffer[j+0] = (byte) ((lat1L >> 56) & 0xff);
        buffer[j+1] = (byte) ((lat1L >> 48) & 0xff);
        buffer[j+2] = (byte) ((lat1L >> 40) & 0xff);
        buffer[j+3] = (byte) ((lat1L >> 32) & 0xff);
        buffer[j+4] = (byte) ((lat1L >> 24) & 0xff);
        buffer[j+5] = (byte) ((lat1L >> 16) & 0xff);
        buffer[j+6] = (byte) ((lat1L >>  8) & 0xff);
        buffer[j+7] = (byte) ((lat1L >>  0) & 0xff);
    }

    public void write(boolean[][] bitmap) {
        int window = 0; // java does not support bitwise operation on byte, use int to represent only a byte
        int offset;
        for (int i = 0; i < resX; i ++) {
            for (int j = 0; j < resY; j ++) {
                // get offset of the byte for current bit
                offset = Constants.HEADER_SIZE + bitmapHeaderSize + i * bitmapOneLineSize; // line head
                offset = offset + j / 8; // byte within the line

                if (j % 8 == 0) {
                    if (j >= 8) {
                        // write previous byte
                        buffer[offset - 1] = (byte) window;
                    }
                    window = 0;
                }
                else {
                    window = window << 1;
                }

                if (bitmap[i][j]) {
                    window = window | 1;
                }
            }

            // write the last byte of this line
            offset = Constants.HEADER_SIZE + bitmapHeaderSize + i * bitmapOneLineSize; // line head
            offset = offset + (resY-1) / 8; // byte within the line
            buffer[offset] = (byte) window;
        }
    }

    public byte[] getBuffer() {
        return buffer;
    }
}
