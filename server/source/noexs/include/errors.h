#pragma once

/* Custom Gecko Specific Error Codes */
enum {
    Module_TCPGecko=349
};

enum {
	TCPGeckoError_initfail=1,
	TCPGeckoError_socketfail,
	TCPGeckoError_socketopt,
	TCPGeckoError_bindfail,
	TCPGeckoError_listenfail,
	TCPGeckoError_acceptfail,
    TCPGeckoError_iofail,
    TCPGeckoError_invalid_cmd,
    TCPGeckoError_not_attached,
    TCPGeckoError_already_attached,
    TCPGeckoError_buffer_too_small,
    TCPGeckoError_invalid_watchpoint,
    TCPGeckoError_watchpoint_not_set,
    TCPGeckoError_no_free_watchpoints,
    TCPGeckoError_incorrect_debugger
};