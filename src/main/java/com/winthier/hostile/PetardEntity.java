package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

@Getter @RequiredArgsConstructor
public final class PetardEntity implements CustomEntity, HostileMob, TickableEntity {
    private final HostilePlugin plugin;
    private final Type hostileType = Type.PETARD;
    private final String customId = hostileType.customId;
    private static final double HEALTH = 20;
    private static final int COUNTDOWN = 6;

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Zombie.class, e -> {
                e.setCustomName("Petard");
                e.setBaby(false);
                e.getEquipment().setHelmet(makeArmor(Material.LEATHER_HELMET));
                e.getEquipment().setChestplate(makeArmor(Material.LEATHER_CHESTPLATE));
                e.getEquipment().setLeggings(makeArmor(Material.LEATHER_LEGGINGS));
                e.getEquipment().setBoots(makeArmor(Material.LEATHER_BOOTS));
                e.getEquipment().setItemInMainHand(new ItemStack(Material.TNT));
                e.getEquipment().setItemInOffHand(new ItemStack(Material.TNT));
                e.getEquipment().setHelmetDropChance(0);
                e.getEquipment().setChestplateDropChance(0);
                e.getEquipment().setLeggingsDropChance(0);
                e.getEquipment().setBootsDropChance(0);
                e.getEquipment().setItemInMainHandDropChance(0);
                e.getEquipment().setItemInOffHandDropChance(0);
                e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                e.setHealth(HEALTH);
                e.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.4);
                e.setRemoveWhenFarAway(true);
            });
    }

    @Override
    public EntityWatcher createEntityWatcher(Entity entity) {
        return new Watcher((Zombie)entity, this);
    }

    private ItemStack makeArmor(Material mat) {
        ItemStack item = new ItemStack(mat);
        LeatherArmorMeta meta = (LeatherArmorMeta)item.getItemMeta();
        meta.setColor(Color.RED);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onTick(EntityWatcher watcher) {
        ((Watcher)watcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    class Watcher implements EntityWatcher {
        private final Zombie entity;
        private final PetardEntity customEntity;
        private int ticks;
        private int countdown = COUNTDOWN;

        void onTick() {
            ticks += 1;
            if (ticks % 20 != 0) return;
            if (entity.getTarget() == null) {
                entity.setCustomName("Petard");
                entity.setCustomNameVisible(false);
                countdown = COUNTDOWN;
                return;
            } else {
                countdown -= 1;
                if (countdown < 0) {
                    entity.setCustomName("Petard");
                    entity.getWorld().createExplosion(entity.getEyeLocation(), 4f, true);
                    entity.remove();
                } else {
                    entity.setCustomName("" + countdown);
                    entity.setCustomNameVisible(true);
                }
            }
        }
    }
}
