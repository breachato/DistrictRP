package dev.breach.DistrictRP.staffpanel.http;

import dev.breach.DistrictRP.staffpanel.StaffPanelManager;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class StaffPanelHttpServer {

    private final StaffPanelManager mgr;
    private final String bind;
    private final int port;
    private final String apiToken;
    private com.sun.net.httpserver.HttpServer server;

    public StaffPanelHttpServer(StaffPanelManager mgr, String bind, int port, String apiToken) {
        this.mgr = mgr;
        this.bind = bind;
        this.port = port;
        this.apiToken = apiToken;
    }

    public void start() throws Exception {
        this.server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(bind, port), 0);
        StaffPanelHandlers h = new StaffPanelHandlers(mgr, apiToken);

        server.createContext("/api/auth/login", h::login);
        server.createContext("/api/departments", h::departments);
        server.createContext("/api/staffers", h::staffers);
        server.createContext("/api/counters", h::counters);
        server.createContext("/api/flags", h::flags);
        server.createContext("/api/columns", h::columns);
        server.createContext("/api/stats", h::stats);
        server.createContext("/api/sync-staff", h::syncStaff);
        server.createContext("/api/staff/manual", h::manualStaff);

        server.setExecutor(Executors.newFixedThreadPool(6));
        server.start();
    }

    public void stop() {
        if (server != null) server.stop(0);
    }
}