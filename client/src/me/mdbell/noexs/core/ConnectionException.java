package me.mdbell.noexs.core;

public class ConnectionException extends RuntimeException{

    public ConnectionException(Exception e) {
        super(e);
    }

    public ConnectionException(String s) {
        super(s);
    }

    public ConnectionException(String s, Result rc) {
        super(s); //TODO parse the rc module and desc.
    }

    public ConnectionException(Result rc){
        //TODO parse the rc module and desc
        super(rc.toString());
    }
}
