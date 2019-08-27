package me.mdbell.noexs.ui;

import me.mdbell.noexs.io.net.NetworkConstants;

import java.io.File;
import java.util.prefs.Preferences;

public class Settings {

    private static final String HOST_KEY = "HOST";
    private static final String POINTER_DEPTH_KEY = "PTR_DEPTH";
    private static final String POINTER_OFFSET_KEY = "PTR_OFFSET";
    private static final String POINTER_THREAD_KEY = "PTR_THREADS";

    private static final String MEM_VIEW_ENDIAN_KEY = "MEM_ENDIAN";

    private static final String CHOOSE_FILE_KEY = "CHOOSER_FILE";

    private static final Preferences prefs = Preferences.userNodeForPackage(Settings.class);

    private Settings(){

    }

    public static File getChooserDir(){
        File f = new File(prefs.get(CHOOSE_FILE_KEY, "./"));
        if(!f.exists()) {
            f = new File("./");
        }
        return f;
    }

    public static void setChooserFile(File f){
        prefs.put(CHOOSE_FILE_KEY, f.getAbsolutePath());
    }

    public static String getConnectionHost(){
        return prefs.get(HOST_KEY, NetworkConstants.DEFAULT_HOST);
    }

    public static void setConnectionHost(String host) {
        prefs.put(HOST_KEY, host);
    }

    public static long getPointerOffset() {
        return prefs.getLong(POINTER_OFFSET_KEY, 0x200);
    }

    public static int getPointerDepth() {
        return prefs.getInt(POINTER_DEPTH_KEY, 1);
    }

    public static void setPointerDepth(int depth){
        prefs.putInt(POINTER_DEPTH_KEY, depth);
    }

    public static void setPointerOffset(long value) {
        prefs.putLong(POINTER_OFFSET_KEY, value);
    }

    public static int getPointerThreadCount() {
        return prefs.getInt(POINTER_THREAD_KEY, Runtime.getRuntime().availableProcessors());
    }

    public static boolean shouldSwapEndian(){
        return prefs.getBoolean(MEM_VIEW_ENDIAN_KEY, false);
    }

    public static void setPointerThreadCount(int count){
        prefs.putInt(POINTER_THREAD_KEY, count);
    }

    public static void setSwapEndian(boolean swapEndian) {
        prefs.putBoolean(MEM_VIEW_ENDIAN_KEY, swapEndian);
    }
}
