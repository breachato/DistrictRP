package dev.breach.DistrictRP.commands.utils;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class BuildCommand implements CommandExecutor, Listener {

    public static final String BUILD_WORLD       = "build";
    public static final String BUILD_PERMISSION  = "districtrp.buildworldaccess";

    public static final double X     = 20000.5;
    public static final double Y     = 91;
    public static final double Z     = -1.5;
    public static final float  YAW   = 0f;
    public static final float  PITCH = 0f;

    private static final String KICK_MESSAGE =
            "§e§lᴅɪꜱᴛʀɪᴄɪᴛʀᴘ\n\n§cNon hai il permesso per accedere a questo mondo.";

    private final DistrictRP plugin;

    public BuildCommand(DistrictRP plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }

        if (!p.hasPermission(BUILD_PERMISSION)) {
            MessageUtils.send(p, "&r");
            MessageUtils.send(p, "&c&lᴀᴄᴄᴇꜱꜱᴏ ɴᴇɢᴀᴛᴏ");
            MessageUtils.send(p, "&r");
            MessageUtils.send(p, "&7Non hai il permesso per accedere al mondo &fAssets&7.");
            MessageUtils.send(p, "&r");
            return true;
        }

        World w = Bukkit.getWorld(BUILD_WORLD);
        if (w == null) {
            MessageUtils.send(p, "&cMondo build non trovato.");
            return true;
        }

        p.teleport(new Location(w, X, Y, Z, YAW, PITCH));
        MessageUtils.send(p, "&r");
        MessageUtils.send(p, "&e&lᴛᴇʟᴇᴛʀᴀꜱᴘᴏʀᴛᴏ");
        MessageUtils.send(p, "&r");
        MessageUtils.send(p, "&7Sei stato teletraportato presso il mondo &fAssets&7.");
        MessageUtils.send(p, "&r");
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player p = event.getPlayer();
        checkAndKick(p);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin, () -> checkAndKick(p));
    }

    private void checkAndKick(Player p) {
        if (p.getWorld().getName().equalsIgnoreCase(BUILD_WORLD)) {

            if (!p.hasPermission(BUILD_PERMISSION)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        p.kickPlayer(KICK_MESSAGE)
                );
            }
        }
    }
}