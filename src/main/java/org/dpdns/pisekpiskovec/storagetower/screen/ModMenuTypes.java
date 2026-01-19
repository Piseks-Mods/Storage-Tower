package org.dpdns.pisekpiskovec.storagetower.screen;

import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, StorageTower.MOD_ID);

    public static final RegistryObject<MenuType<StorageControllerMenu>> STORAGE_CONTROLLER = MENUS.register("storage_controller", () -> IForgeMenuType.create((windowId, inv, data) -> {
        return new StorageControllerMenu(windowId, inv, inv.player.level().getBlockEntity(data.readBlockPos()));
    }));

    public static final RegistryObject<MenuType<StorageCraftingControllerMenu>> STORAGE_CONTROLLER_CRAFTING = MENUS.register("storage_controller_crafting", () -> IForgeMenuType.create((windowId, inv, data) -> {
        return new StorageCraftingControllerMenu(windowId, inv, inv.player.level().getBlockEntity(data.readBlockPos()));
    }));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}
