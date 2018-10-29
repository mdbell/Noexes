#include "debug.h"
#include "errors.h"


#define RETURN_NOT_ATTACHED() {       \
    Result rc = attached();         \
    if(R_FAILED(rc)){               \
        return rc;                  \
    }                               \
}

void Gecko::Debugger::addEventCallback(std::function<Result(Gecko::DebugEvent&)> callback){
    callbacks.push_back(callback);
}

Result Gecko::Debugger::fireEvent(Gecko::DebugEvent& event){
    Result rc = 0;
    std::list<std::function<Result(Gecko::DebugEvent&)>>::iterator it;
    for (it=callbacks.begin(); it!=callbacks.end() && R_SUCCEEDED(rc); ++it) {
        rc = (*it)(event);
    }
    return rc;
}

Result Gecko::Debugger::flushEvents(){
    RETURN_NOT_ATTACHED();
    Result rc = 0;
    do{
        Gecko::DebugEvent event;
		rc = svcGetDebugEvent((u8*)&event, handle);
        if(R_SUCCEEDED(rc)){
            fireEvent(event);
        }
	}while(R_SUCCEEDED(rc));
    return rc;
}

u64 Gecko::Debugger::attachedPid(){
    return pid;
}

Result Gecko::Debugger::attach(u64 pid){
    Result rc = attached();
    if(R_SUCCEEDED(rc)){
        return MAKERESULT(Module_TCPGecko, TCPGeckoError_already_attached);
    }
    rc = svcDebugActiveProcess(&handle, pid);
    if(R_SUCCEEDED(rc)){
        this->pid = pid;
    }
    return rc;
}

Result Gecko::Debugger::attached(){
    if(!handle){
        return MAKERESULT(Module_TCPGecko, TCPGeckoError_not_attached);
    }
    return 0;
}

Result Gecko::Debugger::detatch(){
    RETURN_NOT_ATTACHED();
    Result rc = svcCloseHandle(handle);
    if(R_SUCCEEDED(rc)){
        pid = 0;
        handle = 0;
    }
    return rc;
}

Result Gecko::Debugger::resume(){
    RETURN_NOT_ATTACHED();
    flushEvents();
    return svcContinueDebugEvent(handle, 4 | 2 | 1, 0, 0);
}

Result Gecko::Debugger::pause(){
    RETURN_NOT_ATTACHED();
    return svcBreakDebugProcess(handle);
}

Result Gecko::Debugger::query(MemoryInfo* to, u64 addr){
    RETURN_NOT_ATTACHED();
    u32 pageinfo; // ignored
    return svcQueryDebugProcessMemory(to, &pageinfo, handle, addr);
}

Result Gecko::Debugger::listPids(u64* pids, u32* count, u32 max){
    return svcGetProcessList(count, pids, max);
}

Result Gecko::Debugger::readMem(void *buffer, u64 addr, u64 size){
    RETURN_NOT_ATTACHED();
    return svcReadDebugProcessMemory(buffer, handle, addr, size);
}

Result Gecko::Debugger::writeMem(void *buffer, u64 addr, u64 size){
    RETURN_NOT_ATTACHED();
    return svcWriteDebugProcessMemory(handle, buffer, addr, size);
}

Result Gecko::Debugger::setBreakpoint(u32 id, u64 flag, u64 value){
    if(value == 0){
        value = handle;
    }
    return svcSetHardwareBreakpoint(id, flag, value);
}

Gecko::Debugger::~Debugger(){
    Result rc = attached();
    if(R_SUCCEEDED(rc)){
        rc = detatch();
    }
}