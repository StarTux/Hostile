package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.item.UncraftableItem;
import com.winthier.custom.util.Dirty;
import com.winthier.custom.util.Msg;
import com.winthier.generic_events.ItemNameEvent;
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
        watcher.setPlayerPlaced(true);
        watcher.setNatural(isNatural(context.getItemStack()));
        watcher.save();
        EntityType et = getSpawnedType(context.getItemStack());
        if (et != null) {
            CreatureSpawner spawner = (CreatureSpawner)event.getBlock().getState();
            spawner.setSpawnedType(et);
            spawner.update();
        }
    }

    @EventHandler
    public void onItemName(ItemNameEvent event, ItemContext context) {
        EntityType type = getSpawnedType(event.getItem());
        if (type == null) return;
        event.setItemName(Msg.camelCase(type.name()) + " Spawner");
    }

    public static EntityType getSpawnedType(ItemStack item) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        String name = config.getString("entity");
        if (name == null) return null;
        return EntityType.valueOf(name.toUpperCase());
    }

    public static void setSpawnedType(ItemStack item, EntityType type) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        config.setString("entity", type.name());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + Msg.camelCase(type.name()) + " Spawner");
        item.setItemMeta(meta);
    }

    public static boolean isNatural(ItemStack item) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        return config.getBoolean("natural");
    }

    public static void setNatural(ItemStack item, boolean natural) {
        Dirty.TagWrapper config = Dirty.TagWrapper.getItemConfigOf(item);
        config.setBoolean("natural", natural);
    }
}
