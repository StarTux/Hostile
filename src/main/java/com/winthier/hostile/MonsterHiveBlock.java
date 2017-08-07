package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockContext;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.block.CustomBlock;
import com.winthier.custom.block.TickableBlock;
import com.winthier.custom.entity.CustomEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
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
        CreatureSpawner spawner = (CreatureSpawner)block.getState();
        spawner.setSpawnedType(EntityType.BLAZE);
        spawner.setDelay(9999);
        spawner.update();
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

    @Override
    public void blockWasRemoved(BlockWatcher watcher) {
        ((Watcher)watcher).onRemove();
    }

    @Override
    public void blockWillUnload(BlockWatcher watcher) {
        ((Watcher)watcher).onRemove();
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
        private BossBar bossbar;
        private int ticks = 0;

        // Saved state
        private int ticksLived = 0;
        private int level = 1;
        private int levelTicks = 0;
        private boolean levelDefeated;
        private int mobCount = 0;

        // Statistics
        private int playersNearby = 0;
        private int hostilesNearby = 0;

        // Cooldowns
        private int cooldownShell = 1;
        private int cooldownSoil = 1;
        private int cooldownFort = 1;

        private final Map<UUID, MobType> mobs = new HashMap<>();
        private final List<MobType> spawnMobs = new ArrayList<>();

        void onTick() {
            if (!plugin.isKillWorld(block.getWorld())) return;
            // Register
            if (plugin.getHiveTicks() == 0) {
                plugin.registerHive(this);
            }
            // Check identity
            if (block.getType() != Material.MOB_SPAWNER
                && block.getType() != Material.BEDROCK) {
                CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(this);
                plugin.unregisterHive(this);
                return;
            }
            // Count hostiles and players nearby once per second
            if (ticks % 200 == 0 && block.getType() == Material.MOB_SPAWNER) {
                try {
                    CreatureSpawner creatureSpawner = (CreatureSpawner)block.getState();
                    if (creatureSpawner != null) {
                        creatureSpawner.setDelay(9999);
                        creatureSpawner.update();
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning(CUSTOM_ID + ": Block=" + block + ": Type=" + block.getType());
                    e.printStackTrace();
                }
            }
            List<Player> nearbyPlayers = new ArrayList<>();
            if ((ticks++ % 10) == 0) {
                playersNearby = 0;
                hostilesNearby = 0;
                final double radius = 32.0;
                for (Entity nearby: block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)) {
                    if (nearby instanceof Player) {
                        Player player = (Player)nearby;
                        nearbyPlayers.add(player);
                        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
                            playersNearby += 1;
                        }
                    } else {
                        CustomEntity e = CustomPlugin.getInstance().getEntityManager().getCustomEntity(nearby);
                        if (e != null && e instanceof HostileMob) {
                            hostilesNearby += ((HostileMob)e).getHostileType().weight;
                        }
                    }
                }
                if (bossbar != null) {
                    if (playersNearby == 0) {
                        bossbar.removeAll();
                    } else {
                        for (Player player: new ArrayList<Player>(bossbar.getPlayers())) {
                            if (!nearbyPlayers.contains(player)) bossbar.removePlayer(player);
                        }
                        for (Player player: nearbyPlayers) bossbar.addPlayer(player);
                    }
                }
                // Check spawned mobs
                if (levelTicks > 0 && playersNearby > 0 && spawnMobs.isEmpty()) {
                    if (mobs.isEmpty()) {
                        levelDefeated = true;
                    } else {
                        for (Iterator<Map.Entry<UUID, MobType>> iter = mobs.entrySet().iterator(); iter.hasNext();) {
                            Map.Entry<UUID, MobType> entry = iter.next();
                            UUID uuid = entry.getKey();
                            MobType type = entry.getValue();
                            Entity e = plugin.getServer().getEntity(uuid);
                            if (e == null) {
                                iter.remove();
                                spawnMobs.add(type);
                            }
                        }
                    }
                }
            }
            if (playersNearby == 0) return;
            // Update ticks and level
            if (levelDefeated) {
                level += 1;
                levelTicks = 0;
                levelDefeated = false;
                save();
            }
            // Spawn mobs
            int currentLevelTicks = levelTicks;
            levelTicks += 1;
            if (currentLevelTicks == 0) {
                if (bossbar != null) {
                    bossbar.removeAll();
                    bossbar.setVisible(false);
                }
                int innerLevel = (level - 1) % 10;
                Map<MobType, Integer> newMobs = new HashMap<>();
                if (innerLevel == 9 && level > 10) {
                    if (level == 50 || level == 100) {
                        spawnMobs.add(BossEntity.BossType.WITHER);
                    } else {
                        List<BossEntity.BossType> bossTypes = new ArrayList<>();
                        for (BossEntity.BossType bossType: BossEntity.BossType.values()) {
                            if (!bossType.rare) bossTypes.add(bossType);
                        }
                        spawnMobs.add(bossTypes.get(random.nextInt(bossTypes.size())));
                    }
                }
                int totalChance = 0;
                for (HostileMob.Type type: HostileMob.Type.values()) {
                    if (level >= type.minLevel) {
                        totalChance += type.chance;
                        newMobs.put(type, type.chance);
                    }
                }
                for (VanillaMob type: VanillaMob.values()) {
                    if (level >= type.minLevel) {
                        totalChance += type.chance;
                        newMobs.put(type, type.chance);
                    }
                }
                int totalMobs = 10 + (level / 10) * 10;
                for (MobType type: newMobs.keySet()) {
                    int chance = newMobs.get(type);
                    int amount = Math.max(1, (totalMobs * chance) / totalChance);
                    for (int i = 0; i < amount; i += 1) spawnMobs.add(type);
                }
                bossbar = plugin.getServer().createBossBar("Level " + level, BarColor.RED, BarStyle.SOLID);
                bossbar.setVisible(true);
                bossbar.setProgress(1.0);
                mobCount = spawnMobs.size();
            } else if (currentLevelTicks == 20) {
                // Announce
                String title = "";
                String subtitle = "" + ChatColor.RED + "Level " + level;
                for (Player player: block.getWorld().getPlayers()) {
                    Block pb = player.getLocation().getBlock();
                    if (Math.abs(pb.getX() - block.getX()) < 32
                        && Math.abs(pb.getY() - block.getY()) < 32
                        && Math.abs(pb.getZ() - block.getZ()) < 32) {
                        player.sendTitle(title, subtitle, 10, 20, 10);
                    }
                }
            } else if (currentLevelTicks > 100) {
                if (!spawnMobs.isEmpty()) {
                    MobType mobType = spawnMobs.get(spawnMobs.size() - 1);
                    Entity e = plugin.tryToSpawnMob(block, mobType);
                    if (e != null && e.getType() != EntityType.ENDERMAN) {
                        e.addScoreboardTag("ShowOnMiniMap");
                        spawnMobs.remove(spawnMobs.size() - 1);
                        mobs.put(e.getUniqueId(), mobType);
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
            plugin.registerHive(this);
        }

        void save() {
            Map<String, Object> map = new HashMap<>();
            map.put("level", level);
            CustomPlugin.getInstance().getBlockManager().saveBlockData(this, map);
        }

        void onBreak() {
            plugin.unregisterHive(this);
            int amount = Math.min(64, level / 10 + 1);
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), new ItemStack(Material.DIAMOND, amount));
            CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(this);
            if (bossbar != null) {
                bossbar.setVisible(false);
                bossbar.removeAll();
                bossbar = null;
            }
        }

        void entityDidDie(Entity e) {
            MobType type = mobs.remove(e.getUniqueId());
            if (type != null) {
                if (bossbar != null) {
                    int totalMobs = spawnMobs.size() + mobs.size();
                    double percentage = (double)totalMobs / (double)mobCount;
                    bossbar.setProgress(percentage);
                }
            }
        }

        void onRemove() {
            plugin.unregisterHive(this);
            if (bossbar != null) {
                bossbar.setVisible(false);
                bossbar.removeAll();
                bossbar = null;
            }
        }
    }
}
