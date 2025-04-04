package de.coryson.corysSpawner.spawner;

import de.coryson.corysSpawner.CorysSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Spawner {

    private static final Map<Location, Inventory> inventories = new HashMap<>();
    private final Location location;
    private final SpawnerType type;
    private long nextDropTime;
    private int storedExp = 0;

    public Spawner(Location location, SpawnerType type) {
        this.location = location;
        this.type = type;

        Inventory inv = CorysSpawner.getInstance().getStorage().loadInventory(location);
        Inventory spawnerInventory = Bukkit.createInventory(new SpawnerListener.SpawnerInventoryHolder(location), 27, "Spawner - Inventar");

        if (inv != null) {
            for (int i = 0; i < Math.min(18, inv.getSize()); i++) {
                spawnerInventory.setItem(i, inv.getItem(i));
            }
        }

        inventories.put(location, spawnerInventory);
        this.storedExp = CorysSpawner.getInstance().getStorage().loadExp(location);
        CorysSpawner.getInstance().getStorage().saveSpawner(location, type, getFilteredInventory());
        CorysSpawner.getInstance().getStorage().saveExp(location, storedExp);

        Block block = location.getBlock();
        if (block.getState() instanceof TileState state) {
            state.getPersistentDataContainer().set(CorysSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING, type.name());
            state.update();
        }

        if (block.getState() instanceof CreatureSpawner spawner) {
            spawner.setSpawnedType(type.getEntityType());
            spawner.update();
        }

        resetNextDropTime();
    }

    public SpawnerType getType() {
        return type;
    }

    public Location getLocation() {
        return location;
    }

    public Inventory getInventory() {
        return inventories.get(location);
    }

    public Inventory getFilteredInventory() {
        Inventory original = getInventory();
        if (original == null) return null;

        Inventory filtered = Bukkit.createInventory(null, 18);
        for (int i = 0; i < 18; i++) {
            ItemStack item = original.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                filtered.setItem(i, item.clone());
            }
        }
        return filtered;
    }

    public void tick() {
        if (!isPlayerNearby()) return;

        Inventory inv = inventories.get(location);
        boolean isCompletelyFull = true;

        if (inv != null) {
            for (int i = 0; i < 18; i++) {
                ItemStack item = inv.getItem(i);
                if (item == null || item.getType() == Material.AIR || item.getAmount() < 64) {
                    isCompletelyFull = false;
                    break;
                }
            }
        }

        if (System.currentTimeMillis() >= nextDropTime) {
            if (!isCompletelyFull) {
                generateLoot();
            }
            resetNextDropTime();
        }
    }

    public void generateLoot() {
        Inventory inv = inventories.get(location);
        if (inv == null) return;

        for (Drop drop : type.loot) {
            ItemStack item = new ItemStack(drop.material(), drop.amount());
            boolean added = false;

            for (int i = 0; i < 18; i++) {
                ItemStack current = inv.getItem(i);
                if (current == null || current.getType() == Material.AIR) {
                    inv.setItem(i, item);
                    added = true;
                    break;
                } else if (current.isSimilar(item) && current.getAmount() + item.getAmount() <= current.getMaxStackSize()) {
                    current.setAmount(current.getAmount() + item.getAmount());
                    added = true;
                    break;
                }
            }

            if (!added) {
                Objects.requireNonNull(location.getWorld()).dropItemNaturally(location.clone().add(0.5, 1.0, 0.5), item);
            }
        }

        storedExp += 3;
        CorysSpawner.getInstance().getStorage().saveExp(location, storedExp);
        CorysSpawner.getInstance().getStorage().saveSpawner(location, type, getFilteredInventory());

        Location effectLoc = location.clone().add(0.5, 1.0, 0.5);
        Objects.requireNonNull(location.getWorld()).spawnParticle(Particle.FLAME, effectLoc, 10, 0.2, 0.2, 0.2, 0.01);
        location.getWorld().playSound(effectLoc, Sound.ITEM_FIRECHARGE_USE, 0.3f, 1.5f);
    }

    public int collectExp() {
        int exp = storedExp;
        storedExp = 0;
        CorysSpawner.getInstance().getStorage().saveExp(location, storedExp);
        return exp;
    }

    public void remove() {
        removeInventory(location);
    }

    private void resetNextDropTime() {
        nextDropTime = System.currentTimeMillis() + 10_000 + (long) (Math.random() * 30_000);
    }

    private boolean isPlayerNearby() {
        Location center = location.clone().add(0.5, 0.5, 0.5);
        for (Player player : Objects.requireNonNull(location.getWorld()).getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= 256) {
                return true;
            }
        }
        return false;
    }

    public int getItemCount() {
        Inventory inv = getInventory();
        if (inv == null) return 0;

        int count = 0;
        for (int i = 0; i < 18; i++) {
            ItemStack item = inv.getItem(i);
            if (item != null && item.getType() != Material.AIR) {
                count += item.getAmount();
            }
        }
        return count;
    }

    public int getStoredExp() {
        return storedExp;
    }

    public static Inventory getInventory(Location location) {
        return inventories.get(location);
    }

    public static void removeInventory(Location location) {
        inventories.remove(location);
    }
}