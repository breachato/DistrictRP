package dev.breach.DistrictRP.commands.roleplay.supporto;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.ticket.TicketCategoryGUI;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SupportoCommand implements CommandExecutor, Listener {

    private static final Set<UUID> PENDING = new HashSet<>();

    private final DistrictRP plugin;
    private final TicketCategoryGUI ticketGui;

    public SupportoCommand(DistrictRP plugin, TicketCategoryGUI ticketGui) {
        this.plugin = plugin;
        this.ticketGui = ticketGui;
    }

    public static void addPending(UUID uuid) { PENDING.add(uuid); }
    public static void removePending(UUID uuid) { PENDING.remove(uuid); }
    public static boolean isPending(UUID uuid) { return PENDING.contains(uuid); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            MessageUtils.sendMsg(sender, "general.only-player");
            return true;
        }

        List<String> lines = MessageUtils.getList("supporto.message");
        String hoverText = MessageUtils.get("supporto.hover-continue");

        for (String line : lines) {
            String stripped = ChatColor.stripColor(MessageUtils.color(line)).toLowerCase();

            boolean hasContinueMarker = stripped.contains("[continua]")
                    || stripped.contains("[ᴄᴏɴᴛɪɴᴜᴀ]")
                    || stripped.contains("continua");

            if (hasContinueMarker) {
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

        addPending(player.getUniqueId());
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String raw = event.getMessage();
        if (raw == null || !raw.startsWith("/")) return;

        String cmd = raw.substring(1);
        int spaceIdx = cmd.indexOf(' ');
        if (spaceIdx >= 0) cmd = cmd.substring(0, spaceIdx);

        int colonIdx = cmd.indexOf(':');
        if (colonIdx >= 0) cmd = cmd.substring(colonIdx + 1);

        if (!cmd.toLowerCase(Locale.ROOT).equals("supporto_continua")) return;

        event.setCancelled(true);

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