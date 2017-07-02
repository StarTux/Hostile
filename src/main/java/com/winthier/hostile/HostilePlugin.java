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

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String args[]) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("test")) {
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

    boolean tryToSpawnMob(Block block, int level) {
        int rad = 10 + random.nextInt(8);
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
            if (level < type.minLevel) continue;
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
        return true;
    }

    public boolean isKillWorld(World world) {
        return killWorlds.contains(world.getName());
    }
}
