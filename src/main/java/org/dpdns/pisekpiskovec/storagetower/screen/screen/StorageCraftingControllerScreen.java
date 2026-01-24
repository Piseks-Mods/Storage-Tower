package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

import java.util.List;

public class StorageCraftingControllerScreen extends AbstractContainerScreen<StorageCraftingControllerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(StorageTower.MOD_ID, "textures/gui/storage_crafting_controller.png");

    private final StorageCraftingControllerBlockEntity blockEntity;

    public StorageCraftingControllerScreen(StorageCraftingControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageHeight = 166;
        this.imageWidth = 176;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        // Simple gray background
        pGuiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF404040);

        // Crafting grid area bg
        pGuiGraphics.fill(leftPos + 29, topPos + 16, leftPos + 83, topPos + 70, 0xFF303030);

        // Crafting result area bg
        pGuiGraphics.fill(leftPos + 123, topPos + 34, leftPos + 141, topPos + 52, 0xFF303030);

        // Storage display area background
        pGuiGraphics.fill(leftPos + 8, topPos + 18, leftPos + 26, topPos + 78, 0xFF202020);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        //renderBackground(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderBg(pGuiGraphics, pPartialTick, pMouseX, pMouseY);
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

            for (int i = 0; i < Math.min(items.size(), 3); i++) /* Only 3 slots */ {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    int slotY = startY + i * 18;

                    pGuiGraphics.renderItem(stack, startX, slotY);
                    pGuiGraphics.renderItemDecorations(this.font, stack, startX, slotY);
                }
            }
        }
    }
}
