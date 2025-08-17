package com.instancednodes.nodes;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/** Public API for other plugins (e.g., Special-Items Veinminer) */
public class NodeAPI {
    public static boolean harvestMineBlock(Player p, Block b) {
        NodeManager nm = NodeManager.getInstance();
        return nm != null && nm.processMineBlockExternal(p, b);
    }
}
