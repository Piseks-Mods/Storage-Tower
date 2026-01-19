package org.dpdns.pisekpiskovec.storagetower.screen.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;

public class StorageCraftingControllerMenu extends AbstractContainerMenu {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private final CraftingContainer craftingContainer;
    private final ResultContainer resultContainer;
    private final ContainerLevelAccess access;

    public StorageCraftingControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER_CRAFTING.get(), id);
        this.blockEntity = (StorageCraftingControllerBlockEntity) entity;
        this.craftingContainer = new TransientCraftingContainer(this, 3, 3);
        this.resultContainer = new ResultContainer();
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());

        // Crafting result slot
        this.addSlot(new ResultSlot(playerInv.player, this.craftingContainer, this.resultContainer, 0, 124, 35));

        // Crafting grid 3x3
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(this.craftingContainer, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void slotsChanged(Container pContainer) {
        this.access.execute((level, pos) -> {
            slotChangedCraftingGrid(this, level, null, this.craftingContainer, this.resultContainer);
        });
    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer) {
        if (!level.isClientSide) {
            ItemStack result = ItemStack.EMPTY;
            var optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);

            if (optional.isPresent()) {
                CraftingRecipe recipe = optional.get();
                if (resultContainer.setRecipeUsed(level, null, recipe)) {
                    result = recipe.assemble(craftingContainer, level.registryAccess());
                }
            }
            resultContainer.setItem(0, result);
            menu.setRemoteSlot(0, result);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (pIndex == 0) {
                // Result slot
                this.access.execute((level, pos) -> {
                    slotStack.getItem().onCraftedBy(slotStack, level, pPlayer);
                });

                if (!this.moveItemStackTo(slotStack, 10, 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(slotStack, itemStack);
            } else if (pIndex >= 10 && pIndex < 46) {
                // From player inventory to crafting grid
                if (!this.moveItemStackTo(slotStack, 1, 10, false)) {
                    if (pIndex < 37) {
                        if (!this.moveItemStackTo(slotStack, 37, 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(slotStack, 10, 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            } else if (!this.moveItemStackTo(slotStack, 10, 46, false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
            if (pIndex == 0) {
                pPlayer.drop(slotStack, false);
            }
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return blockEntity != null && !blockEntity.isRemoved() && pPlayer.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((level, pos) -> {
            this.clearContainer(pPlayer, this.craftingContainer);
        });
    }

    public StorageCraftingControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
