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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;

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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockDamage(BlockDamageEvent event, BlockContext context) {
        for (Entity e: context.getBlock().getWorld().getNearbyEntities(context.getBlock().getLocation().add(0.5, 0.5, 0.5), 16.0, 16.0, 16.0)) {
            if (e instanceof Monster) {
                ((Monster)e).setTarget(event.getPlayer());
            }
        }
    }

    @Getter @RequiredArgsConstructor
    public final class Watcher implements BlockWatcher {
        private final Block block;
        private final MonsterHiveBlock customBlock;
        private int ticksLived = 0;
        private int spawnCount = 0;
        @Setter private int level = 1;

        void onTick() {
            if (!plugin.isKillWorld(block.getWorld())) return;
            if (plugin.getHiveTicks() == 0) {
                plugin.registerHive(block, level);
            }
            ticksLived += 1;
            if (ticksLived > (level + 1) * 20 * 10) {
                level += 1;
                ticksLived = 0;
                spawnCount = 0;
                save();
            }
            if (block.getType() != Material.MOB_SPAWNER) {
                CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(this);
                plugin.unregisterHive(block);
                return;
            }
            if (ticksLived % 5 == 0) {
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
                        if (e != null && e instanceof HostileMob) hostilesNearby += ((HostileMob)e).getHostileType().weight;
                    }
                }
                if (playersNearby > 0) {
                    int dx = plugin.getRandom().nextInt(3) - plugin.getRandom().nextInt(3);
                    int dy = plugin.getRandom().nextInt(3) - plugin.getRandom().nextInt(3);
                    int dz = plugin.getRandom().nextInt(3) - plugin.getRandom().nextInt(3);
                    if (dx != 0 || dy != 0 || dz != 0) {
                        Block armor = block.getRelative(dx, dy, dz);
                        int dist = Math.max(Math.abs(dx), Math.max(Math.abs(dy), Math.abs(dz)));
                        if (dist == 1) {
                            armor.setType(Material.WEB);
                        } else if (dist == 2) {
                            if (level >= 10) {
                                armor.setType(Material.OBSIDIAN);
                            } else if (level >= 5) {
                                armor.setType(Material.ENDER_STONE);
                            }
                        }
                    }
                    CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
                    if (creatureSpawner != null) creatureSpawner.setDelay(999);
                    if (hostilesNearby <= level + 1) {
                        HostileMob.Type type = plugin.tryToSpawnMob(block, level, 16);
                        if (type != null) spawnCount += type.weight;
                    }
                    Location levelLoc = block.getLocation().add(0.5, 1.5, 0.5);
                    for (Entity nearby: block.getWorld().getNearbyEntities(levelLoc, 1, 1, 1)) {
                        if (CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby) instanceof MonsterHiveLevelEntity.Watcher) return;
                    }
                    CustomPlugin.getInstance().getEntityManager().spawnEntity(levelLoc, MonsterHiveLevelEntity.CUSTOM_ID);
                }
            }
        }

        void load() {
            Map<String, Object> map = (Map<String, Object>)CustomPlugin.getInstance().getBlockManager().loadBlockData(this);
            if (map == null) return;
            if (map.containsKey("level")) level = ((Number)map.get("level")).intValue();
            if (map.containsKey("ticks_lived")) ticksLived = ((Number)map.get("ticks_lived")).intValue();
            if (map.containsKey("spawn_count")) spawnCount = ((Number)map.get("spawn_count")).intValue();
            plugin.registerHive(block, level);
        }

        void save() {
            Map<String, Object> map = new HashMap<>();
            map.put("level", level);
            map.put("ticks_lived", ticksLived);
            map.put("spawnCount", spawnCount);
            CustomPlugin.getInstance().getBlockManager().saveBlockData(this, map);
        }

        void onBreak() {
            plugin.unregisterHive(block);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.DIAMOND, 1 + plugin.getRandom().nextInt(level + 1)));
        }
    }
}
