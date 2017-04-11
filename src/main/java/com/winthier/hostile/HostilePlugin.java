package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.event.CustomRegisterEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class HostilePlugin extends JavaPlugin implements Listener {
    private final HashMap<UUID, Integer> killCount = new HashMap<>();
    private final ArrayList<String> killWorlds = new ArrayList<>();
    private final ArrayList<CustomEntity> mobs = new ArrayList<>();
    private int killThreshold = 32;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        killCount.clear();
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        saveDefaultConfig();
        reloadConfig();
        killWorlds.clear();
        killWorlds.addAll(getConfig().getStringList("KillWorlds"));
        killThreshold = getConfig().getInt("KillThreshold");
        mobs.clear();
        mobs.add(new FartGoblinEntity(this));
        mobs.add(new SkellingtonEntity(this));
        mobs.add(new BatterBatEntity(this));
        mobs.add(new FatzoEntity(this));
        for (CustomEntity mob: mobs) event.addEntity(mob);
    }

    int addKillCount(Player player, int amount) {
        UUID uuid = player.getUniqueId();
        Integer value = killCount.get(uuid);
        value = value == null ? amount : value + amount;
        killCount.put(uuid, value);
        return value;
    }

    int getKillCount(Player player) {
        Integer value = killCount.get(player.getUniqueId());
        return value == null ? 0 : value;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        killCount.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!killWorlds.contains(event.getEntity().getWorld().getName())) return;
        if (!(event.getEntity() instanceof Monster)) return;
        if (event.getEntity().getCustomName() != null) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        switch (player.getGameMode()) {
        case SURVIVAL:
        case ADVENTURE:
            break;
        default: return;
        }
        Location loc = player.getLocation();
        boolean aboveGround = loc.getWorld().getHighestBlockYAt(loc) <= loc.getY();
        if (!aboveGround) return;
        if (addKillCount(player, 1) >= killThreshold) {
            int ret = tryToSpawnMobsForPlayer(player);
            if (ret > 0) killCount.remove(player.getUniqueId());
        }
    }

    int tryToSpawnMobsForPlayer(Player player) {
        Random random = new Random(System.currentTimeMillis());
        int rad = 16;
        Block block = player.getLocation().getBlock();
        if (random.nextBoolean()) {
            block = block.getRelative(random.nextBoolean() ? rad : -rad,
                                      random.nextInt(rad) - random.nextInt(rad),
                                      random.nextInt(rad + rad) - rad);
        } else {
            block = block.getRelative(random.nextInt(rad + rad) - rad,
                                      random.nextInt(rad) - random.nextInt(rad),
                                      random.nextBoolean() ? rad : -rad);
        }
        while (block.getY() < 0) block = block.getRelative(0, 1, 0);
        while (block.getType() != Material.AIR) {
            switch (block.getType()) {
            case LAVA:
            case STATIONARY_LAVA:
            case WATER:
            case STATIONARY_WATER:
            case MAGMA:
            case CACTUS:
                return 0;
            default: break;
            }
            block = block.getRelative(0, 1, 0);
        }
        if (block.getY() - player.getLocation().getBlockY() > 32) return 0;
        if (block.getRelative(0, 1, 0).getType() != Material.AIR) return 0;
        Location location = block.getLocation().add(0.5, 0.0, 0.5);
        int count = random.nextInt(9) + 3;
        for (int i = 0; i < count; i += 1) {
            CustomEntity mob = mobs.get(random.nextInt(mobs.size()));
            CustomPlugin.getInstance().getEntityManager().spawnEntity(location, mob.getCustomId());
        }
        if (count > 0) {
            block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ENDERDRAGON_AMBIENT, SoundCategory.HOSTILE, 1.5f, 2.0f);
            getLogger().info(String.format("%d hostiles spawned for %s at %s %d,%d,%d", count, player.getName(), block.getWorld().getName(), block.getX(), block.getY(), block.getZ()));
        }
        return count;
    }

    public boolean isKillWorld(World world) {
        return killWorlds.contains(world.getName());
    }
}
