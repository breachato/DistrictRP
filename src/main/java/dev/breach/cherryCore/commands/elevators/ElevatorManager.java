package dev.breach.cherryCore.commands.elevators;

import dev.breach.cherryCore.CherryCore;
import dev.breach.cherryCore.functions.DataManager;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Gestione centralizzata degli ascensori.
 * Persistenza su elevators.yml:
 *
 * elevators:
 *   <id>:
 *     loc: { world,x,y,z,yaw,pitch }
 *     owner: <uuid>
 *     type: <material>
 *     etype: classic|express|vip|freight|glass
 *     group: <name>
 *     floor: <custom name|null>
 *     cooldown: <seconds|null>
 *     links: [<id>, <id>, ...]
 *
 * config:
 *   cooldown_default: 2
 *   floor_prefix: "Piano"
 *   disabled_worlds: [<world_name>, ...]
 */
public class ElevatorManager {

    private final CherryCore plugin;
    private final FileConfiguration cfg;

    public ElevatorManager(CherryCore plugin) {
        this.plugin = plugin;
        this.cfg = plugin.getDataManager().elevators();

        if (!cfg.isSet("config.cooldown_default")) cfg.set("config.cooldown_default", 2);
        if (!cfg.isSet("config.floor_prefix"))     cfg.set("config.floor_prefix", "Piano");
        if (!cfg.isSet("config.disabled_worlds"))  cfg.set("config.disabled_worlds", new ArrayList<String>());
        save();
    }

    public void save() { plugin.getDataManager().saveElevators(); }

    // ===== Config =====
    public int getDefaultCooldown()      { return cfg.getInt("config.cooldown_default", 2); }
    public void setDefaultCooldown(int v){ cfg.set("config.cooldown_default", Math.max(0, v)); save(); }

    public String getFloorPrefix()           { return cfg.getString("config.floor_prefix", "Piano"); }
    public void setFloorPrefix(String pref)  { cfg.set("config.floor_prefix", pref); save(); }

    public List<String> getDisabledWorlds()  { return cfg.getStringList("config.disabled_worlds"); }
    public boolean isWorldDisabled(String w) { return getDisabledWorlds().contains(w); }

    // ===== ID helpers =====
    public static String locToId(Location l) {
        return l.getWorld().getName() + "_" +
                l.getBlockX() + "_" +
                l.getBlockY() + "_" +
                l.getBlockZ();
    }

    public Location getLocation(String id) {
        ConfigurationSection sec = cfg.getConfigurationSection("elevators." + id + ".loc");
        return DataManager.sectionToLoc(sec);
    }

    public boolean exists(String id)        { return cfg.isSet("elevators." + id); }
    public boolean exists(Location l)       { return exists(locToId(l)); }

    public Set<String> allIds() {
        ConfigurationSection s = cfg.getConfigurationSection("elevators");
        return s == null ? Collections.emptySet() : new HashSet<>(s.getKeys(false));
    }

    // ===== CRUD =====
    public void create(Location l, Player owner, String etype, String material) {
        String id = locToId(l);
        cfg.set("elevators." + id + ".loc", DataManager.locToMap(l));
        cfg.set("elevators." + id + ".owner", owner.getUniqueId().toString());
        cfg.set("elevators." + id + ".type", material);
        cfg.set("elevators." + id + ".etype", etype);
        cfg.set("elevators." + id + ".group", "default");
        save();
    }

    public void delete(String id) {
        // Pulizia link reciproci
        List<String> links = getLinks(id);
        for (String other : links) {
            List<String> ol = getLinks(other);
            ol.remove(id);
            cfg.set("elevators." + other + ".links", ol);
        }
        cfg.set("elevators." + id, null);
        save();
    }

    public void deleteAll() {
        cfg.set("elevators", null);
        save();
    }

    // ===== Properties =====
    public String getOwner(String id)       { return cfg.getString("elevators." + id + ".owner"); }

    public String getEtype(String id)       { return cfg.getString("elevators." + id + ".etype", "classic"); }
    public void setEtype(String id, String v){ cfg.set("elevators." + id + ".etype", v); save(); }

    public String getGroup(String id)       { return cfg.getString("elevators." + id + ".group", "default"); }
    public void setGroup(String id, String g){ cfg.set("elevators." + id + ".group", g); save(); }

    public String getCustomFloor(String id) { return cfg.getString("elevators." + id + ".floor"); }
    public void setCustomFloor(String id, String name) {
        cfg.set("elevators." + id + ".floor", name); save();
    }
    public void resetCustomFloor(String id) {
        cfg.set("elevators." + id + ".floor", null); save();
    }

