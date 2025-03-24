package de.coryson.corysLootSpawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LootSpawner extends JavaPlugin {

    private static LootSpawner instance;
    public static final NamespacedKey SPAWNER_TYPE_KEY = new NamespacedKey("lootspawner", "spawner_type");

    private final Set<LootSpawnerBlock> spawners = new HashSet<>();
    private LootSpawnerDataStorage storage;

    public static LootSpawner getInstance() {
        return instance;
    }

    public LootSpawnerDataStorage getStorage() {
        return storage;
    }

    @Override
    public void onEnable() {
        instance = this;

        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        try {
            this.storage = new LootSpawnerDataStorage(getDataFolder());
        } catch (SQLException e) {
            getLogger().severe("There was an error while initializing the database!");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getCommand("givespawner").setExecutor(new LootSpawnerCommand());
        Bukkit.getPluginManager().registerEvents(new LootSpawnerListener(), this);

        new BukkitRunnable() {
            @Override
            public void run() {
                for (LootSpawnerBlock block : spawners) {
                    block.tick();
                }
            }
        }.runTaskTimer(this, 20, 20);

        loadAllSpawners();
    }

    public void registerSpawner(LootSpawnerBlock block) {
        spawners.add(block);
    }

    public void unregisterSpawner(LootSpawnerBlock block) {
        spawners.remove(block);
    }

    public LootSpawnerBlock getSpawnerByLocation(Location loc) {
        return spawners.stream()
                .filter(s -> s.getLocation().equals(loc))
                .findFirst()
                .orElse(null);
    }

    public void loadAllSpawners() {
        Map<Location, SpawnerType> saved = storage.getAllSpawnerEntries();

        for (Map.Entry<Location, SpawnerType> entry : saved.entrySet()) {
            LootSpawnerBlock block = new LootSpawnerBlock(entry.getKey(), entry.getValue());
            Block b = entry.getKey().getBlock();
            if (b.getState() instanceof CreatureSpawner spawner) {
                spawner.setSpawnedType(entry.getValue().getEntityType());
                spawner.setSpawnCount(0);
                spawner.setMinSpawnDelay(999999);
                spawner.setMaxSpawnDelay(999999);
                spawner.setDelay(999999);
                spawner.setRequiredPlayerRange(0);
                spawner.update();
            }
            registerSpawner(block);
        }

        getLogger().info(saved.size() + " Loot-Spawners loaded.");
    }
}