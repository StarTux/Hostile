package com.winthier.hostile;

import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.entity.TickableEntity;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Difficulty;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.LlamaSpit;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.SpectralArrow;
import org.bukkit.util.Vector;

@Getter @RequiredArgsConstructor
public final class AngryParrotEntity implements CustomEntity, TickableEntity, HostileMob {
    private final HostilePlugin plugin;
    private final Type hostileType = Type.ANGRY_PARROT;
    private final String customId = hostileType.customId;
    private static final double HEALTH = 5;
    private static final int PROJECTILE_COUNTDOWN = 20 * 5;

    @Override
    public Entity spawnEntity(Location location) {
        return location.getWorld().spawn(location, Parrot.class, e -> {
                e.setCustomName("Angry Parrot");
                e.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                e.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
                e.setHealth(HEALTH);
                e.setRemoveWhenFarAway(true);
            });
    }

    @Override
    public Watcher createEntityWatcher(Entity entity) {
        return new Watcher((Parrot)entity, this);
    }

    @Override
    public void onTick(EntityWatcher entityWatcher) {
        if (entityWatcher.getEntity().getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            entityWatcher.getEntity().remove();
            return;
        }
        ((Watcher)entityWatcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    public final class Watcher implements EntityWatcher {
        private final Parrot entity;
        private final AngryParrotEntity customEntity;
        private int ticks = 0;
        private int projectileCountdown = PROJECTILE_COUNTDOWN;

        void onTick() {
            final int interval = 5;
            entity.setVelocity(new Vector(0, 0, 0));
            ticks += 1;
            if (ticks < interval) return;
            ticks = 0;
            entity.setAI(false);
            Block block = entity.getLocation().getBlock();
            Player target = null;
            int min = Integer.MAX_VALUE;
            int dirX = 0;
            int dirZ = 0;
            for (Player online: entity.getWorld().getPlayers()) {
                if (online.getGameMode() != GameMode.SURVIVAL
                    && online.getGameMode() != GameMode.ADVENTURE) continue;
                Block pb = online.getLocation().getBlock();
                if (pb.getLightFromSky() == 0) continue;
                int dx = pb.getX() - block.getX();
                int dz = pb.getZ() - block.getZ();
                int dist = dx * dx + dz * dz;
                if (dist < min) {
                    min = dist;
                    target = online;
                    dirX = dx;
                    dirZ = dz;
                }
            }
            if (target == null) return;
            Block targetBlock = target.getLocation().getBlock();
            ArrayList<Block> blocks = new ArrayList<>();
            blocks.add(block);
            if (block.getY() < targetBlock.getY() + 8) {
                blocks.add(block.getRelative(BlockFace.UP));
                blocks.add(block.getRelative(BlockFace.UP));
                blocks.add(block.getRelative(BlockFace.UP));
                blocks.add(block.getRelative(BlockFace.UP));
            } else if (block.getY() < targetBlock.getY() + 10) {
                blocks.add(block.getRelative(BlockFace.UP));
            }
            if (block.getY() > targetBlock.getY() + 12) blocks.add(block.getRelative(BlockFace.DOWN));
            blocks.add(block.getRelative(BlockFace.UP));
            blocks.add(block.getRelative(BlockFace.DOWN));
            blocks.add(block.getRelative(BlockFace.NORTH));
            blocks.add(block.getRelative(BlockFace.EAST));
            blocks.add(block.getRelative(BlockFace.SOUTH));
            blocks.add(block.getRelative(BlockFace.WEST));
            boolean nearTarget = dirX * dirX + dirZ * dirZ <= 4;
            if (!nearTarget) {
                if (dirX < -8) {
                    blocks.add(block.getRelative(-1, 0, 0));
                    blocks.add(block.getRelative(-1, 0, 0));
                    blocks.add(block.getRelative(-1, 0, 0));
                }
                if (dirX > 8) {
                    blocks.add(block.getRelative(1, 0, 0));
                    blocks.add(block.getRelative(1, 0, 0));
                    blocks.add(block.getRelative(1, 0, 0));
                }
                if (dirZ < -8) {
                    blocks.add(block.getRelative(0, 0, -1));
                    blocks.add(block.getRelative(0, 0, -1));
                    blocks.add(block.getRelative(0, 0, -1));
                }
                if (dirZ > 8) {
                    blocks.add(block.getRelative(0, 0, 1));
                    blocks.add(block.getRelative(0, 0, 1));
                    blocks.add(block.getRelative(0, 0, 1));
                }
            }
            for (Iterator<Block> iter = blocks.iterator(); iter.hasNext();) {
                if (iter.next().getType() != Material.AIR) iter.remove();
            }
            if (blocks.isEmpty()) return;
            Random random = new Random(System.currentTimeMillis());
            block = blocks.get(random.nextInt(blocks.size()));
            Location loc2 = block.getLocation().add(0.5, 0.5, 0.5);
            Vector direction = target.getEyeLocation().toVector().subtract(loc2.toVector()).normalize();
            loc2.setDirection(direction);
            entity.teleport(loc2);
            if (!plugin.isKillWorld(entity.getWorld())) return;
            projectileCountdown -= plugin.getRandom().nextInt(3) * interval;
            if (projectileCountdown > 0) {
                return;
            } else {
                projectileCountdown = PROJECTILE_COUNTDOWN;
            }
            switch (plugin.getRandom().nextInt(6)) {
            case 0:
                entity.launchProjectile(Fireball.class, direction.multiply(2.0));
                break;
            case 1:
                entity.launchProjectile(SmallFireball.class, direction.multiply(2.0));
                break;
            case 2:
                entity.launchProjectile(LargeFireball.class, direction.multiply(2.0));
                break;
            case 3:
                entity.launchProjectile(Arrow.class, direction.multiply(2.0));
                break;
            case 4:
                entity.launchProjectile(SpectralArrow.class, direction.multiply(2.0));
                break;
            case 5:
                entity.launchProjectile(LlamaSpit.class, direction.multiply(2.0));
                break;
            default:
                return;
            }
            entity.getWorld().playSound(entity.getEyeLocation(), Sound.ENTITY_PARROT_IMITATE_WITHER, SoundCategory.HOSTILE, 2.0f, 2.0f);
        }
    }
}
