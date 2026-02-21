package org.dpdns.pisekpiskovec.storagetower.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.dpdns.pisekpiskovec.storagetower.block.entity.ModBlockEntities;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageInterfaceBlockEntity;
import org.jetbrains.annotations.Nullable;

public class StorageControllerBlock extends BaseEntityBlock {

    protected StorageControllerBlock(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pPos, BlockState pState) {
        return new StorageControllerBlockEntity(pPos, pState);
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }

    @Override
    public InteractionResult use(BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHit) {
        if (!pLevel.isClientSide && pPlayer instanceof ServerPlayer serverPlayer) {
            BlockEntity be = pLevel.getBlockEntity(pPos);
            if (be instanceof StorageControllerBlockEntity controller) {
                NetworkHooks.openScreen(serverPlayer, controller, pPos);
            }
        }
        return InteractionResult.sidedSuccess(pLevel.isClientSide);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level pLevel, BlockState pState, BlockEntityType<T> pBlockEntityType) {
        return createTickerHelper(pBlockEntityType, ModBlockEntities.STORAGE_CONTROLLER.get(), (lvl, pos, st, be) -> be.tick(lvl, pos, st));
    }

    @Override
    public void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pMovedByPiston) {
        super.onPlace(pState, pLevel, pPos, pOldState, pMovedByPiston);
        if (pState.getBlock() != pOldState.getBlock()) {
            invalidateNearbyControllers(pLevel, pPos);
        }
    }

    private void invalidateNearbyControllers(Level pLevel, BlockPos pPos) {
        if (pLevel.isClientSide) return;

        for (int y = -64; y <= 320; y++) {
            BlockPos checkPos = pPos.offset(0, y, 0);
            if (!pLevel.isLoaded(checkPos)) continue;

            var state = pLevel.getBlockState(checkPos);
            if (state.is(ModBlocks.STORAGE_CONTROLLER.get()) || state.is(ModBlocks.STORAGE_CONTROLLER_CRAFTING.get()) || state.is(ModBlocks.STORAGE_INTERFACE.get())) {
                BlockEntity be = pLevel.getBlockEntity(checkPos);
                if (be instanceof StorageControllerBlockEntity controller) {
                    controller.invalidateTower();
                } else if (be instanceof StorageCraftingControllerBlockEntity controllerr) {
                    controllerr.invalidateTower();
                } else if (be instanceof StorageInterfaceBlockEntity iface) {
                    iface.invalidateTower();
                }
            }
        }
    }

    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pMovedByPiston) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof StorageControllerBlockEntity controller) {
                controller.invalidateTower();
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston);
    }
}
