package dev.breach.DistrictRP.functions.loading;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.GameProfile;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import com.comphenix.protocol.wrappers.WrappedSignedProperty;
import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfile;
import dev.breach.DistrictRP.commands.roleplay.profile.RPProfileManager;
import dev.breach.DistrictRP.functions.MessageUtils;
import dev.breach.DistrictRP.functions.camera.CameraManager;
import dev.breach.DistrictRP.functions.servermode.ServerMode;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LoadingScreenManager implements Listener {

    private final DistrictRP plugin;
    private final CameraManager cameraManager;
    private ProtocolManager protocolManager;
    private boolean protocolAvailable;

    private final Map<UUID, Location> savedLocations = new HashMap<>();
    private final Map<UUID, Integer> fakeEntityIds = new HashMap<>();
    private final Map<UUID, UUID> fakePlayerUuids = new HashMap<>();

    public LoadingScreenManager(DistrictRP plugin, CameraManager cameraManager) {
        this.plugin = plugin;
        this.cameraManager = cameraManager;
        try {
            this.protocolManager = ProtocolLibrary.getProtocolManager();
            this.protocolAvailable = true;
        } catch (Throwable t) {
            this.protocolAvailable = false;
            plugin.getLogger().warning("[LoadingScreen] ProtocolLib non disponibile.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("loading.enabled", true)) return;
        if (!protocolAvailable) return;
        if (plugin.getServerModeManager() == null) return;
        if (plugin.getServerModeManager().getCurrent() != ServerMode.ROLEPLAY) return;

        Player p = event.getPlayer();
        String bypassPerm = plugin.getConfig().getString("loading.bypass-permission", "DistrictRP.loading.bypass");
        if (p.hasPermission(bypassPerm)) return;

        if (!cameraManager.hasCamera()) return;

        savedLocations.put(p.getUniqueId(), p.getLocation().clone());

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!p.isOnline()) return;
                startLoading(p);
            }
        }.runTaskLater(plugin, 20L);
    }

    private void startLoading(Player p) {
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(cameraManager.getCameraLocation());

        spawnFakeNpc(p);

        int duration = plugin.getConfig().getInt("loading.duration-seconds", 15);
        int ticksTotal = duration * 20;
        int ticksPerStep = 40;
        int steps = ticksTotal / ticksPerStep;

        new BukkitRunnable() {
            int step = 0;

            @Override
            public void run() {
                if (!p.isOnline()) {
                    cancel();
                    cleanup(p);
                    return;
                }

                if (step >= steps) {
                    cancel();
                    endLoading(p);
                    return;
                }

                showLoadingStep(p, step, steps);
                step++;
            }
        }.runTaskTimer(plugin, 0L, ticksPerStep);
    }

    private void showLoadingStep(Player p, int step, int totalSteps) {
        RPProfileManager pm = plugin.getRoleplay() != null ? plugin.getRoleplay().getProfileManager() : null;
        RPProfile profile = pm != null ? pm.get(p.getUniqueId()) : null;

        String title = MessageUtils.color(plugin.getConfig().getString(
                "loading.title", "&#FCD05C&lᴅɪᴛʀᴄᴛʀᴘ"));
        String subtitle;

        if (profile != null && profile.hasRpName()) {
            String name = profile.getRpFullName();
            String job = profile.getJob() != null ? profile.getJob() : "";
            String azienda = profile.getAzienda() != null ? profile.getAzienda() : "";

            switch (step % 4) {
                case 0 -> subtitle = MessageUtils.color("&7ɴᴏᴍᴇ: &f" + name);
                case 1 -> subtitle = MessageUtils.color("&7ʟᴠᴏʀᴏ: &f" + job);
                case 2 -> subtitle = MessageUtils.color("&7ᴀᴢᴇɴᴅ: &f" + (azienda.isEmpty() ? "-" : azienda));
                default -> subtitle = MessageUtils.color("&7ᴄᴀʀᴄᴀᴍɴᴛᴏ... &f" + ((step * 100 / totalSteps)) + "%");
            }
        } else {
            subtitle = MessageUtils.color("&7ᴄᴀʀᴄᴀᴍɴᴛᴏ... &f" + ((step * 100 / totalSteps)) + "%");
        }

        p.sendTitle(title, subtitle, 5, 35, 5);
    }

    private void endLoading(Player p) {
        cleanup(p);

        p.setGameMode(GameMode.SURVIVAL);

        Location saved = savedLocations.remove(p.getUniqueId());
        if (saved != null && saved.getWorld() != null) {
            p.teleport(saved);
        } else {
            p.teleport(p.getWorld().getSpawnLocation());
        }

        String welcome = MessageUtils.color(plugin.getConfig().getString(
                "loading.welcome-title", "&#FCD05C&lʙᴇɴᴠᴇɴᴜᴛᴏ"));
        String welcomeSub = MessageUtils.color(plugin.getConfig().getString(
                "loading.welcome-subtitle", "&7ʙᴏɴ ɢɪᴏᴄᴏ"));
        p.sendTitle(welcome, welcomeSub, 10, 40, 10);
    }

    private void spawnFakeNpc(Player p) {
        if (!protocolAvailable) return;
        try {
            UUID fakeUuid = UUID.randomUUID();
            fakePlayerUuids.put(p.getUniqueId(), fakeUuid);

            WrappedGameProfile wrappedProfile = new WrappedGameProfile(fakeUuid, p.getName());

            for (var entry : p.getPlayerProfile().getProperties().entrySet()) {
                for (var prop : entry.getValue()) {
                    wrappedProfile.getProperties().put(
                            entry.getKey(),
                            new WrappedSignedProperty(prop.getName(), prop.getValue(), prop.getSignature())
                    );
                }
            }

            PacketContainer addPlayer = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            addPlayer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.ADD_PLAYER);

            List<PlayerInfoData> dataList = new ArrayList<>();
            dataList.add(new PlayerInfoData(
                    wrappedProfile.getHandle(),
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    com.comphenix.protocol.wrappers.WrappedChatComponent.fromText(p.getName())
            ));
            addPlayer.getPlayerInfoDataLists().write(0, dataList);
            protocolManager.sendServerPacket(p, addPlayer);

            PacketContainer spawnEntity = protocolManager.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN);
            spawnEntity.getIntegers().write(0, p.getEntityId() + 10000);
            spawnEntity.getUUIDs().write(0, fakeUuid);
            spawnEntity.getDoubles()
                    .write(0, p.getLocation().getX())
                    .write(1, p.getLocation().getY())
                    .write(2, p.getLocation().getZ());
            spawnEntity.getBytes()
                    .write(0, (byte) ((int) (p.getLocation().getYaw() * 256.0F / 360.0F)))
                    .write(1, (byte) ((int) (p.getLocation().getPitch() * 256.0F / 360.0F)));
            protocolManager.sendServerPacket(p, spawnEntity);

            fakeEntityIds.put(p.getUniqueId(), p.getEntityId() + 10000);

            sendEquipment(p, fakeUuid);

        } catch (Throwable t) {
            plugin.getLogger().warning("[LoadingScreen] Errore spawn NPC: " + t.getMessage());
        }
    }

    private void sendEquipment(Player p, UUID fakeUuid) {
        try {
            PacketContainer equipment = protocolManager.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT);
            equipment.getIntegers().write(0, p.getEntityId() + 10000);

            List<com.comphenix.protocol.wrappers.Pair<EnumWrappers.ItemSlot, org.bukkit.inventory.ItemStack>> slots = new ArrayList<>();
            ItemStack[] armor = p.getInventory().getArmorContents();
            if (armor[0] != null) slots.add(new com.comphenix.protocol.wrappers.Pair<>(EnumWrappers.ItemSlot.FEET, armor[0]));
            if (armor[1] != null) slots.add(new com.comphenix.protocol.wrappers.Pair<>(EnumWrappers.ItemSlot.LEGS, armor[1]));
            if (armor[2] != null) slots.add(new com.comphenix.protocol.wrappers.Pair<>(EnumWrappers.ItemSlot.CHEST, armor[2]));
            if (armor[3] != null) slots.add(new com.comphenix.protocol.wrappers.Pair<>(EnumWrappers.ItemSlot.HEAD, armor[3]));
            ItemStack mainHand = p.getInventory().getItemInMainHand();
            if (mainHand != null && mainHand.getType() != org.bukkit.Material.AIR) {
                slots.add(new com.comphenix.protocol.wrappers.Pair<>(EnumWrappers.ItemSlot.MAINHAND, mainHand));
            }

            equipment.getSlotStackPairLists().write(0, slots);
            protocolManager.sendServerPacket(p, equipment);
        } catch (Throwable t) {
            plugin.getLogger().warning("[LoadingScreen] Errore equipment NPC: " + t.getMessage());
        }
    }

    private void cleanup(Player p) {
        UUID fakeUuid = fakePlayerUuids.remove(p.getUniqueId());
        Integer entityId = fakeEntityIds.remove(p.getUniqueId());

        if (fakeUuid == null || entityId == null || !protocolAvailable) return;

        try {
            PacketContainer removeEntity = protocolManager.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            removeEntity.getIntLists().write(0, List.of(entityId));
            protocolManager.sendServerPacket(p, removeEntity);

            PacketContainer removePlayer = protocolManager.createPacket(PacketType.Play.Server.PLAYER_INFO);
            removePlayer.getPlayerInfoAction().write(0, EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
            List<PlayerInfoData> removeList = new ArrayList<>();
            removeList.add(new PlayerInfoData(
                    new WrappedGameProfile(fakeUuid, p.getName()).getHandle(),
                    0,
                    EnumWrappers.NativeGameMode.SURVIVAL,
                    null
            ));
            removePlayer.getPlayerInfoDataLists().write(0, removeList);
            protocolManager.sendServerPacket(p, removePlayer);
        } catch (Throwable t) {
            plugin.getLogger().warning("[LoadingScreen] Errore cleanup NPC: " + t.getMessage());
        }
    }
}