package dev.breach.DistrictRP.database;

import java.sql.Connection;
import java.sql.SQLException;

public interface DataStore {

    StorageType getType();

    void initialize() throws Exception;

    void shutdown();

    default boolean isSqlBased() {
        return getType() == StorageType.MARIADB;
    }

    default Connection getConnection() throws SQLException {
        throw new UnsupportedOperationException("Non-SQL storage does not support getConnection()");
    }

    default String getTablePrefix() {
        return "";
    }

    boolean isReady();
}