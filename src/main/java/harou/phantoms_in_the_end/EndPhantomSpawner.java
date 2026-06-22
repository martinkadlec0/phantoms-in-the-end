package harou.phantoms_in_the_end;

import java.util.Iterator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.CustomSpawner;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndPhantomSpawner implements CustomSpawner {
	private int cooldown;

	public static final String MOD_ID = "phantoms-in-the-end";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public EndPhantomSpawner() {
	}

	public void tick(ServerLevel level, boolean spawnMonsters) {
		if (!spawnMonsters) {
			return;
		} else if (!level.getGameRules().get(GameRules.SPAWN_PHANTOMS)) {
			return;
		} else {
			RandomSource random = level.getRandom();
			--this.cooldown;
			if (this.cooldown > 0) {
				return;
			} else {
				// Cooldown is 10x smaller than PhantomSpwaner, because:
				// - we call "spawn" only every 20th tick to have less perf. impact (x20)
				// - *1.5 to compensete for no daylight
				this.cooldown += (60 + random.nextInt(60)) * 1.5;

				// Since there is no day in the end to clear phantoms, this arbitrarily limits them
				int phantomCount = level.getEntities(EntityTypes.PHANTOM, LivingEntity::isAlive).size();
				if (phantomCount >= 8) {
					return;
				}

				int timeSinceRest;
				Iterator<ServerPlayer> playerIterator = level.players().iterator();

				while(true) {
					DifficultyInstance localDifficulty;
					BlockPos blockPos2;
					BlockState blockState;
					FluidState fluidState;
					do {
						BlockPos blockPos;
						do {
							ServerPlayer serverPlayerEntity;
						
							do {
								if (!playerIterator.hasNext()) {
									return;
								}

								serverPlayerEntity = (ServerPlayer)playerIterator.next();
							} while(serverPlayerEntity.isSpectator());

							blockPos = serverPlayerEntity.blockPosition();
							localDifficulty = level.getCurrentDifficultyAt(blockPos);

							ServerStatsCounter serverStatHandler = serverPlayerEntity.getStats();
							timeSinceRest = Mth.clamp(
								serverStatHandler.getValue(Stats.CUSTOM.get(Stats.TIME_SINCE_REST)),
								1,
								Integer.MAX_VALUE
							);
						} while(random.nextInt(timeSinceRest) < 72000);

						blockPos2 = blockPos
							.above(20 + random.nextInt(15))
							.east(-10 + random.nextInt(21))
							.south(-10 + random.nextInt(21));
						blockState = level.getBlockState(blockPos2);
						fluidState = level.getFluidState(blockPos2);
					} while(!NaturalSpawner.isValidEmptySpawnBlock(
						level, blockPos2, blockState, fluidState, EntityTypes.PHANTOM
					));
					
					SpawnGroupData entityData = null;
					// Removed: Adding one to global difficulty, to limit how many phantoms spawn at once
					int spawnAmount = 1 + random.nextInt(localDifficulty.getDifficulty().getId()); 

					for(int m = 0; m < spawnAmount; ++m) {
						Phantom phantomEntity = (Phantom) EntityTypes.PHANTOM.create(level, EntitySpawnReason.NATURAL);
						if (phantomEntity != null) {
							phantomEntity.snapTo(blockPos2, 0.0F, 0.0F);
							entityData = phantomEntity.finalizeSpawn(level, localDifficulty, EntitySpawnReason.NATURAL, entityData);
							level.addFreshEntityWithPassengers(phantomEntity);
						}
					}
				}
			}
		}
	}
}
