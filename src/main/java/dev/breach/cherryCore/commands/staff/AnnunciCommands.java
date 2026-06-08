package dev.breach.cherryCore.commands.staff;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;

public class AnnunciCommands {

    private final CherryCore plugin;

    private static final String ANN_PREFIX   = "&6&l";
    private static final String ANN_SUBTITLE = "&7Preparati!";
    private static final String ANN_SUBTITLE2 = "";
    private static final int    FADE_IN      = 20;
    private static final int    STAY         = 60;   // 3 secondi = 60 tick
    private static final int    FADE_OUT     = 20;

    public AnnunciCommands(CherryCore plugin) {
        this.plugin = plugin;
    }

    // /avantiilprimo
    public CommandExecutor primo() {
        return (sender, cmd, label, args) -> {
            if (!sender.hasPermission("annunci.ilprimo")) {
                MessageUtils.send(sender, "&c✗ Non hai il permesso.");
                return true;
            }
            broadcastTitle(
                    MessageUtils.color(ANN_PREFIX + "AVANTI IL PRIMO!"),
                    MessageUtils.color(ANN_SUBTITLE)
            );
            MessageUtils.send(sender, "&a✓ Annuncio inviato.");
            return true;
        };
    }

    // /avantiunaltro
    public CommandExecutor prossimo() {
        return (sender, cmd, label, args) -> {
            if (!sender.hasPermission("annunci.unaltro")) {
                MessageUtils.send(sender, "&c✗ Non hai il permesso.");
                return true;
            }
            broadcastTitle(
                    MessageUtils.color(ANN_PREFIX + "AVANTI UN ALTRO!"),
                    MessageUtils.color(ANN_SUBTITLE2)
            );
            MessageUtils.send(sender, "&a✓ Annuncio inviato.");
            return true;
        };
    }

    // /annuncio <testo>
    public CommandExecutor annuncio() {
        return (sender, cmd, label, args) -> {
            if (!sender.hasPermission("annunci.custom")) {
                MessageUtils.send(sender, "&c✗ Non hai il permesso.");
                return true;
            }
            if (args.length == 0) {
                MessageUtils.send(sender, "&cUsa: /annuncio <testo>");
                return true;
            }
            String testo = String.join(" ", args);
            broadcastTitle(
                    MessageUtils.color(ANN_PREFIX + testo),
                    MessageUtils.color(ANN_SUBTITLE2)
            );
            MessageUtils.send(sender, "&a✓ Annuncio \"" + testo + "\" inviato.");
            return true;
        };
    }

    // /annunciofull <titolo> <sottotitolo>
    public CommandExecutor annfull() {
        return (sender, cmd, label, args) -> {
            if (!sender.hasPermission("annunci.custom")) {
                MessageUtils.send(sender, "&c✗ Non hai il permesso.");
                return true;
            }
            if (args.length < 2) {
                MessageUtils.send(sender, "&7Usa: /annunciofull <titolo> <sottotitolo>");
                return true;
            }

            // arg[0] = titolo, tutto il resto = sottotitolo
            String titolo     = args[0];
            StringBuilder sb  = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(" ");
                sb.append(args[i]);
            }
            String sottotitolo = sb.toString();

            broadcastTitle(
                    MessageUtils.color(ANN_PREFIX + titolo),
                    MessageUtils.color("&7" + sottotitolo)
            );
            MessageUtils.send(sender, "&a✓ Annuncio inviato.");
            return true;
        };
    }

    // ============================================================
    // Helper
    // ============================================================
    private void broadcastTitle(String title, String subtitle) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            MessageUtils.title(p, title, subtitle, FADE_IN, STAY, FADE_OUT);
        }
    }
}