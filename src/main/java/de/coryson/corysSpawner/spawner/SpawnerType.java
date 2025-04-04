package de.coryson.corysSpawner.spawner;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import java.util.List;

public enum SpawnerType {
    PIG(EntityType.PIG, List.of(
            new Drop(Material.PORKCHOP, 3)
    )),
    COW(EntityType.COW, List.of(
            new Drop(Material.BEEF, 3),
            new Drop(Material.LEATHER, 1)
    )),
    SPIDER(EntityType.SPIDER, List.of(
            new Drop(Material.STRING, 3),
            new Drop(Material.SPIDER_EYE, 1)
    )),
    SKELETON(EntityType.SKELETON, List.of(
            new Drop(Material.BONE, 3),
            new Drop(Material.ARROW, 1)
    )),
    CREEPER(EntityType.CREEPER, List.of(
            new Drop(Material.GUNPOWDER, 3)
    )),
    ZOMBIFIED_PIGLIN(EntityType.ZOMBIFIED_PIGLIN, List.of(
            new Drop(Material.GOLD_NUGGET, 3)
    )),
    BLAZE(EntityType.BLAZE, List.of(
            new Drop(Material.BLAZE_ROD, 3)
    )),
    IRON_GOLEM(EntityType.IRON_GOLEM, List.of(
            new Drop(Material.IRON_INGOT, 3),
            new Drop(Material.POPPY, 1)
    ));

    public final EntityType entityType;
    public final List<Drop> loot;

    SpawnerType(EntityType entityType, List<Drop> loot) {
        this.entityType = entityType;
        this.loot = loot;
    }

    public static SpawnerType fromString(String name) {
        try {
            return valueOf(name.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
    public EntityType getEntityType() {
        return entityType;
    }
}
