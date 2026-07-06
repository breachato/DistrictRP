package dev.breach.DistrictRP.functions;

import dev.breach.DistrictRP.DistrictRP;

public class LobbyModeManager {

    private final DistrictRP plugin;

    public LobbyModeManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public boolean isEnabled() {
        return plugin.getConfig().getBoolean("lobby-mode.enabled", false);
    }
}