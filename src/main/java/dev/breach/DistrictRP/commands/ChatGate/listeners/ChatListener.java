package dev.breach.DistrictRP.commands.ChatGate.listeners;

import dev.breach.DistrictRP.commands.ChatGate.managers.ChatManager;
import dev.breach.DistrictRP.commands.ChatGate.models.CustomChat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatListener implements Listener {

    private final ChatManager chatManager = ChatManager.getInstance();

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();

        if (!chatManager.hasToggledChat(player)) return;

        CustomChat chat = chatManager.getToggledChat(player);

        if (chat == null) return;

        event.setCancelled(true);

        String message = event.getMessage();

        chatManager.sendChatMessage(chat, player, message);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        chatManager.removeToggled(event.getPlayer());
    }
}