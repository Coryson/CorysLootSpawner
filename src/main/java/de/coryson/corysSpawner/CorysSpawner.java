package de.coryson.corysSpawner;

import de.coryson.corysSpawner.spawner.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CorysSpawner extends JavaPlugin {

    private static CorysSpawner instance;
    public static final NamespacedKey SPAWNER_TYPE_KEY = new NamespacedKey("lootspawner", "spawner_type");

    private final Set<Spawner> spawners = new HashSet<>();
    private Datastorage storage;

    public static CorysSpawner getInstance() {
        return instance;
    }

    public Datastorage getStorage() {
        return storage;
    }

    @Override
    public void onEnable() {
        instance = this;
        if (!getDataFolder().exists()) getDataFolder().mkdirs();

        try {
            storage = new Datastorage(getDataFolder());
        } catch (SQLException e) {
            getLogger().severe("Error initializing the database!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        Objects.requireNonNull(getCommand("spawner")).setExecutor(new Command());
        Bukkit.getPluginManager().registerEvents(new SpawnerListener(), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                spawners.forEach(Spawner::tick);
            }
        }.runTaskTimer(this, 20L, 20L);

        loadAllSpawners();
    }

    @Override
    public void onDisable() {
        spawners.forEach(block -> {
            Inventory inv = Spawner.getInventory(block.getLocation());
            if (inv != null) storage.saveSpawner(block.getLocation(), block.getType(), inv);
            storage.saveExp(block.getLocation(), block.collectExp());
        });
        getLogger().info("All spawners saved on shutdown.");
    }

    public void registerSpawner(Spawner block) {
        spawners.add(block);
    }

    public void unregisterSpawner(Spawner block) {
        spawners.remove(block);
    }

    public Spawner getSpawnerByLocation(Location loc) {
        return spawners.stream()
                .filter(s -> s.getLocation().equals(loc))
                .findFirst()
                .orElse(null);
    }

    public void loadAllSpawners() {
        Map<Location, SpawnerType> saved = storage.getAllSpawnerEntries();
        saved.forEach((loc, type) -> registerSpawner(new Spawner(loc, type)));
        getLogger().info(saved.size() + " Loot-Spawners loaded.");
    }
}