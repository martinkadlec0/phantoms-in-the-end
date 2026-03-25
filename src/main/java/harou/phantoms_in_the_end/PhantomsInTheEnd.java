package harou.phantoms_in_the_end;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.level.Level;

public class PhantomsInTheEnd implements ModInitializer {
	public static final String MOD_ID = "phantoms-in-the-end";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final EndPhantomSpawner endPhantomSpawner = new EndPhantomSpawner();
	
	@Override
	public void onInitialize() {
		LOGGER.info("PhantomsInTheEnd initialized");

		// Add custom phantom spawning in the End
		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			if (level.dimension() == Level.END) {
				if (level.getGameTime() % 20 == 0) {
					endPhantomSpawner.tick(level, level.isSpawningMonsters());
				}
			}
		});
	}
}