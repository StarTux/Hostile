package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Spider;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Getter @RequiredArgsConstructor
public final class QueenSpiderEntity implements CustomEntity, HostileMob, TickableEntity {
    private final HostilePlugin plugin;
    private final Type hostileType = Type.QUEEN_SPIDER;
    private final String customId = hostileType.customId;
    private static final double HEALTH = 100;
    private static final int COUNTDOWN = 20 * 5;

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Spider.class, e -> {
                e.setCustomName("Queen Spider");
                e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                e.setHealth(HEALTH);
                e.setRemoveWhenFarAway(true);
            });
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        return new Watcher((Spider)e, this);
    }

    @Override
    public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    class Watcher implements EntityWatcher {
        private final Spider entity;
        private final QueenSpiderEntity customEntity;
        private int ticks;
        private int countdown = COUNTDOWN;

        void onTick() {
            ticks += 1;
            if (ticks % 20 != 0) return;
            if (entity.getTarget() == null) return;
            if (entity.getLocation().getBlock().getType() == Material.AIR) entity.getLocation().getBlock().setType(Material.WEB);
            countdown -= plugin.getRandom().nextInt(3) * 20;
            if (countdown > 0) {
                return;
            } else {
                countdown = COUNTDOWN;
            }
            entity.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, true), true);
            entity.getWorld().playSound(entity.getEyeLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.HOSTILE, 2.0f, 1.2f);
            if (!plugin.isKillWorld(entity.getWorld())) {
                if (plugin.getRandom().nextInt(3) == 0) entity.teleport(entity.getTarget());
                return;
            }
            if (plugin.getRandom().nextInt(3) == 0) {
                int babyCount = 0;
                for (Entity nearby: entity.getNearbyEntities(8, 8, 8)) {
                    if (nearby.getType() == EntityType.CAVE_SPIDER) babyCount += 1;
                }
                if (babyCount >= 8) return;
                int amount = plugin.getRandom().nextInt(5) + 1;
                for (int i = 0; i < amount; i += 1) {
                    CaveSpider baby = entity.getWorld().spawn(entity.getLocation(), CaveSpider.class);
                    baby.addScoreboardTag("NoHostileScore");
                }
            }
        }
    }
}
