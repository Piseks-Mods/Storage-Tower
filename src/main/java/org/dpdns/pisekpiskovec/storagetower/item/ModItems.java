package org.dpdns.pisekpiskovec.storagetower.item;

import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, StorageTower.MOD_ID);

    // Item registrations goes here
    public static final RegistryObject<Item> STORAGE_UPGRADE = ITEMS.register("storage_upgrade", () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
