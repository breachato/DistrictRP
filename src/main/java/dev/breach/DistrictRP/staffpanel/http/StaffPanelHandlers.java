package dev.breach.DistrictRP.staffpanel.http;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import dev.breach.DistrictRP.staffpanel.PasswordUtil;
import dev.breach.DistrictRP.staffpanel.StaffPanelManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.io.IOException;
import java.util.*;

public class StaffPanelHandlers {

    private final StaffPanelManager mgr;
    private final String apiToken;

    public StaffPanelHandlers(StaffPanelManager mgr, String apiToken) {
        this.mgr = mgr;
        this.apiToken = apiToken;
    }

    private boolean checkToken(HttpExchange ex) throws IOException {
        String h = ex.getRequestHeaders().getFirst("X-API-Token");
        if (apiToken == null || apiToken.isEmpty() || !apiToken.equals(h)) {
            JsonUtil.writeError(ex, 401, "unauthorized");
            return false;
        }
        return true;
    }

    private JsonObject parseBody(HttpExchange ex) throws IOException {
        String body = JsonUtil.readBody(ex);
        if (body == null || body.isEmpty()) return new JsonObject();
        try {
            return JsonParser.parseString(body).getAsJsonObject();
        } catch (Throwable t) {
            return new JsonObject();
        }
    }

    public void login(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { JsonUtil.writeError(ex, 405, "method_not_allowed"); return; }

        JsonObject body = parseBody(ex);
        String email = body.has("email") ? body.get("email").getAsString().trim() : "";
        String password = body.has("password") ? body.get("password").getAsString() : "";

        if (!PasswordUtil.isValidEmail(email) || password.isEmpty()) {
            JsonUtil.writeError(ex, 400, "invalid_credentials");
            return;
        }

        Map<String, Object> user = mgr.accounts().findByEmail(email).join();
        if (user == null) { JsonUtil.writeError(ex, 401, "invalid_credentials"); return; }

        String hash = (String) user.get("password_hash");
        if (hash == null || !PasswordUtil.verify(password, hash)) {
            JsonUtil.writeError(ex, 401, "invalid_credentials");
            return;
        }

        mgr.accounts().touchLastLogin((String) user.get("uuid"));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("user", Map.of(
                "uuid", user.get("uuid"),
                "username", user.get("username"),
                "email", user.get("email"),
                "role", user.get("role") != null ? user.get("role") : "staff"
        ));
        JsonUtil.writeJson(ex, 200, out);
    }

    public void departments(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        String m = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] parts = path.split("/");

        if ("GET".equalsIgnoreCase(m)) {
            List<Map<String, Object>> depts = mgr.departments().listAll().join();
            for (Map<String, Object> d : depts) {
                String id = (String) d.get("id");
                d.put("columns", mgr.columns().listFor(id).join());
                d.put("memberCount", mgr.staffDept().listByDepartment(id).join().size());
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("departments", depts);
            JsonUtil.writeJson(ex, 200, out);
            return;
        }

        if ("POST".equalsIgnoreCase(m)) {
            JsonObject b = parseBody(ex);
            String id = b.get("id").getAsString();
            String name = b.get("name").getAsString();
            String color = b.has("color") ? b.get("color").getAsString() : "#c9a84c";
            int position = b.has("position") ? b.get("position").getAsInt() : 0;
            mgr.departments().upsert(id, name, color, position).join();
            JsonUtil.writeJson(ex, 200, Map.of("success", true));
            return;
        }

        if ("DELETE".equalsIgnoreCase(m) && parts.length >= 4) {
            String id = parts[3];
            mgr.departments().delete(id).join();
            JsonUtil.writeJson(ex, 200, Map.of("success", true));
            return;
        }

        JsonUtil.writeError(ex, 405, "method_not_allowed");
    }

