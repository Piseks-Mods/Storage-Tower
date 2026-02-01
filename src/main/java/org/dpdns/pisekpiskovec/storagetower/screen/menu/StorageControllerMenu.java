package org.dpdns.pisekpiskovec.storagetower.screen.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        // Intercept clicks on ghost slots to prevent certain operations
        if (pSlotId >= 0 && pSlotId < STORAGE_SLOTS) {
            // Ghost slots
            if (pClickType == ClickType.PICKUP || pClickType == ClickType.PICKUP_ALL) {
                super.clicked(pSlotId, pButton, pClickType, pPlayer); // Allow normal pickup
                return;
            } else if (pClickType == ClickType.QUICK_MOVE) {
                super.clicked(pSlotId, pButton, pClickType, pPlayer); // Allow shift-click
                return;
            } else {
                return; // Block all other click types on ghost slots (SWAP, CLONE, THROW, etc.)
            }
        }

        // For non-ghost slots, handle normally
        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        System.out.println("REQUEST BELOW MADE USING: shift+click");

        // Server-side only check
        if (pPlayer.level().isClientSide) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(pIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        if (pIndex < STORAGE_SLOTS) {
            // From storage to player
            TowerNetwork network = blockEntity.getTower();
            if (network != null && !slotStack.isEmpty()) {
                ItemStack extracted = network.extractItem(slotStack, slotStack.getCount(), false);
                if (!extracted.isEmpty()) {
                    if (!pPlayer.getInventory().add(extracted)) /* Try to add to player inventory */ {
                        pPlayer.drop(extracted, false); // If couldn't add, drop it
                    }
                }
            }
        } else {
            // From player to storage
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                ItemStack remaining = network.insertItem(slotStack, false);
                slotStack.setCount(remaining.getCount());
                if (remaining.isEmpty()) {
                    slot.set(ItemStack.EMPTY);
                } else {
                    slot.setChanged();
                }
            }
        }

        return originalStack;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity == null || blockEntity.getLevel() == null) {
            return;
        }

        // Only update on server size
        if (!blockEntity.getLevel().isClientSide) {
            TowerNetwork network = blockEntity.getTower();
            if (network != null && network.isValid()) {
                var items = network.getAllItems();
                for (int i = 0; i < STORAGE_SLOTS; i++) {
                    ItemStack newStack = i < items.size() ? items.get(i).copy() : ItemStack.EMPTY;
                    this.setRemoteSlot(i, newStack);
                }
            } else {
                // Clear all slots if network is invalid
                for (int i = 0; i < STORAGE_SLOTS; i++) {
                    this.setRemoteSlot(i, ItemStack.EMPTY);
                }
            }
        }
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
            System.out.println("REQUEST BELOW MADE USING: dragging item into/outto slot");

            // Only process on server side to avoid double extraction
            if (this.container instanceof StorageDisplayContainer displayContainer) {
                if (displayContainer.blockEntity.getLevel() != null &&
                    displayContainer.blockEntity.getLevel().isClientSide) {
                    // On client, just return what we think we are removing
                    ItemStack displayStack = getItem();
                    if (!displayStack.isEmpty()) {
                        ItemStack result = displayStack.copy();
                        result.setCount(Math.min(pAmount, displayStack.getCount()));
                        return result;
                    }
                    return ItemStack.EMPTY;
                }
            }

            // Server side - do the actual extraction
            ItemStack displayStack = getItem();
            if (displayStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            return this.container.removeItem(this.index, pAmount);
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
