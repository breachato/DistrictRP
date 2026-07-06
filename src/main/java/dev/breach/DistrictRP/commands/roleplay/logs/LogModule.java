package dev.breach.DistrictRP.commands.roleplay.logs;

import java.util.List;
import java.util.UUID;

public interface LogModule {

    String getId();

    String getDisplayName();

    List<LogEntry> fetch(UUID playerFilter, int page, int perPage);

    default int add(UUID player, String playerName, String action) {
        return -1;
    }
}