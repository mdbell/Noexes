#include "gecko.h"
#include "errors.h"

//useful macros for reading/writeing to the socket
#define READ_CHECKED(ctx, to) {                                     \
    int i = ctx.conn.read(&to);                                     \
    if( i < 0 )                                                     \
        return MAKERESULT(Module_TCPGecko, TCPGeckoError_iofail);   \
}

#define READ_BUFFER_CHECKED(ctx, buffer, size) {                    \
    int i = ctx.conn.read(buffer, size);                            \
    if( i < 0 )                                                     \
        return MAKERESULT(Module_TCPGecko, TCPGeckoError_iofail);   \
}

#define WRITE_CHECKED(ctx, value) {                                 \
    int i = ctx.conn.write(value);                                  \
    if( i < 0)                                                      \
        return MAKERESULT(Module_TCPGecko, TCPGeckoError_iofail);   \
}

#define WRITE_BUFFER_CHECKED(ctx, buffer, size) {                   \
    int i = ctx.conn.write(buffer, size);                           \
    if( i < 0)                                                      \
        return MAKERESULT(Module_TCPGecko, TCPGeckoError_iofail);   \
}

Result writeCompressed(Gecko::Context& ctx, u32 len) {
    static u8 tmp[GECKO_BUFFER_SIZE * 2];
    u32 pos = 0;

    for(u32 i = 0; i < len;i++){
        u8 value = ctx.buffer[i];
        u8 rle = 1;
        while(rle < 255 && i + 1 < len && ctx.buffer[i + 1] == value){
            rle++;
            i++;
        }
        tmp[pos++] = value;
        tmp[pos++] = rle;
    }
    u8 compressedFlag = pos > len ? 0 : 1;
    WRITE_CHECKED(ctx, compressedFlag);
    WRITE_CHECKED(ctx, len);
    if(!compressedFlag){
        WRITE_BUFFER_CHECKED(ctx, ctx.buffer, len);
    }else{
        WRITE_CHECKED(ctx, pos);
        WRITE_BUFFER_CHECKED(ctx, tmp, pos);
    }
    return 0;
}

//0x01
static Result _status(Gecko::Context& ctx){
    WRITE_CHECKED(ctx, (u8)ctx.status);
    WRITE_CHECKED(ctx, (u8)VER_MAJOR);
    WRITE_CHECKED(ctx, (u8)VER_MINOR);
    WRITE_CHECKED(ctx, (u8)VER_PATCH);
    return 0;
}

//0x02
static Result _poke8(Gecko::Context& ctx){
    u64 ptr;
    u8 val;
    READ_CHECKED(ctx, ptr);
    READ_CHECKED(ctx, val);
    return ctx.dbg.writeMem(val, ptr);
}

//0x03
static Result _poke16(Gecko::Context& ctx){
    u64 ptr;
    u16 val;
    READ_CHECKED(ctx, ptr);
    READ_CHECKED(ctx, val);
    return ctx.dbg.writeMem(val, ptr);
}

//0x04
static Result _poke32(Gecko::Context& ctx){
    u64 ptr;
    u32 val;
    READ_CHECKED(ctx, ptr);
    READ_CHECKED(ctx, val);
    return ctx.dbg.writeMem(val, ptr);
}

//0x05
static Result _poke64(Gecko::Context& ctx){
    u64 ptr;
    u64 val;
    READ_CHECKED(ctx, ptr);
    READ_CHECKED(ctx, val);
    return ctx.dbg.writeMem(val, ptr);
}

//0x06
static Result _readmem(Gecko::Context& ctx){
	Result rc;
	u64 addr;
	u32 size;
    u32 len;

    READ_CHECKED(ctx, addr);				
    READ_CHECKED(ctx, size);
    rc = ctx.dbg.attached();
    WRITE_CHECKED(ctx, rc);
    if(R_SUCCEEDED(rc)){
        while(size > 0){
            len = size < GECKO_BUFFER_SIZE ? size : GECKO_BUFFER_SIZE;
            rc = ctx.dbg.readMem(ctx.buffer, addr, len);
            WRITE_CHECKED(ctx, rc);
        
            if(R_FAILED(rc)){
                break;
            }
            rc = writeCompressed(ctx, len);
            if(R_FAILED(rc)){
                break;
            }
            addr += len;
            size -= len;
        }
    }
    return rc;
}

//0x07
static Result _writemem(Gecko::Context& ctx){
    u64 addr;
    u32 size;
    u32 len;
    Result rc = 0;
    READ_CHECKED(ctx, addr);
    READ_CHECKED(ctx, size);
    rc = ctx.dbg.attached();
    WRITE_CHECKED(ctx, rc);
    if(R_SUCCEEDED(rc)){
        while(size > 0){
            len = size < GECKO_BUFFER_SIZE ? size : GECKO_BUFFER_SIZE;
            READ_BUFFER_CHECKED(ctx, ctx.buffer, len);
            ctx.dbg.writeMem(ctx.buffer, addr, len);
            addr += len;
            size -= len;
        }
    }
    return 0;
}

//0x08
static Result _resume(Gecko::Context& ctx){
    Result rc = ctx.dbg.resume();
    if(R_SUCCEEDED(rc)){
        ctx.status = Gecko::Status::Running;
    }
    return rc;
}

//0x09
static Result _pause(Gecko::Context& ctx){
    Result rc = ctx.dbg.pause();
    if(R_SUCCEEDED(rc)){
        ctx.status = Gecko::Status::Paused;
    }
    return rc;
}

