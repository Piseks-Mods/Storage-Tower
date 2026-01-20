package org.dpdns.pisekpiskovec.storagetower.client.ui;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.jetbrains.annotations.NotNull;

public class StorageControllerUI {
    private final StorageControllerBlockEntity blockEntity;
    private final Player player;

    public StorageControllerUI(StorageControllerBlockEntity blockEntity, Player player) {
        this.blockEntity = blockEntity;
        this.player = player;
    }

    public ModularUI createUI() {
        ModularUI.Builder builder = new ModularUI.Builder(new ColorRectWidget(0, 0, 176, 166, 0xFF404040), 176, 166);

        // Title
        builder.widget(new LabelWidget(8, 6, "Storage Controller").setTextColor(0x404040));

        // Storage display area - scorllable grid
        WidgetGroup storageGroup = new WidgetGroup(8, 18, 160, 60);
        DraggableScrollableWidgetGroup scrollable = new DraggableScrollableWidgetGroup(0, 0, 160, 60);
        scrollable.setBackground(new ColorRectWidget(0, 0, 160, 60, 0xFF202020));

        // Add items from tower network
        TowerNetwork network = blockEntity.getTower();
        if(network != null) {
            List<ItemStack> items = network.getAllItems();
            int x = 0, y = 0;

            for(ItemStack stack : items) {
                if(!stack.isEmpty()) {
                    ItemStackWidget itemWidget = new ItemStackWidget(x * 18 + 1, y * 18 + 1, stack);
                    scrollable.addWidget(itemWidget);

                    x++;
                    if(x >= 8) {
                        x = 0;
                        y++;
                    }
                }
            }
        }

        storageGroup.addWidget(scrollable);
        builder.widget(storageGroup);

        // Player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int index = col + row * 9 + 9;
                builder.widget(new SlotWidget(player.getInventory(), index, 8 + col * 18, 84 + row * 18, true, true));
            }
        }

        // Hotbar slots
        for (int col = 0; col < 9; col++) {
            builder.widget(new SlotWidget(player.getInventory(), col, 8 + col * 18, 142, true, true));
        }

        return builder.build(blockEntity, player);
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
