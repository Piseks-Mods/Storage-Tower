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
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;

import java.util.List;

public class StorageControllerScreen extends AbstractContainerScreen<StorageControllerMenu> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(StorageTower.MOD_ID, "textures/gui/storage_controller.png");

    private final StorageControllerBlockEntity blockEntity;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    public StorageControllerScreen(StorageControllerMenu menu, Inventory playerInv, Component title) {
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

        // Storage display area background
        pGuiGraphics.fill(x + 8, y + 18, x + 168, y + 72, 0xFF373737);

        // Draw slot backgrounds for storage items
        int slotsPerRow = 8;
        int rows = 3;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < slotsPerRow; col++) {
                int slotX = x + 9 + col * 18;
                int slotY = y + 19 + row * 18;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF8B8B8B);
            }
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
            int slotsPerRow = 8;
            int visibleRows = 3;

            int startIndex = scrollOffset * slotsPerRow;
            int endIndex = Math.min(startIndex + (visibleRows * slotsPerRow), items.size());

            for (int i = startIndex; i < endIndex; i++) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    int displayIndex = i - startIndex;
                    int slotX = startX + (displayIndex % slotsPerRow) * 18;
                    int slotY = startY + (displayIndex / slotsPerRow) * 18;

                    if (pX >= slotX && pX < slotX + 16 && pY >= slotY && pY < slotY + 16) {
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

            // Calculate max scroll
            int slotsPerRow = 8;
            int visibleRows = 3;
            int totalRows = (items.size() + slotsPerRow - 1) / slotsPerRow;
            maxScroll = Math.max(0, totalRows - visibleRows);

            int startX = leftPos + 9;
            int startY = topPos + 19;

            int startIndex = scrollOffset * slotsPerRow;
            int endIndex = Math.min(startIndex + (visibleRows * slotsPerRow), items.size());

            for (int i = startIndex; i < endIndex; i++) {
                ItemStack stack = items.get(i);
                if (!stack.isEmpty()) {
                    int displayIndex = i - startIndex;
                    int slotX = startX + (displayIndex % slotsPerRow) * 18;
                    int slotY = startY + (displayIndex / slotsPerRow) * 18;

                    pGuiGraphics.renderItem(stack, slotX, slotY);
                    pGuiGraphics.renderItemDecorations(this.font, stack, slotX, slotY);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta > 0) {
            scrollOffset = Math.max(0, scrollOffset - 1);
            return true;
        } else if (delta < 0) {
            scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }
}
