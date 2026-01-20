package org.dpdns.pisekpiskovec.storagetower.block.entity;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;

public class StorageControllerBlockEntity extends BlockEntity implements IUIHolder, IManaged {
    private int scanCooldown = 0;
    private TowerNetwork tower;

    public StorageControllerBlockEntity(BlockPos pPos, BlockState pState) {
        super(ModBlockEntities.STORAGE_CONTROLLER.get(), pPos, pState);
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
    public ModularUI createUI(Player player) {
        return new StorageControllerUI(this, player).createUI();
    }

    @Override
    public boolean isInvalid() {
        return isRemoved();
    }

    @Override
    public boolean isRemote() {
        return level != null && level.isClientSide;
    }

    @Override
    public void markAsDirty() {
        setChanged();
    }
}
