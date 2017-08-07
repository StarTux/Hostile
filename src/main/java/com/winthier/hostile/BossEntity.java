package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Blaze;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

@Getter
public final class BossEntity implements CustomEntity, TickableEntity {
    enum BossType implements MobType {
        BABY_ZOMBIE,
        WITHER(true),
        PIG_ZOMBIE,
        BLAZE,
        WITHER_SKELETON,
        CAVE_SPIDER,
        WITCH;

        public final String customId;
        public final boolean rare;

        BossType(boolean rare) {
            this.customId = "hostile:" + name().toLowerCase() + "_boss";
            this.rare = rare;
        }

        BossType() {
            this(false);
        }
    }

    private final HostilePlugin plugin;
    private final BossType bossType;
    private final String customId;
    private static final double HEALTH = 1000.0;

    BossEntity(HostilePlugin plugin, BossType bossType) {
        this.plugin = plugin;
        this.bossType = bossType;
        this.customId = bossType.customId;
    }

    @Override
    public Entity spawnEntity(Location location) {
        switch (bossType) {
        case WITHER:
            return location.getWorld().spawn(location, Wither.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.DARK_RED + "Wither");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                });
        case PIG_ZOMBIE:
            return location.getWorld().spawn(location, PigZombie.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.DARK_GREEN + "Major Payne");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                    e.setBaby(false);
                });
        case WITHER_SKELETON:
            return location.getWorld().spawn(location, WitherSkeleton.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.BLACK + "Lord Skellington");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                });
        case BLAZE:
            return location.getWorld().spawn(location, Blaze.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.YELLOW + "The Pain");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                });
        case WITCH:
            return location.getWorld().spawn(location, Witch.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.DARK_GREEN + "Hazel");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                });
        case CAVE_SPIDER:
            return location.getWorld().spawn(location, CaveSpider.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.DARK_BLUE + "Itsy Bitsy");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                });
        case BABY_ZOMBIE: default:
            return location.getWorld().spawn(location, Zombie.class, e -> {
                    e.setCustomName("" + ChatColor.RESET + ChatColor.DARK_GREEN + "Terrible Timmy");
                    e.setCustomNameVisible(true);
                    e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                    e.setHealth(HEALTH);
                    e.setRemoveWhenFarAway(true);
                    e.setBaby(true);
                    e.getEquipment().clear();
                    e.getEquipment().setHelmet(new ItemStack(Material.DIAMOND_HELMET));
                });
        }
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        return new Watcher((LivingEntity)e, this);
    }

    @Override
    public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    final class Watcher implements EntityWatcher {
        private final LivingEntity entity;
        private final BossEntity customEntity;
        private int ticks;

        void onTick() {
            if (!plugin.isKillWorld(entity.getWorld())) return;
            int currentTicks = ticks;
            ticks += 1;
            double maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
            if (currentTicks % 20 == 0) {
                double newHealth = Math.min(maxHealth, entity.getHealth() + 5.0);
                entity.setHealth(newHealth);
                if (entity instanceof Creature) {
                    Creature creature = (Creature)entity;
                    if (creature.getTarget() == null || !(creature.getTarget() instanceof Player)) {
                        Player newTarget = null;
                        double distance = 9999.0;
                        for (Entity nearby: entity.getNearbyEntities(16, 16, 16)) {
                            Location loc = entity.getLocation();
                            if (nearby instanceof Player && ((Player)nearby).getGameMode() == GameMode.SURVIVAL) {
                                if (newTarget == null || nearby.getLocation().distance(loc) < distance) {
                                    newTarget = (Player)nearby;
                                    distance = newTarget.getLocation().distance(loc);
                                }
                            }
                        }
                        creature.setTarget(newTarget);
                    }
                }
            }
            switch (bossType) {
            case BABY_ZOMBIE:
                if (currentTicks % 100 == 0) {
                    Block block = entity.getLocation().getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.MOB_SPAWNER);
                        CreatureSpawner spawner = (CreatureSpawner)block.getState();
                        spawner.setSpawnedType(EntityType.ZOMBIE);
                        spawner.update();
                        SpawnerBlock.Watcher watcher = (SpawnerBlock.Watcher)CustomPlugin.getInstance().getBlockManager().wrapBlock(block, SpawnerBlock.CUSTOM_ID);
                        watcher.setMarker(true);
                        watcher.save();
                        entity.teleport(entity.getLocation().add(0, 1, 0));
                    }
                }
                break;
            case WITHER:
                break;
            case PIG_ZOMBIE:
                if (currentTicks % 100 == 0) {
                    for (Entity nearby: entity.getNearbyEntities(2, 2, 2)) {
                        if (nearby instanceof Player) {
                            ((Player)nearby).setVelocity(new Vector(0, 5, 0));
                        }
                    }
                }
                break;
            case BLAZE:
                if (currentTicks % 100 == 0) {
                    for (Entity nearby: entity.getNearbyEntities(2, 2, 2)) {
                        if (nearby instanceof Player) {
                            ((Player)nearby).setFireTicks(200);
                        }
                    }
                }
                break;
            case WITHER_SKELETON:
                if (currentTicks % 100 == 0) {
                    for (Entity nearby: entity.getNearbyEntities(2, 2, 2)) {
                        if (nearby instanceof Player) {
                            ((Player)nearby).addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 0), false);
                        }
                    }
                }
                break;
            case CAVE_SPIDER:
                if (currentTicks % 100 == 0) {
                    Block block = entity.getLocation().getBlock();
                    if (block.getType() == Material.AIR) {
                        block.setType(Material.MOB_SPAWNER);
                        CreatureSpawner spawner = (CreatureSpawner)block.getState();
                        spawner.setSpawnedType(EntityType.SPIDER);
                        spawner.update();
                        SpawnerBlock.Watcher watcher = (SpawnerBlock.Watcher)CustomPlugin.getInstance().getBlockManager().wrapBlock(block, SpawnerBlock.CUSTOM_ID);
                        watcher.setMarker(true);
                        watcher.save();
                        entity.teleport(entity.getLocation().add(0, 1, 0));
                    }
                }
                break;
            case WITCH:
                if (currentTicks % 100 == 0) {
                    for (Entity nearby: entity.getNearbyEntities(2, 2, 2)) {
                        if (nearby instanceof Player) {
                            ((Player)nearby).addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1), false);
                        }
                    }
                }
                break;
            default: break;
            }
        }
    }
}
