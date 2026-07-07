package dev.breach.DistrictRP.commands.roleplay.warp;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class Warp {

    private final String name;
    private final String world;
    private final double x, y, z;
    private final float yaw, pitch;
    private final String permission;

    public Warp(String name, String world, double x, double y, double z,
                float yaw, float pitch, String permission) {
        this.name = name;
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.permission = permission == null ? "" : permission;
    }

    public Warp(String name, Location loc, String permission) {
        this(name,
                loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                loc.getYaw(), loc.getPitch(),
                permission);
    }

    public String getName() { return name; }
    public String getWorld() { return world; }
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public String getPermission() { return permission; }

    public boolean hasPermission() {
        return permission != null && !permission.isEmpty();
    }

    public Location toLocation() {
        World w = Bukkit.getWorld(world);
        if (w == null) return null;
        return new Location(w, x, y, z, yaw, pitch);
    }
}