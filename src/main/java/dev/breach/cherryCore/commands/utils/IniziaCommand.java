package dev.breach.cherryCore.commands.utils;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

public class IniziaCommand implements CommandExecutor {

    public static final String CITTA_WORLD = "citta";
    public static final double X     = 30;
    public static final double Y     = 68;
    public static final double Z     = -10;
    public static final float  YAW   = 0f;    // ← cambia se vuoi altro sguardo
    public static final float  PITCH = 0f;

    private final CherryCore plugin;

    public IniziaCommand(CherryCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.send(sender, "&cSolo i giocatori.");
            return true;
        }
        World w = Bukkit.getWorld(CITTA_WORLD);
        if (w == null) {
            MessageUtils.send(p, "&cMondo città non trovato.");
            return true;
        }
        p.teleport(new Location(w, X, Y, Z, YAW, PITCH));
        plugin.spawnInvis.remove(p.getUniqueId());
        p.removePotionEffect(PotionEffectType.INVISIBILITY);
        p.setGameMode(GameMode.SURVIVAL);
        return true;
    }
}