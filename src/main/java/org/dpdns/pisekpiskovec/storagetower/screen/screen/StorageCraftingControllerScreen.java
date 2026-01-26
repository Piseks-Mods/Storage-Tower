package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
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
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public StorageCraftingControllerScreen(StorageCraftingControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageHeight = 166;
        this.imageWidth = 176;
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = leftPos;
        int y = topPos;

        // Main background
        pGuiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFF8B8B8B);

        // Crafting grid area background
        pGuiGraphics.fill(x + 29, y + 16, x + 83, y + 70, 0xFF373737);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + 30 + col * 18;
                int slotY = y + 17 + row * 18;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            }
        }

        // Crafting result area background
        pGuiGraphics.fill(x + 123, y + 34, x + 141, y + 52, 0xFF373737);
        pGuiGraphics.fill(x + 124, y + 35, x + 140, y + 51, 0xFF8B8B8B);

        // Storage display area background
        pGuiGraphics.fill(x + 8, y + 16, x + 26, y + 70, 0xFF373737);
        for (int i = 0; i < 3; i++) {
            int slotX = x + 9;
            int slotY = y + 17 + i * 18;
            pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
        }

        // Render storage items
        renderStorageItems(pGuiGraphics);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.title, 8, 6, 0x404040, false);
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        // Custom tooltip for storage items
        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            List<ItemStack> items = network.getAllItems();

            int startX = leftPos + 9;
            int startY = topPos + 19;

            int startIndex = scrollOffset;
            int endIndex = Math.min(startIndex + 3, items.size());

            for (int i = startIndex; i < endIndex; i++) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    int displayIndex = i - startIndex;
                    int slotY = startY + displayIndex * 18;

                    if (pX >= startX && pX < startX + 16 && pY >= slotY && pY < slotY + 16) {
                        pGuiGraphics.renderTooltip(this.font, stack, pX, pY);
                        break;
                    }
                }
            }
        }
    }

    private void renderStorageItems(GuiGraphics pGuiGraphics) {
        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            List<ItemStack> items = network.getAllItems();

            maxScroll = Math.max(0, items.size() - 3); // Calculate max scroll

            int startX = leftPos + 9;
            int startY = topPos + 19;

            int startIndex = scrollOffset;
            int endIndex = Math.min(startIndex + 3, items.size());

            for (int i = startIndex; i < endIndex; i++) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    int displayIndex = i - startIndex;
                    int slotY = startY + displayIndex * 18;

                    pGuiGraphics.renderItem(stack, startX, slotY);
                    pGuiGraphics.renderItemDecorations(this.font, stack, startX, slotY);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        // Only scroll when mouse is over storage display area
        int startX = leftPos + 8;
        int startY = topPos + 18;
        int endX = startX + 18;
        int endY = startY + 60;

        if (pMouseX >= startX && pMouseX <= endX && pMouseY >= startY && pMouseY <= endY) {
            if (pDelta > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
                return true;
            } else if (pDelta < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                return true;
            }
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
}
