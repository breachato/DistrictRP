package dev.breach.DistrictRP.commands.roleplay.chat;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class ChatSymListener implements Listener {

    private final DistrictRP plugin;
    private final ChatSymManager manager;

    public ChatSymListener(DistrictRP plugin, ChatSymManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String perm = plugin.getConfig().getString("chatsym.permission", "DistrictRP.staffchatsym");
        if (!player.hasPermission(perm)) return;

        String message = event.getMessage();
        if (message == null || message.isEmpty()) return;

        List<Map.Entry<String, String>> sorted = new ArrayList<>(manager.getSymbols().entrySet());
        sorted.sort(Comparator.comparingInt((Map.Entry<String, String> e) -> e.getKey().length()).reversed());

        for (Map.Entry<String, String> entry : sorted) {
            String symbol = entry.getKey();
            if (message.startsWith(symbol)) {
                String cmdName = entry.getValue();
                String rest = message.substring(symbol.length()).trim();
                String fullCommand = cmdName + (rest.isEmpty() ? "" : " " + rest);

                event.setCancelled(true);

                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean success = Bukkit.dispatchCommand(player, fullCommand);
                    if (!success) {
                        plugin.getLogger().warning("[ChatSym] Comando /" + fullCommand + " non eseguito (unknown/errore).");
                    }
                });
                return;
            }
        }
    }
}