package dev.breach.DistrictRP.functions.camera;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CameraManager implements CommandExecutor, TabCompleter {

    private final DistrictRP plugin;
    private Location cameraLocation;

    public CameraManager(DistrictRP plugin) {
        this.plugin = plugin;
        loadCamera();
    }

    private void loadCamera() {
        String worldName = plugin.getConfig().getString("camera.world", "");
        if (worldName.isEmpty()) return;
        World w = plugin.getServer().getWorld(worldName);
        if (w == null) return;
        double x = plugin.getConfig().getDouble("camera.x", 0);
        double y = plugin.getConfig().getDouble("camera.y", 100);
        double z = plugin.getConfig().getDouble("camera.z", 0);
        float yaw = (float) plugin.getConfig().getDouble("camera.yaw", 0);
        float pitch = (float) plugin.getConfig().getDouble("camera.pitch", 0);
        this.cameraLocation = new Location(w, x, y, z, yaw, pitch);
    }

    public boolean hasCamera() {
        return cameraLocation != null;
    }

    public Location getCameraLocation() {
        return cameraLocation;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player p)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        String perm = plugin.getConfig().getString("camera.permission", "DistrictRP.camera");
        if (!p.hasPermission(perm)) {
            MessageUtils.sendMsg(p, "general.no-permission");
            return true;
        }

        if (args.length == 0) {
            MessageUtils.sendList(p, "camera.help");
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "aggiungi" -> {
                this.cameraLocation = p.getLocation().clone();
                saveCamera();
                MessageUtils.sendMsg(p, "camera.added");
            }
            case "rimuovi" -> {
                if (!hasCamera()) {
                    MessageUtils.sendMsg(p, "camera.not-set");
                    return true;
                }
                this.cameraLocation = null;
                removeCameraConfig();
                MessageUtils.sendMsg(p, "camera.removed");
            }
            case "teletrasporta", "tp" -> {
                if (!hasCamera()) {
                    MessageUtils.sendMsg(p, "camera.not-set");
                    return true;
                }
                p.teleport(cameraLocation);
                MessageUtils.sendMsg(p, "camera.teleported");
            }
            default -> MessageUtils.sendList(p, "camera.help");
        }
        return true;
    }

    private void saveCamera() {
        Location loc = cameraLocation;
        plugin.getConfig().set("camera.world", loc.getWorld().getName());
        plugin.getConfig().set("camera.x", loc.getX());
        plugin.getConfig().set("camera.y", loc.getY());
        plugin.getConfig().set("camera.z", loc.getZ());
        plugin.getConfig().set("camera.yaw", loc.getYaw());
        plugin.getConfig().set("camera.pitch", loc.getPitch());
        plugin.saveConfig();
    }

    private void removeCameraConfig() {
        plugin.getConfig().set("camera", null);
        plugin.saveConfig();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                      @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String partial = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String s : Arrays.asList("aggiungi", "rimuovi", "teletrasporta")) {
                if (s.startsWith(partial)) out.add(s);
            }
            return out;
        }
        return Collections.emptyList();
    }
}