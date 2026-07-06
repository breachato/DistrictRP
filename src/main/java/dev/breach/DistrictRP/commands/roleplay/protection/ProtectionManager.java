package dev.breach.DistrictRP.commands.roleplay.protection;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProtectionManager {

    private final DistrictRP plugin;

    public ProtectionManager(DistrictRP plugin) {
        this.plugin = plugin;
    }

    public boolean isNoBuild(String world) {
        return plugin.getConfig().getBoolean("protection.worlds." + world + ".no-build", false);
    }

    public boolean isNoInteract(String world) {
        return plugin.getConfig().getBoolean("protection.worlds." + world + ".no-interact", false);
    }

    public void setNoBuild(String world, boolean state) {
        plugin.getConfig().set("protection.worlds." + world + ".no-build", state);
        plugin.saveConfig();
    }

    public void setNoInteract(String world, boolean state) {
        plugin.getConfig().set("protection.worlds." + world + ".no-interact", state);
        plugin.saveConfig();
    }

    public boolean isWorldConfigured(String world) {
        return plugin.getConfig().isConfigurationSection("protection.worlds." + world);
    }

    public void ensureWorld(String world) {
        if (!isWorldConfigured(world)) {
            plugin.getConfig().set("protection.worlds." + world + ".no-build", false);
            plugin.getConfig().set("protection.worlds." + world + ".no-interact", false);
            plugin.getConfig().set("protection.worlds." + world + ".whitelisted-players", new ArrayList<>());
            plugin.getConfig().set("protection.worlds." + world + ".allowed-interactions", new ArrayList<>());
            plugin.saveConfig();
        }
    }

    public boolean isWhitelisted(String world, Player player) {
        List<String> list = plugin.getConfig().getStringList("protection.worlds." + world + ".whitelisted-players");
        return list.contains(player.getName().toLowerCase());
    }

    public boolean addWhitelist(String world, String playerName) {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(
                "protection.worlds." + world + ".whitelisted-players"));
        String lower = playerName.toLowerCase();
        if (list.contains(lower)) return false;
        list.add(lower);
        plugin.getConfig().set("protection.worlds." + world + ".whitelisted-players", list);
        plugin.saveConfig();
        return true;
    }

    public boolean removeWhitelist(String world, String playerName) {
        List<String> list = new ArrayList<>(plugin.getConfig().getStringList(
                "protection.worlds." + world + ".whitelisted-players"));
        String lower = playerName.toLowerCase();
        if (!list.contains(lower)) return false;
        list.remove(lower);
        plugin.getConfig().set("protection.worlds." + world + ".whitelisted-players", list);
        plugin.saveConfig();
        return true;
    }

    public List<String> getWhitelist(String world) {
        return plugin.getConfig().getStringList("protection.worlds." + world + ".whitelisted-players");
    }

    public Set<Material> getAllowedInteractions(String world) {
        Set<Material> set = new HashSet<>();
        List<String> list = plugin.getConfig().getStringList("protection.worlds." + world + ".allowed-interactions");
        for (String s : list) {
            try {
                Material mat = Material.valueOf(s.toUpperCase());
                set.add(mat);
            } catch (IllegalArgumentException ignored) {}
            if (s.toUpperCase().contains("SHULKER_BOX")) {
                for (Material m : Material.values()) {
                    if (m.name().endsWith("SHULKER_BOX")) set.add(m);
                }
            }
            if (s.toUpperCase().contains("BUTTON")) {
                for (Material m : Material.values()) {
                    if (m.name().endsWith("_BUTTON")) set.add(m);
                }
            }
        }
        return set;
    }

    public List<String> getAllowedInteractionsList(String world) {
        return plugin.getConfig().getStringList("protection.worlds." + world + ".allowed-interactions");
    }

    public boolean addInteraction(String world, String material) {
        List<String> list = new ArrayList<>(getAllowedInteractionsList(world));
        String upper = material.toUpperCase();
        if (list.contains(upper)) return false;
        list.add(upper);
        plugin.getConfig().set("protection.worlds." + world + ".allowed-interactions", list);
        plugin.saveConfig();
        return true;
    }

    public boolean removeInteraction(String world, String material) {
        List<String> list = new ArrayList<>(getAllowedInteractionsList(world));
        String upper = material.toUpperCase();
        if (!list.contains(upper)) return false;
        list.remove(upper);
        plugin.getConfig().set("protection.worlds." + world + ".allowed-interactions", list);
        plugin.saveConfig();
        return true;
    }

    public boolean canBypass(Player player) {
        String perm = plugin.getConfig().getString("protection.permission-bypass", "DistrictRP.protection.bypass");
        return player.hasPermission(perm);
    }

    public boolean showParticle() {
        return plugin.getConfig().getBoolean("protection.particle-on-deny", true);
    }

    public String getParticleType() {
        return plugin.getConfig().getString("protection.particle-type", "CAMPFIRE_SIGNAL_SMOKE");
    }

    public int getParticleCount() {
        return plugin.getConfig().getInt("protection.particle-count", 5);
    }

    public double getParticleSpeed() {
        return plugin.getConfig().getDouble("protection.particle-speed", 0.05);
    }
}