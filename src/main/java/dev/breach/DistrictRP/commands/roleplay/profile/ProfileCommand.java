package dev.breach.DistrictRP.commands.roleplay.profile;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfileCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "DistrictRP.profile.admin";

    private static final List<String> SUBS = Arrays.asList(
            "nome", "cognome", "eta", "nascita", "sesso", "bio", "nazionalita", "job", "vedi"
    );

    private final DistrictRP plugin;
    private final RPProfileManager manager;

    public ProfileCommand(DistrictRP plugin, RPProfileManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private String getDefaultJob() {
        return plugin.getConfig().getString("profile.default-job", "DISOCCUPATO");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission(PERM)) {
            MessageUtils.sendMsg(sender, "general.no-permission");
            return true;
        }

        if (args.length < 2) {
            sendHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase();
        String targetName = args[1];
        OfflinePlayer target = resolveTarget(targetName);

        if (target == null || (!target.hasPlayedBefore() && !target.isOnline())) {
            MessageUtils.sendMsg(sender, "general.player-not-found");
            return true;
        }

        RPProfile profile = manager.get(target.getUniqueId());

        switch (sub) {
            case "nome" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-nome"); return true; }
                String name = args[2];
                if (!isValidName(name)) { MessageUtils.sendMsg(sender, "profile.invalid-name"); return true; }
                profile.setRpName(name);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.rpname-set", "name", name, "player", targetName);
            }
            case "cognome" -> {
                if (args.length < 3) {
                    MessageUtils.sendMsg(sender, "profile.usage-cognome");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2) sb.append(" ");
                    sb.append(args[i]);
                }
                String surname = sb.toString();
                if (!surname.matches("[a-zA-ZÀ-ÿ ]+")) {
                    MessageUtils.sendMsg(sender, "profile.invalid-name");
                    return true;
                }
                profile.setRpSurname(surname);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.rpsurname-set",
                        "player", targetName, "surname", surname);
            }
            case "eta" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-eta"); return true; }
                int age;
                try { age = Integer.parseInt(args[2]); }
                catch (NumberFormatException e) { MessageUtils.sendMsg(sender, "profile.invalid-age"); return true; }
                int min = plugin.getConfig().getInt("profile.min-age", 16);
                int max = plugin.getConfig().getInt("profile.max-age", 99);
                if (age < min || age > max) {
                    MessageUtils.sendMsg(sender, "profile.age-out-of-range",
                            "min", String.valueOf(min), "max", String.valueOf(max));
                    return true;
                }
                profile.setIcAge(age);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.age-set", "age", String.valueOf(age), "player", targetName);
            }
            case "nascita" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-nascita"); return true; }
                String date = args[2];
                if (!date.matches("^\\d{2}/\\d{2}/\\d{4}$")) {
                    MessageUtils.sendMsg(sender, "profile.invalid-birthday");
                    return true;
                }
                profile.setIcBirthday(date);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.birthday-set", "date", date, "player", targetName);
            }
            case "sesso" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-sesso"); return true; }
                String g = args[2].toUpperCase();
                if (!g.equals("M") && !g.equals("F") && !g.equals("ALTRO")) {
                    MessageUtils.sendMsg(sender, "profile.invalid-gender");
                    return true;
                }
                profile.setIcGender(g);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.gender-set", "gender", g, "player", targetName);
            }
            case "bio" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-bio"); return true; }
                StringBuilder sb = new StringBuilder();
                for (int i = 2; i < args.length; i++) {
                    if (i > 2) sb.append(" ");
                    sb.append(args[i]);
                }
                String bio = sb.toString();
                int maxLen = plugin.getConfig().getInt("profile.max-bio-length", 200);
                if (bio.length() > maxLen) {
                    MessageUtils.sendMsg(sender, "profile.bio-too-long", "max", String.valueOf(maxLen));
                    return true;
                }
                profile.setIcBio(bio);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.bio-set", "player", targetName);
            }
            case "nazionalita" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-nazionalita"); return true; }
                String nat = args[2];
                if (!isValidName(nat)) { MessageUtils.sendMsg(sender, "profile.invalid-name"); return true; }
                profile.setIcNationality(nat);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.nationality-set", "nationality", nat, "player", targetName);
            }
            case "job" -> {
                if (args.length < 3) { MessageUtils.sendMsg(sender, "profile.usage-job"); return true; }
                String job = String.join(" ", Arrays.copyOfRange(args, 2, args.length)).toUpperCase();
                profile.setJob(job);
                manager.save(target.getUniqueId());
                MessageUtils.sendMsg(sender, "profile.job-set", "job", job, "player", targetName);
            }
            case "vedi" -> {
                String name = target.getName() != null ? target.getName() : "?";
                String job = (profile.getJob() == null || profile.getJob().isEmpty())
                        ? getDefaultJob() : profile.getJob();
                for (String line : MessageUtils.getList("profile.view",
                        "player", name,
                        "rpname", nvl(profile.getRpName()),
                        "rpsurname", nvl(profile.getRpSurname()),
                        "rpfullname", profile.getRpFullName().isEmpty() ? name : profile.getRpFullName(),
                        "age", String.valueOf(profile.getIcAge()),
                        "birthday", nvl(profile.getIcBirthday()),
                        "gender", nvl(profile.getIcGender()),
                        "nationality", nvl(profile.getIcNationality()),
                        "bio", (profile.getIcBio() == null || profile.getIcBio().isEmpty()) ? "-" : profile.getIcBio(),
                        "job", job)) {
                    sender.sendMessage(line);
                }
                if (sender instanceof Player viewer && target.isOnline()) {
                    dev.breach.DistrictRP.functions.profile.ProfileScoreboard sb =
                            new dev.breach.DistrictRP.functions.profile.ProfileScoreboard(plugin);
                    sb.show(viewer, target.getPlayer());
                }
            }
            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender s) {
        for (String line : MessageUtils.getList("profile.help")) s.sendMessage(line);
    }

    private String nvl(String s) {
        return (s == null || s.isEmpty()) ? "-" : s;
    }

    private boolean isValidName(String s) {
        if (s == null || s.isEmpty() || s.length() > 32) return false;
        return s.matches("^[a-zA-ZàèéìòùÀÈÉÌÒÙ'\\-]+$");
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveTarget(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        return Bukkit.getOfflinePlayer(name);
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!sender.hasPermission(PERM)) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            String p = args[0].toLowerCase();
            for (String s : SUBS) if (s.startsWith(p)) out.add(s);
        } else if (args.length == 2) {
            for (Player pl : Bukkit.getOnlinePlayers()) {
                if (pl.getName().toLowerCase().startsWith(args[1].toLowerCase())) out.add(pl.getName());
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("sesso")) {
            for (String g : Arrays.asList("M", "F", "ALTRO")) {
                if (g.toLowerCase().startsWith(args[2].toLowerCase())) out.add(g);
            }
        }
        return out;
    }
}