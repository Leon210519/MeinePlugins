package com.lootforge.armorskins;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.Pair;

public class PacketHider extends PacketAdapter {

    private final ProtocolManager manager;

    public PacketHider(SpecialArmorSkins plugin) {
        super(plugin, PacketType.Play.Server.ENTITY_EQUIPMENT);
        this.manager = ProtocolLibrary.getProtocolManager();
        this.manager.addPacketListener(this);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() != PacketType.Play.Server.ENTITY_EQUIPMENT) return;
        if (!(event.getPacket().getEntityModifier(event).read(0) instanceof Player)) return;
        List<Pair<EnumWrappers.ItemSlot, ItemStack>> list = event.getPacket().getSlotStackPairLists().read(0);
        for (Pair<EnumWrappers.ItemSlot, ItemStack> pair : list) {
            switch (pair.getFirst()) {
                case HEAD:
                case CHEST:
                case LEGS:
                case FEET:
                    pair.setSecond(new ItemStack(Material.AIR));
                    break;
                default:
                    break;
            }
        }
    }

    public void shutdown() {
        manager.removePacketListener(this);
    }
}
