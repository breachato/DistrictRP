package dev.breach.DistrictRP.commands.ChatGate.placeholders;

import dev.breach.DistrictRP.commands.ChatGate.managers.ChatManager;
import dev.breach.DistrictRP.commands.ChatGate.managers.ColorManager;
import dev.breach.DistrictRP.commands.ChatGate.managers.MessageManager;
import dev.breach.DistrictRP.commands.ChatGate.models.CustomChat;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIExpansion extends PlaceholderExpansion {

    @Override public @NotNull String getIdentifier() { return "chatgate"; }
    @Override public @NotNull String getAuthor() { return "DistrictRP"; }
    @Override public @NotNull String getVersion() { return "1.0"; }
    @Override public boolean persist() { return true; }
    @Override public boolean canRegister() { return true; }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        if (identifier.equalsIgnoreCase("chat_displayname")) {
            CustomChat chat = ChatManager.getInstance().getToggledChat(player);
            if (chat == null) return MessageManager.getMessage("none-chat-placeholder");
            return ColorManager.color(chat.displayName());
        }
        return null;
    }
}