package org.dpdns.pisekpiskovec.storagetower.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.block.ModBlocks;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StorageTower.MOD_ID);

    public static final RegistryObject<BlockEntityType<StorageCrateBlockEntity>> STORAGE_CRATE =
            BLOCK_ENTITIES.register("storage_crate", () ->
                    BlockEntityType.Builder.of(StorageCrateBlockEntity::new,
                            ModBlocks.STORAGE_CRATE.get()).build(null));

    public static final RegistryObject<BlockEntityType<StorageControllerBlockEntity>> STORAGE_CONTROLLER =
            BLOCK_ENTITIES.register("storage_controller", () ->
                    BlockEntityType.Builder.of(StorageControllerBlockEntity::new,
                            ModBlocks.STORAGE_CONTROLLER.get()).build(null));

    public static final RegistryObject<BlockEntityType<StorageInterfaceBlockEntity>> STORAGE_INTERFACE =
            BLOCK_ENTITIES.register("storage_interface", () ->
                    BlockEntityType.Builder.of(StorageInterfaceBlockEntity::new,
                            ModBlocks.STORAGE_INTERFACE.get()).build(null));

    public static final RegistryObject<BlockEntityType<StorageCraftingControllerBlockEntity>> STORAGE_CONTROLLER_CRAFTING =
            BLOCK_ENTITIES.register("storage_controller_crafting", () ->
                    BlockEntityType.Builder.of(StorageCraftingControllerBlockEntity::new,
                            ModBlocks.STORAGE_CONTROLLER_CRAFTING.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
