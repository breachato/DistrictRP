package dev.breach.DistrictRP.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.breach.DistrictRP.DistrictRP;

import java.sql.Connection;
import java.sql.SQLException;

public class MariaDBDataStore implements DataStore {

    private final DistrictRP plugin;
    private final DatabaseConfig cfg;
    private HikariDataSource dataSource;
    private boolean ready = false;

    public MariaDBDataStore(DistrictRP plugin, DatabaseConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;
    }

    @Override
    public StorageType getType() {
        return StorageType.MARIADB;
    }

    @Override
    public void initialize() throws Exception {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("[Database] Driver MariaDB non trovato!");
            throw e;
        }

        HikariConfig hc = new HikariConfig();
        hc.setJdbcUrl(cfg.getJdbcUrl());
        hc.setUsername(cfg.getUsername());
        hc.setPassword(cfg.getPassword());
        hc.setDriverClassName("org.mariadb.jdbc.Driver");
        hc.setPoolName("DistrictRP-Pool");
        hc.setMaximumPoolSize(cfg.getPoolSize());
        hc.setMinimumIdle(Math.max(1, cfg.getPoolSize() / 2));
        hc.setConnectionTimeout(cfg.getConnectionTimeoutMs());
        hc.setIdleTimeout(cfg.getIdleTimeoutMs());
        hc.setMaxLifetime(cfg.getMaxLifetimeMs());
        hc.setLeakDetectionThreshold(60000);

        hc.addDataSourceProperty("cachePrepStmts", "true");
        hc.addDataSourceProperty("prepStmtCacheSize", "250");
        hc.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hc.addDataSourceProperty("useServerPrepStmts", "true");
        hc.addDataSourceProperty("useLocalSessionState", "true");
        hc.addDataSourceProperty("rewriteBatchedStatements", "true");
        hc.addDataSourceProperty("cacheResultSetMetadata", "true");
        hc.addDataSourceProperty("cacheServerConfiguration", "true");
        hc.addDataSourceProperty("elideSetAutoCommits", "true");
        hc.addDataSourceProperty("maintainTimeStats", "false");

        try {
            this.dataSource = new HikariDataSource(hc);

            try (Connection conn = dataSource.getConnection()) {
                if (conn == null || !conn.isValid(5)) {
                    throw new SQLException("Connessione test fallita.");
                }
            }

            this.ready = true;
            plugin.getLogger().info("[Database] MariaDB connesso a "
                    + cfg.getHost() + ":" + cfg.getPort() + "/" + cfg.getDatabase()
                    + " (pool size: " + cfg.getPoolSize() + ")");
        } catch (Throwable t) {
            plugin.getLogger().severe("[Database] Errore connessione MariaDB: " + t.getMessage());
            if (dataSource != null) dataSource.close();
            this.ready = false;
            throw t;
        }
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("[Database] Pool MariaDB chiuso.");
        }
        this.ready = false;
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (!ready || dataSource == null || dataSource.isClosed()) {
            throw new SQLException("DataSource non pronto.");
        }
        return dataSource.getConnection();
    }

    @Override
    public String getTablePrefix() {
        return cfg.getTablePrefix();
    }

    @Override
    public boolean isReady() {
        return ready && dataSource != null && !dataSource.isClosed();
    }

    public HikariDataSource getDataSource() {
        return dataSource;
    }
}