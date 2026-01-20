package org.dpdns.pisekpiskovec.storagetower;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;
import org.dpdns.pisekpiskovec.storagetower.screen.screen.StorageControllerScreen;
import org.dpdns.pisekpiskovec.storagetower.screen.screen.StorageCraftingControllerScreen;

public class ClientEvents {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.STORAGE_CONTROLLER.get(), StorageControllerScreen::new);
            MenuScreens.register(ModMenuTypes.STORAGE_CONTROLLER_CRAFTING.get(), StorageCraftingControllerScreen::new);
        });
    }
}
