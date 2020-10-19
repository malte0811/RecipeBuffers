package malte0811.recipebuffers;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.BooleanValue dumpPacket;
    public static final ForgeConfigSpec.BooleanValue logPacketStats;
    public static final ForgeConfigSpec CONFIG_SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        dumpPacket = b.comment("Write the recipe packet to a file. Useful for debugging.")
                .define("dumpPacket", false);
        logPacketStats = b.comment("Write statistics of the recipe packet size to the log")
                .define("logPacketStats", true);
        CONFIG_SPEC = b.build();
    }
}
