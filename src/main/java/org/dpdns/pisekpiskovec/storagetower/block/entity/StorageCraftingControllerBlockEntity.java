package org.dpdns.pisekpiskovec.storagetower.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.jetbrains.annotations.Nullable;

public class StorageCraftingControllerBlockEntity extends BlockEntity implements MenuProvider {
    private TowerNetwork tower;
    private int scanCooldown = 0;

    public StorageCraftingControllerBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.STORAGE_CONTROLLER_CRAFTING.get(), pPos, pBlockState);
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
    public Component getDisplayName() {
        return Component.translatable("container.towerstorage.storage_crafting_controller");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new StorageCraftingControllerMenu(pContainerId, pPlayerInventory, this);
    }
}
