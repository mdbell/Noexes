#include <switch.h>
#include <string.h>
#include "errors.h"
#include "gecko.h"

extern "C" {
	extern u32 __start__;

	u32 __nx_applet_type = AppletType_None;

	//#define INNER_HEAP_SIZE 0x834000 // Arbitrary heap size. 
	#define INNER_HEAP_SIZE 0x41A000 // Reduced heap size. 
	size_t nx_inner_heap_size = INNER_HEAP_SIZE;
	char   nx_inner_heap[INNER_HEAP_SIZE];

	void __libnx_initheap(void);
	void __appInit(void);
	void __appExit(void);
}

static Gecko::Context g_Context;
static FILE* g_debugFile = NULL;

void __libnx_initheap(void) {
	void*  addr = nx_inner_heap;
	size_t size = nx_inner_heap_size;

	/* Newlib */
	extern char* fake_heap_start;
	extern char* fake_heap_end;

	fake_heap_start = (char*)addr;
	fake_heap_end   = (char*)addr + size;
}

void __appInit(void) {
	Result rc;
	/* Initialize services */
	rc = smInitialize();
	if (R_FAILED(rc)) {
		fatalSimple(MAKERESULT(Module_Libnx, LibnxError_InitFail_SM));
	}

	rc = ldrDmntInitialize();
	if (R_FAILED(rc)) {
		fatalSimple(MAKERESULT(Module_Libnx, LibnxError_AlreadyInitialized));
	}

	rc = pmdmntInitialize();
	if (R_FAILED(rc)) {
		fatalSimple(MAKERESULT(Module_Libnx, LibnxError_NotInitialized));
	}

	rc = socketInitialize(socketGetDefaultInitConfig());
	if (R_FAILED(rc)) {
		fatalSimple(MAKERESULT(Module_TCPGecko, TCPGeckoError_initfail));
	}
	
	rc= pminfoInitialize();
	if (R_FAILED(rc)) {
		fatalSimple(rc);
	}
    
    rc = fsInitialize();
    if (R_FAILED(rc)) {
        fatalSimple(MAKERESULT(Module_Libnx, LibnxError_InitFail_FS));
    }
    
    rc = fsdevMountSdmc();
    if (R_FAILED(rc)) {
        fatalSimple(rc); // maybe set a variable like noSd or something? It doesn't HAVE to log.
    }
}

void __appExit(void) {
	/* Cleanup services. */
    fsdevUnmountAll();
    fsExit();
    pminfoExit();
    socketExit();
    pmdmntExit();
    ldrDmntExit();
	smExit();
}

void logString(const char* str, int len){
    if(g_debugFile){
        fprintf(g_debugFile, str);
        fflush(g_debugFile);
    }
}

static int net_main(Gecko::Context& ctx) {
    int cmd;		
	cmd = ctx.conn.read();
    if(cmd < 0){
        return -1;
    }
    Result rc = cmd_decode(ctx, cmd);
    if(rc != MAKERESULT(Module_TCPGecko, TCPGeckoError_iofail)){
        return 1;
    }
    return 0;
}

static Result _eventCallback(Gecko::DebugEvent event){
    switch(event.event_type){
        case 0:
        {
            Gecko::ProcessAttachData pad = event.data.proc_attach;
            printf("ProcessAttachEvent(title_id:%08lx, pid:%08lx, name:'%s', mmu_flags:%d)\r\n", pad.title_id, pad.pid, pad.name, pad.mmu_flags);
        }
        break;
        case 1:
        {
            Gecko::ThreadAttachData tad = event.data.thread_attach;
            printf("ThreadAttachEvent(thread_id:%08lx, tls_pointer:%08lx, entry_point:%08lx)\r\n", tad.thread_id, tad.tls_pointer, tad.entry_point);
        }
        break;
        default:
        printf("UnknownEvent(type:%d Data:", event.event_type);
        for(u32 i = 0; i < DEBUG_DATA_SIZE; i++){
            printf("%02X", event.data.raw[i]);
        }
        printf(")\r\n");
    }
    return 0;
}



int main(int argc, char **argv)
{
    //g_debugFile = fopen("Log.txt", "w");
    g_Context.dbg.addEventCallback(_eventCallback);
    
    while(appletMainLoop() && !g_Context.exit){
        g_Context.reset();
        if(g_Context.conn.connect()){
            g_Context.status = Gecko::Status::Running;
            while(appletMainLoop() && !g_Context.exit && g_Context.status && net_main(g_Context) > 0) {
                g_Context.dbg.flushEvents();
                svcSleepThread(0);
            }
        }
    }
    if(g_debugFile){
        fclose(g_debugFile);
        g_debugFile = NULL;
    }
    g_Context.reset();
    g_Context.exit = true;
	return 0;
}