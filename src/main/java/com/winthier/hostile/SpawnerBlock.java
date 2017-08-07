package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.block.CustomBlock;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;

/**
 * Code for BlockBreakEvent is in HostilePlugin because it handles
 * events pertaining to normal (non-custom) spawners as well.
 */
@Getter @RequiredArgsConstructor
public final class SpawnerBlock implements CustomBlock {
    public static final String CUSTOM_ID = "hostile:spawner";
    private final HostilePlugin plugin;
    private final Random random = new Random(System.currentTimeMillis());

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public void setBlock(Block block) {
        block.setType(Material.MOB_SPAWNER);
    }

    @Override
    public BlockWatcher createBlockWatcher(Block block) {
        return new Watcher(block, this);
    }

    @Override
    public void blockWasLoaded(BlockWatcher watcher) {
        if (watcher.getBlock().getType() != Material.MOB_SPAWNER) {
            CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(watcher);
        } else {
            ((Watcher)watcher).load();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockExplode(BlockExplodeEvent event, BlockContext context) {
        CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(context.getBlockWatcher());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityExplode(EntityExplodeEvent event, BlockContext context) {
        CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(context.getBlockWatcher());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onSpawnerSpawn(SpawnerSpawnEvent event, BlockContext context) {
        ((Watcher)context.getBlockWatcher()).onSpawnerSpawn();
    }

    @Getter @Setter @RequiredArgsConstructor
    final class Watcher implements BlockWatcher {
        final Block block;
        final SpawnerBlock customBlock;
        // State
        private boolean playerPlaced = false;
        private boolean natural = false;
        private boolean marker = false;
        private int level = 0;
        // Trans
        private transient int spawnCount = 0;

        void onSpawnerSpawn() {
            spawnCount += 1;
            if (playerPlaced || natural) return;
            final int spawnerLimit = 1000;
            int rnd = random.nextInt(spawnerLimit);
            if (rnd < spawnCount) {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                        block.getWorld().createExplosion(block.getLocation().add(0.5, 0.5, 0.5), 4f, true);
                        if (block.getType() == Material.MOB_SPAWNER) block.breakNaturally();
                    });
                plugin.getLogger().info(String.format("Exploded spawner in %s at %d %d %d (%d/%d)", block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), spawnCount, spawnerLimit));
            }
        }

        void save() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("player_placed", playerPlaced);
            map.put("natural", natural);
            map.put("marker", marker);
            map.put("level", level);
            CustomPlugin.getInstance().getBlockManager().saveBlockData(this, map);
        }

        void load() {
            Map<String, Object> map = (Map<String, Object>)CustomPlugin.getInstance().getBlockManager().loadBlockData(this);
            if (map != null) {
                if (map.containsKey("player_placed")) playerPlaced = (Boolean)map.get("player_placed");
                if (map.containsKey("natural")) natural = (Boolean)map.get("natural");
                if (map.containsKey("marker")) marker = (Boolean)map.get("marker");
                if (map.containsKey("level")) level = ((Number)map.get("level")).intValue();
            }
        }
    }
}
