package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.ore.OrePlugin;
import com.winthier.ore.WorldGenerator;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;

public interface HostileMob extends CustomEntity {
    enum Type {
        FART_GOBLIN(FartGoblinEntity.class, 10, 0),
        FATZO(FatzoEntity.class, 10, 0),
        SKELLINGTON(SkellingtonEntity.class, 10, 0),
        QUEEN_SPIDER(QueenSpiderEntity.class, 5, 5),
        ANGRY_PARROT(AngryParrotEntity.class, 5, 10),
        BATTER_BAT(BatterBatEntity.class, 5, 15),
        PETARD(PetardEntity.class, 10, 20);

        public final Class<? extends CustomEntity> clazz;
        public final int chance;
        public final int minLevel;
        public final String customId;

        Type(Class<? extends CustomEntity> clazz, int chance, int minLevel) {
            this.clazz = clazz;
            this.chance = chance;
            this.minLevel = minLevel;
            this.customId = "hostile:" + name().toLowerCase();
        }

        CustomEntity newInstance(HostilePlugin plugin) {
            try {
                return clazz.getConstructor(HostilePlugin.class).newInstance(plugin);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    Type getHostileType();

    @EventHandler(ignoreCancelled = true)
    default void onEntityDeath(EntityDeathEvent event, EntityContext context) {
        if (event.getEntity().getKiller() == null) return;
        WorldGenerator gen = OrePlugin.getInstance().getWorldGenerator(event.getEntity().getWorld());
        if (gen == null) return;
        ItemStack item = gen.getRandomLootItem();
        if (item == null) return;
        event.getDrops().add(item);
    }

    @EventHandler(ignoreCancelled = true)
    default void onEntityTarget(EntityTargetEvent event, EntityContext context) {
        if (event.getTarget() == null) return;
        if (!(event.getTarget() instanceof Player)) {
            event.setCancelled(true);
        }
    }

    @Override
    default void entityWasSpawned(EntityWatcher watcher) {
        watcher.getEntity().addScoreboardTag("ShowOnMiniMap");
    }
}
