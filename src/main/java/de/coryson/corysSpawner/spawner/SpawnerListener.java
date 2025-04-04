package de.coryson.corysSpawner.spawner;

import de.coryson.corysSpawner.CorysSpawner;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SpawnerListener implements org.bukkit.event.Listener {

    private final Map<Player, Location> openSpawnerContext = new HashMap<>();
    private final Map<Player, Inventory> previousInventories = new HashMap<>();

    public static class SpawnerInventoryHolder implements InventoryHolder {
        private final Location location;

        public SpawnerInventoryHolder(Location location) {
            this.location = Utils.toBlockLocation(location);
        }

        @Override
        public Inventory getInventory() {
            return Spawner.getInventory(location);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        ItemMeta itemMeta = event.getItemInHand().getItemMeta();
        if (itemMeta == null) return;

        String typeName = itemMeta.getPersistentDataContainer().get(CorysSpawner.SPAWNER_TYPE_KEY, PersistentDataType.STRING);
        if (typeName == null) return;

        try {
            SpawnerType type = SpawnerType.valueOf(typeName);
            Spawner blockObj = new Spawner(Utils.toBlockLocation(block.getLocation()), type);
            CorysSpawner.getInstance().registerSpawner(blockObj);
        } catch (IllegalArgumentException ignored) {
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.SPAWNER) return;

        Player player = event.getPlayer();
        if (player.getGameMode() == org.bukkit.GameMode.CREATIVE) {
            event.setCancelled(true);
            player.sendMessage("§cDu kannst den Spawner im Kreativmodus nicht abbauen");
            return;
        }

        Location loc = Utils.toBlockLocation(block.getLocation());
        Spawner spawner = CorysSpawner.getInstance().getSpawnerByLocation(loc);
        if (spawner != null) {
            int exp = spawner.collectExp();
            if (exp > 0) {
                Objects.requireNonNull(loc.getWorld()).spawn(loc.clone().add(0.5, 0.5, 0.5), ExperienceOrb.class, orb -> orb.setExperience(exp));
            }

            Inventory inv = spawner.getInventory();
            if (inv != null) {
                for (int i = 0; i < 18; i++) {
                    ItemStack item = inv.getItem(i);
                    if (item != null && item.getType() != Material.AIR) {
                        Objects.requireNonNull(loc.getWorld()).dropItemNaturally(loc.clone().add(0.5, 1.0, 0.5), item.clone());
                    }
                }
                CorysSpawner.getInstance().getStorage().saveSpawner(loc, spawner.getType(), spawner.getFilteredInventory());
            }

            CorysSpawner.getInstance().getStorage().deleteSpawner(loc);
            spawner.remove();
            CorysSpawner.getInstance().unregisterSpawner(spawner);
        }
    }

    @EventHandler
    public void onSpawnerSpawn(SpawnerSpawnEvent event) {
        Location loc = event.getSpawner().getLocation();
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        if (CorysSpawner.getInstance().getSpawnerByLocation(blockLoc) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK && event.hasBlock()) {
            Player player = event.getPlayer();
            if (player.isSneaking()) return;

            Block block = event.getClickedBlock();
            if (block != null && block.getType() == Material.SPAWNER) {
                Location loc = Utils.toBlockLocation(block.getLocation());
                Spawner spawner = CorysSpawner.getInstance().getSpawnerByLocation(loc);
                if (spawner != null) {
                    event.setCancelled(true);

                    Bukkit.getScheduler().runTask(CorysSpawner.getInstance(), () -> {
                        Inventory gui = Bukkit.createInventory(null, 27, "Spawner");

                        ItemStack openInv = new ItemStack(Material.CHEST);
                        ItemMeta invMeta = openInv.getItemMeta();
                        assert invMeta != null;
                        invMeta.setDisplayName("§x§F§F§6§D§2§DInventar öffnen");
                        invMeta.setLore(Collections.singletonList("§x§F§F§6§D§2§D" + spawner.getItemCount() + " §fItems im Inventar"));
                        openInv.setItemMeta(invMeta);
                        gui.setItem(11, openInv);

                        ItemStack collectExp = new ItemStack(Material.EXPERIENCE_BOTTLE);
                        ItemMeta expMeta = collectExp.getItemMeta();
                        assert expMeta != null;
                        expMeta.setDisplayName("§x§9§A§C§D§3§2XP einsammeln");
                        expMeta.setLore(Collections.singletonList("§x§9§A§C§D§3§2" + spawner.getStoredExp() + " §fXP-Punkte"));
                        collectExp.setItemMeta(expMeta);
                        gui.setItem(15, collectExp);

                        ItemStack placeholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                        ItemMeta phMeta = placeholder.getItemMeta();
                        assert phMeta != null;
                        phMeta.setDisplayName("§r");
                        placeholder.setItemMeta(phMeta);
                        for (int i = 0; i < 27; i++) {
                            if (gui.getItem(i) == null) {
                                gui.setItem(i, placeholder);
                            }
                        }

                        previousInventories.put(player, gui);
                        player.playSound(player.getLocation(), Sound.BLOCK_IRON_TRAPDOOR_OPEN, 0.6f, 1.8f);
                        player.openInventory(gui);
                        openSpawnerContext.put(player, loc);
                    });
                }
            }
        }
    }

    @EventHandler
    public void onExplosion(EntityExplodeEvent event) {
        event.blockList().removeIf(block -> {
            if (block.getType() == Material.SPAWNER) {
                Location loc = block.getLocation();
                Spawner spawner = CorysSpawner.getInstance().getSpawnerByLocation(loc);
                return spawner != null;
            }
            return false;
        });
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        String title = event.getView().getTitle();
        int rawSlot = event.getRawSlot();

        if (title.equals("Spawner - Inventar")) {
            if (rawSlot >= 27) {
                event.setCancelled(true);
                return;
            }

            if (rawSlot >= 18) {
                if (clicked == null || clicked.getType() != Material.BARRIER || !"§cZurück".equals(clicked.getItemMeta().getDisplayName())) {
                    event.setCancelled(true);
                    return;
                }
            }

            if (rawSlot == 22 && clicked.getType() == Material.BARRIER && "§cZurück".equals(clicked.getItemMeta().getDisplayName())) {
                Inventory previous = previousInventories.get(player);
                if (previous != null) {
                    player.openInventory(previous);
                    player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                    event.setCancelled(true);
                }
            }
        }

        if (title.equals("Spawner")) {
            event.setCancelled(true);
            if (clicked == null || !clicked.hasItemMeta()) return;

            String name = Objects.requireNonNull(clicked.getItemMeta()).getDisplayName();
            Location loc = Utils.toBlockLocation(openSpawnerContext.get(player));
            if (loc == null) return;

            Spawner spawner = CorysSpawner.getInstance().getSpawnerByLocation(loc);
            if (spawner == null) return;

            if (name.equals("§x§F§F§6§D§2§DInventar öffnen")) {
                Inventory inv = spawner.getInventory();
                if (inv != null) {
                    ItemStack back = new ItemStack(Material.BARRIER);
                    ItemMeta meta = back.getItemMeta();
                    assert meta != null;
                    meta.setDisplayName("§cZurück");
                    meta.setLore(Collections.singletonList("§8→ §fKlicke, um ins vorherige Menü zurückzukehren"));
                    back.setItemMeta(meta);
                    inv.setItem(22, back);

                    ItemStack ph = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
                    ItemMeta phMeta = ph.getItemMeta();
                    assert phMeta != null;
                    phMeta.setDisplayName("§r");
                    ph.setItemMeta(phMeta);
                    for (int i = 18; i < 27; i++) {
                        if (inv.getItem(i) == null) {
                            inv.setItem(i, ph);
                        }
                    }

                    player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
                    player.openInventory(inv);
                }
            } else if (name.equals("§x§9§A§C§D§3§2XP einsammeln")) {
                int exp = spawner.collectExp();
                if (exp > 0) {
                    Location dropLocation = player.getLocation();
                    ExperienceOrb orb = player.getWorld().spawn(dropLocation, ExperienceOrb.class);
                    orb.setExperience(exp);

                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.4f, 1.5f);
                    player.sendMessage("§fDu hast die XP-Punkte erhalten");
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.4f, 1.5f);
                    player.sendMessage("§cIn diesem Spawner befinden sich keine XP-Punkte");
                }
                player.closeInventory();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        openSpawnerContext.remove(player);
        previousInventories.remove(player);
    }
}