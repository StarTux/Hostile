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
        FART_GOBLIN(10, 0) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new FartGoblinEntity(plugin);
            }
        },
        BATTER_BAT(5, 20) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new BatterBatEntity(plugin);
            }
        },
        FATZO(10, 0) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new FatzoEntity(plugin);
            }
        },
        SKELLINGTON(10, 0) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new SkellingtonEntity(plugin);
            }
        },
        PETARD(10, 15) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new PetardEntity(plugin);
            }
        },
        ANGRY_PARROT(5, 10) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new AngryParrotEntity(plugin);
            }
        },
        QUEEN_SPIDER(5, 5) {
            public CustomEntity newInstance(HostilePlugin plugin) {
                return new QueenSpiderEntity(plugin);
            }
        };

        public final int chance;
        public final int minLevel;
        public final String customId;

        Type(int chance, int minLevel) {
            this.chance = chance;
            this.minLevel = minLevel;
            this.customId = "hostile:" + name().toLowerCase();
        }

        abstract CustomEntity newInstance(HostilePlugin plugin);
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
