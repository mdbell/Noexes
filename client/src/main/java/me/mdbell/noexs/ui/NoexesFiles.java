package me.mdbell.noexs.ui;

import java.io.File;
import java.io.IOException;

public class NoexesFiles {

    private NoexesFiles(){

    }

    private static final File tmp = new File("./tmp");

    static{
        if(!tmp.exists()) {
            tmp.mkdirs();
        }
    }

    public static File createTempFile(String ext) throws IOException {
        File res = new File(tmp, "" + System.currentTimeMillis() + "." + ext);
        res.createNewFile();
        return res;
    }

    public static File createTempDir(){
        File res = new File(tmp, "" + System.currentTimeMillis());
        res.mkdirs();
        return res;
    }

    public static File getTempDir() {
        return tmp;
    }
}
