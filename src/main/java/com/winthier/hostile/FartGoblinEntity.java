package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

@Getter @RequiredArgsConstructor
public final class FartGoblinEntity implements CustomEntity, HostileMob {
    private final HostilePlugin plugin;
    private final Type hostileType = Type.FART_GOBLIN;
    private final String customId = hostileType.customId;
    private static final double HEALTH = 50;
    private static final double SPEED = 0.5;

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Creeper.class, c -> {
                c.setCustomName("Fart Goblin");
                c.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                c.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(SPEED);
                c.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                c.setHealth(HEALTH);
                c.setRemoveWhenFarAway(true);
            });
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event, EntityContext context) {
        event.setCancelled(true);
        Creeper creeper = (Creeper)context.getEntity();
        creeper.getWorld().playSound(creeper.getEyeLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.HOSTILE, 1.0f, 0.01f);
        int offset = plugin.getRandom().nextInt(3);
        creeper.getWorld().spawn(creeper.getLocation().add(0, (double)offset, 0), AreaEffectCloud.class, c -> {
                switch (plugin.getRandom().nextInt(10)) {
                case 0:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 20, 1), true);
                    break;
                case 1:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.SLOW, 20 * 20, 2), true);
                    break;
                case 2:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.HARM, 1, 0), true);
                    break;
                case 3:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.WITHER, 20 * 10, 1), true);
                    break;
                case 4:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.LEVITATION, 20 * 3, 0), true);
                    break;
                case 5:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.HUNGER, 20 * 20, 1), true);
                    break;
                case 6:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.CONFUSION, 20 * 10, 0), true);
                    break;
                case 7:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.BLINDNESS, 20 * 10, 0), true);
                    break;
                case 8:
                default:
                    c.addCustomEffect(new PotionEffect(PotionEffectType.POISON, 20 * 20, 1), true);
                    break;
                }
                c.setColor(Color.GREEN);
                c.setDuration(200);
                c.setRadius(2.0f);
                c.setRadiusPerTick(1.0f / 100.0f);
                c.setSource(creeper);
            });
    }

    @EventHandler
    public void onAreaEffectCloudApply(AreaEffectCloudApplyEvent event, EntityContext context) {
        event.getAffectedEntities().remove(context.getEntity());
    }
}
