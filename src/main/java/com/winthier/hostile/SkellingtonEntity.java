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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Skeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter @RequiredArgsConstructor
public final class SkellingtonEntity implements CustomEntity, HostileMob, TickableEntity {
    private final HostilePlugin plugin;
    private final Type hostileType = Type.SKELLINGTON;
    private final String customId = hostileType.customId;
    private static final double HEALTH = 100;

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Skeleton.class, s -> {
                s.setCustomName("Skellington");
                s.getEquipment().setItemInMainHand(getBow());
                s.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
                s.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                s.getEquipment().setHelmetDropChance(0);
                s.getEquipment().setItemInMainHandDropChance(0);
                s.getEquipment().setItemInOffHandDropChance(0);
                s.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                s.setHealth(HEALTH);
                s.setRemoveWhenFarAway(true);
            });
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity e) {
        return new Watcher((Skeleton)e, this);
    }

    @Override
    public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    private ItemStack getSword() {
        ItemStack item = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getBow() {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.ARROW_FIRE, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    @Getter @RequiredArgsConstructor
    class Watcher implements EntityWatcher {
        private final Skeleton entity;
        private final SkellingtonEntity customEntity;
        private int ticks;

        void onTick() {
            ticks += 1;
            if (ticks % 10 != 0) return;
            LivingEntity target = entity.getTarget();
            if (target == null) return;
            if (!target.getWorld().equals(entity.getWorld())) return;
            double distance = target.getLocation().distance(entity.getLocation());
            if (distance < 4.0) {
                if (entity.getEquipment().getItemInMainHand().getType() == Material.IRON_SWORD) return;
                entity.getEquipment().setItemInMainHand(getSword());
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
            } else {
                if (entity.getEquipment().getItemInMainHand().getType() == Material.BOW) return;
                entity.getEquipment().setItemInMainHand(getBow());
                entity.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0.0);
            }
            entity.getWorld().playSound(entity.getEyeLocation(), Sound.ENTITY_SNOWBALL_THROW, SoundCategory.HOSTILE, 1.0f, 0.8f);
        }
    }
}
