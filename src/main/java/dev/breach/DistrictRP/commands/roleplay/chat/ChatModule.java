package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.functions.MessageUtils;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;

public class ChatModule implements Listener {

    private final DistrictRP plugin;
    private final RPProfileManager profiles;

    public ChatModule(DistrictRP plugin, RPProfileManager profiles) {
        this.plugin = plugin;
        this.profiles = profiles;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        String message = event.getMessage();

        boolean lobbyMode = plugin.getLobbyModeManager() != null && plugin.getLobbyModeManager().isEnabled();

        if (lobbyMode) {
            handleLobbyChat(event, sender, message);
            return;
        }

        if (!plugin.getConfig().getBoolean("chat.proximity.enabled", true)) return;

        double radius = plugin.getConfig().getDouble("chat.proximity.radius", 15);
        event.setCancelled(true);

        String formatRaw = plugin.getConfig().getString("chat.formats.normal",
                "&7%player% &8» &f%message%");
        RPProfile profile = profiles.get(sender.getUniqueId());
        String rpName = profile.hasRpName() ? profile.getRpName() : sender.getName();

        String legacy = formatRaw
                .replace("%player%", rpName)
                .replace("%rp_name%", rpName)
                .replace("%message%", message);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            legacy = PlaceholderAPI.setPlaceholders(sender, legacy);
        }
        legacy = MessageUtils.color(legacy);

        BaseComponent[] component = buildHoverComponent(sender, profile, legacy, false);

        Location loc = sender.getLocation();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getWorld().equals(loc.getWorld())) continue;
            if (p.equals(sender)) {
                p.spigot().sendMessage(component);
                continue;
            }
            if (p.getLocation().distance(loc) <= radius) {
                p.spigot().sendMessage(component);
            }
        }
    }

    private void handleLobbyChat(AsyncPlayerChatEvent event, Player sender, String message) {
        event.setCancelled(true);

        String rankSymbol = getRankSymbol(sender);
        String vipSuffix = getVipSuffix(sender);

        String formatRaw = plugin.getConfig().getString("lobby-mode.format",
                "%rank%&f%player%%vip% &8» &7%message%");

        String legacy = formatRaw
                .replace("%rank%", rankSymbol.isEmpty() ? "" : rankSymbol + " ")
                .replace("%player%", sender.getName())
                .replace("%vip%", vipSuffix.isEmpty() ? "" : " " + vipSuffix)
                .replace("%message%", message);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            legacy = PlaceholderAPI.setPlaceholders(sender, legacy);
        }
        legacy = MessageUtils.color(legacy);

        RPProfile profile = profiles.get(sender.getUniqueId());
        BaseComponent[] component = buildHoverComponent(sender, profile, legacy, true);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.spigot().sendMessage(component);
        }
    }

    private String getRankSymbol(Player player) {
        ConfigurationSection ranks = plugin.getConfig().getConfigurationSection("stafflist.ranks");
        if (ranks == null) return "";
        for (String order : plugin.getConfig().getStringList("stafflist.order")) {
            ConfigurationSection r = ranks.getConfigurationSection(order);
            if (r == null) continue;
            String perm = r.getString("permission", "");
            String symbol = r.getString("symbol", "");
            if (!perm.isEmpty() && player.hasPermission(perm) && symbol != null && !symbol.isEmpty()) {
                return symbol;
            }
        }
        return "";
    }

    private String getVipSuffix(Player player) {
        ConfigurationSection vips = plugin.getConfig().getConfigurationSection("vip-symbols");
        if (vips == null) return "";
        for (String order : plugin.getConfig().getStringList("vip-symbols.order")) {
            ConfigurationSection v = vips.getConfigurationSection(order);
            if (v == null) continue;
            String perm = v.getString("permission", "");
            String symbol = v.getString("symbol", "");
            if (perm.isEmpty() || symbol == null || symbol.isEmpty()) continue;

            boolean hasExplicit = player.getEffectivePermissions().stream()
                    .anyMatch(pai -> pai.getPermission().equalsIgnoreCase(perm) && pai.getValue());

            if (hasExplicit) {
                return symbol;
            }
        }
        return "";
    }

    private BaseComponent[] buildHoverComponent(Player sender, RPProfile profile, String legacyMessage, boolean lobbyMode) {
        boolean hoverEnabled = plugin.getConfig().getBoolean("chat.hover.enabled", true);
        TextComponent[] parts = fromLegacy(legacyMessage);

        if (!hoverEnabled) return parts;

        String rpName = profile.hasRpName() ? profile.getRpName() : sender.getName();
        String cf = sender.getName();

        List<String> hoverLines;
        if (lobbyMode) {
            hoverLines = plugin.getConfig().getStringList("lobby-mode.hover-lines");
            if (hoverLines.isEmpty()) {
                hoverLines = List.of(
                        "&7Nome: &f" + cf,
                        "",
                        "&#FCD05Cᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴄᴏᴘɪᴀʀᴇ ɪʟ ɴᴏᴍᴇ"
                );
            }
        } else if (profile.hasRpName()) {
            hoverLines = plugin.getConfig().getStringList("chat.hover.lines");
            if (hoverLines.isEmpty()) {
                hoverLines = List.of(
                        "&7Nome: &f" + rpName,
                        "&7Codice Fiscale: &f" + cf,
                        "",
                        "&#FCD05Cᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴄᴏᴘɪᴀʀᴇ ɪʟ ɴᴏᴍᴇ"
                );
            }
        } else {
            hoverLines = plugin.getConfig().getStringList("chat.hover.lines-no-rp");
            if (hoverLines.isEmpty()) {
                hoverLines = List.of(
                        "&7Nome: &f" + cf,
                        "",
                        "&#FCD05Cᴄʟɪᴄᴄᴀ ᴘᴇʀ ᴄᴏᴘɪᴀʀᴇ ɪʟ ɴᴏᴍᴇ"
                );
            }
        }

        StringBuilder hoverText = new StringBuilder();
        for (int i = 0; i < hoverLines.size(); i++) {
            String line = hoverLines.get(i)
                    .replace("%rp_name%", rpName)
                    .replace("%player%", cf)
                    .replace("%cf%", cf);
            hoverText.append(MessageUtils.color(line));
            if (i < hoverLines.size() - 1) hoverText.append("\n");
        }

        HoverEvent hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new Text(TextComponent.fromLegacyText(hoverText.toString())));
        ClickEvent click = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, cf);

        for (TextComponent part : parts) {
            part.setHoverEvent(hover);
            part.setClickEvent(click);
        }
        return parts;
    }

    private TextComponent[] fromLegacy(String legacy) {
        BaseComponent[] base = TextComponent.fromLegacyText(legacy);
        TextComponent[] out = new TextComponent[base.length];
        for (int i = 0; i < base.length; i++) {
            if (base[i] instanceof TextComponent tc) {
                out[i] = tc;
            } else {
                TextComponent tc = new TextComponent();
                tc.addExtra(base[i]);
                out[i] = tc;
            }
        }
        return out;
    }
}