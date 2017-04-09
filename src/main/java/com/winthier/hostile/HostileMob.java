package com.winthier.hostile;

import com.winthier.custom.entity.EntityContext;
import com.winthier.ore.OrePlugin;
import com.winthier.ore.WorldGenerator;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

public interface HostileMob {
    @EventHandler(ignoreCancelled = true)
    default void onEntityDeath(EntityDeathEvent event, EntityContext context) {
        if (event.getEntity().getKiller() == null) return;
        WorldGenerator gen = OrePlugin.getInstance().getWorldGenerator(event.getEntity().getWorld());
        if (gen == null) return;
        ItemStack item = gen.getRandomLootItem();
        if (item == null) return;
        event.getDrops().add(item);
    }
}
