package com.fbp.engine.api;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpApiServer {
    private static final int DEFAULT_BACKLOG = 0;
    private final HttpServer server;

    public HttpApiServer(String host, int port) throws IOException {
        this.server = HttpServer.create(
                new InetSocketAddress(host, port),
                DEFAULT_BACKLOG);

        server.createContext("/", new RouterHandler());
    }

    public void start(){
        server.start();
    }
    public void stop(int delay){
        server.stop(delay);
    }
}
