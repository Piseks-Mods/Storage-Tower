package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;
import org.jetbrains.annotations.NotNull;

public class StorageCraftingControllerScreen extends ModularUIGuiContainer {
    private final StorageCraftingControllerBlockEntity blockEntity;

    public StorageCraftingControllerScreen(StorageCraftingControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
    }

    public ModularUI createUI(Inventory playerInv) {
        ModularUI.Builder builder = new ModularUI.Builder(176, 166)
                .background(new ColorRectWidget(0, 0, 176, 166, 0xFF404040));

        // Title
        builder.widget(new LabelWidget(8, 6, "Storage Controller")
                .setTextColor(0x404040));

        // Crafting grid (3x3)
        for (int row = 0; row <3; row++) {
            for(int col = 0; col <3; col++) {
                int index = col + row * 3 + 1; // +1 bcs slot 0 is result
                builder.slot(new SlotWidget(((StorageCraftingControllerMenu) menu).slots.get(index).container,
                        col + row * 3, 30 + col * 18, 17 + row * 18, true, true));
            }
        }

        // Crafting result slot
        builder.slot(new SlotWidget(((StorageCraftingControllerMenu) menu).slots.get(0).container,
                0, 124, 35, false, true));

        // Storage display area
        builder.widget(createStorageDisplay());

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                builder.slot(new SlotWidget(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18, true, true));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            builder.slot(new SlotWidget(playerInv, col, 8 + col * 18, 142, true, true));
        }

        return builder.build(blockEntity, playerInv.player);
    }

    private Widget createStorageDisplay() {
        WidgetGroup group = new WidgetGroup(8, 18, 20, 60);

        group.addWidget(new DraggableScrollableWidgetGroup(0,0,20,60)
                .setBackground(new ColorRectWidget(0,0,20,60,0xFF202020)));

        // Compact vertical display
        group.addWidget(new ButtonWidget(0,0,20,60, cd -> {
            updateStorageDisplay(group);
        }).setButtonTexture(null));

        return group;
    }

    private void updateStorageDisplay(WidgetGroup group){
        TowerNetwork network = blockEntity.getTower();
        if(network != null) {
            List<ItemStack> items = network.getAllItems();

            group.clearAllWidgets();

            int y = 0;
            for(ItemStack stack : items) {
                if (!stack.isEmpty() && y * 18 < 60) {
                    ItemStackWidget itemWidget = new ItemStackWidget(1, y * 18, stack);
                    group.addWidget(itemWidget);
                    y++;
                }
            }
        }
    }

    private static class ItemStackWidget extends Widget {
        private final ItemStack stack;

        public ItemStackWidget(int x, int y, ItemStack stack) {
            super(x, y, 18, 18);
            this.stack = stack;
        }

        @Override
        public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            graphics.renderItem(stack, getPosition().x, getPosition().y);
            graphics.renderItemDecorations(minecraft.font, stack, getPosition().x, getPosition().y);
        }
    }
}
