package de.coryson.corysLootSpawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.persistence.PersistentDataType;

public class LootSpawnerListener implements Listener {

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        var itemMeta = event.getItemInHand().getItemMeta();
        if (itemMeta == null) return;

        var container = itemMeta.getPersistentDataContainer();
        String typeName = container.get(LootSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING);
        if (typeName == null) return;

        var state = (TileState) block.getState();
        state.getPersistentDataContainer().set(LootSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING, typeName);
        state.update();

        SpawnerType type = SpawnerType.valueOf(typeName);
        LootSpawnerBlock blockObj = new LootSpawnerBlock(block.getLocation(), type);
        LootSpawner.getInstance().registerSpawner(blockObj);
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        var state = (TileState) block.getState();
        String typeName = state.getPersistentDataContainer().get(LootSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING);
        if (typeName == null) return;

        Location loc = block.getLocation();
        LootSpawnerBlock spawner = LootSpawner.getInstance().getSpawnerByLocation(loc);
        if (spawner != null) {
            spawner.remove();
            LootSpawner.getInstance().unregisterSpawner(spawner);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null || event.getClickedBlock().getType() != Material.SPAWNER) return;
        if (!(event.getClickedBlock().getState() instanceof TileState state)) return;

        event.setUseItemInHand(Event.Result.DENY);

        String typeName = state.getPersistentDataContainer().get(LootSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING);
        if (typeName == null) return;

        Location location = event.getClickedBlock().getLocation();
        Inventory inv = LootSpawnerBlock.getInventory(location);
        if (inv == null) {
            inv = Bukkit.createInventory(new LootSpawnerBlock.SpawnerInventoryHolder(location), 18, typeName + " Spawner");
            LootSpawnerBlock.registerInventory(location, inv);
        }

        event.getPlayer().openInventory(inv);
    }


    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        LootSpawnerBlock.handleInventoryClick(event);
    }
}
