package org.dpdns.pisekpiskovec.storagetower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StorageInterfaceBlockEntity extends BlockEntity {
    private TowerNetwork tower;
    private final LazyOptional<IItemHandler> itemHandlerLazy = LazyOptional.of(this::createHandler);
    private int scanCooldown = 0;
    private int extractionIndex = 0;

    public StorageInterfaceBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.STORAGE_INTERFACE.get(), pPos, pBlockState);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        scanCooldown--;
        if (scanCooldown <= 0) {
            scanCooldown = 20; // Rescan every second
            if (tower == null || !tower.isValid()) {
                tower = new TowerNetwork(level, pos);
                tower.scanTower();
            }
        }
    }

    private IItemHandler createHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return tower != null ? Math.min(tower.getTotalSlots(), 100) : 0;
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                if (tower == null) return ItemStack.EMPTY;

                List<ItemStack> items = tower.getAllItems();
                if (slot >= 0 && slot < items.size()) {
                    return items.get(slot);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (tower == null) return stack;
                return tower.insertItem(stack, simulate);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (tower == null) return ItemStack.EMPTY;

                // Get all items from storage
                List<ItemStack> items = tower.getAllItems();
                if (items.isEmpty()) return ItemStack.EMPTY;

                // Use round-robin extraction to cycle through different items
                // This prevents hoppers from always extracting the same item type
                if (!simulate) extractionIndex = extractionIndex % Math.max(1, items.size());

                int indexToExtract = extractionIndex % Math.max(1, items.size());
                ItemStack targetStack = items.get(indexToExtract);
                if (targetStack.isEmpty()) return ItemStack.EMPTY;

                // Extract the requested amount (or less if not available)
                int toExtract = Math.min(amount, Math.min(targetStack.getCount(), targetStack.getMaxStackSize()));
                ItemStack extracted = tower.extractItem(targetStack, toExtract, simulate);

                // Move to next item for next extraction
                if (!simulate && !extracted.isEmpty()) {
                    extractionIndex++;
                }
                return extracted;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }

            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return true;
            }
        };
    }

    public TowerNetwork getTower() {
        if (tower == null) {
            tower = new TowerNetwork(level, worldPosition);
            tower.scanTower();
        }
        return tower;
    }

    public void invalidateTower() {
        if (tower != null) {
            tower.invalidate();
            tower = null;
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerLazy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerLazy.invalidate();
    }
}
