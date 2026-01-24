package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;

import java.util.List;

public class StorageControllerScreen extends AbstractContainerScreen<StorageControllerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(StorageTower.MOD_ID, "textures/gui/storage_controller.png");

    private final StorageControllerBlockEntity blockEntity;

    public StorageControllerScreen(StorageControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageHeight = 166;
        this.imageWidth = 176;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        // Simple gray background
        pGuiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF404040);

        // Storage display area background
        pGuiGraphics.fill(leftPos + 8, topPos + 18, leftPos + 168, topPos + 78, 0xFF202020);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);

        // Render storage items
        renderStorageItems(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    private void renderStorageItems(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            List<ItemStack> items = network.getAllItems();

            int startX = leftPos + 9;
            int startY = topPos + 19;
            int slotsPerRow = 8;

            for (int i = 0; i < Math.min(items.size(), 24); i++) /* Limit to visible area */ {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    int slotX = startX + (i % slotsPerRow) * 18;
                    int slotY = startY + (i / slotsPerRow) * 18;

                    pGuiGraphics.renderItem(stack, slotX, slotY);
                    pGuiGraphics.renderItemDecorations(this.font, stack, slotX, slotY);
                }
            }
        }
    }
}
