package dev.example.industrialdrill;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;

public final class IndustrialDrillMod implements ModInitializer {
    public static final String MOD_ID = "industrial_drill";

    public static final EntityType<DrillEntity> DRILL = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "drill"),
        EntityType.Builder.<DrillEntity>of(DrillEntity::new, MobCategory.MISC)
            .sized(2.6F, 2.0F)
            .clientTrackingRange(10)
            .updateInterval(1)
            .build("drill")
    );

    public static final Item DRILL_KIT = Registry.register(
        BuiltInRegistries.ITEM,
        ResourceLocation.fromNamespaceAndPath(MOD_ID, "drill_kit"),
        new Item(new Item.Properties().stacksTo(1))
    );

    @Override
    public void onInitialize() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
            .register(entries -> entries.accept(DRILL_KIT));
        ServerTickEvents.END_WORLD_TICK.register(DrillEntity::tickWorld);
    }
}
