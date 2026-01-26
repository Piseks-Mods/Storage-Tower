package org.dpdns.pisekpiskovec.storagetower.screen.menu;

import net.minecraft.server.level.ServerPlayer;
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
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;

public class StorageCraftingControllerMenu extends AbstractContainerMenu {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private final CraftingContainer craftingContainer;
    private final ResultContainer resultContainer;
    private final ContainerLevelAccess access;
    private final Player player;
    private static final int STORAGE_SLOTS = 3; // 3 vertical slots

    public StorageCraftingControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER_CRAFTING.get(), id);
        this.blockEntity = (StorageCraftingControllerBlockEntity) entity;
        this.craftingContainer = new TransientCraftingContainer(this, 3, 3);
        this.resultContainer = new ResultContainer();
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.player = playerInv.player;

        // Storage display slots
        for (int i = 0; i < 3; i++) {
            this.addSlot(new StorageDisplaySlot(blockEntity, i, 9, 19 + i * 18));
        }

        // Crafting result slot
        this.addSlot(new ResultSlot(this.player, this.craftingContainer, this.resultContainer, 0, 124, 35));

        // Crafting grid
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
            slotChangedCraftingGrid(this, level, this.player, this.craftingContainer, this.resultContainer);
        });
    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer) {
        if (!level.isClientSide) {
            ItemStack result = ItemStack.EMPTY;
            var optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);

            if (optional.isPresent()) {
                CraftingRecipe recipe = optional.get();
                if (resultContainer.setRecipeUsed(level, (ServerPlayer) player, recipe)) {
                    result = recipe.assemble(craftingContainer, level.registryAccess());
                }
            }

            resultContainer.setItem(0, result);
            menu.setRemoteSlot(0, result);
            menu.broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (pIndex < STORAGE_SLOTS) {
                // From storage to player inv
                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 46, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex == STORAGE_SLOTS) {
                // Result slot - craft the item
                this.access.execute((level, pos) -> {
                    slotStack.getItem().onCraftedBy(slotStack, level, pPlayer);
                });

                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(slotStack, itemStack);
            } else if (pIndex >= STORAGE_SLOTS + 1 && pIndex < STORAGE_SLOTS + 10) {
                // From crafting grid to player inventory
                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= STORAGE_SLOTS + 10 && pIndex < STORAGE_SLOTS + 46) {
                // From player inventory
                // Try crafting grid first, then storage
                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 1, STORAGE_SLOTS + 10, false)) {
                    // Try inserting into tower network
                    TowerNetwork network = blockEntity.getTower();
                    if (network != null) {
                        ItemStack remaining = network.insertItem(slotStack, false);
                        slotStack.setCount(remaining.getCount());
                        if (remaining.isEmpty()) {
                            slot.set(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        return itemStack;
                    }

                    if (pIndex < STORAGE_SLOTS + 37) {
                        if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 37, STORAGE_SLOTS + 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
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
            if (pIndex == STORAGE_SLOTS) {
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

    private static class StorageDisplaySlot extends Slot {
        private final StorageCraftingControllerBlockEntity blockEntity;
        private final int slotIndex;

        public StorageDisplaySlot(StorageCraftingControllerBlockEntity blockEntity, int index, int x, int y) {
            super(null, index, x, y);
            this.blockEntity = blockEntity;
            this.slotIndex = index;
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return false; // Cant place items directly in display slots
        }

        @Override
        public ItemStack getItem() {
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                var items = network.getAllItems();
                if (slotIndex < items.size()) {
                    return items.get(slotIndex);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void set(ItemStack pStack) {

        }

        @Override
        public void setChanged() {

        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }

        @Override
        public ItemStack remove(int pAmount) {
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                var items = network.getAllItems();
                if (slotIndex < items.size()) {
                    ItemStack stack = items.get(slotIndex);
                    return network.extractItem(stack, Math.min(pAmount, stack.getCount()), false);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public boolean mayPickup(Player pPlayer) {
            return true;
        }
    }
}
