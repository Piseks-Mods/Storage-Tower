package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;

public class StorageControllerScreen extends AbstractContainerScreen<StorageControllerMenu> {
    private final StorageControllerBlockEntity blockEntity;
    private EditBox searchBox;

    public StorageControllerScreen(StorageControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageHeight = 204;
        this.imageWidth = 176;
        this.inventoryLabelY = this.imageHeight - 92;
    }

    @Override
    protected void init() {
        super.init();

        // Creates search box
        this.searchBox = new EditBox(this.font, this.leftPos + 8, this.topPos + 5, 160, 10, Component.translatable("gui.storagetower.search"));
        this.searchBox.setBordered(true);
        this.searchBox.setMaxLength(50);
        this.searchBox.setResponder(this::onSearchChanged);
        this.searchBox.setValue(menu.getSearchFilter());
        this.addRenderableWidget(this.searchBox);
    }

    private void onSearchChanged(String newValue) {
        menu.setSearchFilter(newValue);
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int x = leftPos;
        int y = topPos;

        pGuiGraphics.fill(x, y, x + imageWidth, y + imageHeight, 0xFFC6C6C6); // Main background
        pGuiGraphics.fill(x + 7, y + 4, x + 169, y + 16, 0XFF000000); // Search box background
        pGuiGraphics.fill(x + 7, y + 17, x + 169, y + 109, 0XFF8B8B8B); // Storage display area background

        // Draw slot backgrounds for storage items (9x5 grid)
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 8 + col * 18;
                int slotY = y + 18 + row * 18;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFC6C6C6);
            }
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.inventoryLabelY, 0x404040, false);
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if (this.searchBox.isFocused()) {
            if (pKeyCode == 256) { // ESC key
                this.searchBox.setFocused(false);
                return true;
            }
            return this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        // Autofocus search box when typing
        if (pKeyCode != 256 && !this.searchBox.isFocused()) { // Not ESC
            this.searchBox.setFocused(true);
            return this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.searchBox.isFocused()) {
            return this.searchBox.charTyped(pCodePoint, pModifiers);
        }

        // Autofocus search box when typing
        this.searchBox.setFocused(true);
        return this.searchBox.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.searchBox.mouseClicked(pMouseX, pMouseY, pButton)) {
            return true;
        }

        // Unfocus search box when clicking elsewhere
        if (this.searchBox.isFocused()) {
            this.searchBox.setFocused(false);
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.searchBox != null) {
            this.searchBox.tick();
        }
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String searchText = this.searchBox.getValue();
        super.resize(pMinecraft, pWidth, pHeight);
        this.searchBox.setValue(searchText);
    }
}
