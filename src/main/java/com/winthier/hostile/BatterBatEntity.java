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
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Bat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.material.MaterialData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

@Getter @RequiredArgsConstructor
public final class BatterBatEntity implements CustomEntity, TickableEntity, HostileMob {
    private final HostilePlugin plugin;
    private final String customId = "hostile:batter_bat";
    private static final double HEALTH = 1;

    @Override
    public Entity spawnEntity(Location location) {
        Bat bat = location.getWorld().spawn(location, Bat.class, new Consumer<Bat>() {
            @Override
            public void accept(Bat bat) {
                bat.setCustomName("Batter Bat");
                bat.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(HEALTH);
                bat.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.0);
                bat.setHealth(HEALTH);
                bat.setRemoveWhenFarAway(false);
            }
        });
        return bat;
    }

    @Override
    public Watcher createEntityWatcher(Entity entity) {
        return new Watcher((Bat)entity, this);
    }

    @Override
    public void onTick(EntityWatcher entityWatcher) {
        if (!plugin.isKillWorld(entityWatcher.getEntity().getWorld())
            || entityWatcher.getEntity().getWorld().getDifficulty() == Difficulty.PEACEFUL) {
            entityWatcher.getEntity().remove();
        }
        ((Watcher)entityWatcher).onTick();
    }

    @Getter @RequiredArgsConstructor
    public final class Watcher implements EntityWatcher {
        private final Bat entity;
        private final BatterBatEntity customEntity;
        private int ticks = 0;

        void onTick() {
            entity.setVelocity(new Vector(0, 0, 0));
            ticks += 1;
            if (ticks < 3) return;
            ticks = 0;
            Block block = entity.getLocation().getBlock();
            Player target = null;
            int min = Integer.MAX_VALUE;
            int dirX = 0;
            int dirZ = 0;
            for (Player online: entity.getWorld().getPlayers()) {
                if (online.getGameMode() != GameMode.SURVIVAL
                    && online.getGameMode() != GameMode.ADVENTURE) continue;
                Block pb = online.getLocation().getBlock();
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
            ArrayList<Block> blocks = new ArrayList<>();
            blocks.add(block);
            if (block.getY() < 80) {
                blocks.add(block.getRelative(BlockFace.UP));
                blocks.add(block.getRelative(BlockFace.UP));
                blocks.add(block.getRelative(BlockFace.UP));
                blocks.add(block.getRelative(BlockFace.UP));
            } else if (block.getY() < 100) {
                blocks.add(block.getRelative(BlockFace.UP));
            }
            if (block.getY() > 90) blocks.add(block.getRelative(BlockFace.DOWN));
            blocks.add(block.getRelative(BlockFace.UP));
            blocks.add(block.getRelative(BlockFace.DOWN));
            blocks.add(block.getRelative(BlockFace.NORTH));
            blocks.add(block.getRelative(BlockFace.EAST));
            blocks.add(block.getRelative(BlockFace.SOUTH));
            blocks.add(block.getRelative(BlockFace.WEST));
            boolean nearTarget = dirX * dirX + dirZ * dirZ <= 4;
            if (!nearTarget) {
                if (dirX < 0) {
                    blocks.add(block.getRelative(-1, 0, 0));
                    blocks.add(block.getRelative(-1, 0, 0));
                    blocks.add(block.getRelative(-1, 0, 0));
                }
                if (dirX > 0) {
                    blocks.add(block.getRelative(1, 0, 0));
                    blocks.add(block.getRelative(1, 0, 0));
                    blocks.add(block.getRelative(1, 0, 0));
                }
                if (dirZ < 0) {
                    blocks.add(block.getRelative(0, 0, -1));
                    blocks.add(block.getRelative(0, 0, -1));
                    blocks.add(block.getRelative(0, 0, -1));
                }
                if (dirZ > 0) {
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
            entity.teleport(block.getLocation().add(0.5, 0.5, 0.5));
            if (nearTarget && block.getY() > 76 && random.nextInt(10) == 0) {
                entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 3, 0, true), true);
                Location loc = block.getLocation().add(0.5, 0.5, 0.5);
                switch (random.nextInt(5)) {
                case 0:
                    entity.getWorld().spawnFallingBlock(loc, new MaterialData(Material.SAND)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, -1, 0), new MaterialData(Material.SAND)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(-1, 0, 0), new MaterialData(Material.SAND)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(1, 0, 0), new MaterialData(Material.SAND)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, -1), new MaterialData(Material.SAND)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, 1), new MaterialData(Material.SAND)).setDropItem(false);
                    break;
                case 1:
                    entity.getWorld().spawnFallingBlock(loc, new MaterialData(Material.SAND, (byte)1)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, -1, 0), new MaterialData(Material.SAND, (byte)1)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(-1, 0, 0), new MaterialData(Material.SAND, (byte)1)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(1, 0, 0), new MaterialData(Material.SAND, (byte)1)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, -1), new MaterialData(Material.SAND, (byte)1)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, 1), new MaterialData(Material.SAND, (byte)1)).setDropItem(false);
                    break;
                case 2:
                    entity.getWorld().spawnFallingBlock(loc, new MaterialData(Material.GRAVEL)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, -1, 0), new MaterialData(Material.GRAVEL)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(-1, 0, 0), new MaterialData(Material.GRAVEL)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(1, 0, 0), new MaterialData(Material.GRAVEL)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, -1), new MaterialData(Material.GRAVEL)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, 1), new MaterialData(Material.GRAVEL)).setDropItem(false);
                    break;
                case 3:
                    entity.getWorld().spawnFallingBlock(loc, new MaterialData(Material.MAGMA)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(-1, 0, 0), new MaterialData(Material.MAGMA)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(1, 0, 0), new MaterialData(Material.MAGMA)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, -1), new MaterialData(Material.MAGMA)).setDropItem(false);
                    entity.getWorld().spawnFallingBlock(loc.clone().add(0, 0, 1), new MaterialData(Material.MAGMA)).setDropItem(false);
                    break;
                case 4:
                    entity.getWorld().spawnEntity(loc, EntityType.PRIMED_TNT);
                    break;
                default: break;
                }
            }
        }
    }
}