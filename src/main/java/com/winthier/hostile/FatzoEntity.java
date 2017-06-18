package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter @RequiredArgsConstructor
public final class FatzoEntity implements CustomEntity, HostileMob {
    private final HostilePlugin plugin;
    private final Type hostileType = Type.FATZO;
    private final String customId = hostileType.customId;
    private static final double HEALTH = 50;
    private static final double SPEED = 0.2;

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Zombie.class, e -> {
                e.setCustomName("Fatzo");
                e.setBaby(false);
                e.getEquipment().setHelmet(makeArmor(Material.CHAINMAIL_HELMET));
                e.getEquipment().setChestplate(makeArmor(Material.CHAINMAIL_CHESTPLATE));
                e.getEquipment().setLeggings(makeArmor(Material.CHAINMAIL_LEGGINGS));
                e.getEquipment().setBoots(makeArmor(Material.CHAINMAIL_BOOTS));
                e.getEquipment().setHelmetDropChance(0);
                e.getEquipment().setChestplateDropChance(0);
                e.getEquipment().setLeggingsDropChance(0);
                e.getEquipment().setBootsDropChance(0);
                e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                e.setHealth(HEALTH);
                e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(SPEED);
            });
    }

    private ItemStack makeArmor(Material mat) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.THORNS, 15, true);
        item.setItemMeta(meta);
        return item;
    }
}
