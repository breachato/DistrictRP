package dev.breach.DistrictRP.commands.roleplay.supporto;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCategoryGUI;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class SupportoCommand implements CommandExecutor {

    private final DistrictRP plugin;
    private final TicketCategoryGUI ticketGui;

    public SupportoCommand(DistrictRP plugin, TicketCategoryGUI ticketGui) {
        this.plugin = plugin;
        this.ticketGui = ticketGui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        List<String> lines = MessageUtils.getList("supporto.message");
        String hoverText = MessageUtils.get("supporto.hover-continue");

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String stripped = net.md_5.bungee.api.ChatColor.stripColor(MessageUtils.color(line));

            if (stripped.contains("[Continua]")) {
                TextComponent comp = new TextComponent(TextComponent.fromLegacyText(line));
                comp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(MessageUtils.color(hoverText)))));
                comp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        "/supporto_continua"));
                player.spigot().sendMessage(comp);
            } else {
                player.sendMessage(line);
            }
        }

        SupportoListener.addPending(player.getUniqueId());
        return true;
    }
}