package dev.breach.DistrictRP.commands.ChatGate.placeholders;

import dev.breach.DistrictRP.commands.ChatGate.ChatGate;
import dev.breach.DistrictRP.commands.ChatGate.ChatGate.CustomChat;
import dev.breach.DistrictRP.functions.MessageUtils;
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
            CustomChat chat = ChatGate.getInstance().getToggledChat(player);
            if (chat == null) return ChatGate.getInstance().message("none-chat-placeholder");
            return MessageUtils.color(chat.displayName());
        }
        return null;
    }
}
