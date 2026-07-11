package dev.breach.DistrictRP.database.repository;

import dev.breach.DistrictRP.DistrictRP;
import dev.breach.DistrictRP.commands.roleplay.appuntamenti.Appuntamento;
import dev.breach.DistrictRP.database.Repository;
import dev.breach.DistrictRP.database.tables.AppuntamentiTable;
import dev.breach.DistrictRP.database.tables.AppuntamentiTable.Row;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class AppuntamentoRepository implements Repository {

    private final DistrictRP plugin;
    private AppuntamentiTable table;

    public AppuntamentoRepository(DistrictRP plugin) {
        this.plugin = plugin;
        if (plugin.getDatabaseManager() != null && plugin.getDatabaseManager().isMariaDb()) {
            this.table = plugin.getDatabaseManager().getTable("appuntamenti", AppuntamentiTable.class);
        }
    }

    @Override
    public boolean isAvailable() {
        return table != null;
    }

    public CompletableFuture<Integer> book(UUID uuid, String name, String reparto, String giorno, String orario) {
        if (!isAvailable()) return CompletableFuture.completedFuture(-1);
        return table.book(uuid, name, reparto, giorno, orario);
    }

    public CompletableFuture<Boolean> cancel(int id) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return table.cancel(id);
    }

    public CompletableFuture<List<Appuntamento>> fetchAll() {
        if (!isAvailable()) return CompletableFuture.completedFuture(new ArrayList<>());
        return CompletableFuture.supplyAsync(() -> {
            List<Appuntamento> out = new ArrayList<>();
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement("SELECT * FROM " + table.getTableName() + " ORDER BY id ASC")) {
                try (var rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Row r = new Row();
                        r.id = rs.getInt("id");
                        r.playerUuid = UUID.fromString(rs.getString("player_uuid"));
                        r.playerName = rs.getString("player_name");
                        r.reparto = rs.getString("reparto");
                        r.giorno = rs.getString("giorno");
                        r.orario = rs.getString("orario");
                        r.createdAt = rs.getLong("created_at");
                        out.add(toAppuntamento(r));
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[AppuntamentoRepo] loadAll: " + e.getMessage());
            }
            return out;
        });
    }

    public CompletableFuture<Boolean> isSlotTaken(String reparto, String giorno, String orario) {
        if (!isAvailable()) return CompletableFuture.completedFuture(false);
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM " + table.getTableName() +
                    " WHERE reparto=? AND giorno=? AND orario=?";
            try (var c = plugin.getDatabaseManager().getDataStore().getConnection();
                 var ps = c.prepareStatement(sql)) {
                ps.setString(1, reparto);
                ps.setString(2, giorno);
                ps.setString(3, orario);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            } catch (Exception ignored) {}
            return false;
        });
    }

    private Appuntamento toAppuntamento(Row r) {
        return new Appuntamento(r.id, r.playerUuid, r.playerName, r.reparto, r.giorno, r.orario, r.createdAt);
    }
}