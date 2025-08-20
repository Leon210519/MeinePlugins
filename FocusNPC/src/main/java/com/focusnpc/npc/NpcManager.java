package com.focusnpc.npc;

import com.focusnpc.FocusNPCPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.*;

public class NpcManager {
    private final FocusNPCPlugin plugin;
    private final boolean citizens;
    private Object npcRegistry;
    private Method createNpc;
    private Method spawn;
    private Method getId;
    private Method destroy;
    private Method getById;
    private Method getOrAddTrait;
    private Method getEntity;
    private Class<?> skinTraitClass;
    private Method setSkinName;
    private final List<FocusNpc> npcs = new ArrayList<>();

    public NpcManager(FocusNPCPlugin plugin) {
        this.plugin = plugin;
        this.citizens = Bukkit.getPluginManager().isPluginEnabled("Citizens");
        if (citizens) {
            try {
                Class<?> api = Class.forName("net.citizensnpcs.api.CitizensAPI");
                npcRegistry = api.getMethod("getNPCRegistry").invoke(null);
                createNpc = npcRegistry.getClass().getMethod("createNPC", EntityType.class, String.class);
                Class<?> npcClass = Class.forName("net.citizensnpcs.api.npc.NPC");
                spawn = npcClass.getMethod("spawn", Location.class);
                getId = npcClass.getMethod("getId");
                destroy = npcClass.getMethod("destroy");
                getById = npcRegistry.getClass().getMethod("getById", int.class);
                getOrAddTrait = npcClass.getMethod("getOrAddTrait", Class.class);
                getEntity = npcClass.getMethod("getEntity");
                skinTraitClass = Class.forName("net.citizensnpcs.api.trait.trait.SkinTrait");
                setSkinName = skinTraitClass.getMethod("setSkinName", String.class);

                // register click event via reflection
                Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("net.citizensnpcs.api.event.NPCRightClickEvent");
                Bukkit.getPluginManager().registerEvent(eventClass, new Listener() {}, EventPriority.NORMAL, new EventExecutor() {
                    @Override
                    public void execute(Listener listener, Event event) throws EventException {
                        try {
                            Object npc = eventClass.getMethod("getNPC").invoke(event);
                            int id = (int) getId.invoke(npc);
                            Object player = eventClass.getMethod("getClicker").invoke(event);
                            if (player instanceof org.bukkit.entity.Player p) {
                                FocusNpc fn = getByCitizenId(id);
                                if (fn != null) {
                                    plugin.getGuiFactory().openGui(p, fn.getType());
                                }
                            }
                        } catch (Exception ignored) {}
                    }
                }, plugin);
            } catch (Exception e) {
                // disable citizens features on failure
            }
        }
    }

    public void load() {
        npcs.clear();
        List<Map<?, ?>> list = plugin.getConfig().getMapList("npcs.saved");
        for (Map<?, ?> map : list) {
            try {
                NpcType type = NpcType.valueOf((String) map.get("type"));
                World world = Bukkit.getWorld((String) map.get("world"));
                if (world == null) continue;
                double x = ((Number) map.get("x")).doubleValue();
                double y = ((Number) map.get("y")).doubleValue();
                double z = ((Number) map.get("z")).doubleValue();
                float yaw = ((Number) (map.containsKey("yaw") ? map.get("yaw") : 0D)).floatValue();
                float pitch = ((Number) (map.containsKey("pitch") ? map.get("pitch") : 0D)).floatValue();
                String skin = (String) map.get("skin");
                Location loc = new Location(world, x, y, z, yaw, pitch);
                spawnNpc(type, loc, skin);
            } catch (Exception ignored) {}
        }
    }

    public void reload() {
        despawnAll();
        load();
    }

    public FocusNpc spawnNpc(NpcType type, Location loc, String skin) {
        FocusNpc fnpc = new FocusNpc(type, loc, skin);
        if (citizens && npcRegistry != null) {
            try {
                Object npc = createNpc.invoke(npcRegistry, EntityType.PLAYER, getName(type));
                spawn.invoke(npc, loc);
                Object trait = getOrAddTrait.invoke(npc, skinTraitClass);
                if (skin != null && !skin.isEmpty()) {
                    setSkinName.invoke(trait, skin);
                }
                Object entity = getEntity.invoke(npc);
                if (entity instanceof LivingEntity le) {
                    le.setSilent(true);
                }
                int id = (int) getId.invoke(npc);
                fnpc.setCitizen(npc);
                fnpc.setCitizenId(id);
            } catch (Exception ignored) {
            }
        } else {
            Villager villager = (Villager) loc.getWorld().spawnEntity(loc, EntityType.VILLAGER);
            villager.setProfession(type == NpcType.FARMER ? Villager.Profession.FARMER : Villager.Profession.TOOLSMITH);
            villager.setAI(false);
            villager.setSilent(true);
            villager.setCustomName(getName(type));
            villager.setCustomNameVisible(true);
            fnpc.setEntityId(villager.getUniqueId());
        }
        npcs.add(fnpc);
        return fnpc;
    }

    public void despawnAll() {
        for (FocusNpc npc : npcs) {
            if (citizens && npc.getCitizen() != null) {
                try { destroy.invoke(npc.getCitizen()); } catch (Exception ignored) {}
            } else if (npc.getEntityId() != null) {
                Entity ent = Bukkit.getEntity(npc.getEntityId());
                if (ent != null) ent.remove();
            }
        }
        npcs.clear();
    }

    public boolean removeNearest(Location loc) {
        FocusNpc nearest = getNearest(loc);
        if (nearest == null) return false;
        if (citizens && nearest.getCitizen() != null) {
            try { destroy.invoke(nearest.getCitizen()); } catch (Exception ignored) {}
        } else if (nearest.getEntityId() != null) {
            Entity ent = Bukkit.getEntity(nearest.getEntityId());
            if (ent != null) ent.remove();
        }
        npcs.remove(nearest);
        save();
        return true;
    }

    private FocusNpc getNearest(Location loc) {
        FocusNpc best = null;
        double dist = Double.MAX_VALUE;
        for (FocusNpc n : npcs) {
            Location l = n.getLocation();
            if (!l.getWorld().equals(loc.getWorld())) continue;
            double d = l.distanceSquared(loc);
            if (d < dist) {
                dist = d;
                best = n;
            }
        }
        if (dist > 25) return null;
        return best;
    }

    public FocusNpc getByCitizenId(int id) {
        for (FocusNpc n : npcs) {
            if (n.getCitizenId() != null && n.getCitizenId() == id) return n;
        }
        return null;
    }

    public FocusNpc getByEntityId(UUID id) {
        for (FocusNpc n : npcs) {
            if (id.equals(n.getEntityId())) return n;
        }
        return null;
    }

    public List<FocusNpc> getNpcs() { return Collections.unmodifiableList(npcs); }

    public void save() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (FocusNpc n : npcs) {
            Location l = n.getLocation();
            Map<String, Object> map = new HashMap<>();
            map.put("type", n.getType().name());
            map.put("world", l.getWorld().getName());
            map.put("x", l.getX());
            map.put("y", l.getY());
            map.put("z", l.getZ());
            map.put("yaw", l.getYaw());
            map.put("pitch", l.getPitch());
            map.put("skin", n.getSkin());
            list.add(map);
        }
        plugin.getConfig().set("npcs.saved", list);
        plugin.saveConfig();
    }

    private String getName(NpcType type) {
        return type == NpcType.FARMER ? ChatColor.GREEN + "Farmer" : ChatColor.AQUA + "Miner";
    }
}