    public int getCooldown(String id) {
        if (cfg.isSet("elevators." + id + ".cooldown"))
            return cfg.getInt("elevators." + id + ".cooldown");
        return getDefaultCooldown();
    }
    public void setCooldown(String id, int v) {
        cfg.set("elevators." + id + ".cooldown", Math.max(0, v)); save();
    }

    public boolean canEdit(Player p, String id) {
        if (p.hasPermission("elevators.admin")) return true;
        String owner = getOwner(id);
        return owner != null && owner.equals(p.getUniqueId().toString());
    }

    // ===== Links =====
    public List<String> getLinks(String id) {
        return new ArrayList<>(cfg.getStringList("elevators." + id + ".links"));
    }

    public void linkBoth(String a, String b) {
        List<String> la = getLinks(a);
        if (!la.contains(b)) la.add(b);
        cfg.set("elevators." + a + ".links", la);

        List<String> lb = getLinks(b);
        if (!lb.contains(a)) lb.add(a);
        cfg.set("elevators." + b + ".links", lb);
        save();
    }

    public void unlinkBoth(String a, String b) {
        List<String> la = getLinks(a); la.remove(b);
        cfg.set("elevators." + a + ".links", la);
        List<String> lb = getLinks(b); lb.remove(a);
        cfg.set("elevators." + b + ".links", lb);
        save();
    }

    public boolean isLinked(String a, String b) {
        return getLinks(a).contains(b) || getLinks(b).contains(a);
    }

    // ===== Naming =====
    public String getAutoFloorName(String id) {
        int num = getFloorNumber(id);
        String prefix = getFloorPrefix();
        if (num == 1) return prefix + " Terra";
        return prefix + " " + (num - 1);
    }

    public String getFloorName(String id) {
        String custom = getCustomFloor(id);
        return custom != null ? custom : getAutoFloorName(id);
    }

    /**
     * Numera i piani della "colonna" o del gruppo collegato in base alla Y.
     */
    public int getFloorNumber(String id) {
        Location loc = getLocation(id);
        if (loc == null) return 1;
        List<String> column = getColumn(id);
        List<Double> ys = new ArrayList<>();
        for (String otherId : column) {
            Location ol = getLocation(otherId);
            if (ol != null) ys.add(ol.getY());
        }
        Collections.sort(ys);
        for (int i = 0; i < ys.size(); i++) {
            if (ys.get(i) == loc.getY()) return i + 1;
        }
        return 1;
    }

    /**
     * Restituisce tutti gli ID nella stessa "colonna logica":
     *   stessa world+x+z+group OPPURE collegati via link.
     */
    public List<String> getColumn(String id) {
        Location origin = getLocation(id);
        if (origin == null) return Collections.singletonList(id);
        String group = getGroup(id);
        List<String> result = new ArrayList<>();
        for (String other : allIds()) {
            Location ol = getLocation(other);
            if (ol == null) continue;
            boolean same = false;
            if (ol.getWorld().equals(origin.getWorld())
                    && ol.getBlockX() == origin.getBlockX()
                    && ol.getBlockZ() == origin.getBlockZ()
                    && getGroup(other).equals(group)) {
                same = true;
            }
            if (isLinked(id, other)) same = true;
            if (other.equals(id))    same = true;
            if (same) result.add(other);
        }
        return result;
    }

    // ===== Movement =====
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public boolean isOnCooldown(Player p) {
        Long until = cooldowns.get(p.getUniqueId());
        return until != null && until > System.currentTimeMillis();
    }

    public void setCooldownFor(Player p, int seconds) {
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + seconds * 1000L);
    }

    /**
     * Cerca il piano destinazione in direzione "up" o "down" rispetto a originId.
     * @return ID destinazione o null.
     */
    public String findDestination(String originId, boolean up) {
        Location origin = getLocation(originId);
        if (origin == null) return null;
        List<String> col = getColumn(originId);
        String best = null;
        double bestY = 0;
        for (String other : col) {
            if (other.equals(originId)) continue;
            Location ol = getLocation(other);
            if (ol == null) continue;
            double y = ol.getY();
            if (up && y > origin.getY()) {
                if (best == null || y < bestY) { best = other; bestY = y; }
            } else if (!up && y < origin.getY()) {
                if (best == null || y > bestY) { best = other; bestY = y; }
            }
        }
        return best;
    }
}