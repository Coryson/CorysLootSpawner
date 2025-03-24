package de.coryson.corysLootSpawner;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.*;
import java.sql.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class LootSpawnerDataStorage {

    private final Connection connection;

    public LootSpawnerDataStorage(File pluginFolder) throws SQLException {
        File dbFile = new File(pluginFolder, "lootspawners.db");
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        this.connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS spawners (
                    world TEXT,
                    x INTEGER,
                    y INTEGER,
                    z INTEGER,
                    type TEXT,
                    inventory TEXT,
                    PRIMARY KEY(world, x, y, z)
                )
            """);
        }
    }

    public void saveSpawner(Location loc, SpawnerType type, Inventory inv) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            INSERT OR REPLACE INTO spawners (world, x, y, z, type, inventory)
            VALUES (?, ?, ?, ?, ?, ?)
        """)) {
            stmt.setString(1, loc.getWorld().getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            stmt.setString(5, type.name());
            stmt.setString(6, inventoryToBase64(inv));
            stmt.executeUpdate();
        } catch (SQLException | IOException e) {
            Bukkit.getLogger().severe("There was an error while saving: " + e.getMessage());
        }
    }

    public Inventory loadInventory(Location loc) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT inventory FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?
        """)) {
            stmt.setString(1, loc.getWorld().getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return inventoryFromBase64(rs.getString("inventory"));
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("There was an error while loading: " + e.getMessage());
        }
        return null;
    }

    public void deleteSpawner(Location loc) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            DELETE FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?
        """)) {
            stmt.setString(1, loc.getWorld().getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[There was an error while deleting: " + e.getMessage());
        }
    }

    public Map<Location, SpawnerType> getAllSpawnerEntries() {
        Map<Location, SpawnerType> map = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM spawners")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String world = rs.getString("world");
                int x = rs.getInt("x");
                int y = rs.getInt("y");
                int z = rs.getInt("z");
                String typeName = rs.getString("type");

                World bukkitWorld = Bukkit.getWorld(world);
                if (bukkitWorld == null) {
                    Bukkit.getLogger().warning("World not found: " + world + " – Loot-Spawners skipped.");
                    continue;
                }

                Location loc = new Location(bukkitWorld, x, y, z);
                SpawnerType type = SpawnerType.valueOf(typeName);
                map.put(loc, type);
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("There was an error while loading the LootSpawners: " + e.getMessage());
        }
        return map;
    }

    private String inventoryToBase64(Inventory inventory) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(inventory.getSize());
            for (ItemStack item : inventory.getContents()) {
                dataOutput.writeObject(item);
            }
        }
        return Base64.getEncoder().encodeToString(outputStream.toByteArray());
    }

    private Inventory inventoryFromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);

        int size = dataInput.readInt();
        Inventory inventory = Bukkit.createInventory(null, size);
        for (int i = 0; i < size; i++) {
            ItemStack item = (ItemStack) dataInput.readObject();
            inventory.setItem(i, item);
        }

        return inventory;
    }
}