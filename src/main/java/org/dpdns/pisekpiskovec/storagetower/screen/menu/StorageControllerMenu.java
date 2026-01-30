package org.dpdns.pisekpiskovec.storagetower.screen.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;

public class StorageControllerMenu extends AbstractContainerMenu {
    private final StorageControllerBlockEntity blockEntity;
    private final StorageDisplayContainer storageContainer;
    private static final int STORAGE_SLOTS = 24; // 8x3 grid, where would be 9th column there will be scrollbar

    public StorageControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER.get(), id);
        this.blockEntity = (StorageControllerBlockEntity) entity;
        this.storageContainer = new StorageDisplayContainer(blockEntity);

        // Storage slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                this.addSlot(new StorageDisplaySlot(storageContainer, col + row * 8, 9 + col * 18, 19 + row * 18));
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
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (pIndex < STORAGE_SLOTS) {
                // From storage to player inventory
                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS, STORAGE_SLOTS + 36, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(slotStack, itemStack);
            } else {
                // From player inventory to storage
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
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
        }

        return itemStack;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return blockEntity != null && !blockEntity.isRemoved() && pPlayer.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }

    public StorageControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }

    private static class StorageDisplayContainer implements Container {
        private final StorageControllerBlockEntity blockEntity;

        public StorageDisplayContainer(StorageControllerBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public int getContainerSize() {
            return 24;
        }

        @Override
        public boolean isEmpty() {
            TowerNetwork network = blockEntity.getTower();
            return network == null || network.getAllItems().isEmpty();
        }

        @Override
        public ItemStack getItem(int pSlot) {
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                var items = network.getAllItems();
                if (pSlot < items.size()) {
                    return items.get(pSlot);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int pSlot, int pAmount) {
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                var items = network.getAllItems();
                if (pSlot < items.size()) {
                    ItemStack stack = items.get(pSlot);
                    ItemStack filter = stack.copy();
                    return network.extractItem(filter, Math.min(pAmount, stack.getCount()), false);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int pSlot) {
            return removeItem(pSlot, 64);
        }

        @Override
        public void setItem(int pSlot, ItemStack pStack) {
            // Noop
        }

        @Override
        public void setChanged() {
            blockEntity.setChanged();
        }

        @Override
        public boolean stillValid(Player pPlayer) {
            return true;
        }

        @Override
        public void clearContent() {
            // Noop
        }
    }

    private static class StorageDisplaySlot extends Slot {
        public StorageDisplaySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return false; // Can't place items directly in display slots
        }

        @Override
        public boolean mayPickup(Player pPlayer) {
            return true;
        }

        @Override
        public ItemStack remove(int pAmount) {
            // Get the item at this slot position
            ItemStack displayStack = getItem();
            if(displayStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            // Extract from the network
            ItemStack extracted = this.container.removeItem(this.index, pAmount);

            // Mark container as changed to trigger client sync
            if(!extracted.isEmpty()) {
                this.setChanged();
            }

            return extracted;
        }

        @Override
        public void onTake(Player pPlayer, ItemStack pStack) {
            this.setChanged();
            super.onTake(pPlayer, pStack);
        }

        @Override
        public void set(ItemStack pStack) {
            // Noop
        }

        @Override
        public ItemStack safeInsert(ItemStack pStack) {
            return pStack; // Ghosts can't accept items
        }

        @Override
        public ItemStack safeInsert(ItemStack pStack, int pIncrement) {
            return pStack; // Ghosts can't accept items
        }
    }
}
