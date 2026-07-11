package dev.breach.DistrictRP.commands.roleplay.plot;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlotBlockerListener implements Listener {

    private static final List<String> BLOCKED = Arrays.asList(
            "plot", "p", "ps", "plotsquared"
    );

    private final DistrictRP plugin;

    public PlotBlockerListener(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (!plugin.getConfig().getBoolean("plot.block-ps-commands", true)) return;

        String bypassPerm = plugin.getConfig().getString(
                "plot.bypass-block-permission", "DistrictRP.plot.psbypass");
        if (event.getPlayer().hasPermission(bypassPerm)) return;

        String msg = event.getMessage();
        if (!msg.startsWith("/")) return;

        String cmd = msg.substring(1);
        int spaceIdx = cmd.indexOf(' ');
        String cmdName = (spaceIdx >= 0 ? cmd.substring(0, spaceIdx) : cmd).toLowerCase(Locale.ROOT);

        int colonIdx = cmdName.indexOf(':');
        if (colonIdx >= 0) cmdName = cmdName.substring(colonIdx + 1);

        if (!BLOCKED.contains(cmdName)) return;

        event.setCancelled(true);

        String args = spaceIdx >= 0 ? cmd.substring(spaceIdx) : "";
        String redirect = "/plots" + args;

        event.getPlayer().performCommand(redirect.substring(1));
    }
}