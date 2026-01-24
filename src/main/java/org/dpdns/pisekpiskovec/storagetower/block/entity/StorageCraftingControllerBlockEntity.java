package org.dpdns.pisekpiskovec.storagetower.block.entity;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.dpdns.pisekpiskovec.storagetower.client.ui.StorageCraftingControllerUI;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;

public class StorageCraftingControllerBlockEntity extends BlockEntity implements IUIHolder, IManaged {
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
    public ModularUI createUI(Player player) {
        return new StorageCraftingControllerUI(this, player).createUI();
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

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return null;
    }

    @Override
    public IManagedStorage getSyncStorage() {
        return null;
    }

    @Override
    public void onChanged() {

    }
}
