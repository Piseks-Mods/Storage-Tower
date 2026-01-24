package org.dpdns.pisekpiskovec.storagetower.client.ui;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.items.ItemStackHandler;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class StorageCraftingControllerUI {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private final Player player;
    private final ItemStackHandler craftingGrid = new ItemStackHandler(9);
    private final ItemStackHandler resultSlot = new ItemStackHandler(1);

    public StorageCraftingControllerUI(StorageCraftingControllerBlockEntity blockEntity, Player player) {
        this.blockEntity = blockEntity;
        this.player = player;
    }

    public ModularUI createUI() {
        ModularUI.Builder builder = new ModularUI.Builder(new ColorRectWidget(0, 0, 176, 166, 0xFF404040), 176, 166);

        // Title
        builder.widget(new LabelWidget(8, 6, "Controller").setTextColor(0x404040));

        // Crafting grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = col + row * 3;
                SlotWidget slot = new SlotWidget((Container) craftingGrid, index, 30 + col * 18, 17 + row * 18, true, true);
                slot.setChangeListener(this::onCraftingGridChanged);
                builder.widget(slot);
            }
        }

        // Crafting result slot
        SlotWidget resultSlotWidget = new SlotWidget((Container) resultSlot, 0, 124, 35, false, true);
        resultSlotWidget.setOnSlotChanged(this::onResultTaken);
        builder.widget(resultSlotWidget);

        // Storage display
        WidgetGroup storageGroup = new WidgetGroup(8, 18, 20, 60);
        DraggableScrollableWidgetGroup scrollable = new DraggableScrollableWidgetGroup(0, 0, 20, 60);
        scrollable.setBackground(new ColorRectWidget(0, 0, 20, 60, 0xFF202020));

        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            List<ItemStack> items = network.getAllItems();
            int y = 0;

            for (ItemStack stack : items) {
                if (!stack.isEmpty() && y * 18 < 240) {
                    ItemStackWidget itemWidget = new ItemStackWidget(1, y * 18 + 1, stack);
                    scrollable.addWidget(itemWidget);
                    y++;
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

    private void onCraftingGridChanged() {
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) {
            // Create a temporary crafting container
            CraftingContainer craftingContainer = new TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
                @Override
                public @NotNull ItemStack quickMoveStack(@NotNull Player pPlayer, int pIndex) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(@NotNull Player pPlayer) {
                    return true;
                }
            }, 3, 3);

            for (int i = 0; i < 9; i++) {
                craftingContainer.setItem(i, craftingGrid.getStackInSlot(i));
            }

            var recipeOpt = Objects.requireNonNull(blockEntity.getLevel().getServer()).getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, blockEntity.getLevel());

            if (recipeOpt.isPresent()) {
                CraftingRecipe recipe = recipeOpt.get();
                ItemStack result = recipe.assemble(craftingContainer, blockEntity.getLevel().registryAccess());
                resultSlot.setStackInSlot(0, result);
            } else {
                resultSlot.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
    }

    private void onResultTaken() {
        // Consume crafting ingredients
        for (int i = 0; i < 9; i++) {
            ItemStack stack = craftingGrid.getStackInSlot(i);
            if (!stack.isEmpty()) {
                stack.shrink(1);
                craftingGrid.setStackInSlot(i, stack);
            }
        }
        onCraftingGridChanged();
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
