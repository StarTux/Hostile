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
import org.bukkit.util.Consumer;

@Getter @RequiredArgsConstructor
public final class FatzoEntity implements CustomEntity, HostileMob {
    private final HostilePlugin plugin;
    private final String customId = "hostile:fatzo";
    private static final double HEALTH = 50;
    private static final double SPEED = 0.2;

    @Override
    public Entity spawnEntity(Location location) {
        Zombie zombie = location.getWorld().spawn(location, Zombie.class, new Consumer<Zombie>() {
            @Override
            public void accept(Zombie zombie) {
                zombie.setCustomName("Fatzo");
                zombie.getEquipment().setHelmet(makeArmor(Material.CHAINMAIL_HELMET, 3));
                zombie.getEquipment().setChestplate(makeArmor(Material.CHAINMAIL_CHESTPLATE, 3));
                zombie.getEquipment().setLeggings(makeArmor(Material.CHAINMAIL_LEGGINGS, 3));
                zombie.getEquipment().setBoots(makeArmor(Material.CHAINMAIL_BOOTS, 3));
                zombie.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                zombie.setHealth(HEALTH);
                zombie.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(SPEED);
            }
        });
        return zombie;
    }

    private ItemStack makeArmor(Material mat, int lvl) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.THORNS, lvl, true);
        item.setItemMeta(meta);
        return item;
    }
}
