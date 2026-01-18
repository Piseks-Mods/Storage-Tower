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

public class StorageInterfaceBlockEntity extends BlockEntity {
    private TowerNetwork tower;
    private final LazyOptional<IItemHandler> itemHandlerLazy = LazyOptional.of(this::createHandler);
    private int scanCooldown = 0;

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
                return tower != null ? tower.getTotalSlots() : 0;
            }

            @Override
            public @NotNull ItemStack getStackInSlot(int slot) {
                return ItemStack.EMPTY;
            }

            @Override
            public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                if (tower == null) return stack;
                return tower.insertItem(stack, simulate);
            }

            @Override
            public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
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
