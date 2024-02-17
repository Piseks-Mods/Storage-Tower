package cz.pisekpiskovec.storagetower.procedures;

import net.minecraft.world.IWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;

import java.util.Map;

import cz.pisekpiskovec.storagetower.PiseksStorageTowerMod;

public class StorageCrateRedstoneLockProcedure {

	public static double executeProcedure(Map<String, Object> dependencies) {
		if (dependencies.get("world") == null) {
			if (!dependencies.containsKey("world"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency world for procedure StorageCrateRedstoneLock!");
			return 0;
		}
		if (dependencies.get("x") == null) {
			if (!dependencies.containsKey("x"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency x for procedure StorageCrateRedstoneLock!");
			return 0;
		}
		if (dependencies.get("y") == null) {
			if (!dependencies.containsKey("y"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency y for procedure StorageCrateRedstoneLock!");
			return 0;
		}
		if (dependencies.get("z") == null) {
			if (!dependencies.containsKey("z"))
				PiseksStorageTowerMod.LOGGER.warn("Failed to load dependency z for procedure StorageCrateRedstoneLock!");
			return 0;
		}
		IWorld world = (IWorld) dependencies.get("world");
		double x = dependencies.get("x") instanceof Integer ? (int) dependencies.get("x") : (double) dependencies.get("x");
		double y = dependencies.get("y") instanceof Integer ? (int) dependencies.get("y") : (double) dependencies.get("y");
		double z = dependencies.get("z") instanceof Integer ? (int) dependencies.get("z") : (double) dependencies.get("z");
		if (new Object() {
			public boolean getValue(IWorld world, BlockPos pos, String tag) {
				TileEntity tileEntity = world.getTileEntity(pos);
				if (tileEntity != null)
					return tileEntity.getTileData().getBoolean(tag);
				return false;
			}
		}.getValue(world, new BlockPos(x, y, z), "IsInspected")) {
			return 15;
		}
		return 0;
	}
}
