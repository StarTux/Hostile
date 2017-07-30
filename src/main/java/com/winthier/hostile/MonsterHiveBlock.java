package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.block.CustomBlock;
import com.winthier.custom.block.TickableBlock;
import com.winthier.custom.entity.CustomEntity;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

@RequiredArgsConstructor
public final class MonsterHiveBlock implements CustomBlock, TickableBlock {
    public static final String CUSTOM_ID = "hostile:monster_hive";
    private final HostilePlugin plugin;
    private final Random random = new Random(System.currentTimeMillis());

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

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event, BlockContext context) {
        if (event.getPlayer().isOp()) {
            ((Watcher)context.getBlockWatcher()).level += 1;
        }
    }

    @Getter @RequiredArgsConstructor
    public final class Watcher implements BlockWatcher {
        private final Block block;
        private final MonsterHiveBlock customBlock;
        private int ticks = 0;
        private int ticksLived = 0;
        private int spawnCount = 0;
        @Setter private int level = 1;
        private int spawnCooldown = 0;
        private int playersNearby = 0;
        private int hostilesNearby = 0;
        private int cooldownShell = 1;
        private int cooldownSoil = 1;
        private int cooldownFort = 1;

        void onTick() {
            if (!plugin.isKillWorld(block.getWorld())) return;
            // Register
            if (plugin.getHiveTicks() == 0) {
                plugin.registerHive(block, level);
            }
            // Check identity
            if (block.getType() != Material.MOB_SPAWNER) {
                CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(this);
                plugin.unregisterHive(block);
                return;
            }
            // Count hostiles and players nearby once per second
            if (ticks++ % 20 == 0) {
                playersNearby = 0;
                hostilesNearby = 0;
                final double radius = 32.0;
                for (Entity nearby: block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)) {
                    if (nearby instanceof Player) {
                        Player player = (Player)nearby;
                        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) continue;
                        playersNearby += 1;
                    } else {
                        CustomEntity e = CustomPlugin.getInstance().getEntityManager().getCustomEntity(nearby);
                        if (e != null && e instanceof HostileMob) {
                            hostilesNearby += ((HostileMob)e).getHostileType().weight;
                        }
                    }
                }
                // Spawn level billboard if necessary
                Location levelLoc = block.getLocation().add(0.5, 1.5, 0.5);
                for (Entity nearby: block.getWorld().getNearbyEntities(levelLoc, 1, 1, 1)) {
                    if (CustomPlugin.getInstance().getEntityManager().getEntityWatcher(nearby) instanceof MonsterHiveLevelEntity.Watcher) return;
                }
                CustomPlugin.getInstance().getEntityManager().spawnEntity(levelLoc, MonsterHiveLevelEntity.CUSTOM_ID);
            }
            if (playersNearby == 0) return;
            // Update ticks and level
            ticksLived += 1;
            if (ticksLived > (20 * 30) + 200 * (level / 10 + 1)) {
                level += 1;
                ticksLived = 0;
                spawnCount = 0;
                save();
                if (level == 50 || level == 100) {
                    block.getWorld().spawnEntity(block.getLocation().add(10.0, 0.5, 10.0), EntityType.WITHER);
                }
            }
            try {
                CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
                if (creatureSpawner != null) creatureSpawner.setDelay(999);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Spawn mobs
            int mobsToSpawn = 10 + 6 * (level / 10 + 1);
            if (spawnCount <= mobsToSpawn) {
                if (spawnCooldown > 0) {
                    spawnCooldown -= 1;
                } else {
                    int weight = plugin.tryToSpawnMob(block, level, 16);
                    if (weight > 0) {
                        spawnCount += weight;
                        spawnCooldown = 20;
                    }
                }
            }
            // Build fortifications
            double cx = random.nextDouble() - 0.5;
            double cy = random.nextDouble() - 0.5;
            double cz = random.nextDouble() - 0.5;
            if (cooldownShell > 0) {
                if (level >= 10) cooldownShell -= 1;
            } else {
                Material matShell;
                if (level >= 40) {
                    matShell = Material.OBSIDIAN;
                } else if (level >= 30) {
                    matShell = Material.NETHER_BRICK;
                } else if (level >= 20) {
                    matShell = Material.NETHERRACK;
                } else {
                    matShell = Material.STAINED_GLASS;
                }
                Vector vecShell = new Vector(cx, cy, cz).normalize();
                BlockIterator iterShell = new BlockIterator(block.getWorld(), new Vector(block.getX(), block.getY(), block.getZ()), vecShell, 0.0, 10);
                while (iterShell.hasNext()) {
                    Block blockShell = iterShell.next();
                    if (blockShell.equals(block)) continue;
                    int dx = block.getX() - blockShell.getX();
                    int dy = block.getY() - blockShell.getY();
                    int dz = block.getZ() - blockShell.getZ();
                    if (dx * dx + dy * dy + dz * dz > 9) break;
                    if (blockShell.getType() == matShell) continue;
                    blockShell.setType(matShell);
                    if (matShell == Material.STAINED_GLASS) blockShell.setData((byte)15);
                    cooldownShell = 20;
                    break;
                }
            }
            if (cooldownSoil > 0) {
                if (level >= 20) cooldownSoil -= 1;
            } else {
                Vector vecSoil = new Vector(cx, 0.0, cz).normalize().multiply(random.nextDouble() * 28.0);
                Block blockSoil = block.getWorld().getHighestBlockAt(block.getX() + vecSoil.getBlockX(), block.getZ() + vecSoil.getBlockZ()).getRelative(0, -1, 0);
                if (blockSoil.getY() < block.getY() - 4) {
                    Material mat;
                    switch (random.nextInt(7)) {
                    case 0: mat = Material.NETHER_BRICK; break;
                    case 1: mat = Material.RED_NETHER_BRICK; break;
                    case 3: case 4: mat = Material.ENDER_STONE; break;
                    case 5: case 6: default: mat = Material.OBSIDIAN;
                    }
                    block.getWorld().spawnFallingBlock(blockSoil.getLocation().add(0.5, 128.0, 0.5), mat.getNewData((byte)0)).setDropItem(false);
                } else {
                    switch (blockSoil.getType()) {
                    case AIR: break;
                    case DIRT:
                    case GRASS:
                    case GRASS_PATH:
                    case SAND:
                    case STONE:
                        blockSoil.setType(Material.NETHERRACK);
                        cooldownSoil = 10;
                        break;
                    case LEAVES:
                    case LEAVES_2:
                        blockSoil.setType(Material.FIRE);
                        cooldownSoil = 10;
                        break;
                    case WATER:
                    case STATIONARY_WATER:
                        if (blockSoil.getData() == 0) {
                            blockSoil.setType(Material.ICE);
                            cooldownSoil = 3;
                        }
                        break;
                    case LAVA:
                    case STATIONARY_LAVA:
                        blockSoil.setType(Material.OBSIDIAN);
                        cooldownSoil = 3;
                        break;
                    default:
                        cooldownSoil = 20;
                    }
                }
            }
            if (cooldownFort > 0) {
                if (level >= 30) cooldownFort -= 1;
            } else {
                double extFort = random.nextDouble() * 4.0;
                Vector vecFort = new Vector(cx, 0.0, cz).normalize().multiply(16.0 + extFort);
                Block blockFort = block.getWorld().getHighestBlockAt(block.getX() + vecFort.getBlockX(), block.getZ() + vecFort.getBlockZ());
                if (blockFort.getY() < block.getY() + (int)extFort) {
                    Material mat;
                    if (random.nextBoolean()) {
                        mat = Material.NETHER_BRICK;
                    } else {
                        mat = Material.RED_NETHER_BRICK;
                    }
                    block.getWorld().spawnFallingBlock(blockFort.getLocation().add(0.5, 128.0, 0.5), mat.getNewData((byte)0)).setDropItem(false);
                }
                cooldownFort = 2;
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
            int amount = Math.min(64, level / 10 + 1);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.DIAMOND, amount));
        }
    }
}
