package de.coryson.corysLootSpawner;

import org.bukkit.Material;

public class Drop {
    public final Material material;
    public final int amount;

    public Drop(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }
}
