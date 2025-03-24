package de.coryson.corysLootSpawner;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.block.CreatureSpawner;

public class LootSpawnerCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return false;
        if (args.length != 1) {
            player.sendMessage("§cBenutzung: /givespawner <mob>");
            return true;
        }

        SpawnerType type = SpawnerType.fromString(args[0]);
        if (type == null) {
            player.sendMessage("§cUnbekannter Mob-Typ!");
            return true;
        }

        ItemStack spawner = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();
        CreatureSpawner state = (CreatureSpawner) meta.getBlockState();
        state.setSpawnedType(type.entityType);
        meta.setBlockState(state);

        meta.getPersistentDataContainer().set(LootSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING, type.name());
        meta.setDisplayName("§6" + type.name().replace("_", " ") + " Loot-Spawner");
        spawner.setItemMeta(meta);

        player.getInventory().addItem(spawner);
        player.sendMessage("§aSpawner erhalten: §e" + type.name());
        return true;
    }
}
