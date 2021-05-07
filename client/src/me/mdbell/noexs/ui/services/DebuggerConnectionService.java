package me.mdbell.noexs.ui.services;

import javafx.concurrent.ScheduledService;
import javafx.concurrent.Task;
import me.mdbell.noexs.core.IConnection;
import me.mdbell.noexs.io.net.SocketConnection;
import me.mdbell.noexs.ui.models.ConnectionType;

import java.net.InetSocketAddress;
import java.net.Socket;

public class DebuggerConnectionService extends ScheduledService<IConnection> implements IMessageArguments {
    private String host;
    private int port;
    private int timeout = 1000;
    private ConnectionType type;

    private final Object[] args = new Object[4];

    public DebuggerConnectionService() {
        super();
        setMaximumFailureCount(10);
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    protected Task<IConnection> createTask() {
        if (type == ConnectionType.NETWORK) {
            return new Task<>() {
                @Override
                protected IConnection call() throws Exception {
                    args[0] = host;
                    args[1] = port;
                    args[2] = getCurrentFailureCount() + 1;
                    args[3] = getMaximumFailureCount();
                    updateMessage("main.conn.service.conn_attempt");
                    Socket s = new Socket();
                    InetSocketAddress addr = new InetSocketAddress(host, port);
                    s.connect(addr, timeout);
                    s.setTcpNoDelay(true);
                    return new SocketConnection(s);
                }
            };
        }
        return new Task<>() {
            @Override
            protected IConnection call() {
                throw new UnsupportedOperationException("Unsupported connection type:" + type);
            }
        };
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }

    @Override
    public Object[] getMessageArguments() {
        return args;
    }
}
