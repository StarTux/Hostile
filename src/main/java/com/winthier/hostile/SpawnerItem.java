package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.util.Dirty;
import com.winthier.custom.util.Msg;
import com.winthier.generic_events.ItemNameEvent;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter @RequiredArgsConstructor
public final class SpawnerItem implements CustomItem, UncraftableItem {
    public static final String CUSTOM_ID = "hostile:spawner";
    private final HostilePlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.MOB_SPAWNER, amount);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event, ItemContext context) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(context.getItemStack());
        SpawnerBlock.Watcher watcher = (SpawnerBlock.Watcher)CustomPlugin.getInstance().getBlockManager().wrapBlock(event.getBlock(), SpawnerBlock.CUSTOM_ID);
        State state = getState(context.getItemStack());
        watcher.setPlayerPlaced(true);
        watcher.setNatural(state.natural);
        watcher.setLevel(state.level);
        watcher.save();
        CreatureSpawner spawner = (CreatureSpawner)event.getBlock().getState();
        spawner.setSpawnedType(state.spawnedType);
        spawner.update();
    }

    @EventHandler
    public void onItemName(ItemNameEvent event, ItemContext context) {
        State state = getState(context.getItemStack());
        EntityType type = state.spawnedType;
        int level = state.level;
        event.setItemName("Level " + level + " " + Msg.camelCase(type.name()) + " Spawner");
    }

    @Data
    static final class State {
        private EntityType spawnedType;
        private boolean natural;
        private int level;
    }

    static State getState(ItemStack item) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        State result = new State();
        String tmp = config.getString("entity");
        if (tmp != null) {
            result.spawnedType = EntityType.valueOf(tmp.toUpperCase());
        } else {
            result.spawnedType = EntityType.ZOMBIE;
        }
        result.natural = config.getBoolean("natural");
        result.level = config.getInt("level");
        return result;
    }

    static void setState(ItemStack item, State state) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        config.setString("entity", state.spawnedType.name());
        config.setBoolean("natural", state.natural);
        config.setInt("level", state.level);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + "Level " + state.level + " " + Msg.camelCase(state.spawnedType.name()) + " Spawner");
        item.setItemMeta(meta);
    }
}
