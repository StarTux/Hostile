package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.block.BlockWatcher;
import com.winthier.custom.entity.EntityWatcher;
import com.winthier.custom.event.CustomRegisterEvent;
import com.winthier.custom.event.CustomTickEvent;
import com.winthier.ore.DungeonRevealEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import lombok.Getter;
import lombok.Value;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapCursor;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;

@Getter
public final class HostilePlugin extends JavaPlugin implements Listener {
    private final ArrayList<String> killWorlds = new ArrayList<>();
    private final Random random = new Random(System.currentTimeMillis());
    private final Map<Loc, MonsterHiveBlock.Watcher> hives = new HashMap<>();
    private int hiveTicks = 0;

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
        for (BossEntity.BossType bossType: BossEntity.BossType.values()) {
            event.addEntity(new BossEntity(this, bossType));
        }
        event.addBlock(new MonsterHiveBlock(this));
        event.addEntity(new MonsterHiveLevelEntity(this));
        event.addBlock(new SpawnerBlock(this));
        event.addItem(new SpawnerItem(this));
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
        for (MetadataValue meta: event.getEntity().getMetadata(SpawnerBlock.METADATA_KEY)) {
            if (meta.getOwningPlugin() == this) {
                int level = meta.asInt();
                if (level < 0) level = 0;
                for (Iterator<ItemStack> iter = event.getDrops().iterator(); iter.hasNext();) {
                    ItemStack drop = iter.next();
                    if (drop.getType().getMaxStackSize() > 1) {
                        int amount;
                        switch (level) {
                        case 0:
                            amount = 0;
                            break;
                        case 1:
                            amount = drop.getAmount() / 2;
                            break;
                        case 2:
                            amount = drop.getAmount();
                            break;
                        case 3:
                            amount = (drop.getAmount() * 3) / 2;
                            break;
                        case 4:
                            amount = drop.getAmount() * 2;
                            break;
                        default:
                            amount = (drop.getAmount() * level) / 2;
                        }
                        if (amount <= 0) {
                            iter.remove();
                        } else {
                            drop.setAmount(amount);
                        }
                    }
                }
                break;
            }
        }
        if (!isKillWorld(event.getEntity().getWorld())) return;
        for (MonsterHiveBlock.Watcher hive: hives.values()) hive.entityDidDie(event.getEntity());
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
        if (chance == 0) return;
        if (random.nextInt(100) >= chance) return;
        Block block = event.getEntity().getKiller().getLocation().getBlock();
        if (!isKillWorld(block.getWorld())) return;
        tryToSpawnHive(block);
    }

    void registerHive(MonsterHiveBlock.Watcher watcher) {
        hives.put(new Loc(watcher.getBlock()), watcher);
    }

    void unregisterHive(MonsterHiveBlock.Watcher watcher) {
        hives.remove(new Loc(watcher.getBlock()));
    }

    void on10Ticks() {
        for (Player player: getServer().getOnlinePlayers()) {
            if (!isKillWorld(player.getWorld())) {
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

    boolean tryToSpawnHive(Block block) {
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
        for (int i = 0; i < 3; i += 1) {
            if (block.getRelative(0, 1, 0).getType() == Material.AIR) {
                block = block.getRelative(0, 1, 0);
            }
        }
        for (Entity nearby: block.getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), 8, 8, 8)) {
            if (nearby.getType() == EntityType.PLAYER) return false;
        }
        Loc newLoc = new Loc(block);
        for (Loc loc: hives.keySet()) {
            if (loc.dist(newLoc) < 128) {
                return false;
            }
        }
        MonsterHiveBlock.Watcher watcher = (MonsterHiveBlock.Watcher)CustomPlugin.getInstance().getBlockManager().setBlock(block, MonsterHiveBlock.CUSTOM_ID);
        watcher.save();
        registerHive(watcher);
        getLogger().info("Spawned hive at " + newLoc.world + " " + newLoc.x + "," + newLoc.y + "," + newLoc.z);
        return true;
    }

    Entity tryToSpawnMob(Block block, MobType type, int tries) {
        for (int i = 0; i < tries; i += 1) {
            Entity e = tryToSpawnMob(block, type);
            if (e != null) return e;
        }
        return null;
    }

    Entity tryToSpawnMob(Block block, MobType type) {
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
        if (block.getY() < 1 || block.getY() > 127 || below.isLiquid()) return null;
        Location location = block.getLocation().add(0.5, 0.0, 0.5);
        if (type instanceof HostileMob.Type) {
            HostileMob.Type htype = (HostileMob.Type)type;
            switch (htype) {
            case BATTER_BAT:
            case ANGRY_PARROT:
                if (block.getLightFromSky() == 0) return null;
                break;
            case QUEEN_SPIDER:
                if (block.getLightLevel() > 7) return null;
                break;
            default: break;
            }
            EntityWatcher watcher = CustomPlugin.getInstance().getEntityManager().spawnEntity(location, htype.customId);
            return watcher.getEntity();
        } else if (type instanceof BossEntity.BossType) {
            BossEntity.BossType btype = (BossEntity.BossType)type;
            EntityWatcher watcher = CustomPlugin.getInstance().getEntityManager().spawnEntity(location, btype.customId);
            return watcher.getEntity();
        } else if (type instanceof VanillaMob) {
            VanillaMob vtype = (VanillaMob)type;
            switch (vtype.entityType) {
            case SPIDER:
                if (block.getLightLevel() > 7) return null;
                break;
            default: break;
            }
            return vtype.spawn(location);
        } else {
            return null;
        }
    }

    public boolean isKillWorld(World world) {
        return killWorlds.contains(world.getName());
    }

    @EventHandler
    public void onDungeonReveal(DungeonRevealEvent event) {
        for (CreatureSpawner spawner: event.getSpawners()) {
            SpawnerBlock.Watcher watcher = (SpawnerBlock.Watcher)CustomPlugin.getInstance().getBlockManager().wrapBlock(spawner.getBlock(), SpawnerBlock.CUSTOM_ID);
            watcher.setPlayerPlaced(false);
            watcher.setNatural(false);
            watcher.save();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.MOB_SPAWNER) return;
        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        if (tool == null || !tool.containsEnchantment(Enchantment.SILK_TOUCH)) return;
        BlockWatcher bw = CustomPlugin.getInstance().getBlockManager().getBlockWatcher(block);
        if (bw != null) {
            if (!(bw instanceof SpawnerBlock.Watcher)) return;
            SpawnerBlock.Watcher watcher = (SpawnerBlock.Watcher)bw;
            if (watcher.isMarker()) return;
            CustomPlugin.getInstance().getBlockManager().removeBlockWatcher(watcher);
            SpawnerItem.State state = new SpawnerItem.State();
            state.setSpawnedType(((CreatureSpawner)block.getState()).getSpawnedType());
            state.setNatural(watcher.isNatural());
            state.setLevel(watcher.getLevel());
            ItemStack item = CustomPlugin.getInstance().getItemManager().spawnItemStack(SpawnerItem.CUSTOM_ID, 1);
            SpawnerItem.setState(item, state);
            getServer().getScheduler().runTask(this, () -> block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item));
        } else {
            SpawnerItem.State state = new SpawnerItem.State();
            state.setSpawnedType(((CreatureSpawner)block.getState()).getSpawnedType());
            state.setNatural(true);
            state.setLevel(0);
            ItemStack item = CustomPlugin.getInstance().getItemManager().spawnItemStack(SpawnerItem.CUSTOM_ID, 1);
            SpawnerItem.setState(item, state);
            getServer().getScheduler().runTask(this, () -> block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), item));
        }
    }
}
