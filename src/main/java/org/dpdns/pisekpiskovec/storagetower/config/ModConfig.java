package org.dpdns.pisekpiskovec.storagetower.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.Builder;
import net.minecraftforge.common.ForgeConfigSpec.IntValue;

public class ModConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Builder builder = new Builder();
        COMMON = new Common(builder);
        COMMON_SPEC = builder.build();
    }

    public static class Common {
        public final IntValue maxScanDistance;

        Common(ForgeConfigSpec.Builder builder) {
            builder.push("storage_tower");

            maxScanDistance = builder.comment("Maximal network search distance in a column").defineInRange("maxScanDistance", 64, 0, 383);

            builder.pop();
        }
    }
}
