package dev.breach.DistrictRP.commands.roleplay.supporto;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCategoryGUI;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SupportoListener implements Listener {

    private static final String CMD_CONTINUA = "/supporto_continua";

    private static final Set<UUID> PENDING = new HashSet<>();

    private final DistrictRP plugin;
    private final TicketCategoryGUI ticketGui;

    public SupportoListener(DistrictRP plugin, TicketCategoryGUI ticketGui) {
        this.plugin = plugin;
        this.ticketGui = ticketGui;
    }

    public static void addPending(UUID uuid) {
        PENDING.add(uuid);
    }

    public static void removePending(UUID uuid) {
        PENDING.remove(uuid);
    }

    public static boolean isPending(UUID uuid) {
        return PENDING.contains(uuid);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage().toLowerCase().trim();

        if (!msg.equals(CMD_CONTINUA)) return;
        event.setCancelled(true);
        event.setMessage("/dummyinternalsupporto");

        if (!PENDING.contains(player.getUniqueId())) {
            MessageUtils.sendMsg(player, "general.invalid-args");
            return;
        }

        PENDING.remove(player.getUniqueId());
        ticketGui.open(player, "Richiesta supporto rapido", TicketCategoryGUI.Mode.SUPPORTO);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PENDING.remove(event.getPlayer().getUniqueId());
    }
}