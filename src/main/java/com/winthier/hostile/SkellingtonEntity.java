package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Consumer;

@Getter @RequiredArgsConstructor
public final class SkellingtonEntity implements CustomEntity, HostileMob {
    private final HostilePlugin plugin;
    private final String customId = "hostile:skellington";
    private static final double HEALTH = 100;

    @Override
    public Entity spawnEntity(Location location) {
        Skeleton skeleton = location.getWorld().spawn(location, Skeleton.class, new Consumer<Skeleton>() {
            @Override
            public void accept(Skeleton skeleton) {
                skeleton.setCustomName("Skellington");
                skeleton.getEquipment().setItemInMainHand(getBow());
                skeleton.getEquipment().setItemInOffHand(new ItemStack(Material.SHIELD));
                skeleton.getEquipment().setHelmet(new ItemStack(Material.IRON_HELMET));
                skeleton.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                skeleton.setHealth(HEALTH);
            }
        });
        return skeleton;
    }

    private void adaptToPlayer(Skeleton skeleton, Player player) {
        if (!player.getWorld().equals(skeleton.getWorld())) return;
        double distance = player.getLocation().distance(skeleton.getLocation());
        if (distance < 4.0) {
            if (skeleton.getEquipment().getItemInMainHand().getType() == Material.IRON_SWORD) return;
            skeleton.getEquipment().setItemInMainHand(getSword());
            skeleton.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
        } else {
            if (skeleton.getEquipment().getItemInMainHand().getType() == Material.BOW) return;
            skeleton.getEquipment().setItemInMainHand(getBow());
            skeleton.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(0.0);
        }
        skeleton.getWorld().playSound(skeleton.getEyeLocation(), Sound.ENTITY_SNOWBALL_THROW, SoundCategory.HOSTILE, 1.0f, 0.8f);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event, EntityContext context) {
        Player player;
        if (context.getPosition() == EntityContext.Position.ENTITY) {
            if (event.getDamager() instanceof Player) {
                player = (Player)event.getDamager();
            } else if (event.getDamager() instanceof Projectile
                       && ((Projectile)event.getDamager()).getShooter() instanceof Player) {
                player = (Player)((Projectile)event.getDamager()).getShooter();
            } else {
                event.setCancelled(true);
                return;
            }
        } else if (context.getPosition() == EntityContext.Position.DAMAGER) {
            if (!(event.getEntity() instanceof Player)) return;
            player = (Player)event.getEntity();
        } else {
            return;
        }
        adaptToPlayer((Skeleton)context.getEntity(), player);
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
}
