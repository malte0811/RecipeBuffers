package malte0811.recipebuffers;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    public static final ForgeConfigSpec.BooleanValue dumpPacket;
    public static final ForgeConfigSpec.BooleanValue logPacketStats;
    public static final ForgeConfigSpec.BooleanValue runSerializerInSingleplayer;
    public static final ForgeConfigSpec.BooleanValue writeRecipeLength;
    public static final ForgeConfigSpec.IntValue debugLogLevel;
    public static final ForgeConfigSpec CONFIG_SPEC;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        dumpPacket = b.comment("Write the recipe packet to a file. Useful for debugging.")
                .define("dumpPacket", false);
        logPacketStats = b.comment("Write statistics of the recipe packet size to the log")
                .define("logPacketStats", true);
        debugLogLevel = b.comment("Controls different levels of debug output that can be useful for debugging issues")
                .defineInRange("debugLogLevel", 0, 0, 2);
        runSerializerInSingleplayer = b.comment(
                "Set to true to serialize and deserialize the recipe packet even in singleplayer.",
                "This is useless for anything but debugging issues"
        )
                .define("runSerializerInSingleplayer", false);
        writeRecipeLength = b.comment(
                "Prefix recipes by their length in the packet. This allows early detection of broken serializers")
                .define("writeRecipeLength", false);
        CONFIG_SPEC = b.build();
    }
}
