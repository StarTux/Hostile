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
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Consumer;

@Getter @RequiredArgsConstructor
public final class FartGoblinEntity implements CustomEntity, HostileMob {
    private final HostilePlugin plugin;
    private final String customId = "hostile:fart_goblin";
    private static final double HEALTH = 100;
    private static final double SPEED = 0.5;

    @Override
    public Entity spawnEntity(Location location) {
        Creeper creeper = location.getWorld().spawn(location, Creeper.class, new Consumer<Creeper>() {
            @Override
            public void accept(Creeper creeper) {
                creeper.setCustomName("Fart Goblin");
                creeper.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                creeper.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(SPEED);
                creeper.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).setBaseValue(1.0);
                creeper.setHealth(HEALTH);
            }
        });
        return creeper;
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event, EntityContext context) {
        event.setCancelled(true);
        Creeper creeper = (Creeper)context.getEntity();
        creeper.getWorld().playSound(creeper.getEyeLocation(), Sound.ENTITY_PLAYER_BURP, SoundCategory.HOSTILE, 1.0f, 0.01f);
        creeper.getWorld().spawn(creeper.getLocation(), AreaEffectCloud.class, new Consumer<AreaEffectCloud>() {
            @Override
            public void accept(AreaEffectCloud cloud) {
                cloud.setBasePotionData(new PotionData(PotionType.POISON, false, false));
                cloud.setColor(Color.GREEN);
                cloud.setDuration(200);
                cloud.setRadius(2.0f);
                cloud.setRadiusPerTick(1.0f / 100.0f);
            }
        });
    }
}
