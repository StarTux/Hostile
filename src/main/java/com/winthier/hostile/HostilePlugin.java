package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.event.CustomTickEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.Getter;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class HostilePlugin extends JavaPlugin implements Listener {
    private final ArrayList<String> killWorlds = new ArrayList<>();
    private final Random random = new Random(System.currentTimeMillis());
    private final Map<Loc, Integer> hives = new HashMap<>();
    private int hiveTicks = 0;
    private int playerIndex = 0;

    @Value
    public final class Loc {
        private final String world;
        private final int x, y, z;
        public Loc(Block block) {
            this.world = block.getWorld().getName();
            this.x = block.getX();
            this.y = block.getY();
            this.z = block.getZ();
        }
        public int dist(Loc other) {
            return Math.max(Math.max(Math.abs(x - other.x), Math.abs(y - other.y)), Math.abs(z - other.z));
        }
        public Block getBlock() {
            return getServer().getWorld(world).getBlockAt(x, y, z);
        }
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, () -> on10Ticks(), 10, 10);
    }

    @Override
    public void onDisable() {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        Player player = sender instanceof Player ? (Player)sender : null;
        String cmd = args.length > 0 ? args[0].toLowerCase() : null;
        if (cmd == null) {
            return false;
        } else if (cmd.equals("test")) {
            VanillaMob vanillaMob = VanillaMob.valueOf(args[1].toUpperCase());
            Entity entity = vanillaMob.spawn(player.getLocation());
            if (entity == null) {
                player.sendMessage("Failed to spawn " + vanillaMob + "!");
            } else {
                player.sendMessage(vanillaMob + " spawned (" + entity.getType() + ")");
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
        event.addBlock(new MonsterHiveBlock(this));
        event.addEntity(new MonsterHiveLevelEntity(this));
    }

    @EventHandler
    public void onCustomTick(CustomTickEvent event) {
        switch (event.getType()) {
        case WILL_TICK_BLOCKS:
            if (hiveTicks <= 0) {
                hives.clear();
            } else {
                hiveTicks -= 1;
            }
            break;
        case DID_TICK_BLOCKS:
            if (hiveTicks <= 0) {
                hiveTicks = 20;
            }
            break;
        default: break;
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (!isKillWorld(event.getEntity().getWorld())) return;
        long time = event.getEntity().getWorld().getTime();
        if (time < 13000 || time > 23000) return;
        if (!(event.getEntity() instanceof Monster)) return;
        if (event.getEntity().getKiller() == null) return;
        if (event.getEntity().getLocation().getBlock().getLightFromSky() == 0) return;
        int chance = 0;
        Player player = event.getEntity().getKiller();
        for (ItemStack item: player.getEquipment().getArmorContents()) {
            if (item == null) continue;
            switch (item.getType()) {
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
                chance += 1;
            default: break;
            }
        }
        if (random.nextInt(40) >= chance) return;
        Block block = event.getEntity().getKiller().getLocation().getBlock();
        if (!isKillWorld(block.getWorld())) return;
        tryToSpawnHive(block, 0);
    }

    void registerHive(Block block, int level) {
        hives.put(new Loc(block), level);
    }

    void unregisterHive(Block block) {
        hives.remove(new Loc(block));
    }

    void on10Ticks() {
        for (Player player: getServer().getOnlinePlayers()) {
            long time = player.getWorld().getTime();
            if (!isKillWorld(player.getWorld())
                || time < 13000 || time > 23000) {
                player.removeMetadata("MiniMapCursors", this);
                continue;
            }
            List<Map> list = new ArrayList<>();
            String world = player.getWorld().getName();
            Loc playerLoc = new Loc(player.getLocation().getBlock());
            for (Loc loc: hives.keySet()) {
                if (!loc.getWorld().equals(world)) continue;
                int dist = loc.dist(playerLoc);
                if (dist >= 256) continue;
                Map<String, Object> map = new HashMap<>();
                map.put("block", loc.getBlock());
                map.put("type", MapCursor.Type.RED_MARKER);
                list.add(map);
            }
            player.setMetadata("MiniMapCursors", new FixedMetadataValue(this, list));
        }
    }

    boolean tryToSpawnHive(Block block, int level, int tries) {
        for (int i = 0; i < tries; i += 1) {
            if (tryToSpawnHive(block, level)) return true;
        }
        return false;
    }

    boolean tryToSpawnHive(Block block, int level) {
        int rad = 32 + random.nextInt(64);
        int dx, dz;
        if (random.nextBoolean()) {
            dx = random.nextBoolean() ? rad : -rad;
            dz = random.nextInt(rad + rad) - rad;
        } else {
            dx = random.nextInt(rad + rad) - rad;
            dz = random.nextBoolean() ? rad : -rad;
        }
        block = block.getWorld().getHighestBlockAt(block.getX() + dx, block.getZ() + dz);
        if (block.getY() < 1 || block.getY() > 127 || block.isLiquid()) return false;
        switch (block.getBiome()) {
        case MUSHROOM_ISLAND:
        case MUSHROOM_ISLAND_SHORE:
            return false;
        default:
            break;
        }
        if (block.getRelative(0, 1, 0).getType() == Material.AIR) {
            block = block.getRelative(0, 1, 0);
        }
        for (Entity nearby: block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 8, 8, 8)) {
            if (nearby.getType() == EntityType.PLAYER) return false;
        }
        Loc newLoc = new Loc(block);
        for (Loc loc: hives.keySet()) {
            if (level <= hives.get(loc) && loc.dist(newLoc) < 128) {
                return false;
            }
        }
        MonsterHiveBlock.Watcher watcher = (MonsterHiveBlock.Watcher)CustomPlugin.getInstance().getBlockManager().setBlock(block, MonsterHiveBlock.CUSTOM_ID);
        watcher.setLevel(level);
        watcher.save();
        registerHive(block, level);
        getLogger().info("Spawned hive level " + level + " at " + newLoc.world + " " + newLoc.x + "," + newLoc.y + "," + newLoc.z);
        return true;
    }

    int tryToSpawnMob(Block block, int level, int tries) {
        for (int i = 0; i < tries; i += 1) {
            int weight = tryToSpawnMob(block, level);
            if (weight > 0) return weight;
        }
        return 0;
    }

    int tryToSpawnMob(Block block, int level) {
        int rad = 8 + random.nextInt(5);
        int dx, dz;
        if (random.nextBoolean()) {
            dx = random.nextBoolean() ? rad : -rad;
            dz = random.nextInt(rad + rad) - rad;
        } else {
            dx = random.nextInt(rad + rad) - rad;
            dz = random.nextBoolean() ? rad : -rad;
        }
        block = block.getWorld().getHighestBlockAt(block.getX() + dx, block.getZ() + dz);
        Block below = block.getRelative(0, -1, 0);
        if (block.getY() < 1 || block.getY() > 127 || below.isLiquid()) return 0;
        ArrayList<Enum> types = new ArrayList<>();
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
        for (VanillaMob type: VanillaMob.values()) {
            if (level < type.minLevel) continue;
            switch (type.entityType) {
            case SPIDER:
                if (block.getLightLevel() > 7) continue;
                break;
            default: break;
            }
            for (int i = 0; i < type.chance; i += 1) types.add(type);
        }
        if (types.size() == 0) return 0;
        Enum type = types.get(random.nextInt(types.size()));
        Location location = block.getLocation().add(0.5, 0.0, 0.5);
        if (type instanceof HostileMob.Type) {
            HostileMob.Type htype = (HostileMob.Type)type;
            CustomPlugin.getInstance().getEntityManager().spawnEntity(location, htype.customId);
            return htype.weight;
        } else if (type instanceof VanillaMob) {
            VanillaMob vtype = (VanillaMob)type;
            vtype.spawn(location);
            return vtype.weight;
        } else {
            return 0;
        }
    }

    public boolean isKillWorld(World world) {
        return killWorlds.contains(world.getName());
    }
}
