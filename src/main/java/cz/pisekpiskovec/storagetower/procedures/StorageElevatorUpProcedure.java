package cz.pisekpiskovec.storagetower.procedures;

import net.minecraft.world.World;
import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.block.BlockState;

import java.util.Map;

import cz.pisekpiskovec.storagetower.block.StorageCrateBlock;
import cz.pisekpiskovec.storagetower.PiseksStorageTowerMod;

public class StorageElevatorUpProcedure {

	public static void executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency world for procedure StorageElevatorUp!");
			return;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency x for procedure StorageElevatorUp!");
			return;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency y for procedure StorageElevatorUp!");
			return;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency z for procedure StorageElevatorUp!");
			return;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		double yLvl = 0;
		yLvl = (new Object() {
			public double getValue(IWorld world, BlockPos pos, String tag) {
				TileEntity tileEntity = world.getTileEntity(pos);
				if (tileEntity != null)
					return tileEntity.getTileData().getDouble(tag);
				return -1;
			}
		}.getValue(world, new BlockPos(x, y, z), "LevelInspector") + 1);
		for (int index0 = 0; index0 < (int) (256 - yLvl); index0++) {
			if ((world.getBlockState(new BlockPos(x, yLvl, z))).getBlock() == StorageCrateBlock.block && (new Object() {
				public boolean getValue(IWorld world, BlockPos pos, String tag) {
					TileEntity tileEntity = world.getTileEntity(pos);
					if (tileEntity != null)
						return tileEntity.getTileData().getBoolean(tag);
					return false;
				}
			}.getValue(world, new BlockPos(x, yLvl, z), "IsInspected")) == false) {
				if (!world.isRemote()) {
					BlockPos _bp = new BlockPos(x, y, z);
					TileEntity _tileEntity = world.getTileEntity(_bp);
					BlockState _bs = world.getBlockState(_bp);
					if (_tileEntity != null)
						_tileEntity.getTileData().putDouble("LevelInspector", yLvl);
					if (world instanceof World)
						((World) world).notifyBlockUpdate(_bp, _bs, _bs, 3);
				}
				break;
			} else {
				yLvl = (yLvl + 1);
			}
		}
	}
}
