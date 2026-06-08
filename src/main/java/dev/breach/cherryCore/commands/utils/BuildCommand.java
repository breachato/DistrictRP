package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BuildCommand implements CommandExecutor {

    public static final String BUILD_WORLD = "build";
    public static final double X     = 20000.5;
    public static final double Y     = 91;
    public static final double Z     = -1.5;
    public static final float  YAW   = 0f;     // ← cambia se vuoi altro sguardo
    public static final float  PITCH = 0f;     // ← 0 = orizzonte

    private final CherryCore plugin;

    public BuildCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        World w = Bukkit.getWorld(BUILD_WORLD);
        if (w == null) {
            MessageUtils.send(p, "&cMondo build non trovato.");
            return true;
        }
        p.teleport(new Location(w, X, Y, Z, YAW, PITCH));
        MessageUtils.send(p, "&r");
        MessageUtils.send(p, "&d&lᴛᴇʟᴇᴛʀᴀꜱᴘᴏʀᴛᴏ");
        MessageUtils.send(p, "&r");
        MessageUtils.send(p, "&7Sei stato teletraportato presso il mondo &fAssets&7.");
        MessageUtils.send(p, "&r");
        return true;
    }
}