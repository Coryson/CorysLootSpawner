package de.coryson.corysLootSpawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class LootSpawnerBlock {

    private static final Map<Location, Inventory> inventories = new HashMap<>();
    private static final long LOOT_INTERVAL_MILLIS = 2 * 60 * 1000;

    private final Location location;
    private final SpawnerType type;
    private ArmorStand timerLine;
    private long nextDropTime; // timestamp in ms

    public LootSpawnerBlock(Location location, SpawnerType type) {
        this.location = location;
        this.type = type;

        Inventory loaded = LootSpawner.getInstance().getStorage().loadInventory(location);
        if (loaded == null) {
            loaded = Bukkit.createInventory(new SpawnerInventoryHolder(location), 18,"§6Loot-Spawner");
        }
        inventories.put(location, loaded);
        nextDropTime = System.currentTimeMillis() + LOOT_INTERVAL_MILLIS;

        Bukkit.getScheduler().runTask(LootSpawner.getInstance(), () -> {
            Block block = location.getBlock();
            if (block.getState() instanceof CreatureSpawner spawner) {
                int safeDelay = 999999;
                spawner.setSpawnedType(type.getEntityType());
                spawner.setSpawnCount(0);
                spawner.setMaxSpawnDelay(safeDelay);
                spawner.setMinSpawnDelay(safeDelay);
                spawner.setDelay(safeDelay);
                spawner.setRequiredPlayerRange(0);
                spawner.update();
            }

            spawnHologram();
        });
    }

    public Location getLocation() {
        return location;
    }

    public void tick() {
        Inventory inv = inventories.get(location);

        boolean isCompletelyFull = true;
        if (inv != null) {
            for (ItemStack item : inv.getStorageContents()) {
                if (item == null || item.getType() == Material.AIR || item.getAmount() < 64) {
                    isCompletelyFull = false;
                    break;
                }
            }
        }

        long millisLeft = nextDropTime - System.currentTimeMillis();
        millisLeft = Math.max(millisLeft, 0);

        if (timerLine != null && timerLine.isValid()) {
            if (isCompletelyFull) {
                timerLine.setCustomName("§cVoll (§7" + formatTime(millisLeft) + "§c)");
            } else {
                timerLine.setCustomName("§7⏳ " + formatTime(millisLeft));
            }
        }

        if (System.currentTimeMillis() >= nextDropTime) {
            nextDropTime = System.currentTimeMillis() + LOOT_INTERVAL_MILLIS;
            if (!isCompletelyFull) {
                tryGenerateLoot();
            }
        }
    }

    public void tryGenerateLoot() {
        Inventory inv = inventories.get(location);
        if (inv == null) return;

        for (Drop drop : type.loot) {
            inv.addItem(new ItemStack(drop.material, drop.amount));
        }

        Location effectLoc = location.clone().add(0.5, 1.0, 0.5);
        location.getWorld().spawnParticle(Particle.SMOKE, effectLoc, 10, 0.2, 0.2, 0.2, 0.01);
        location.getWorld().spawnParticle(Particle.FLAME, effectLoc, 5, 0.1, 0.1, 0.1, 0.02);
        location.getWorld().playSound(location, Sound.BLOCK_BEACON_AMBIENT, 0.5f, 2.0f);

        LootSpawner.getInstance().getStorage().saveSpawner(location, type, inv);
    }

    private String formatTime(long millis) {
        long totalSeconds = millis / 1000;
        long min = totalSeconds / 60;
        long sec = totalSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private void spawnHologram() {
        Location holoLoc = location.clone().add(0.5, 1.3, 0.5);

        location.getWorld().getNearbyEntities(holoLoc, 0.5, 1.0, 0.5).stream()
                .filter(e -> e instanceof ArmorStand)
                .filter(e -> e.getCustomName() != null && (e.getCustomName().contains("⏳") || e.getCustomName().contains("Voll")))
                .forEach(Entity::remove);

        timerLine = (ArmorStand) location.getWorld().spawnEntity(holoLoc, EntityType.ARMOR_STAND);
        timerLine.setVisible(false);
        timerLine.setCustomNameVisible(true);
        timerLine.setCustomName("§7⏳ 02:00");
        timerLine.setMarker(true);
        timerLine.setGravity(false);
        timerLine.setSmall(true);
        timerLine.setSilent(true);
        timerLine.setAI(false);
        timerLine.setInvulnerable(true);
    }

    public static void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SpawnerInventoryHolder)) return;

        if (event.getRawSlot() < event.getInventory().getSize()) {
            if (event.getClick().isShiftClick() || event.getClick().isLeftClick() || event.getClick().isRightClick()) {
                ItemStack currentItem = event.getCurrentItem();
                if (currentItem == null || currentItem.getType() == Material.AIR) return;
            } else {
                event.setCancelled(true);
            }
        } else {
            event.setCancelled(true);
        }
    }

    public void remove() {
        if (timerLine != null) timerLine.remove();
        removeInventory(location);
        LootSpawner.getInstance().getStorage().deleteSpawner(location);
    }

    public static Inventory getInventory(Location location) {
        return inventories.get(location);
    }

    public static void registerInventory(Location loc, Inventory inv) {
        inventories.put(loc, inv);
    }

    public static void removeInventory(Location loc) {
        inventories.remove(loc);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LootSpawnerBlock that = (LootSpawnerBlock) obj;
        return this.location.equals(that.location);
    }

    @Override
    public int hashCode() {
        return Objects.hash(location);
    }

    public static class SpawnerInventoryHolder implements InventoryHolder {
        private final Location loc;
        public SpawnerInventoryHolder(Location loc) {
            this.loc = loc;
        }
        @Override
        public Inventory getInventory() {
            return LootSpawnerBlock.getInventory(loc);
        }
    }
}