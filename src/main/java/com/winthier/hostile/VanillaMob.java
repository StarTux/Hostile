package com.winthier.hostile;

import org.bukkit.Location;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Zombie;

enum VanillaMob implements MobType {
    CREEPER           (EntityType.CREEPER,          0, 10, 2),
    ZOMBIE            (EntityType.ZOMBIE,           0, 20, 1),
    ZOMBIE_VILLAGER   (EntityType.ZOMBIE_VILLAGER,  0, 20, 1),
    SKELETON          (EntityType.SKELETON,         0, 20, 1),
    SPIDER            (EntityType.SPIDER,           0, 10, 1),
    SLIME             (EntityType.SLIME,            0,  5, 1),
    MAGMA_CUBE        (EntityType.MAGMA_CUBE,      10,  5, 1),
    STRAY             (EntityType.STRAY,           10,  5, 1),
    HUSK              (EntityType.HUSK,            10,  5, 1),
    SPIDER_JOCKEY     (EntityType.SKELETON,        10,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            Spider spider = location.getWorld().spawn(location, Spider.class);
            Skeleton skeleton = location.getWorld().spawn(location, Skeleton.class);
            spider.addPassenger(skeleton);
            return skeleton;
        }
    },
    CHICKEN_JOCKEY    (EntityType.ZOMBIE,          10,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            Chicken chicken = location.getWorld().spawn(location, Chicken.class);
            Zombie zombie = location.getWorld().spawn(location, Zombie.class, e -> e.setBaby(true));
            chicken.addPassenger(zombie);
            return zombie;
        }
    },
    CAVE_SPIDER       (EntityType.CAVE_SPIDER,     20, 10, 1),
    CHARGED_CREEPER   (EntityType.CREEPER,         20,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            return location.getWorld().spawn(location, Creeper.class, e -> e.setPowered(true));
        }
    },
    CAVE_SPIDER_JOCKEY(EntityType.CREEPER,         30,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            CaveSpider spider = location.getWorld().spawn(location, CaveSpider.class);
            Creeper creeper = location.getWorld().spawn(location, Creeper.class);
            spider.addPassenger(creeper);
            return creeper;
        }
    },
    WITCH             (EntityType.WITCH,           40, 10, 2),
    WITHER_SKELETON   (EntityType.WITHER_SKELETON, 40, 20, 2),
    EVOKER            (EntityType.EVOKER,          40,  5, 3),
    VINDICATOR        (EntityType.EVOKER,          40, 10, 1),
    BLAZE             (EntityType.BLAZE,           40, 10, 1);

    public final EntityType entityType;
    public final int minLevel;
    public final int chance;
    public final int weight;

    VanillaMob(EntityType entityType, int minLevel, int chance, int weight) {
        this.entityType = entityType;
        this.minLevel = minLevel;
        this.chance = chance;
        this.weight = weight;
    }

    LivingEntity spawn(Location location) {
        return (LivingEntity)location.getWorld().spawnEntity(location, entityType);
    }
}
