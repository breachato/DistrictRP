package dev.breach.DistrictRP.commands.roleplay.appuntamenti;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AppuntamentoGUI implements Listener {

    private static final String CMD_REPARTO = "/appuntamento_select_reparto ";
    private static final String CMD_GIORNO = "/appuntamento_select_giorno ";
    private static final String CMD_FASCIA = "/appuntamento_fascia_page ";
    private static final String CMD_ORARIO = "/appuntamento_select_orario ";

    private final DistrictRP plugin;
    private final AppuntamentoManager manager;

    private static final Map<UUID, String> selectedReparto = new HashMap<>();
    private static final Map<UUID, String> selectedGiorno = new HashMap<>();

    public AppuntamentoGUI(DistrictRP plugin, AppuntamentoManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public static void startFlow(DistrictRP plugin, AppuntamentoManager manager, Player player) {
        selectedReparto.remove(player.getUniqueId());
        selectedGiorno.remove(player.getUniqueId());
        showReparti(plugin, manager, player);
    }

    private static void showReparti(DistrictRP plugin, AppuntamentoManager manager, Player player) {
        player.sendMessage("");
        player.sendMessage(MessageUtils.get("appuntamenti.select-reparto"));
        player.sendMessage("");

        Map<String, String> reparti = manager.getReparti();
        for (Map.Entry<String, String> entry : reparti.entrySet()) {
            String id = entry.getKey();
            String display = entry.getValue();

            TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                    MessageUtils.get("appuntamenti.reparto-entry", "name", display)));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(
                            MessageUtils.get("appuntamenti.hover-reparto", "name", display)))));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    CMD_REPARTO.trim() + " " + id));
            player.spigot().sendMessage(line);
        }
    }

    private static void showGiorni(DistrictRP plugin, AppuntamentoManager manager, Player player) {
        player.sendMessage("");
        player.sendMessage(MessageUtils.get("appuntamenti.select-giorno"));
        player.sendMessage("");

        List<String> days = manager.generateDays();
        for (String day : days) {
            TextComponent line = new TextComponent(TextComponent.fromLegacyText(
                    MessageUtils.get("appuntamenti.giorno-entry", "day", day)));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(
                            MessageUtils.get("appuntamenti.hover-giorno", "day", day)))));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    CMD_GIORNO.trim() + " " + day));
            player.spigot().sendMessage(line);
        }
    }

    private static void showFascia(DistrictRP plugin, AppuntamentoManager manager, Player player, int page) {
        List<String> slots = manager.generateTimeSlots();
        int perPage = manager.getSlotsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil(slots.size() / (double) perPage));
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        int start = (page - 1) * perPage;
        int end = Math.min(start + perPage, slots.size());

        player.sendMessage("");
        player.sendMessage(MessageUtils.get("appuntamenti.select-fascia",
                "page", String.valueOf(page), "pages", String.valueOf(totalPages)));
        player.sendMessage("");

        String reparto = selectedReparto.getOrDefault(player.getUniqueId(), "");
        String giorno = selectedGiorno.getOrDefault(player.getUniqueId(), "");

        List<TextComponent> components = new ArrayList<>();

        for (int i = start; i < end; i++) {
            String time = slots.get(i);
            boolean taken = manager.isSlotTaken(reparto, giorno, time);

            TextComponent timeComp = new TextComponent(TextComponent.fromLegacyText(
                    MessageUtils.color((taken ? "&8&m" : "&a") + time)));

            if (!taken) {
                timeComp.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new Text(TextComponent.fromLegacyText(
                                MessageUtils.get("appuntamenti.hover-fascia", "time", time)))));
                timeComp.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        CMD_ORARIO.trim() + " " + time));
            }

            if (!components.isEmpty()) {
                components.add(new TextComponent(MessageUtils.color(" &8| ")));
            }
            components.add(timeComp);

            if ((i - start + 1) % 4 == 0 || i == end - 1) {
                TextComponent fullLine = new TextComponent(MessageUtils.color("&8| "));
                for (TextComponent c : components) fullLine.addExtra(c);
                player.spigot().sendMessage(fullLine);
                components.clear();
            }
        }

        player.sendMessage("");
        TextComponent nav = new TextComponent();
        if (page > 1) {
            TextComponent prev = new TextComponent(TextComponent.fromLegacyText(
                    MessageUtils.get("appuntamenti.indietro")));
            prev.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(MessageUtils.get("appuntamenti.hover-indietro")))));
            prev.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    CMD_FASCIA.trim() + " " + (page - 1)));
            nav.addExtra(prev);
        }
        if (page > 1 && page < totalPages) {
            nav.addExtra(new TextComponent("  "));
        }
        if (page < totalPages) {
            TextComponent next = new TextComponent(TextComponent.fromLegacyText(
                    MessageUtils.get("appuntamenti.avanti")));
            next.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new Text(TextComponent.fromLegacyText(MessageUtils.get("appuntamenti.hover-avanti")))));
            next.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    CMD_FASCIA.trim() + " " + (page + 1)));
            nav.addExtra(next);
        }
        player.spigot().sendMessage(nav);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String msg = event.getMessage();

        if (msg.startsWith(CMD_REPARTO)) {
            event.setCancelled(true);
            event.setMessage("/dummyinternalappuntamento");
            String reparto = msg.substring(CMD_REPARTO.length()).trim();
            selectedReparto.put(player.getUniqueId(), reparto);
            showGiorni(plugin, manager, player);
            return;
        }

        if (msg.startsWith(CMD_GIORNO)) {
            event.setCancelled(true);
            event.setMessage("/dummyinternalappuntamento");
            String giorno = msg.substring(CMD_GIORNO.length()).trim();
            selectedGiorno.put(player.getUniqueId(), giorno);
            showFascia(plugin, manager, player, 1);
            return;
        }

        if (msg.startsWith(CMD_FASCIA)) {
            event.setCancelled(true);
            event.setMessage("/dummyinternalappuntamento");
            int page;
            try {
                page = Integer.parseInt(msg.substring(CMD_FASCIA.length()).trim());
            } catch (NumberFormatException e) {
                page = 1;
            }
            showFascia(plugin, manager, player, page);
            return;
        }

        if (msg.startsWith(CMD_ORARIO)) {
            event.setCancelled(true);
            event.setMessage("/dummyinternalappuntamento");
            String orario = msg.substring(CMD_ORARIO.length()).trim();
            String reparto = selectedReparto.getOrDefault(player.getUniqueId(), "");
            String giorno = selectedGiorno.getOrDefault(player.getUniqueId(), "");

            if (reparto.isEmpty() || giorno.isEmpty()) {
                MessageUtils.sendMsg(player, "general.invalid-args");
                return;
            }

            if (manager.isSlotTaken(reparto, giorno, orario)) {
                MessageUtils.sendMsg(player, "appuntamenti.no-slots");
                return;
            }

            Map<String, String> reparti = manager.getReparti();
            String repartoDisplay = reparti.getOrDefault(reparto, reparto);

            manager.create(player.getUniqueId(), player.getName(), reparto, giorno, orario);
            MessageUtils.sendMsg(player, "appuntamenti.booked",
                    "day", giorno, "time", orario, "reparto", repartoDisplay);

            selectedReparto.remove(player.getUniqueId());
            selectedGiorno.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        selectedReparto.remove(uuid);
        selectedGiorno.remove(uuid);
    }
}