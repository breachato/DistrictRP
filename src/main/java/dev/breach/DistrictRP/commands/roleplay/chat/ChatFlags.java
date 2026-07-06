package dev.breach.DistrictRP.commands.roleplay.chat;

import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ChatFlags {

    private static final Set<UUID> PASSA = new HashSet<>();
    private static final Set<UUID> PASSA_FDO = new HashSet<>();

    public static boolean isAnonymous(Player player) {
        UUID u = player.getUniqueId();
        return PASSA.contains(u) || PASSA_FDO.contains(u);
    }

    public static void setPassa(UUID uuid, boolean state) {
        if (state) PASSA.add(uuid); else PASSA.remove(uuid);
    }

    public static void setPassaFdo(UUID uuid, boolean state) {
        if (state) PASSA_FDO.add(uuid); else PASSA_FDO.remove(uuid);
    }

    public static boolean isPassa(UUID uuid) { return PASSA.contains(uuid); }
    public static boolean isPassaFdo(UUID uuid) { return PASSA_FDO.contains(uuid); }
}