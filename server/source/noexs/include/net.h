/**
 * @file net.h
 * @brief Networking code for TCPGecko
 * @copyright 2018 Matthew Bell (mdbell)
 */
#pragma once

#define DEFAULT_LISTEN_PORT (7331)

#define DISCONNECTED_FD (-1)

namespace Gecko{
    class Connection{
        int sockfd; // socket file descriptor
        public:
        bool connect(int port = DEFAULT_LISTEN_PORT);
        bool connected();
        void disconnect();
        int read(void *buffer, int len); //recvwait
        int read(); // get_cmd
        template <typename T>
        int read(T* to){    return read(to, sizeof(T)); }

        int write(const void *buffer, int len); //sendwait
        template <typename T>
        int write(T from){    return write(&from, sizeof(T)); }

        Connection() : sockfd(DISCONNECTED_FD) {};
        ~Connection();
    };

};