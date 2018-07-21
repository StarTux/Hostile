package com.winthier.hostile;

import com.winthier.custom.CustomPlugin;
import com.winthier.custom.item.CustomItem;
import com.winthier.custom.item.ItemContext;
import com.winthier.custom.util.Dirty;
import com.winthier.custom.util.Msg;
import com.winthier.generic_events.ItemNameEvent;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

@Getter @RequiredArgsConstructor
public final class SpawnerItem implements CustomItem {
    public static final String CUSTOM_ID = "hostile:spawner";
    private final HostilePlugin plugin;

    @Override
    public String getCustomId() {
        return CUSTOM_ID;
    }

    @Override
    public ItemStack spawnItemStack(int amount) {
        return new ItemStack(Material.SPAWNER, amount);
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

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event, ItemContext context) {
        if (context.getPosition() != ItemContext.Position.ANVIL_LEFT) return;
        String text = event.getInventory().getRenameText();
        if (event.getInventory().getItem(1) == null) {
            event.setResult(null);
            return;
        }
        if (!CUSTOM_ID.equals(CustomPlugin.getInstance().getItemManager().getCustomId(event.getInventory().getItem(1)))) {
            event.setResult(null);
            return;
        }
        if (event.getInventory().getItem(0).getAmount() != 1
            || event.getInventory().getItem(1).getAmount() != 1) {
            event.setResult(null);
            return;
        }
        State a = getState(event.getInventory().getItem(0));
        State b = getState(event.getInventory().getItem(1));
        if (a.spawnedType != b.spawnedType) {
            event.setResult(null);
            return;
        }
        if (a.level != b.level) {
            event.setResult(null);
            return;
        }
        ItemStack result = CustomPlugin.getInstance().getItemManager().spawnItemStack(CUSTOM_ID, 1);
        State c = new State();
        c.spawnedType = a.spawnedType;
        c.natural = false;
        c.level = a.level + 1;
        setState(result, c);
        event.setResult(result);
        event.getInventory().setRepairCost(0);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event, ItemContext context) {
        if (event.getInventory().getType() != InventoryType.ANVIL) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;
        if (event.getSlot() != 2) return;
        if (!event.isShiftClick() && event.getCursor() != null && event.getCursor().getType() != Material.AIR) return;
        event.setCancelled(true);
        if (event.isShiftClick()) {
            if (!context.getPlayer().getInventory().addItem(context.getItemStack()).isEmpty()) {
                return;
            }
        } else {
            event.getView().setCursor(context.getItemStack());
        }
        event.getInventory().clear();
        context.getPlayer().playSound(context.getPlayer().getEyeLocation(), Sound.BLOCK_ANVIL_USE, SoundCategory.BLOCKS, 0.3f, 1.5f);
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
