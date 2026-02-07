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
    private static final int STORAGE_SLOTS = 45; // 9x5 grid
    private String searchFilter = "";

    public StorageControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER.get(), id);
        this.blockEntity = (StorageControllerBlockEntity) entity;
        this.storageContainer = new StorageDisplayContainer(blockEntity);

        // Storage slots
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new StorageDisplaySlot(storageContainer, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 180));
        }
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
        this.broadcastChanges();
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        if (pSlotId >= 0 && pSlotId < STORAGE_SLOTS) {
            // Ghost slot handling
            Slot slot = this.slots.get(pSlotId);
            ItemStack slotStack = slot.getItem();

            if (slotStack.isEmpty()) {
                return; // Nothing to do
            }

            TowerNetwork network = blockEntity.getTower();
            if (network == null) {
                return;
            }

            // Left click = extract full stack, Right click = extract 1 item
            // Shift+Left click = extract and move to player inventory
            // Shift+Right click = extract 1 and move to player inventory

            if (pClickType == ClickType.PICKUP) {
                if (pButton == 0) { // Left click - extract full stack
                    ItemStack extracted = network.extractItem(slotStack, Math.min(slotStack.getCount(), slotStack.getMaxStackSize()), false);
                    if (!extracted.isEmpty()) {
                        pPlayer.containerMenu.setCarried(extracted);
                    }
                } else if (pButton == 1) { // Right click - extract 1 item
                    ItemStack extracted = network.extractItem(slotStack, 1, false);
                    if (!extracted.isEmpty()) {
                        ItemStack carried = pPlayer.containerMenu.getCarried();
                        if (carried.isEmpty()) {
                            pPlayer.containerMenu.setCarried(extracted);
                        } else if (ItemStack.isSameItemSameTags(carried, extracted)) {
                            carried.grow(1);
                        }
                    }
                }
                this.broadcastChanges();
                return;
            } else if (pClickType == ClickType.QUICK_MOVE) {
                // Shift+click - move to player inventory
                if (pButton == 0) { // Shift+Left click - full stack
                    ItemStack extracted = network.extractItem(slotStack, Math.min(slotStack.getCount(), slotStack.getMaxStackSize()), false);
                    if (!extracted.isEmpty()) {
                        if (!pPlayer.getInventory().add(extracted)) {
                            pPlayer.drop(extracted, false);
                        }
                    }
                } else if (pButton == 1) { // Shift+Right click - single item
                    ItemStack extracted = network.extractItem(slotStack, 1, false);
                    if (!extracted.isEmpty()) {
                        if (!pPlayer.getInventory().add(extracted)) {
                            pPlayer.drop(extracted, false);
                        }
                    }
                }
                this.broadcastChanges();
                return;
            } else {
                return; // Block all other click types on ghost slots
            }
        }

        super.clicked(pSlotId, pButton, pClickType, pPlayer); // For non-ghost slots, handle normally
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        if (pPlayer.level().isClientSide) {
            return ItemStack.EMPTY;
        }

        Slot slot = this.slots.get(pIndex);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        if (pIndex >= STORAGE_SLOTS) {
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

        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) {
            return;
        }

        TowerNetwork network = blockEntity.getTower();
        if (network != null && network.isValid()) {
            var items = network.getAllItems(searchFilter);

            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack stack = i < items.size() ? items.get(i).copy() : ItemStack.EMPTY;
                this.setRemoteSlot(i, stack);
            }
        } else {
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                this.setRemoteSlot(i, ItemStack.EMPTY);
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

    private record StorageDisplayContainer(StorageControllerBlockEntity blockEntity) implements Container {

        @Override
        public int getContainerSize() {
            return 45;
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
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int pSlot) {
            return ItemStack.EMPTY;
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
            return false;
        }

        @Override
        public boolean mayPickup(Player pPlayer) {
            return false;
        }

        @Override
        public ItemStack remove(int pAmount) {
            return ItemStack.EMPTY;
        }

        @Override
        public void set(ItemStack pStack) {
            // Noop
        }

        @Override
        public ItemStack safeInsert(ItemStack pStack) {
            return pStack;
        }

        @Override
        public ItemStack safeInsert(ItemStack pStack, int pIncrement) {
            return pStack;
        }
    }
}
