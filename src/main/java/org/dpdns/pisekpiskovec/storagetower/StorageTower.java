package org.dpdns.pisekpiskovec.storagetower;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.dpdns.pisekpiskovec.storagetower.block.ModBlocks;
import org.dpdns.pisekpiskovec.storagetower.block.entity.ModBlockEntities;
import org.dpdns.pisekpiskovec.storagetower.config.ModConfig;
import org.dpdns.pisekpiskovec.storagetower.item.ModItems;
import org.dpdns.pisekpiskovec.storagetower.network.ModNetworking;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;
import org.dpdns.pisekpiskovec.storagetower.screen.screen.StorageControllerScreen;
import org.dpdns.pisekpiskovec.storagetower.screen.screen.StorageCraftingControllerScreen;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(StorageTower.MOD_ID)
public class StorageTower {

    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "storagetower";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public StorageTower(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModMenuTypes.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);

        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetworking::register);
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.STORAGE_CRATE);
            event.accept(ModBlocks.STORAGE_INTERFACE);
            event.accept(ModBlocks.STORAGE_CONTROLLER);
            event.accept(ModBlocks.STORAGE_CONTROLLER_CRAFTING);
        } else if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.STORAGE_UPGRADE);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            MenuScreens.register(ModMenuTypes.STORAGE_CONTROLLER.get(), StorageControllerScreen::new);
            MenuScreens.register(ModMenuTypes.STORAGE_CONTROLLER_CRAFTING.get(), StorageCraftingControllerScreen::new);
        }
    }
}
