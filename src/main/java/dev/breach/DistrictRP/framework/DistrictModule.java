package dev.breach.DistrictRP.framework;

import dev.breach.DistrictRP.functions.MessageUtils;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.List;

public abstract class DistrictModule {

    private ModuleContext ctx;
    private ModuleState state = ModuleState.LOADED;

    public abstract String id();

    public String displayName() { return id(); }

    public String description() { return ""; }

    public List<Listener> buildListeners() {
        return Collections.emptyList();
    }

    public void onLoad() {}
    public void onEnable() {}
    public void onDisable() {}
    public void onReload() {}

    void bindContext(ModuleContext ctx) { this.ctx = ctx; }
    public ModuleContext ctx() { return ctx; }

    public void setState(ModuleState s) { this.state = s; }
    public ModuleState state() { return state; }

    public String commandPermission(String root, String... more) {
        StringBuilder sb = new StringBuilder("DistrictRP.modules.").append(id()).append(".command.").append(root);
        for (String s : more) sb.append(".").append(s);
        return sb.toString();
    }

    public String featurePermission(String root, String... more) {
        StringBuilder sb = new StringBuilder("DistrictRP.modules.").append(id()).append(".").append(root);
        for (String s : more) sb.append(".").append(s);
        return sb.toString();
    }

    public String msg(String key, Object... placeholders) {
        if (ctx == null || ctx.messages() == null) return "&c[no ctx: " + key + "]";
        return ctx.messages().get(key, placeholders);
    }

    public List<String> msgList(String key, Object... placeholders) {
        if (ctx == null || ctx.messages() == null) return Collections.emptyList();
        return ctx.messages().getList(key, placeholders);
    }

    public void send(CommandSender sender, String key, Object... placeholders) {
        if (sender == null) return;
        sender.sendMessage(MessageUtils.color(msg(key, placeholders)));
    }

    public void sendList(CommandSender sender, String key, Object... placeholders) {
        if (sender == null) return;
        for (String line : msgList(key, placeholders)) sender.sendMessage(line);
    }

    public String msgMiniMessage(String key, TagResolver... resolvers) {
        return msg(key);
    }
}