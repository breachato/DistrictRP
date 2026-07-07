package dev.breach.DistrictRP.functions.servermode;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ServerModeChatListener implements Listener {

    private final DistrictRP plugin;
    private final ServerModeManager manager;

    public ServerModeChatListener(DistrictRP plugin, ServerModeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        String fmt = manager.getChatFormat();
        if (fmt == null || fmt.isEmpty() || fmt.equalsIgnoreCase("null")) return;

        String rank = getRankSymbol(event.getPlayer());
        String vip = getVipSymbol(event.getPlayer());

        String finalFmt = fmt
                .replace("%rank%", rank == null ? "" : rank)
                .replace("%vip%", vip == null ? "" : vip)
                .replace("%player%", "%1$s")
                .replace("%message%", "%2$s");

        event.setFormat(MessageUtils.color(finalFmt));
    }

    private String getRankSymbol(org.bukkit.entity.Player p) {
        var cfg = plugin.getConfig();
        var ranks = cfg.getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String rk : cfg.getStringList("stafflist.order")) {
            String perm = ranks.getString(rk + ".permission", "");
            if (!perm.isEmpty() && p.hasPermission(perm)) {
                return ranks.getString(rk + ".symbol", "") + " ";
            }
        }
        return "";
    }

    private String getVipSymbol(org.bukkit.entity.Player p) {
        var cfg = plugin.getConfig();
        var vip = cfg.getConfigurationSection("vip-symbols");
        if (vip == null) return "";
        for (String vk : cfg.getStringList("vip-symbols.order")) {
            String perm = vip.getString(vk + ".permission", "");
            if (!perm.isEmpty() && p.hasPermission(perm)) {
                return " " + vip.getString(vk + ".symbol", "");
            }
        }
        return "";
    }
}