package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.block.CustomBlock;
import com.winthier.custom.block.TickableBlock;
import com.winthier.custom.entity.CustomEntity;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

@RequiredArgsConstructor
public final class MonsterHiveBlock implements CustomBlock, TickableBlock {
    public static final String CUSTOM_ID = "hostile:monster_hive";
    private final HostilePlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public void setBlock(Block block) {
        block.setType(Material.MOB_SPAWNER);
        ((CreatureSpawner)block.getState()).setSpawnedType(EntityType.BLAZE);
        ((CreatureSpawner)block.getState()).setDelay(999);
    }

    @Override
    public Watcher createBlockWatcher(Block block) {
        return new Watcher(block, this);
    }

    @Override
    public void onTick(BlockWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @Override
    public void blockWasLoaded(BlockWatcher watcher) {
        ((Watcher)watcher).load();
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event, BlockContext context) {
        event.getSpawner().setDelay(999);
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent event, BlockContext context) {
        ((Watcher)context.getBlockWatcher()).onBreak();
    }

    @Getter @RequiredArgsConstructor
    public final class Watcher implements BlockWatcher {
        private final Block block;
        private final MonsterHiveBlock customBlock;
        private int ticks = 0;
        private int spawnCount = 0;
        @Setter private int level = 0;

        void onTick() {
            if (!plugin.isKillWorld(block.getWorld())) return;
            if (plugin.getHiveTicks() == 0) {
                plugin.registerHive(block, level);
            }
            ticks += 1;
            if (block.getType() != Material.MOB_SPAWNER) {
                CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(this);
                plugin.unregisterHive(block);
                return;
            }
            if (ticks % 20 == 0) {
                int playersNearby = 0;
                int hostilesNearby = 0;
                final double radius = 32.0;
                for (Entity nearby: block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)) {
                    if (nearby instanceof Player) {
                        Player player = (Player)nearby;
                        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
                        playersNearby += 1;
                    } else {
                        CustomEntity e = CustomPlugin.getInstance().getEntityManager().getCustomEntity(nearby);
                        if (e != null && e instanceof HostileMob) hostilesNearby += 1;
                    }
                }
                if (playersNearby == 0) return;
                CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
                if (creatureSpawner != null) creatureSpawner.setDelay(999);
                int dx = plugin.getRandom().nextInt(3) - plugin.getRandom().nextInt(3);
                int dy = plugin.getRandom().nextInt(3) - plugin.getRandom().nextInt(3);
                int dz = plugin.getRandom().nextInt(3) - plugin.getRandom().nextInt(3);
                Block armor = block.getRelative(dx, dy, dz);
                if (armor.getType() == Material.AIR) {
                    int dist = dx * dx + dy * dy + dz * dz;
                    if (dist < 4) {
                        armor.setType(Material.WEB);
                    } else if (dist < 8) {
                        armor.setType(Material.OBSIDIAN);
                    }
                }
                if (hostilesNearby > level + 1) return;
                if (plugin.tryToSpawnMob(block, level, 16)) spawnCount += 1;
                if (spawnCount > (level + 1) * 3) {
                    spawnCount = 0;
                    level += 1;
                    save();
                }
            }
        }

        void load() {
            Map<String, Object> map = (Map<String, Object>)CustomPlugin.getInstance().getBlockManager().loadBlockData(this);
            if (map == null) return;
            level = ((Number)map.get("level")).intValue();
            plugin.registerHive(block, level);
        }

        void save() {
            Map<String, Object> map = new HashMap<>();
            map.put("level", level);
            CustomPlugin.getInstance().getBlockManager().saveBlockData(this, map);
        }

        void onBreak() {
            plugin.tryToSpawnHive(block, level + 1, 16);
            plugin.unregisterHive(block);
        }
    }
}
