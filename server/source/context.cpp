#include "gecko.h"

void Gecko::Context::reset(){
   exit = false;
   status = Stopping;
   conn.disconnect();
   dbg.detatch();
}