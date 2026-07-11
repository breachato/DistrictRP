package dev.breach.DistrictRP.database;

import dev.breach.DistrictRP.DistrictRP;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class DatabaseTable {

    protected final DistrictRP plugin;
    protected final DataStore store;
    protected final String tableName;

    public DatabaseTable(DistrictRP plugin, DataStore store, String rawTableName) {
        this.plugin = plugin;
        this.store = store;
        this.tableName = store.getTablePrefix() + rawTableName;
    }

    public String getTableName() {
        return tableName;
    }

    protected abstract String getCreateTableQuery();

    public void createIfNotExists() {
        if (!store.isSqlBased()) return;
        try (Connection conn = store.getConnection();
             PreparedStatement stmt = conn.prepareStatement(getCreateTableQuery())) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("[DB] Creazione tabella '" + tableName + "' fallita: " + e.getMessage());
        }
    }

    protected void migrate() {}

    protected Connection getConnection() throws SQLException {
        return store.getConnection();
    }
}