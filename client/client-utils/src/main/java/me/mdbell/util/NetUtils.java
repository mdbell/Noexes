package me.mdbell.util;

public class NetUtils {

    private static final String[] BYTE_SUFFEXES = {"B", "KB", "MB", "GB", "TB"};

    private NetUtils(){

    }

    public static String formatSize(double size){
        int i = 0;
        while(i < BYTE_SUFFEXES.length && size > 1024) {
            size /= 1024;
            i++;
        }
        size *= 100;
        size = Math.floor(size);
        size /= 100;
        return size + BYTE_SUFFEXES[i];
    }
}
