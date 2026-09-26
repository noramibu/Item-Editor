package me.noramibu.itemeditor.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ItemEditorTypes {

    @SuppressWarnings("unchecked")
    public static final EntityType<ArmorStand> ARMOR_STAND = (EntityType<ArmorStand>) entity("armor_stand");

    public static final EntityType<?> AXOLOTL = entity("axolotl");
    public static final EntityType<?> COD = entity("cod");
    public static final EntityType<?> GLOW_ITEM_FRAME = entity("glow_item_frame");
    public static final EntityType<?> ITEM_FRAME = entity("item_frame");
    public static final EntityType<?> PUFFERFISH = entity("pufferfish");
    public static final EntityType<?> SALMON = entity("salmon");
    public static final EntityType<?> TADPOLE = entity("tadpole");
    public static final EntityType<?> TROPICAL_FISH = entity("tropical_fish");
    public static final EntityType<?> VILLAGER = entity("villager");
    public static final EntityType<?> ZOMBIE_VILLAGER = entity("zombie_villager");
    public static final EntityType<?> WANDERING_TRADER = entity("wandering_trader");

    public static final BlockEntityType<?> COMMAND_BLOCK = blockEntity("command_block");
    public static final BlockEntityType<?> HANGING_SIGN = blockEntity("hanging_sign");
    public static final BlockEntityType<?> MOB_SPAWNER = blockEntity("mob_spawner");
    public static final BlockEntityType<?> SIGN = blockEntity("sign");

    public static final Item WHITE_BANNER =
            BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("white_banner"));

    private ItemEditorTypes() {}

    private static BlockEntityType<?> blockEntity(String path) {
        return BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(path));
    }

    private static EntityType<?> entity(String path) {
        return BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace(path));
    }
}
