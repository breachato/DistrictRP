package dev.breach.cherryCore.functions;

import dev.breach.cherryCore.CherryCore;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class WhitelistManager {

    private final CherryCore plugin;

    public WhitelistManager(CherryCore plugin) {
        this.plugin = plugin;
    }

    public boolean inWhitelist(Player p) {
        if (p.hasPermission("perms.place"))           return true;
        if (p.hasPermission("perms.admin"))           return true;
        if (p.hasPermission("perms.*"))               return true;
        if (p.hasPermission("cherrycore.buildworld")) return true;
        List<String> list = plugin.getDataManager().perms().getStringList("whitelist");
        return list.contains(p.getUniqueId().toString());
    }

    public boolean add(OfflinePlayer p) {
        List<String> list = plugin.getDataManager().perms().getStringList("whitelist");
        String uuid = p.getUniqueId().toString();
        if (list.contains(uuid)) return false;
        list.add(uuid);
        plugin.getDataManager().perms().set("whitelist", list);
        plugin.getDataManager().perms().set("names." + uuid, p.getName());
        plugin.getDataManager().savePerms();
        return true;
    }

    public boolean remove(OfflinePlayer p) {
        List<String> list = plugin.getDataManager().perms().getStringList("whitelist");
        String uuid = p.getUniqueId().toString();
        if (!list.contains(uuid)) return false;
        list.remove(uuid);
        plugin.getDataManager().perms().set("whitelist", list);
        plugin.getDataManager().perms().set("names." + uuid, null);
        plugin.getDataManager().savePerms();
        return true;
    }

    public List<String> all() {
        return plugin.getDataManager().perms().getStringList("whitelist");
    }

    public String name(String uuid) {
        return plugin.getDataManager().perms().getString("names." + uuid, uuid);
    }

    public void rememberName(Player p) {
        plugin.getDataManager().perms().set("names." + p.getUniqueId(), p.getName());
        plugin.getDataManager().savePerms();
    }
}