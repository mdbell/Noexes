package me.mdbell.noexs.misc;

import me.mdbell.noexs.core.Result;

import java.util.HashMap;
import java.util.Map;

public final class ResultDecoder {

    private static final int GECKO_MODULE_ID = 349;

    private static final Map<Integer, Module> modules = new HashMap<Integer, Module>(){{
        put(GECKO_MODULE_ID, new Module("TCPGecko", new HashMap<Integer, String>(){{
            put(1, "Init Failed");
            put(2, "Socket Failed");
            put(3, "SocketOpt Failed");
            put(4, "Bind Failed");
            put(5, "Listen Failed");
            put(6, "Accept Failed");
            put(7, "General IO Failure");
            put(8, "Invalid Command");
            put(9, "Not Attached");
            put(10, "Already Attached");
            put(11, "Buffer is too small");
        }}));
    }};

    private ResultDecoder(){

    }

    public static Module lookup(Result r) {
        int mod = r.getModule();
        if(!modules.containsKey(mod)) {
            return null;
        }
        return modules.get(mod);
    }

    public static class Module{
        private String name;
        private Map<Integer, String> messages;
        Module(String name, Map<Integer,String> messages) {
            this.name = name;
            this.messages = messages;
        }

        public String getName(){
            return name;
        }

        public String getMessage(int desc){
            if(messages.containsKey(desc)) {
                return messages.get(desc);
            }
            return "UnknownMessage(" + desc + ")";
        }
    }
}
