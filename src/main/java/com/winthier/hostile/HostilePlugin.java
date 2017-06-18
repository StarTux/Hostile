package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.event.CustomRegisterEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class HostilePlugin extends JavaPlugin implements Listener {
    private final HashMap<UUID, Session> sessions = new HashMap<>();
    private final ArrayList<String> killWorlds = new ArrayList<>();
    private final Random random = new Random(System.currentTimeMillis());

    class Session {
        private long lastSpawn;
        private int level;
        private int hostileSpawnedCount;
        private int score;
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        sessions.clear();
    }

    @EventHandler
    public void onCustomRegister(CustomRegisterEvent event) {
        saveDefaultConfig();
        reloadConfig();
        killWorlds.clear();
        killWorlds.addAll(getConfig().getStringList("KillWorlds"));
        for (HostileMob.Type hostileType: HostileMob.Type.values()) {
            event.addEntity(hostileType.newInstance(this));
        }
    }

    Session getSession(Player player) {
        Session result = sessions.get(player.getUniqueId());
        if (result == null) {
            result = new Session();
            sessions.put(player.getUniqueId(), result);
        }
        return result;
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!killWorlds.contains(event.getEntity().getWorld().getName())) return;
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        if (!killWorlds.contains(player.getWorld().getName())) return;
        switch (player.getGameMode()) {
        case SURVIVAL:
        case ADVENTURE:
            break;
        default: return;
        }
        CustomEntity customEntity = CustomPlugin.getInstance().getEntityManager().getCustomEntity(event.getEntity());
        HostileMob hostileMob;
        if (customEntity != null && customEntity instanceof HostileMob) {
            hostileMob = (HostileMob)customEntity;
        } else {
            hostileMob = null;
            if (!(event.getEntity() instanceof Monster)) return;
            if (event.getEntity().getCustomName() != null) return;
        }
        Session session = getSession(player);
        final int scoreFactor = 3;
        if (hostileMob != null) {
            session.score += scoreFactor;
        } else {
            session.score += 1;
        }
        long now = System.currentTimeMillis();
        if (session.score >= session.level * scoreFactor) {
            session.level += 1;
            session.hostileSpawnedCount = 0;
            session.score = 0;
        } else if (session.lastSpawn + 1000 * 120 < now) {
            session.level -= 1;
            session.hostileSpawnedCount = 0;
            session.score = 0;
            session.lastSpawn = now;
        }
        for (int i = 0; i < 16 && session.hostileSpawnedCount < session.level; i += 1) {
            if (tryToSpawnMobsForPlayer(player)) {
                session.hostileSpawnedCount += 1;
                session.lastSpawn = now;
            }
        }
    }

    @EventHandler
    public void onPlayerDeatch(PlayerDeathEvent event) {
        sessions.remove(event.getEntity().getUniqueId());
    }

    boolean tryToSpawnMobsForPlayer(Player player) {
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
        if (block.getType().isSolid()) {
            while (block.getType().isSolid()) block = block.getRelative(0, 1, 0);
        } else {
            do {
                Block nextBlock = block.getRelative(0, -1, 0);
                if (nextBlock.getType().isSolid()) break;
                block = nextBlock;
            } while (block.getY() > 0);
        }
        if (block.getY() < 1 || block.getY() > 128) return false;
        ArrayList<HostileMob.Type> types = new ArrayList<>();
        for (HostileMob.Type type: HostileMob.Type.values()) {
            if (getSession(player).level < type.minLevel) continue;
            switch (type) {
            case BATTER_BAT:
            case ANGRY_PARROT:
                if (block.getLightFromSky() == 0) continue;
                break;
            case QUEEN_SPIDER:
                if (block.getLightLevel() > 7) continue;
                break;
            default:
                break;
            }
            for (int i = 0; i < type.chance; i += 1) types.add(type);
        }
        if (types.size() == 0) return false;
        HostileMob.Type type = types.get(random.nextInt(types.size()));
        Location location = block.getLocation().add(0.5, 0.0, 0.5);
        EntityWatcher watcher = CustomPlugin.getInstance().getEntityManager().spawnEntity(location, type.customId);
        if (watcher.getEntity() instanceof Creature) {
            ((Creature)watcher.getEntity()).setTarget(player);
        }
        return true;
    }

    public boolean isKillWorld(World world) {
        return killWorlds.contains(world.getName());
    }
}
