package dev.breach.DistrictRP.commands.roleplay.ticket;

import org.bukkit.Material;

import java.util.List;

public class TicketCategory {

    private final String id;
    private final String name;
    private final Material material;
    private final int slot;
    private final List<String> lore;
    private final String permissionNotify;

    public TicketCategory(String id, String name, Material material, int slot, List<String> lore, String permissionNotify) {
        this.id = id;
        this.name = name;
        this.material = material;
        this.slot = slot;
        this.lore = lore;
        this.permissionNotify = permissionNotify;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public Material getMaterial() { return material; }
    public int getSlot() { return slot; }
    public List<String> getLore() { return lore; }
    public String getPermissionNotify() { return permissionNotify; }
}