package cech12.bucketlib;

import cech12.bucketlib.api.BucketLibApi;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.stream.Collectors;

@Mod(BucketLibApi.MODID)
public class BucketLib {

    private static final Logger LOGGER = LogManager.getLogger();

    public BucketLib() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
    }

    private void processIMC(final InterModProcessEvent event) {
        //TODO
        LOGGER.info("Got IMC {}", event.getIMCStream().
                map(m->m.messageSupplier().get()).
                collect(Collectors.toList()));
    }

}
