package org.dpdns.pisekpiskovec.storagetower.block.entity;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, StorageTower.MOD_ID);

    public static void register(IEventBus eventBus) { BLOCK_ENTITIES.register(eventBus); }
}
