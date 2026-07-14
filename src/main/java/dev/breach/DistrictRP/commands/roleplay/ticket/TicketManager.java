package dev.breach.DistrictRP.commands.roleplay.ticket;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.database.tables.TicketCommentsTable;
import dev.breach.DistrictRP.database.tables.TicketCommentsTable.CommentRow;
import dev.breach.DistrictRP.database.tables.TicketsTable;
import dev.breach.DistrictRP.database.tables.TicketsTable.TicketRow;
import dev.breach.DistrictRP.functions.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class TicketManager {

    private final DistrictRP plugin;
    private final File file;
    private FileConfiguration config;
    private final Map<Integer, Ticket> tickets = new LinkedHashMap<>();
    private final Map<String, TicketCategory> categories = new LinkedHashMap<>();
    private int nextId = 1;

    private TicketsTable table;
    private TicketCommentsTable commentsTable;
    private boolean useDb;

    public TicketManager(DistrictRP plugin) {
        this.plugin = plugin;
        File dir = new File(plugin.getDataFolder(), "roleplay");
        if (!dir.exists()) dir.mkdirs();
        this.file = new File(dir, "tickets.yml");
        if (!file.exists()) {
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        this.config = YamlConfiguration.loadConfiguration(file);

        var dbm = plugin.getDatabaseManager();
        if (dbm != null && dbm.isMariaDb()) {
            this.table = dbm.getTable("tickets", TicketsTable.class);
            this.commentsTable = dbm.getTable("ticket_comments", TicketCommentsTable.class);
        }
        this.useDb = (table != null && commentsTable != null);

        loadCategories();

        if (useDb) {
            plugin.getLogger().info("[Tickets] Storage: MariaDB (con cache locale)");
            loadFromDb();
        } else {
            plugin.getLogger().info("[Tickets] Storage: YAML");
            loadTicketsYaml();
        }
    }

    public void loadCategories() {
        categories.clear();
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ticket.categories");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            String name = sec.getString(id + ".name", id);
            String matName = sec.getString(id + ".material", "PAPER");
            Material mat;
            try { mat = Material.valueOf(matName.toUpperCase()); }
            catch (IllegalArgumentException e) { mat = Material.PAPER; }
            int slot = sec.getInt(id + ".slot", 0);
            List<String> lore = sec.getStringList(id + ".lore");
            String permNotify = sec.getString(id + ".permission-notify", "");
            categories.put(id.toLowerCase(), new TicketCategory(id, name, mat, slot, lore, permNotify));
        }
    }

    private void loadTicketsYaml() {
        tickets.clear();
        nextId = config.getInt("next-id", 1);
        ConfigurationSection sec = config.getConfigurationSection("tickets");
        if (sec == null) return;
        for (String key : sec.getKeys(false)) {
            try {
                int id = Integer.parseInt(key);
                String base = "tickets." + key + ".";
                UUID author = UUID.fromString(config.getString(base + "author"));
                String authorName = config.getString(base + "author-name", "?");
                String category = config.getString(base + "category", "generale");
                String reason = config.getString(base + "reason", "");
                long openedAt = config.getLong(base + "opened-at", System.currentTimeMillis());

                Ticket t = new Ticket(id, author, authorName, category, reason, openedAt);
                t.setOpen(config.getBoolean(base + "open", true));

                String cb = config.getString(base + "closed-by", null);
                if (cb != null) t.setClosedBy(UUID.fromString(cb));
                t.setClosedByName(config.getString(base + "closed-by-name", null));
                t.setCloseReason(config.getString(base + "close-reason", null));
                t.setClosedAt(config.getLong(base + "closed-at", 0));

                String clb = config.getString(base + "claimed-by", null);
                if (clb != null) t.setClaimedBy(UUID.fromString(clb));
                t.setClaimedByName(config.getString(base + "claimed-by-name", null));

                for (String cs : config.getStringList(base + "comments")) {
                    TicketComment tc = TicketComment.deserialize(cs);
                    if (tc != null) t.addComment(tc);
                }
                tickets.put(id, t);
            } catch (Exception ignored) {}
        }
    }

    private void loadFromDb() {
        fetchAll().thenAccept(list -> {
            tickets.clear();
            int maxId = 0;
            for (Ticket t : list) {
                tickets.put(t.getId(), t);
                if (t.getId() > maxId) maxId = t.getId();
            }
            nextId = maxId + 1;
            plugin.getLogger().info("[Tickets] Caricati " + tickets.size() + " ticket dal database.");
        }).exceptionally(t -> {
            plugin.getLogger().warning("[Tickets] Errore caricamento DB: " + t.getMessage());
            return null;
        });
    }

    public void saveAll() {
        if (useDb) return;
        saveAllYaml();
    }

    private void saveAllYaml() {
        FileConfiguration newConfig = new YamlConfiguration();
        newConfig.set("next-id", nextId);
        ConfigurationSection ticketsSection = newConfig.createSection("tickets");
        for (Ticket t : tickets.values()) {
            ConfigurationSection ts = ticketsSection.createSection(String.valueOf(t.getId()));
            ts.set("author", t.getAuthor().toString());
            ts.set("author-name", t.getAuthorName());
            ts.set("category", t.getCategory());
            ts.set("reason", t.getReason());
            ts.set("opened-at", t.getOpenedAt());
            ts.set("open", t.isOpen());
            if (t.getClosedBy() != null) ts.set("closed-by", t.getClosedBy().toString());
            ts.set("closed-by-name", t.getClosedByName());
            ts.set("close-reason", t.getCloseReason());
            ts.set("closed-at", t.getClosedAt());
            if (t.getClaimedBy() != null) ts.set("claimed-by", t.getClaimedBy().toString());
            ts.set("claimed-by-name", t.getClaimedByName());
            List<String> cs = new ArrayList<>();
            for (TicketComment c : t.getComments()) cs.add(c.serialize());
            ts.set("comments", cs);
        }
        this.config = newConfig;
        try { config.save(file); }
        catch (IOException e) { plugin.getLogger().warning("Errore salvataggio tickets.yml: " + e.getMessage()); }
    }

    private String getServerOrigin() {
        return plugin.getConfig().getString("server-id", Bukkit.getServer().getName());
    }

    public Ticket create(UUID author, String authorName, String category, String reason) {
        if (useDb) {
            Ticket local = new Ticket(-1, author, authorName, category.toLowerCase(), reason, System.currentTimeMillis());
            Integer newId = createTicket(local, getServerOrigin()).join();
            if (newId == null || newId < 0) {
                plugin.getLogger().warning("[Tickets] create fallito su DB");
                return null;
            }
            Ticket t = new Ticket(newId, author, authorName, category.toLowerCase(), reason, System.currentTimeMillis());
            tickets.put(newId, t);
            notifyStaffCategory(t);
            sendBotNotify(t);
            return t;
        } else {
            int id = nextId++;
            Ticket t = new Ticket(id, author, authorName, category.toLowerCase(), reason, System.currentTimeMillis());
            tickets.put(id, t);
            saveAll();
            notifyStaffCategory(t);
            sendBotNotify(t);
            return t;
        }
    }

    public boolean close(int id, UUID closer, String closerName, String reason) {
        Ticket t = tickets.get(id);
        if (t == null || !t.isOpen()) return false;
        t.setOpen(false);
        t.setClosedBy(closer);
        t.setClosedByName(closerName);
        t.setCloseReason(reason);
        t.setClosedAt(System.currentTimeMillis());

        if (useDb) closeTicket(id, closer, closerName, reason);
        else saveAll();

        Player author = Bukkit.getPlayer(t.getAuthor());
        if (author != null) {
            author.sendMessage(MessageUtils.get("ticket.player-notify-closed",
                    "id", String.valueOf(id), "staff", closerName));
        }
        if (plugin.getRoleplay() != null && plugin.getRoleplay().getBotManager() != null) {
            if (plugin.getRoleplay().getBotManager().isDiscordActive()) {
                plugin.getRoleplay().getBotManager().getDiscordBot().sendTicketClosed(t, closerName, reason);
            }
            if (plugin.getRoleplay().getBotManager().isTelegramActive()) {
                plugin.getRoleplay().getBotManager().getTelegramBot().sendTicketClosed(t, closerName, reason);
            }
        }
        return true;
    }

    public boolean reopen(int id) {
        Ticket t = tickets.get(id);
        if (t == null || t.isOpen()) return false;
        t.setOpen(true);
        t.setClosedBy(null);
        t.setClosedByName(null);
        t.setCloseReason(null);
        t.setClosedAt(0);

        if (useDb) reopenTicket(id);
        else saveAll();
        return true;
    }

    public boolean claim(int id, UUID staff, String staffName) {
        Ticket t = tickets.get(id);
        if (t == null) return false;
        t.setClaimedBy(staff);
        t.setClaimedByName(staffName);

        if (useDb) claimTicket(id, staff, staffName);
        else saveAll();

        Player author = Bukkit.getPlayer(t.getAuthor());
        if (author != null) {
            author.sendMessage(MessageUtils.get("ticket.player-notify-claimed",
                    "id", String.valueOf(id), "staff", staffName));
        }
        return true;
    }

    public boolean comment(int id, UUID commenter, String commenterName, String text, boolean staff) {
        Ticket t = tickets.get(id);
        if (t == null) return false;
        TicketComment tc = new TicketComment(commenter, commenterName, text, System.currentTimeMillis(), staff);
        t.addComment(tc);

        if (useDb) addComment(id, tc);
        else saveAll();

        Player author = Bukkit.getPlayer(t.getAuthor());
        if (author != null && !author.getUniqueId().equals(commenter)) {
            author.sendMessage(MessageUtils.get("ticket.player-notify-comment",
                    "id", String.valueOf(id)));
        }
        return true;
    }

    public boolean unclaim(int id) {
        Ticket t = tickets.get(id);
        if (t == null) return false;
        t.setClaimedBy(null);
        t.setClaimedByName(null);

        if (useDb) unclaimTicket(id);
        else saveAll();
        return true;
    }

    public boolean changeCategory(int id, String newCategory) {
        Ticket t = tickets.get(id);
        if (t == null) return false;
        if (!hasCategory(newCategory)) return false;
        t.setCategory(newCategory.toLowerCase());

        if (useDb) updateCategory(id, newCategory.toLowerCase());
        else saveAll();
        return true;
    }

    public boolean cancelComment(int ticketId, int commentIndex) {
        Ticket t = tickets.get(ticketId);
        if (t == null) return false;
        if (commentIndex < 0 || commentIndex >= t.getComments().size()) return false;
        TicketComment c = t.getComments().get(commentIndex);
        if (c.isCancelled()) return false;
        c.setCancelled(true);
        c.setText(MessageUtils.get("ticket.comment-cancelled-placeholder")
                .replace("§", "").replaceAll("&.", "").replaceAll("&#[A-Fa-f0-9]{6}", ""));

        if (!useDb) saveAll();
        return true;
    }

    public List<Ticket> listOpen() {
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : tickets.values()) if (t.isOpen()) out.add(t);
        out.sort(Comparator.comparingInt(Ticket::getId).reversed());
        return out;
    }

    public List<Ticket> listAll() {
        List<Ticket> out = new ArrayList<>(tickets.values());
        out.sort(Comparator.comparingInt(Ticket::getId).reversed());
        return out;
    }

    public List<String> getQuickReplies(String category) {
        if (category == null || category.isEmpty()) {
            return plugin.getConfig().getStringList("ticket.quick-replies.generale");
        }
        List<String> list = plugin.getConfig().getStringList("ticket.quick-replies." + category.toLowerCase());
        if (list.isEmpty()) {
            list = plugin.getConfig().getStringList("ticket.quick-replies.generale");
        }
        return list;
    }

    public Set<String> getQuickReplyCategories() {
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("ticket.quick-replies");
        if (sec == null) return new LinkedHashSet<>();
        return sec.getKeys(false);
    }

    public String getDateFormat() {
        return plugin.getConfig().getString("ticket.date-format", "dd/MM/yyyy HH:mm");
    }

    public Ticket get(int id) { return tickets.get(id); }

    public List<Ticket> listByAuthor(UUID author) {
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (t.getAuthor().equals(author)) out.add(t);
        }
        out.sort(Comparator.comparingInt(Ticket::getId).reversed());
        return out;
    }

    public List<Ticket> listClaimed(UUID staff) {
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (t.isClaimed() && t.getClaimedBy().equals(staff) && t.isOpen()) out.add(t);
        }
        out.sort(Comparator.comparingInt(Ticket::getId).reversed());
        return out;
    }

    public List<Ticket> list(UUID authorFilter, String categoryFilter) {
        List<Ticket> out = new ArrayList<>();
        for (Ticket t : tickets.values()) {
            if (authorFilter != null && !t.getAuthor().equals(authorFilter)) continue;
            if (categoryFilter != null && !t.getCategory().equalsIgnoreCase(categoryFilter)) continue;
            out.add(t);
        }
        out.sort(Comparator.comparingInt(Ticket::getId).reversed());
        return out;
    }

    public int countOpenByAuthor(UUID author) {
        int c = 0;
        for (Ticket t : tickets.values()) {
            if (t.isOpen() && t.getAuthor().equals(author)) c++;
        }
        return c;
    }

    public Collection<TicketCategory> getCategories() { return categories.values(); }
    public TicketCategory getCategory(String id) { return categories.get(id.toLowerCase()); }
    public boolean hasCategory(String id) { return categories.containsKey(id.toLowerCase()); }

    public int getPerPage() {
        return plugin.getConfig().getInt("ticket.list-per-page", 10);
    }

    private void notifyStaffCategory(Ticket t) {
        TicketCategory cat = getCategory(t.getCategory());
        String permNotify = cat != null ? cat.getPermissionNotify() : "";
        String staffPerm = plugin.getConfig().getString("ticket.staff-permission", "DistrictRP.ticket.staff");
        String msg = MessageUtils.get("ticket.staff-notify",
                "player", t.getAuthorName(),
                "id", String.valueOf(t.getId()),
                "category", t.getCategory(),
                "reason", t.getReason());
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.hasPermission(staffPerm) || (!permNotify.isEmpty() && p.hasPermission(permNotify))) {
                p.sendMessage(msg);
            }
        }
    }

    private void sendBotNotify(Ticket t) {
        if (plugin.getRoleplay() == null || plugin.getRoleplay().getBotManager() == null) return;
        if (plugin.getRoleplay().getBotManager().isDiscordActive()) {
            plugin.getRoleplay().getBotManager().getDiscordBot().sendTicketCreated(t);
        }
        if (plugin.getRoleplay().getBotManager().isTelegramActive()) {
            plugin.getRoleplay().getBotManager().getTelegramBot().sendTicketCreated(t);
        }
    }

    public boolean isUsingDatabase() { return useDb; }

    // --- accesso DB (ex TicketRepository, tickets + ticket_comments) ---

    private boolean dbReady() { return table != null && commentsTable != null; }

    public CompletableFuture<Integer> createTicket(Ticket t, String serverOrigin) {
        if (!dbReady()) return CompletableFuture.completedFuture(-1);
        return table.insert(t.getAuthor(), t.getAuthorName(), t.getCategory(), t.getReason(), serverOrigin);
    }

    public CompletableFuture<Boolean> closeTicket(int id, UUID staff, String staffName, String reason) {
        if (!dbReady()) return CompletableFuture.completedFuture(false);
        return table.close(id, staff, staffName, reason);
    }

    public CompletableFuture<Boolean> reopenTicket(int id) {
        if (!dbReady()) return CompletableFuture.completedFuture(false);
        return table.updateState(id, "OPEN");
    }

    public CompletableFuture<Boolean> claimTicket(int id, UUID staff, String staffName) {
        if (!dbReady()) return CompletableFuture.completedFuture(false);
        return table.claim(id, staff, staffName);
    }

    public CompletableFuture<Boolean> unclaimTicket(int id) {
        if (!dbReady()) return CompletableFuture.completedFuture(false);
        return table.claim(id, new UUID(0L, 0L), null);
    }

    public CompletableFuture<Boolean> updateCategory(int id, String category) {
        if (!dbReady()) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("UPDATE " + table.getTableName() +
                         " SET category=?, updated_at=? WHERE id=?")) {
                ps.setString(1, category);
                ps.setLong(2, System.currentTimeMillis());
                ps.setInt(3, id);
                return ps.executeUpdate() > 0;
            } catch (Exception e) {
                plugin.getLogger().warning("[Tickets] updateCategory: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Integer> addComment(int ticketId, TicketComment c) {
        if (!dbReady()) return CompletableFuture.completedFuture(-1);
        return commentsTable.add(ticketId, c.getCommenter(), c.getCommenterName(), c.isStaff(), c.getText());
    }

    public CompletableFuture<Boolean> cancelComment(int commentDbId, String replacementText) {
        if (!dbReady()) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("UPDATE " + commentsTable.getTableName() +
                         " SET content=? WHERE id=?")) {
                ps.setString(1, replacementText);
                ps.setInt(2, commentDbId);
                return ps.executeUpdate() > 0;
            } catch (Exception e) {
                return false;
            }
        });
    }

    public CompletableFuture<Ticket> loadTicket(int id) {
        if (!dbReady()) return CompletableFuture.completedFuture(null);
        return table.get(id).thenCompose(row -> {
            if (row == null) return CompletableFuture.completedFuture(null);
            return commentsTable.listByTicket(id).thenApply(commentRows -> buildTicket(row, commentRows));
        });
    }

    public CompletableFuture<List<Ticket>> fetchAll() {
        if (!dbReady()) return CompletableFuture.completedFuture(new ArrayList<>());
        return CompletableFuture.supplyAsync(() -> {
            List<Ticket> out = new ArrayList<>();
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("SELECT * FROM " + table.getTableName() + " ORDER BY id ASC")) {
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        TicketRow r = new TicketRow();
                        r.id = rs.getInt("id");
                        r.authorUuid = UUID.fromString(rs.getString("author_uuid"));
                        r.authorName = rs.getString("author_name");
                        r.category = rs.getString("category");
                        r.reason = rs.getString("reason");
                        r.state = rs.getString("state");
                        String cu = rs.getString("claimed_by_uuid");
                        r.claimedByUuid = cu != null ? UUID.fromString(cu) : null;
                        r.claimedByName = rs.getString("claimed_by_name");
                        String cbu = rs.getString("closed_by_uuid");
                        r.closedByUuid = cbu != null ? UUID.fromString(cbu) : null;
                        r.closedByName = rs.getString("closed_by_name");
                        r.closeReason = rs.getString("close_reason");
                        r.createdAt = rs.getLong("created_at");
                        r.updatedAt = rs.getLong("updated_at");
                        long ca = rs.getLong("closed_at");
                        r.closedAt = rs.wasNull() ? null : ca;
                        r.serverOrigin = rs.getString("server_origin");
                        out.add(buildTicket(r, commentsTable.listByTicket(r.id).join()));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[Tickets] loadAll: " + e.getMessage());
            }
            return out;
        });
    }

    private Ticket buildTicket(TicketRow r, List<CommentRow> commentRows) {
        Ticket t = new Ticket(r.id, r.authorUuid, r.authorName, r.category, r.reason, r.createdAt);
        t.setOpen("OPEN".equalsIgnoreCase(r.state));
        t.setClosedBy(r.closedByUuid);
        t.setClosedByName(r.closedByName);
        t.setCloseReason(r.closeReason);
        if (r.closedAt != null) t.setClosedAt(r.closedAt);
        if (r.claimedByUuid != null && !r.claimedByUuid.equals(new UUID(0L, 0L))) {
            t.setClaimedBy(r.claimedByUuid);
            t.setClaimedByName(r.claimedByName);
        }
        if (commentRows != null) {
            for (CommentRow cr : commentRows) {
                t.addComment(new TicketComment(cr.authorUuid, cr.authorName, cr.content, cr.createdAt, cr.staff));
            }
        }
        return t;
    }
}