package malte0811.recipebuffers;

import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(RecipeBuffers.MODID)
public class RecipeBuffers {
    public static final String MODID = "recipebuffers";

    public static final Logger LOGGER = LogManager.getLogger(MODID);

    public RecipeBuffers() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.CONFIG_SPEC);
    }
}
