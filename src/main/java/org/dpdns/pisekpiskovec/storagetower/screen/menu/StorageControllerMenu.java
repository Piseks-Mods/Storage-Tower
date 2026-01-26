package org.dpdns.pisekpiskovec.storagetower.screen.menu;

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
    private static final int STORAGE_SLOTS = 24; // 8x3 grid, where would be 9th slot there will be scrollbar

    public StorageControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER.get(), id);
        this.blockEntity = (StorageControllerBlockEntity) entity;

        // Storage slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                this.addSlot(new StorageDisplaySlot(blockEntity, col + row * 8, 9 + col * 18, 19 + row * 18));
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

    private static class StorageDisplaySlot extends Slot {
        private final StorageControllerBlockEntity blockEntity;
        private final int slotIndex;

        public StorageDisplaySlot(StorageControllerBlockEntity blockEntity, int index, int x, int y) {
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
