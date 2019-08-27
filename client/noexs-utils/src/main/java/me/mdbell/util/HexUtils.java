package me.mdbell.util;

public class HexUtils {

    private HexUtils(){

    }

    public static String formatAddress(long addr) {
        return pad('0', 10, Long.toUnsignedString(addr, 16).toUpperCase());
    }

    public static String formatSize(long size) {
        return "0x" + pad('0', 10,
                Long.toUnsignedString(size, 16).toUpperCase());
    }

    public static String pad(char with, int len, String str) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < len) {
            sb.insert(0, with);
        }
        return sb.toString();
    }

    public static String formatInt(int value) {
        return pad('0', 8, Integer.toUnsignedString(value, 16).toUpperCase());
    }

    public static String formatTitleId(long titleId) {
        return pad('0', 16, Long.toUnsignedString(titleId, 16).toUpperCase());
    }

    public static String formatAccess(int access){
        StringBuilder sb = new StringBuilder();

        if ((access & 1) != 0) {
            sb.append('R');
        } else {
            sb.append('-');
        }

        if ((access & 2) != 0) {
            sb.append('W');
        } else {
            sb.append('-');
        }

        if ((access & 4) != 0) {
            sb.append('X');
        } else {
            sb.append('-');
        }

        return sb.toString();
    }
}
