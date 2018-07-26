package me.mdbell.noexs.core;

public interface Commands {

    int COMMAND_STATUS = 0x01;

    int COMMAND_POKE8 = 0x02;
    int COMMAND_POKE16 = 0x03;
    int COMMAND_POKE32 = 0x04;
    int COMMAND_POKE64 = 0x05;

    int COMMAND_READ = 0x06;
    int COMMAND_WRITE = 0x07;
    int COMMAND_CONTINUE = 0x08;
    int COMMAND_PAUSE = 0x09;
    int COMMAND_ATTACH = 0x0A;
    int COMMAND_DETATCH = 0x0B;
    int COMMAND_QUERY_MEMORY = 0x0C;
    int COMMAND_QUERY_MEMORY_MULTI = 0x0D;
    int COMMAND_CURRENT_PID = 0x0E;
    int COMMAND_GET_ATTACHED_PID = 0x0F;
    int COMMAND_GET_PIDS = 0x10;
    int COMMAND_GET_TITLEID =  0x11;
    int COMMAND_DISCONNECT = 0x12;
    int COMMAND_READ_MULTI = 0x13;
    int COMMAND_SET_BREAKPOINT = 0x14;
}
