/**
 * @file gecko.h
 * @brief Switch useful tools.
 * @copyright your dad
 */
#pragma once

#include <switch.h>

#include "debug.h"
#include "net.h"

#define VER_MAJOR (1)
#define VER_MINOR (1)
#define VER_PATCH (0)

#define GECKO_BUFFER_SIZE (2048 * 4)

namespace Gecko{
    
    enum Status{
        Stopping,
        Running,
        Paused,
        Searching
    };

    struct Context{
        Connection conn;
        Status status; // current gecko state
        Debugger dbg;
        bool exit;
        u8 buffer[GECKO_BUFFER_SIZE]; // use this buffer for reading/writing chunks of data
        void reset();
};

};

void logString(const char* str, int len);

#define printf(fmt, ...) {                                          \
    char buffer[2048];                                              \
    logString(buffer, snprintf(buffer, 2048,fmt, ##__VA_ARGS__));   \
}

Result cmd_decode(Gecko::Context& ctx, int cmd);