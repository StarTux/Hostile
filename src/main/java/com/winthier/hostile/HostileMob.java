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
        // Class, Chance, MinLevel, Weight
        SKELLINGTON (SkellingtonEntity.class, 10, 10, 4),
        FART_GOBLIN ( FartGoblinEntity.class, 20,  5, 4),
        FATZO       (      FatzoEntity.class, 20,  5, 4),
        ANGRY_PARROT(AngryParrotEntity.class, 30,  2, 4),
        QUEEN_SPIDER(QueenSpiderEntity.class, 40,  2, 8),
        BATTER_BAT  (  BatterBatEntity.class, 50,  2, 8),
        PETARD      (     PetardEntity.class, 50,  2, 8);

        public final Class<? extends CustomEntity> clazz;
        public final int minLevel;
        public final int chance;
        public final int weight;
        public final String customId;

        Type(Class<? extends CustomEntity> clazz, int minLevel, int chance, int weight) {
            this.clazz = clazz;
            this.minLevel = minLevel;
            this.chance = chance;
            this.weight = weight;
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
        event.setDroppedExp((event.getDroppedExp() + 1) * 10);
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

    @Override
    default void entityWillUnload(EntityWatcher watcher) {
        watcher.getEntity().remove();
    }
}
