package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

public class StorageCraftingControllerScreen extends AbstractContainerScreen<StorageCraftingControllerMenu> {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private EditBox searchBox;

    public StorageCraftingControllerScreen(StorageCraftingControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageHeight = 168;
        this.imageWidth = 176;
        this.inventoryLabelY = this.imageHeight - 82;
    }

    @Override
    protected void init() {
        super.init();

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
        pGuiGraphics.fill(x + 7, y + 4, x + 169, y + 16, 0xFF000000); // Search box background

        // Storage display area background
        pGuiGraphics.fill(x + 7, y + 17, x + 169, y + 55, 0xFF8B8B8B);
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 8 + col * 18;
                int slotY = y + 18 + row * 18;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFC6C6C6);
            }
        }

        // Crafting grid area background
        pGuiGraphics.fill(x + 52, y + 17, x + 106, y + 71, 0xFF8B8B8B);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + 53 + col * 18;
                int slotY = y + 18 + row * 18;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFC6C6C6);
            }
        }

        // Crafting result area background
        pGuiGraphics.fill(x + 142, y + 32, x + 160, y + 50, 0xFF8B8B8B);
        pGuiGraphics.fill(x + 143, y + 33, x + 159, y + 49, 0xFFC6C6C6);

        pGuiGraphics.fill(x + 115, y + 36, x + 135, y + 44, 0xFF8B8B8B); // Arrow from crafting to result
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
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
        if (pKeyCode != 256 && !this.searchBox.isFocused()) {
            this.searchBox.setFocused(true);
            return this.searchBox.keyPressed(pKeyCode, pScanCode, pModifiers);
        }

        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    public boolean charTyped(char pCodePoint, int pModifiers) {
        if (this.searchBox.isFocused()) return this.searchBox.charTyped(pCodePoint, pModifiers);

        // Autofocus search box when typing
        this.searchBox.setFocused(true);
        return this.searchBox.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.searchBox.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        if (this.searchBox.isFocused()) this.searchBox.setFocused(false); // Unfocus search box when clicking elsewhere
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.searchBox != null) this.searchBox.tick();
    }

    @Override
    public void resize(Minecraft pMinecraft, int pWidth, int pHeight) {
        String searchText = this.searchBox.getValue();
        super.resize(pMinecraft, pWidth, pHeight);
        this.searchBox.setValue(searchText);
    }
}
