package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.entity.CustomEntity;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.event.CustomRegisterEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.UUID;
import lombok.Data;
import lombok.Getter;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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

    @Value
    class Loc {
        private final String world;
        private final int x, y, z;
        Loc(Block block) {
            this.world = block.getWorld().getName();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
        }
        int dist(Loc other) {
            return Math.max(Math.max(Math.abs(x - other.x), Math.abs(y - other.y)), Math.abs(z - other.z));
        }
    }

    @Data
    class Session {
        private long lastKill;
        private Loc loc;
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

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String args[]) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if ("info".equals(cmd) && args.length <= 2) {
            Player target;
            if (args.length < 2 && player == null) {
                sender.sendMessage("Player expected");
                return true;
            } else if (args.length >= 2) {
                target = getServer().getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage("Player not found: " + args[1]);
                    return true;
                }
            } else {
                target = player;
            }
            Session session = sessions.get(target.getUniqueId());
            if (session == null) {
                sender.sendMessage(target.getName() + " no session!");
            } else {
                sender.sendMessage(target.getName() + " " + session);
            }
        } else {
            return false;
        }
        return true;
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
            if (event.getEntity().getScoreboardTags().contains("NoHostileScore")) return;
        }
        Session session = getSession(player);
        final int scoreFactor = 3;
        if (hostileMob != null) {
            session.score += scoreFactor;
        } else {
            session.score += 1;
        }
        long now = System.currentTimeMillis();
        Loc newLoc = new Loc(player.getLocation().getBlock());
        if (session.loc == null || !session.loc.world.equals(newLoc.world) || session.loc.dist(newLoc) > 127) {
            session.level = 0;
            session.hostileSpawnedCount = 0;
            session.score = 0;
        } else if (session.lastKill + 1000 * 60 < now) {
            session.level -= 1;
            if (session.level < 0) session.level = 0;
            session.hostileSpawnedCount = session.level;
            session.score = 0;
        } else if (session.score >= session.level * scoreFactor) {
            session.level += 1;
            session.hostileSpawnedCount = 0;
            session.score = 0;
        }
        for (int i = 0; i < 8; i += 1) {
            if (tryToSpawnMobForPlayer(player)) {
                session.hostileSpawnedCount += 1;
                break;
            }
        }
        for (int i = 0; i < session.level && session.hostileSpawnedCount < session.level / 2; i += 1) {
            if (tryToSpawnMobForPlayer(player)) {
                session.hostileSpawnedCount += 1;
            }
        }
        session.loc = newLoc;
        session.lastKill = now;
    }

    @EventHandler
    public void onPlayerDeatch(PlayerDeathEvent event) {
        sessions.remove(event.getEntity().getUniqueId());
    }

    boolean tryToSpawnMobForPlayer(Player player) {
        int rad = 10 + random.nextInt(8);
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
        if (block.getY() < 1 || block.getY() > 127 || block.isLiquid()) return false;
        for (Entity nearby: block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.0, 0.5), 8, 8, 8)) {
            if (nearby.getType() == EntityType.PLAYER) return false;
        }
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
