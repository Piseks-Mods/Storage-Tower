package org.dpdns.pisekpiskovec.storagetower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemStackHandler;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;

public class StorageCrateBlockEntity extends BlockEntity {
    private final ItemStackHandler inventory = new ItemStackHandler(36) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private TowerNetwork tower;

    public StorageCrateBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.STORAGE_CRATE.get(), pPos, pBlockState);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public TowerNetwork getTower() {
        return tower;
    }

    public void setTower(TowerNetwork tower) {
        this.tower = tower;
    }

    public void invalidateTower() {
        if (tower != null) {
            tower.invalidate();
            tower = null;
        }
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();

        // First pass: try to merge with existing stacks
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, remaining)) {
                int space = slotStack.getMaxStackSize() - slotStack.getCount();
                if (space > 0) {
                    int toInsert = Math.min(space, remaining.getCount());
                    if (!simulate) {
                        slotStack.grow(toInsert);
                        inventory.setStackInSlot(i, slotStack);
                    }
                    remaining.shrink(toInsert);
                    if (remaining.isEmpty()) return ItemStack.EMPTY;
                }
            }
        }

        // Second pass: find empty slots
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                int toInsert = Math.min(remaining.getMaxStackSize(), remaining.getCount());
                if (!simulate) {
                    ItemStack insertStack = remaining.copy();
                    insertStack.setCount(toInsert);
                    inventory.setStackInSlot(i, insertStack);
                }
                remaining.shrink(toInsert);
                if (remaining.isEmpty()) return ItemStack.EMPTY;
            }
        }

        return remaining;
    }

    public ItemStack extractItem(ItemStack filter, int amount, boolean simulate) {
        int remaining = amount;
        ItemStack result = ItemStack.EMPTY;

        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack slotStack = inventory.getStackInSlot(i);
            if (!slotStack.isEmpty() && ItemStack.isSameItemSameTags(slotStack, filter)) {
                int toExtract = Math.min(remaining, slotStack.getCount());

                if (result.isEmpty()) {
                    result = slotStack.copy();
                    result.setCount(toExtract);
                } else {
                    result.grow(toExtract);
                }

                if (!simulate) {
                    slotStack.shrink(toExtract);
                    if (slotStack.isEmpty()) {
                        inventory.setStackInSlot(i, ItemStack.EMPTY);
                    } else {
                        inventory.setStackInSlot(i, slotStack);
                    }
                }

                remaining -= toExtract;
                if (remaining <= 0) break;
            }
        }

        return result;
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        super.saveAdditional(pTag);
        pTag.put("Inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        if (pTag.contains("Inventory")) {
            inventory.deserializeNBT(pTag.getCompound("Inventory"));
        }
    }

    public void tick(Level lvl, BlockPos pos, BlockState state) {
    }
}
