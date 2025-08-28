package com.lootpets.service;

import com.lootpets.LootPetsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collects short-lived per-player traces for debugging purposes. The trace is
 * purely in-memory and safe for use on production servers when enabled by an
 * administrator.
 */
public class TraceService {
    private record Session(UUID player, long end, List<String> events, CommandSender receiver) {}

    private final LootPetsPlugin plugin;
    private final DebugService debug;
    private final Map<UUID, Session> sessions = new ConcurrentHashMap<>();

    public TraceService(LootPetsPlugin plugin, DebugService debug) {
        this.plugin = plugin;
        this.debug = debug;
    }

    public void start(CommandSender sender, Player target, int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        Session s = new Session(target.getUniqueId(), end, new ArrayList<>(), sender);
        sessions.put(target.getUniqueId(), s);
        Bukkit.getScheduler().runTaskLater(plugin, () -> stop(target.getUniqueId(), sender), seconds * 20L);
    }

    public void stop(UUID uuid, CommandSender sender) {
        Session s = sessions.remove(uuid);
        if (s == null) {
            if (sender != null) {
                sender.sendMessage(color(plugin.getLang().getString("trace-none")));
            }
            return;
        }
        CommandSender recv = sender != null ? sender : s.receiver;
        recv.sendMessage(color(plugin.getLang().getString("trace-stop")
                .replace("%player%", getName(uuid))
                .replace("%count%", String.valueOf(s.events.size()))));
        writeFile(uuid, s.events);
    }

    private String getName(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        return p != null ? p.getName() : uuid.toString();
    }

    private void writeFile(UUID uuid, List<String> events) {
        File dir = new File(plugin.getDataFolder(), "diagnostics/traces");
        dir.mkdirs();
        String ts = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        File out = new File(dir, "trace-" + uuid + "-" + ts + ".log");
        try (FileWriter w = new FileWriter(out, StandardCharsets.UTF_8)) {
            for (String e : events) {
                w.write(e);
                w.write("\n");
            }
        } catch (IOException ignored) {}
    }

    public void log(UUID uuid, String message) {
        Session s = sessions.get(uuid);
        if (s == null) return;
        s.events.add(System.currentTimeMillis() + ": " + message);
    }

    public void shutdown() {
        sessions.clear();
    }

    private String color(String in) {
        return com.lootpets.util.Colors.color(in);
    }
}
