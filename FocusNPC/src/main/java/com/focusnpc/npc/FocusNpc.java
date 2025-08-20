package com.focusnpc.npc;

import org.bukkit.Location;

import java.util.UUID;

public class FocusNpc {
    private final NpcType type;
    private final Location location;
    private final String skin;
    private Object citizen;
    private Integer citizenId;
    private UUID entityId;

    public FocusNpc(NpcType type, Location location, String skin) {
        this.type = type;
        this.location = location;
        this.skin = skin;
    }

    public NpcType getType() { return type; }
    public Location getLocation() { return location; }
    public String getSkin() { return skin; }
    public Object getCitizen() { return citizen; }
    public void setCitizen(Object citizen) { this.citizen = citizen; }
    public Integer getCitizenId() { return citizenId; }
    public void setCitizenId(Integer citizenId) { this.citizenId = citizenId; }
    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
}
