package de.coryson.corysSpawner.spawner;

import de.coryson.corysSpawner.CorysSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.persistence.PersistentDataType;

public class Command implements CommandExecutor {

    private String getLocalizedName(SpawnerType type) {
        return switch (type) {
            case COW -> "Kuh";
            case SPIDER -> "Spinnen";
            case PIG -> "Schweine";
            case SKELETON -> "Skelett";
            case CREEPER -> "Creeper";
            case ZOMBIFIED_PIGLIN -> "Zombifizierter Piglin";
            case BLAZE -> "Lohen";
            case IRON_GOLEM -> "Eisengolem";
            default -> type.name().toLowerCase();
        };
    }

    private ItemStack createSpawner(SpawnerType type) {
        ItemStack spawner = new ItemStack(Material.SPAWNER);
        BlockStateMeta meta = (BlockStateMeta) spawner.getItemMeta();

        meta.setDisplayName("§x§5§8§0§3§4§2" + getLocalizedName(type) + " Spawner");
        meta.getPersistentDataContainer().set(CorysSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING, type.name());
        meta.addEnchant(Enchantment.MULTISHOT, 1, true);
        meta.addItemFlags(
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ADDITIONAL_TOOLTIP
        );

        spawner.setItemMeta(meta);
        return spawner;
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {

        // /spawner (ohne Argumente)
        if (args.length == 0) {
            sender.sendMessage("§eVerwendung: §7/spawner give <Spieler> <Typ>");
            return true;
        }

        // /spawner give <Spieler> <Typ>
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) {

            if (!sender.hasPermission("lootspawner.give")) {
                sender.sendMessage("§cDazu hast du keine Berechtigung.");
                return true;
            }

            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                sender.sendMessage("§cSpieler nicht gefunden.");
                return true;
            }

            SpawnerType type = SpawnerType.fromString(args[2]);
            if (type == null) {
                sender.sendMessage("§cUnbekannter Mob-Typ!");
                return true;
            }

            ItemStack spawner = createSpawner(type);
            target.getInventory().addItem(spawner);
            target.sendMessage("§aDu hast einen §e" + getLocalizedName(type) + " Loot-Spawner §aerhalten.");
            sender.sendMessage("§aSpawner erfolgreich an §e" + target.getName() + " §agegeben.");
            return true;
        }

        // Falsche Benutzung
        sender.sendMessage("§cBenutzung: /spawner give <Spieler> <Typ>");
        return true;
    }
}
