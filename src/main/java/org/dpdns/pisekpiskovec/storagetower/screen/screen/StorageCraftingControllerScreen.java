package org.dpdns.pisekpiskovec.storagetower.screen.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.ModNetworking;
import org.dpdns.pisekpiskovec.storagetower.network.packet.GridClickPacket;
import org.dpdns.pisekpiskovec.storagetower.network.packet.GridInsertPacket;
import org.dpdns.pisekpiskovec.storagetower.network.packet.SearchFilterPacket;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

import java.util.List;

public class StorageCraftingControllerScreen extends AbstractContainerScreen<StorageCraftingControllerMenu> {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private EditBox searchBox;

    private static final int GRID_START_X = 8;
    private static final int GRID_START_Y = 18;
    private static final int GRID_COLS = 9;
    private static final int GRID_ROWS = 2;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_SLOTS = GRID_COLS * GRID_ROWS;

    private int scrollOffset = 0;

    public StorageCraftingControllerScreen(StorageCraftingControllerMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title);
        this.blockEntity = menu.getBlockEntity();
        this.imageHeight = 208;
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
        ModNetworking.sendToServer(new SearchFilterPacket(newValue));
        scrollOffset = 0;
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
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLS; col++) {
                int slotX = x + GRID_START_X + col * SLOT_SIZE;
                int slotY = y + GRID_START_Y + row * SLOT_SIZE;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFC6C6C6);
            }
        }

        // Crafting grid area background
        pGuiGraphics.fill(x + 52, y + 57, x + 106, y + 111, 0xFF8B8B8B);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int slotX = x + 53 + col * SLOT_SIZE;
                int slotY = y + 58 + row * SLOT_SIZE;
                pGuiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFFC6C6C6);
            }
        }

        // Crafting result area background
        pGuiGraphics.fill(x + 142, y + 72, x + 160, y + 90, 0xFF8B8B8B);
        pGuiGraphics.fill(x + 143, y + 73, x + 159, y + 89, 0xFFC6C6C6);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderStorageItems(pGuiGraphics);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
    }

    private void renderStorageItems(GuiGraphics pGuiGraphics) {
        List<ItemStack> items = menu.getClientItems();

        int startIndex = scrollOffset * GRID_COLS;
        int endIndex = Math.min(startIndex + VISIBLE_SLOTS, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                int displayIndex = i - startIndex;
                int col = displayIndex % GRID_COLS;
                int row = displayIndex / GRID_COLS;

                int x = leftPos + GRID_START_X + col * SLOT_SIZE;
                int y = topPos + GRID_START_Y + row * SLOT_SIZE;

                pGuiGraphics.pose().pushPose();
                pGuiGraphics.pose().translate(0, 0, 100);
                pGuiGraphics.renderItem(stack, x, y);
                pGuiGraphics.renderItemDecorations(this.font, stack, x, y);
                pGuiGraphics.pose().popPose();
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY) {
        pGuiGraphics.drawString(this.font, this.playerInventoryTitle, 8, this.imageHeight - 94, 0x404040, false);
    }

    @Override
    protected void renderTooltip(GuiGraphics pGuiGraphics, int pX, int pY) {
        super.renderTooltip(pGuiGraphics, pX, pY);

        List<ItemStack> items = menu.getClientItems();

        int startIndex = scrollOffset * GRID_COLS;
        int endIndex = Math.min(startIndex + VISIBLE_SLOTS, items.size());

        for (int i = startIndex; i < endIndex; i++) {
            ItemStack stack = items.get(i);
            if (!stack.isEmpty()) {
                int displayIndex = i - startIndex;
                int col = displayIndex % GRID_COLS;
                int row = displayIndex / GRID_COLS;
                int slotX = leftPos + GRID_START_X + col * SLOT_SIZE;
                int slotY = topPos + GRID_START_Y + row * SLOT_SIZE;
                if (pX >= slotX && pX < slotX + 16 && pY >= slotY && pY < slotY + 16) {
                    pGuiGraphics.renderTooltip(this.font, stack, pX, pY);
                    break;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (this.searchBox.mouseClicked(pMouseX, pMouseY, pButton)) return true;
        if (this.searchBox.isFocused()) this.searchBox.setFocused(false);

        int relX = (int) (pMouseX - leftPos);
        int relY = (int) (pMouseY - topPos);

        if (relX >= GRID_START_X && relX < GRID_START_X + GRID_COLS * SLOT_SIZE && relY >= GRID_START_Y && relY < GRID_START_Y + GRID_ROWS * SLOT_SIZE) {
            ItemStack carried = this.menu.getCarried();
            if (!carried.isEmpty()) {
                ModNetworking.sendToServer(new GridInsertPacket(pButton == 0));
                return true;
            }

            // Not carrying item
            int col = (relX - GRID_START_X) / SLOT_SIZE;
            int row = (relY - GRID_START_Y) / SLOT_SIZE;
            int gridSlot = scrollOffset * GRID_COLS + row * GRID_COLS + col;

            ClickType clickType = hasShiftDown() ? ClickType.QUICK_MOVE : ClickType.PICKUP;
            ModNetworking.sendToServer(new GridClickPacket(gridSlot, pButton, clickType));
            return true;
        }

        return super.mouseClicked(pMouseX, pMouseY, pButton);
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
        this.searchBox.setFocused(true); // Autofocus search box when typing
        return this.searchBox.charTyped(pCodePoint, pModifiers);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        int relX = (int) (pMouseX - leftPos);
        int relY = (int) (pMouseY - topPos);

        if (relX >= GRID_START_X && relX < GRID_START_X + GRID_COLS * SLOT_SIZE && relY >= GRID_START_Y && relY < GRID_START_Y + GRID_ROWS * SLOT_SIZE) {
            List<ItemStack> items = menu.getClientItems();
            int maxRows = (items.size() + SLOT_SIZE - 1) / GRID_COLS;
            int maxScroll = Math.max(0, maxRows - GRID_ROWS);

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

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.searchBox != null) this.searchBox.tick();
    }
}
