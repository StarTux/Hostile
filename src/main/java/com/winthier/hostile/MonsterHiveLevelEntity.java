package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityContext;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

public final class MonsterHiveLevelEntity implements CustomEntity, TickableEntity {
    public static final String CUSTOM_ID = "hostile:monster_hive_level";
    private final HostilePlugin plugin;

    MonsterHiveLevelEntity(HostilePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setMarker(true);
                as.setSmall(true);
                as.setGravity(false);
            });
    }

    @Override
    public Watcher createEntityWatcher(Entity entity) {
        return new Watcher((ArmorStand)entity, this);
    }

    @Override
    public void onTick(EntityWatcher entityWatcher) {
        ((Watcher)entityWatcher).onTick();
    }

    @Override
    public void entityWillUnload(EntityWatcher entityWatcher) {
        ((Watcher)entityWatcher).remove();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamage(EntityDamageEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityCombust(EntityCombustEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event, EntityContext context) {
        event.setCancelled(true);
    }

    @Getter @Setter @RequiredArgsConstructor
    public final class Watcher implements EntityWatcher {
        private final ArmorStand entity;
        private final MonsterHiveLevelEntity customEntity;
        private int ticks = -1;

        void remove() {
            entity.remove();
            CustomPlugin.getInstance().getEntityManager().removeEntityWatcher(this);
        }

        void onTick() {
            ticks += 1;
            if (ticks % 20 != 0) return;
            BlockWatcher watcher = CustomPlugin.getInstance().getBlockManager().getBlockWatcher(entity.getLocation().getBlock().getRelative(0, -1, 0));
            if (watcher == null || !(watcher instanceof MonsterHiveBlock.Watcher)) {
                remove();
                return;
            }
            int level = ((MonsterHiveBlock.Watcher)watcher).getLevel();
            entity.setCustomName("" + level);
            entity.setCustomNameVisible(true);
        }
    }
}
