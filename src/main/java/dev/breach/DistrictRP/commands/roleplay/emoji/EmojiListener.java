package dev.breach.DistrictRP.commands.roleplay.emoji;

import dev.breach.DistrictRP.DistrictRP;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class EmojiListener implements Listener {

    private final DistrictRP plugin;
    private final EmojiManager manager;

    public EmojiListener(DistrictRP plugin, EmojiManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        boolean access = player.hasPermission(manager.getPermission());
        String replaced = manager.replaceAll(event.getMessage(), access);
        if (!replaced.equals(event.getMessage())) {
            event.setMessage(replaced);
        }
    }
}