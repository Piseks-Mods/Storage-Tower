package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;
import org.jetbrains.annotations.NotNull;

public class StorageControllerScreen extends ModularUIGuiContainer {
    private final StorageControllerBlockEntity blockEntity;

    public StorageControllerScreen(StorageControllerMenu menu, Inventory playerInv, Component title) {
        super(modularUI, windowId);
        this.blockEntity = menu.getBlockEntity();
    }

    public ModularUI createUI(Inventory playerInv) {
        ModularUI.Builder builder = new ModularUI.Builder(176, 166)
                .background(new ColorRectWidget(0, 0, 176, 166, 0xFF404040));

        // Title
        builder.widget(new LabelWidget(8, 6, "Storage Controller")
                .setTextColor(0x404040));

        // Storage display area
        builder.widget(createStorageDisplay());

        // Player inventory
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                builder.slot(new SlotWidget(playerInv, index, 8 + col * 18, 84 + row * 18, true, true));
            }
        }

        // Hotbar
        for (int col = 0; col < 9; col++) {
            builder.slot(new SlotWidget(playerInv, col, 8 + col * 18, 142, true, true));
        }

        return builder.build(blockEntity, playerInv.player);
    }

    private Widget createStorageDisplay() {
        WidgetGroup group = new WidgetGroup(8, 18, 160, 60);

        group.addWidget(new DraggableScrollableWidgetGroup(0, 0, 160, 60).setBackground(new ColorRectWidget(0, 0, 160, 60, 0xFF202020)));

        // This will be populated dynamically with items from the network
        group.addWidget(new ButtonWidget(0, 0, 160, 60, cd -> {
            // Refresh storage display
            updateStorageDisplay(group);
        }).setButtonTexture(null));

        return group;
    }

    private void updateStorageDisplay(WidgetGroup group) {
        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            List<ItemStack> items = network.getAllItems();

            // Clear existing display
            group.clearAllWidgets();

            // Display items in grid
            int x = 0, y = 0;
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    ItemStackWidget itemWidget = new ItemStackWidget(x * 18, y * 18, stack);
                    group.addWidget(itemWidget);

                    x++;
                    if (x >= 8) {
                        x = 0;
                        y++;
                    }
                }
            }
        }
    }

    private static class ItemStackWidget extends Widget {
        private final ItemStack stack;

        public ItemStackWidget(int x, int y, ItemStack stack){
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