    public void staffers(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        String m = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String[] q = path.split("/");

        if ("GET".equalsIgnoreCase(m) && q.length >= 4) {
            String deptId = q[3];
            List<Map<String, Object>> staffers = mgr.staffDept().listByDepartment(deptId).join();
            for (Map<String, Object> s : staffers) {
                UUID uuid = UUID.fromString((String) s.get("uuid"));
                s.put("counters", mgr.counters().valuesFor(uuid, deptId).join());
                s.put("flags", mgr.flags().flagsFor(uuid, deptId).join());
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("success", true);
            out.put("staffers", staffers);
            JsonUtil.writeJson(ex, 200, out);
            return;
        }

        if ("DELETE".equalsIgnoreCase(m) && q.length >= 5) {
            String deptId = q[3];
            UUID uuid = UUID.fromString(q[4]);
            mgr.staffDept().unassign(uuid, deptId).join();
            JsonUtil.writeJson(ex, 200, Map.of("success", true));
            return;
        }

        JsonUtil.writeError(ex, 405, "method_not_allowed");
    }

    public void manualStaff(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { JsonUtil.writeError(ex, 405, "method_not_allowed"); return; }
        JsonObject b = parseBody(ex);
        String deptId = b.get("departmentId").getAsString();
        String username = b.get("username").getAsString();

        OfflinePlayer op = Bukkit.getOfflinePlayer(username);
        UUID uuid = op.getUniqueId();
        if (uuid == null) { JsonUtil.writeError(ex, 404, "player_not_found"); return; }
        String name = op.getName() != null ? op.getName() : username;
        mgr.staffDept().assign(uuid, name, deptId).join();
        JsonUtil.writeJson(ex, 200, Map.of("success", true, "uuid", uuid.toString(), "username", name));
    }

    public void counters(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { JsonUtil.writeError(ex, 405, "method_not_allowed"); return; }
        JsonObject b = parseBody(ex);
        String deptId = b.get("departmentId").getAsString();
        UUID uuid = UUID.fromString(b.get("uuid").getAsString());
        String col = b.get("column").getAsString();
        String op = b.has("op") ? b.get("op").getAsString() : "set";
        int val = b.has("value") ? b.get("value").getAsInt() : 0;
        int delta = b.has("delta") ? b.get("delta").getAsInt() : 0;

        int newVal;
        if ("increment".equalsIgnoreCase(op)) newVal = mgr.counters().increment(uuid, deptId, col, delta).join();
        else newVal = mgr.counters().setValue(uuid, deptId, col, val).join();

        JsonUtil.writeJson(ex, 200, Map.of("success", true, "value", newVal));
    }

    public void flags(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { JsonUtil.writeError(ex, 405, "method_not_allowed"); return; }
        JsonObject b = parseBody(ex);
        String deptId = b.get("departmentId").getAsString();
        UUID uuid = UUID.fromString(b.get("uuid").getAsString());
        String flag = b.get("flag").getAsString();
        boolean active = b.get("active").getAsBoolean();
        String note = b.has("note") ? b.get("note").getAsString() : null;
        mgr.flags().setFlag(uuid, deptId, flag, active, note).join();
        JsonUtil.writeJson(ex, 200, Map.of("success", true));
    }

    public void columns(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        String m = ex.getRequestMethod();
        if ("POST".equalsIgnoreCase(m)) {
            JsonObject b = parseBody(ex);
            String deptId = b.get("departmentId").getAsString();
            String col = b.get("column").getAsString();
            int pos = b.has("position") ? b.get("position").getAsInt() : 0;
            mgr.columns().add(deptId, col, pos).join();
            JsonUtil.writeJson(ex, 200, Map.of("success", true));
            return;
        }
        if ("DELETE".equalsIgnoreCase(m)) {
            JsonObject b = parseBody(ex);
            String deptId = b.get("departmentId").getAsString();
            String col = b.get("column").getAsString();
            mgr.columns().remove(deptId, col).join();
            JsonUtil.writeJson(ex, 200, Map.of("success", true));
            return;
        }
        JsonUtil.writeError(ex, 405, "method_not_allowed");
    }

    public void stats(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        List<Map<String, Object>> depts = mgr.departments().listAll().join();
        int totalStaff = mgr.staffDept().countDistinctStaffers().join();
        int total = mgr.counters().sumAll().join();

        Map<String, Object> topDept = null;
        int topVal = -1;
        List<Map<String, Object>> perDept = new ArrayList<>();
        for (Map<String, Object> d : depts) {
            String id = (String) d.get("id");
            int s = mgr.counters().sumForDepartment(id).join();
            Map<String, Object> row = new LinkedHashMap<>(d);
            row.put("total", s);
            perDept.add(row);
            if (s > topVal) { topVal = s; topDept = row; }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("success", true);
        out.put("activeStaff", totalStaff);
        out.put("totalTickets", total);
        out.put("totalDepartments", depts.size());
        out.put("topDepartment", topDept);
        out.put("perDepartment", perDept);
        out.put("lastUpdate", System.currentTimeMillis());
        JsonUtil.writeJson(ex, 200, out);
    }

    public void syncStaff(HttpExchange ex) throws IOException {
        if (!checkToken(ex)) return;
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { JsonUtil.writeError(ex, 405, "method_not_allowed"); return; }
        int count = mgr.syncStaffFromRanks();
        JsonUtil.writeJson(ex, 200, Map.of("success", true, "synced", count));
    }
}