#include <arpa/inet.h>
#include <sys/socket.h>
#include <unistd.h>

#include "errors.h"
#include "gecko.h"
#include "net.h"

static void kill(int& sockfd){
    if(sockfd != DISCONNECTED_FD){
        shutdown(sockfd, SHUT_WR);
        close(sockfd);
        sockfd = DISCONNECTED_FD;
    }
}

bool Gecko::Connection::connect(int port){
    int sockfd = -1, clientfd = -1, ret;
    struct sockaddr_in addr;
    
    disconnect();
    
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = htons(port);

    sockfd = ret = socket(AF_INET, SOCK_STREAM, IPPROTO_TCP);
    if(ret < 0) {
        fatalSimple(MAKERESULT(Module_TCPGecko, TCPGeckoError_socketfail));
        return false;
    }
    
    int yes = 1;
    ret = setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(yes));
    if(ret != 0)
    {
        fatalSimple(MAKERESULT(Module_TCPGecko, TCPGeckoError_socketopt));
        return false;
    }
    
    ret = bind(sockfd, (struct sockaddr *)&addr, 16);
    if (ret < 0) {
        fatalSimple(MAKERESULT(Module_TCPGecko, TCPGeckoError_bindfail));
        return false;
    }
    
    ret = listen(sockfd, 5);
    if (ret < 0) {
        fatalSimple(MAKERESULT(Module_TCPGecko, TCPGeckoError_listenfail));
        return false;
    }
    
    socklen_t len = 16;
    clientfd = ret = accept(sockfd, (struct sockaddr *)&addr, &len);
    if (ret == -1) {
        kill(sockfd);
        return false;
    }
    
    kill(sockfd);
    
    this->sockfd = clientfd;
    return true;
}

bool Gecko::Connection::connected(){
    return this->sockfd != DISCONNECTED_FD;
}

void Gecko::Connection::disconnect(){
    if(connected()){
        kill(sockfd);
    }
}

int Gecko::Connection::read(void *buffer, int len) {
    int ret;
    u8* ptr = (u8*)buffer;
	while (len > 0) {
		ret = recv(this->sockfd, ptr, len, 0);
		if (ret < 0)
			goto error;
		len -= ret;
		ptr += ret;
	}
	return 0;
error:
	return ret;
}

int Gecko::Connection::read(){
    u8 buffer;
	int ret;
    ret = recv(this->sockfd, &buffer, sizeof(u8), 0);
	if (ret < 0) return ret;
	if (ret == 0) return -1;
	return buffer;
}

int Gecko::Connection::write(const void *buffer, int len) {
    int ret;
    u8* ptr = (u8*)buffer;
	while (len > 0) {
		ret = send(this->sockfd, ptr, len, 0);
		if(ret < 0)
			goto error;
		len -= ret;
        ptr += ret;
	}
	return 0;
error:
	return ret;
}

Gecko::Connection::~Connection(){
    disconnect();
}

