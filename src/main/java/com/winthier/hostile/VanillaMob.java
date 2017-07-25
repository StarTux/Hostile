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

enum VanillaMob {
    CREEPER           (EntityType.CREEPER,  0, 10, 2),
    ZOMBIE            (EntityType.ZOMBIE,   0, 20, 1),
    SKELETON          (EntityType.SKELETON, 0, 20, 1),
    SPIDER            (EntityType.SPIDER,   0, 10, 1),
    ENDERMAN          (EntityType.ENDERMAN, 10, 10, 2),
    SPIDER_JOCKEY     (EntityType.SKELETON, 10,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            Spider spider = location.getWorld().spawn(location, Spider.class);
            Skeleton skeleton = location.getWorld().spawn(location, Skeleton.class);
            spider.addPassenger(skeleton);
            return skeleton;
        }
    },
    CHARGED_CREEPER   (EntityType.CREEPER, 30,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            return location.getWorld().spawn(location, Creeper.class, e -> e.setPowered(true));
        }
    },
    CHICKEN_JOCKEY    (EntityType.ZOMBIE,  10,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            Chicken chicken = location.getWorld().spawn(location, Chicken.class);
            Zombie zombie = location.getWorld().spawn(location, Zombie.class, e -> e.setBaby(true));
            chicken.addPassenger(zombie);
            return zombie;
        }
    },
    CAVE_SPIDER_JOCKEY(EntityType.CREEPER, 40,  5, 2) {
        @Override public LivingEntity spawn(Location location) {
            CaveSpider spider = location.getWorld().spawn(location, CaveSpider.class);
            Creeper creeper = location.getWorld().spawn(location, Creeper.class);
            spider.addPassenger(creeper);
            return creeper;
        }
    },
    ;

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
