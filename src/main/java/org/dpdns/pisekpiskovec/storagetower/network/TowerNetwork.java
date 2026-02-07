package org.dpdns.pisekpiskovec.storagetower.network;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.ItemStackHandler;
import org.dpdns.pisekpiskovec.storagetower.block.ModBlocks;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCrateBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class TowerNetwork {
    private final Level level;
    private final BlockPos masterPos;
    private final List<StorageCrateBlockEntity> crates = new ArrayList<>();
    private boolean valid = true;

    public TowerNetwork(Level level, BlockPos masterPos) {
        this.level = level;
        this.masterPos = masterPos;
    }

    public void scanTower() {
        System.out.println("SCAN REQUEST");
        crates.clear();

        // Scan upwards
        BlockPos pos = masterPos.above();
        while (isTowerBlock(pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageCrateBlockEntity crate) {
                crates.add(crate);
                crate.setTower(this);
            }
            pos = pos.above();
        }

        // Scan downwards
        pos = masterPos.below();
        while (isTowerBlock(pos)) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof StorageCrateBlockEntity crate) {
                crates.add(crate);
                crate.setTower(this);
            }
            pos = pos.below();
        }

        System.out.println("SCAN FINISHED");
    }

    private boolean isTowerBlock(BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        var state = level.getBlockState(pos);
        return state.is(ModBlocks.STORAGE_CRATE.get()) || state.is(ModBlocks.STORAGE_CONTROLLER.get()) || state.is(ModBlocks.STORAGE_CONTROLLER_CRAFTING.get()) || state.is(ModBlocks.STORAGE_INTERFACE.get());
    }

    public ItemStack insertItem(ItemStack stack, boolean simulate) {
        System.out.println("INSERT REQUEST: " + stack + " x" + stack.getCount() + "; simulation=" + simulate);
        if (!valid || stack.isEmpty()) return stack;

        ItemStack remaining = stack.copy();
        for (StorageCrateBlockEntity crate : crates) {
            remaining = crate.insertItem(remaining, simulate);
            if (remaining.isEmpty()) break;
        }
        System.out.println("INSERT RESULT: " + remaining + " x" + remaining.getCount());
        return remaining;
    }

    public ItemStack extractItem(ItemStack filter, int amount, boolean simulate) {
        System.out.println("EXTRACT REQUEST: " + filter + " x" + amount + "; simulation=" + simulate);

        if (!valid || filter.isEmpty() || amount <= 0) return ItemStack.EMPTY;

        int remaining = amount;
        ItemStack result = ItemStack.EMPTY;

        for (StorageCrateBlockEntity crate : crates) {
            ItemStack extracted = crate.extractItem(filter, remaining, simulate);
            if (!extracted.isEmpty()) {
                if (result.isEmpty()) {
                    result = extracted;
                } else {
                    result.grow(extracted.getCount());
                }
                remaining -= extracted.getCount();
                if (remaining <= 0) break;
            }
        }

        System.out.println("EXTRACT RESULT: " + result + " x" + result.getCount());
        return result;
    }

    public List<ItemStack> getAllItems() {
        return getAllItems(null);
    }

    public List<ItemStack> getAllItems(String filter) {
        List<ItemStack> items = new ArrayList<>();
        if (!valid) return items;

        for (StorageCrateBlockEntity crate : crates) {
            ItemStackHandler handler = crate.getInventory();
            for (int i = 0; i < handler.getSlots(); i++) {
                ItemStack stack = handler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    // Merge with existing stacks
                    boolean merged = false;
                    for (ItemStack existing : items) {
                        if (ItemStack.isSameItemSameTags(existing, stack)) {
                            existing.grow(stack.getCount());
                            merged = true;
                            break;
                        }
                    }
                    if (!merged) {
                        items.add(stack.copy());
                    }
                }
            }
        }

        // Apply filter if provided
        if (filter != null && !filter.trim().isEmpty()) {
            String lowerFilter = filter.toLowerCase();
            items.removeIf(stack -> !stack.getHoverName().getString().toLowerCase().contains(lowerFilter));
        }

        // Sort items alphabetically by display name
        items.sort((a, b) -> a.getHoverName().getString().compareToIgnoreCase(b.getHoverName().getString()));

        return items;
    }

    public int getTotalSlots() {
        return crates.size() * 36;
    }

    public void invalidate() {
        valid = false;
        for (StorageCrateBlockEntity crate : crates) {
            crate.setTower(null);
        }
        crates.clear();
    }

    public boolean isValid() {
        return valid;
    }

    public BlockPos getMasterPos() {
        return masterPos;
    }
}
