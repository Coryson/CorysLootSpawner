package de.coryson.corysSpawner.spawner;

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
import java.util.Objects;

public class Datastorage {

    private final Connection connection;

    public Datastorage(File pluginFolder) throws SQLException {
        String url = "jdbc:sqlite:" + new File(pluginFolder, "spawners.db").getAbsolutePath();
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
                    exp INTEGER DEFAULT 0,
                    PRIMARY KEY(world, x, y, z)
                )
            """);
        }
    }

    public void saveSpawner(Location loc, SpawnerType type, Inventory inv) {
        if (inv == null) {
            Bukkit.getLogger().warning("[CorysLootSpawner] Tried to save null inventory at " + loc);
            return;
        }
        try (PreparedStatement stmt = connection.prepareStatement("""
            INSERT OR REPLACE INTO spawners (world, x, y, z, type, inventory, exp)
            VALUES (?, ?, ?, ?, ?, ?, COALESCE((SELECT exp FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?), 0))
        """)) {
            stmt.setString(1, Objects.requireNonNull(loc.getWorld()).getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            stmt.setString(5, type.name());
            stmt.setString(6, inventoryToBase64(inv));
            stmt.setString(7, loc.getWorld().getName());
            stmt.setInt(8, loc.getBlockX());
            stmt.setInt(9, loc.getBlockY());
            stmt.setInt(10, loc.getBlockZ());
            stmt.executeUpdate();
        } catch (SQLException | IOException e) {
            Bukkit.getLogger().severe("[CorysLootSpawner] Error while saving: " + e.getMessage());
        }
    }

    public void saveExp(Location loc, int exp) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            UPDATE spawners SET exp = ? WHERE world = ? AND x = ? AND y = ? AND z = ?
        """)) {
            stmt.setInt(1, exp);
            stmt.setString(2, Objects.requireNonNull(loc.getWorld()).getName());
            stmt.setInt(3, loc.getBlockX());
            stmt.setInt(4, loc.getBlockY());
            stmt.setInt(5, loc.getBlockZ());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CorysLootSpawner] Could not save exp: " + e.getMessage());
        }
    }

    public int loadExp(Location loc) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT exp FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?
        """)) {
            stmt.setString(1, Objects.requireNonNull(loc.getWorld()).getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("exp");
            }
        } catch (SQLException e) {
            Bukkit.getLogger().severe("[CorysLootSpawner] Could not load exp: " + e.getMessage());
        }
        return 0;
    }

    public Inventory loadInventory(Location loc) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            SELECT inventory FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?
        """)) {
            stmt.setString(1, Objects.requireNonNull(loc.getWorld()).getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return inventoryFromBase64(rs.getString("inventory"));
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CorysLootSpawner] Error while loading: " + e.getMessage());
        }
        return null;
    }

    public void deleteSpawner(Location loc) {
        try (PreparedStatement stmt = connection.prepareStatement("""
            DELETE FROM spawners WHERE world = ? AND x = ? AND y = ? AND z = ?
        """)) {
            stmt.setString(1, Objects.requireNonNull(loc.getWorld()).getName());
            stmt.setInt(2, loc.getBlockX());
            stmt.setInt(3, loc.getBlockY());
            stmt.setInt(4, loc.getBlockZ());
            stmt.executeUpdate();
        } catch (SQLException e) {
            Bukkit.getLogger().warning("[CorysLootSpawner] Error while deleting: " + e.getMessage());
        }
    }

    public Map<Location, SpawnerType> getAllSpawnerEntries() {
        Map<Location, SpawnerType> map = new HashMap<>();
        try (PreparedStatement stmt = connection.prepareStatement("SELECT * FROM spawners")) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                World world = Bukkit.getWorld(rs.getString("world"));
                if (world == null) {
                    Bukkit.getLogger().warning("[CorysLootSpawner] World not found: " + rs.getString("world"));
                    continue;
                }
                Location loc = new Location(world, rs.getInt("x"), rs.getInt("y"), rs.getInt("z"));
                map.put(loc, SpawnerType.valueOf(rs.getString("type")));
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[CorysLootSpawner] Error while loading spawners: " + e.getMessage());
        }
        return map;
    }

    private String inventoryToBase64(Inventory inventory) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream)) {
            dataOutput.writeInt(inventory.getSize());
            for (ItemStack item : inventory.getContents()) {
                dataOutput.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        }
    }

    private Inventory inventoryFromBase64(String data) throws IOException, ClassNotFoundException {
        byte[] bytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream)) {
            Inventory inventory = Bukkit.createInventory(null, dataInput.readInt());
            for (int i = 0; i < inventory.getSize(); i++) {
                inventory.setItem(i, (ItemStack) dataInput.readObject());
            }
            return inventory;
        }
    }
}