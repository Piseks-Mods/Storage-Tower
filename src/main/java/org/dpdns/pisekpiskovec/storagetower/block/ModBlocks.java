package org.dpdns.pisekpiskovec.storagetower.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, StorageTower.MOD_ID);

    // Block registrations goes here
    public static final RegistryObject<Block> STORAGE_CRATE = registerBlock("storage_crate",
            () -> new StorageCrateBlock(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> STORAGE_CONTROLLER = registerBlock("storage_controller",
            () -> new StorageControllerBlock(BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> STORAGE_INTERFACE = registerBlock("storage_interface",
            () -> new StorageInterfaceBlock(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));

    public static final RegistryObject<Block> STORAGE_CONTROLLER_CRAFTING = registerBlock("storage_controller_crafting",
            () -> new StorageCraftingControllerBlock(BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
