package com.lootfactory.prestige;

import com.lootfactory.factory.FactoryManager;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Field;
import java.util.*;

/**
 * FactoryGatewayImpl — tailored for LootFactory's FactoryManager
 *
 * Uses FactoryManager internals:
 *  - byLocation: Map<Location, FactoryInstance>
 *  - byOwner:    Map<UUID, Set<FactoryInstance>>
 * And FactoryInstance fields: owner(UUID), typeId(String), level(int), xpSeconds(double), location(Location)
 *
 * Reset implementation:
 *  - Prefer calling instance methods if they exist.
 *  - Otherwise, set fields directly: level = given level; xpSeconds = 0.0.
 */
public final class FactoryGatewayImpl implements FactoryGateway {

    private final FactoryManager fm;

    // reflected caches
    private Field fByLocation;
    private Field fByOwner;

    private Class<?> factoryInstanceClass;
    private Field fiOwner;
    private Field fiTypeId;
    private Field fiLevel;
    private Field fiXpSeconds;
    private Field fiLocation;

    public FactoryGatewayImpl(FactoryManager fm) {
        this.fm = fm;
        initReflection();
    }

    @SuppressWarnings("unchecked")
    private void initReflection() {
        try {
            fByLocation = FactoryManager.class.getDeclaredField("byLocation");
            fByLocation.setAccessible(true);
        } catch (Throwable ignored) {}
        try {
            fByOwner = FactoryManager.class.getDeclaredField("byOwner");
            fByOwner.setAccessible(true);
        } catch (Throwable ignored) {}

        // Detect inner FactoryInstance class by its fields
        try {
            for (Class<?> c : FactoryManager.class.getDeclaredClasses()) {
                try {
                    Field o = c.getDeclaredField("owner");
                    Field t = c.getDeclaredField("typeId");
                    Field l = c.getDeclaredField("level");
                    Field xs = c.getDeclaredField("xpSeconds");
                    Field loc = c.getDeclaredField("location");
                    if (o.getType() == UUID.class && t.getType() == String.class && l.getType() == int.class && xs.getType() == double.class) {
                        factoryInstanceClass = c;
                        fiOwner = o; fiOwner.setAccessible(true);
                        fiTypeId = t; fiTypeId.setAccessible(true);
                        fiLevel = l; fiLevel.setAccessible(true);
                        fiXpSeconds = xs; fiXpSeconds.setAccessible(true);
                        fiLocation = loc; fiLocation.setAccessible(true);
                        break;
                    }
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}

        // Fallback: top-level class
        if (factoryInstanceClass == null) {
            try {
                Class<?> c = Class.forName("com.lootfactory.factory.FactoryInstance");
                Field o = c.getDeclaredField("owner");
                Field t = c.getDeclaredField("typeId");
                Field l = c.getDeclaredField("level");
                Field xs = c.getDeclaredField("xpSeconds");
                Field loc = c.getDeclaredField("location");
                factoryInstanceClass = c;
                fiOwner = o; fiOwner.setAccessible(true);
                fiTypeId = t; fiTypeId.setAccessible(true);
                fiLevel = l; fiLevel.setAccessible(true);
                fiXpSeconds = xs; fiXpSeconds.setAccessible(true);
                fiLocation = loc; fiLocation.setAccessible(true);
            } catch (Throwable ignored) {}
        }
    }

    @Override
    public FactoryHandle getFactory(UUID owner, String factoryType) {
        if (owner == null || factoryType == null) return null;
        Object fi = findByOwnerAndType(owner, factoryType);
        if (fi == null) {
            fi = findByOwnerAndType(owner, factoryType.toUpperCase(Locale.ROOT));
            if (fi == null) fi = findByOwnerAndType(owner, factoryType.toLowerCase(Locale.ROOT));
            if (fi == null) fi = findByOwnerAndType(owner, factoryType.replace(' ', '_'));
        }
        if (fi == null) return null;
        return wrap(fi);
    }

    @Override
    public FactoryHandle getFactoryByLocation(UUID owner, Location location) {
        if (owner == null || location == null) return null;

        Object fi = findByLocation(location);
        if (fi != null && ownerEquals(fi, owner)) {
            return wrap(fi);
        }

        // fallback: iterate owner's set
        Set<Object> set = getByOwnerSet(owner);
        if (set != null) {
            for (Object f : set) {
                Location loc = getLocation(f);
                if (loc == null) continue;
                if (sameBlock(loc, location) || nearBlock(loc, location)) {
                    return wrap(f);
                }
            }
        }

        // last resort: scan all byLocation values and filter owner
        Map<Location, Object> bl = getByLocationMap();
        if (bl != null) {
            for (Object v : bl.values()) {
                if (!ownerEquals(v, owner)) continue;
                Location loc = getLocation(v);
                if (loc == null) continue;
                if (sameBlock(loc, location) || nearBlock(loc, location)) {
                    return wrap(v);
                }
            }
        }

        return null;
    }

    // --------- internal finders ---------

    private Object findByOwnerAndType(UUID owner, String type) {
        Set<Object> set = getByOwnerSet(owner);
        if (set != null) {
            for (Object f : set) {
                String t = getTypeId(f);
                if (t != null && t.equalsIgnoreCase(type)) return f;
            }
        }
        // fallback: scan all and filter owner+type
        Map<Location, Object> bl = getByLocationMap();
        if (bl != null) {
            for (Object v : bl.values()) {
                if (!ownerEquals(v, owner)) continue;
                String t = getTypeId(v);
                if (t != null && t.equalsIgnoreCase(type)) return v;
            }
        }
        return null;
    }

    private Object findByLocation(Location location) {
        Map<Location, Object> bl = getByLocationMap();
        if (bl != null) {
            Object fi = bl.get(location.getBlock().getLocation());
            if (fi != null) return fi;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        Location test = location.clone().add(dx, dy, dz).getBlock().getLocation();
                        fi = bl.get(test);
                        if (fi != null) return fi;
                    }
                }
            }
        }
        return null;
    }

    // --------- reflection utils ---------

    @SuppressWarnings("unchecked")
    private Map<Location, Object> getByLocationMap() {
        try { return (Map<Location, Object>) fByLocation.get(fm); } catch (Throwable ignored) { return null; }
    }

    @SuppressWarnings("unchecked")
    private Set<Object> getByOwnerSet(UUID owner) {
        try {
            Map<UUID, Set<Object>> map = (Map<UUID, Set<Object>>) fByOwner.get(fm);
            return map == null ? null : map.get(owner);
        } catch (Throwable ignored) { return null; }
    }

    private boolean ownerEquals(Object fi, UUID owner) {
        try { return owner.equals(fiOwner.get(fi)); } catch (Throwable ignored) { return false; }
    }

    private String getTypeId(Object fi) {
        try { Object t = fiTypeId.get(fi); return t == null ? null : String.valueOf(t); } catch (Throwable ignored) { return null; }
    }

    private int getLevel(Object fi) {
        try { Object l = fiLevel.get(fi); return l instanceof Integer ? (Integer) l : 0; } catch (Throwable ignored) { return 0; }
    }

    private Location getLocation(Object fi) {
        try { Object l = fiLocation.get(fi); return l instanceof Location ? ((Location) l).getBlock().getLocation() : null; } catch (Throwable ignored) { return null; }
    }

    private boolean sameBlock(Location a, Location b) {
        return a.getWorld() != null && b.getWorld() != null &&
               a.getWorld().getUID().equals(b.getWorld().getUID()) &&
               a.getBlockX() == b.getBlockX() &&
               a.getBlockY() == b.getBlockY() &&
               a.getBlockZ() == b.getBlockZ();
    }

    private boolean nearBlock(Location a, Location b) {
        if (a.getWorld() == null || b.getWorld() == null) return false;
        if (!a.getWorld().getUID().equals(b.getWorld().getUID())) return false;
        return Math.abs(a.getBlockX() - b.getBlockX()) <= 1 &&
               Math.abs(a.getBlockY() - b.getBlockY()) <= 1 &&
               Math.abs(a.getBlockZ() - b.getBlockZ()) <= 1;
    }

    private FactoryHandle wrap(Object fi) {
        return new FactoryHandle() {
            @Override public String getType() { return String.valueOf(getTypeId(fi)); }
            @Override public int getLevel() { return FactoryGatewayImpl.this.getLevel(fi); }
            @Override public void resetToLevel(int level, int keepStoragePercent, boolean keepCosmetics) {
                // 1) Try direct methods if present
                try { fi.getClass().getMethod("resetToLevel", int.class, int.class, boolean.class).invoke(fi, level, keepStoragePercent, keepCosmetics); return; } catch (Throwable ignored) {}
                try { fi.getClass().getMethod("resetToLevel", int.class).invoke(fi, level); return; } catch (Throwable ignored) {}
                try { fi.getClass().getMethod("reset", int.class).invoke(fi, level); return; } catch (Throwable ignored) {}

                // 2) Field-based reset (targeted to your FactoryInstance)
                try {
                    fiLevel.set(fi, level);
                    if (fiXpSeconds != null) fiXpSeconds.set(fi, 0.0d);
                    return;
                } catch (Throwable t) {
                    throw new IllegalStateException("Unable to reset factory fields via reflection", t);
                }
            }
        };
    }
}