//0x0A
static Result _attach(Gecko::Context& ctx){
    u64 pid;
    READ_CHECKED(ctx, pid);
    Result rc = ctx.dbg.attach(pid);
    if(R_SUCCEEDED(rc)){
        ctx.status = Gecko::Status::Paused;
    }
    return rc;
}

//0x0B
static Result _detatch(Gecko::Context& ctx){
    Result rc = ctx.dbg.detatch();
    if(R_SUCCEEDED(rc)){
        ctx.status = Gecko::Status::Running;
    }
    return rc;
}

//0x0C
static Result _querymem_single(Gecko::Context& ctx){
    Result rc = 0;
    u64 addr;
    MemoryInfo info = {};
    
    READ_CHECKED(ctx, addr);

    rc = ctx.dbg.query(&info, addr);

    WRITE_CHECKED(ctx, info.addr);
    WRITE_CHECKED(ctx, info.size);
    WRITE_CHECKED(ctx, info.type);
    WRITE_CHECKED(ctx, info.perm);
    return rc;
}

//0x0D
static Result _querymem_multi(Gecko::Context& ctx) {
    Result rc = 0;
    u64 addr;
    u32 requestCount;
    u32 count = 0;
    MemoryInfo info = {};
    READ_CHECKED(ctx, addr);
    READ_CHECKED(ctx, requestCount);
           
    for(count = 0; count < requestCount; count++){
        rc =ctx.dbg.query(&info, addr);
        WRITE_CHECKED(ctx, info.addr);
        WRITE_CHECKED(ctx, info.size);
        WRITE_CHECKED(ctx, info.type);
        WRITE_CHECKED(ctx, info.perm);
        WRITE_CHECKED(ctx, rc);
        if(info.type == 0x10 || R_FAILED(rc)){
            break;
        }
        addr += info.size;
    }
    return rc;
}

//0x0E
static Result _current_pid(Gecko::Context& ctx){
    u64 pid;
    Result rc = pmdmntGetApplicationPid(&pid);
    WRITE_CHECKED(ctx, pid);
    return rc;
}

//0x0F
static Result _attached_pid(Gecko::Context& ctx){
    WRITE_CHECKED(ctx, ctx.dbg.attachedPid());
    return 0;
}

//0x10
static Result _list_pids(Gecko::Context& ctx){
    Result rc;
	int maxpids = GECKO_BUFFER_SIZE / sizeof(u64);
	u32 count;
    rc = ctx.dbg.listPids((u64*)ctx.buffer, &count, maxpids);
    WRITE_CHECKED(ctx, count);
    WRITE_BUFFER_CHECKED(ctx, ctx.buffer, count * sizeof(u64));
    return rc;
}

//0x11
static Result _get_titleid(Gecko::Context& ctx){
    Result rc;
    u64 pid;
    u64 title_id;
    
    READ_CHECKED(ctx, pid);
    
	rc = pminfoGetTitleId(&title_id, pid);
	if (R_FAILED(rc)) {
        title_id = 0;
	}
    WRITE_CHECKED(ctx, title_id);
    return rc;
}

//0x12
static Result _disconnect(Gecko::Context& ctx){
    ctx.status = Gecko::Status::Stopping;
    return 0;
}


//0x13
static Result _readmem_multi(Gecko::Context& ctx){
    Result rc = 0;
    u32 size;
    u32 data_size;
    u32 count = 0;
    u64 addr;
    READ_CHECKED(ctx, size);
    READ_CHECKED(ctx, data_size);
    rc = ctx.dbg.attached();
    if(R_SUCCEEDED(rc)){
        if(data_size > GECKO_BUFFER_SIZE){
            rc = MAKERESULT(Module_TCPGecko, TCPGeckoError_buffer_too_small);
        }
    }
    WRITE_CHECKED(ctx, rc);
    if(R_SUCCEEDED(rc)){
        for(count = 0; count < size; count++){
            READ_CHECKED(ctx, addr);
            rc = ctx.dbg.readMem(ctx.buffer, addr, data_size);
            WRITE_CHECKED(ctx, rc);
            if(R_FAILED(rc)){
                break;
            }
            WRITE_BUFFER_CHECKED(ctx,ctx.buffer, data_size);
        }
    }
    return rc;
}

//0x14
static Result _set_breakpoint(Gecko::Context& ctx){
    u32 id;
    u64 addr;
    u64 flags;
    READ_CHECKED(ctx, id);
    READ_CHECKED(ctx, addr);
    READ_CHECKED(ctx, flags);
    return ctx.dbg.setBreakpoint(id, flags, addr);
}

Result cmd_decode(Gecko::Context& ctx, int cmd){
    static Result (*cmds[255])(Gecko::Context&) =   {NULL, _status, _poke8, _poke16, _poke32, _poke64, _readmem,
                                                    _writemem, _resume, _pause, _attach, _detatch, _querymem_single,
                                                    _querymem_multi, _current_pid, _attached_pid, _list_pids,
                                                    _get_titleid, _disconnect, _readmem_multi, _set_breakpoint};
    Result rc = 0;
    if(cmds[cmd]){
        rc = cmds[cmd](ctx);
    }else{
        rc = MAKERESULT(Module_TCPGecko, TCPGeckoError_invalid_cmd);
    }
    WRITE_CHECKED(ctx, rc);
    return rc;
}
