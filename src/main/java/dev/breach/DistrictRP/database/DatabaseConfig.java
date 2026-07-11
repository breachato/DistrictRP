package dev.breach.DistrictRP.database;

import org.bukkit.configuration.ConfigurationSection;

public class DatabaseConfig {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final String tablePrefix;
    private final int poolSize;
    private final long connectionTimeoutMs;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;
    private final boolean useSSL;

    public DatabaseConfig(ConfigurationSection sec) {
        if (sec == null) {
            this.host = "localhost";
            this.port = 3306;
            this.database = "districtrp";
            this.username = "root";
            this.password = "";
            this.tablePrefix = "drp_";
            this.poolSize = 10;
            this.connectionTimeoutMs = 30000;
            this.idleTimeoutMs = 600000;
            this.maxLifetimeMs = 1800000;
            this.useSSL = false;
        } else {
            this.host = sec.getString("host", "localhost");
            this.port = sec.getInt("port", 3306);
            this.database = sec.getString("database", "districtrp");
            this.username = sec.getString("username", "root");
            this.password = sec.getString("password", "");
            this.tablePrefix = sec.getString("table-prefix", "drp_");
            this.poolSize = sec.getInt("pool-size", 10);
            this.connectionTimeoutMs = sec.getLong("connection-timeout-ms", 30000);
            this.idleTimeoutMs = sec.getLong("idle-timeout-ms", 600000);
            this.maxLifetimeMs = sec.getLong("max-lifetime-ms", 1800000);
            this.useSSL = sec.getBoolean("use-ssl", false);
        }
    }

    public String getJdbcUrl() {
        return "jdbc:mariadb://" + host + ":" + port + "/" + database
                + "?useSSL=" + useSSL
                + "&autoReconnect=true"
                + "&characterEncoding=utf8"
                + "&useUnicode=true";
    }

    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getDatabase() { return database; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getTablePrefix() { return tablePrefix; }
    public int getPoolSize() { return poolSize; }
    public long getConnectionTimeoutMs() { return connectionTimeoutMs; }
    public long getIdleTimeoutMs() { return idleTimeoutMs; }
    public long getMaxLifetimeMs() { return maxLifetimeMs; }
    public boolean isUseSSL() { return useSSL; }
}