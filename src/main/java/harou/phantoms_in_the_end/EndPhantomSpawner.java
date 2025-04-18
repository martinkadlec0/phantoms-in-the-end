package harou.phantoms_in_the_end;

import java.util.Iterator;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.PhantomEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.ServerStatHandler;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.spawner.SpecialSpawner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EndPhantomSpawner implements SpecialSpawner {
   private int cooldown;

   public static final String MOD_ID = "phantoms-in-the-end";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

   public EndPhantomSpawner() {
   }

   public int spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals) {
      if (!spawnMonsters) {
         return 0;
      } else if (!world.getGameRules().getBoolean(GameRules.DO_INSOMNIA)) {
         return 0;
      } else {
   
        

         Random random = world.random;
         --this.cooldown;
         if (this.cooldown > 0) {
            return 0;
         } else {
            // Cooldown is 10x smaller than PhantomSpwaner, because:
            // - we call "spawn" only every 20th tick to have less perf. impact (x20)
            // - *1.5 to compensete for no daylight
            this.cooldown += (60 + random.nextInt(60)) * 1.5;

            // Since there is no day in the end to clear phantoms, this arbitrarily limits them
            int phantomCount = world.getEntitiesByType(EntityType.PHANTOM, LivingEntity::isAlive).size();
            if (phantomCount >= 8) {
               return 0;
            }

            int succesfulSpawnAttempts = 0;
            int timeSinceRest;
            Iterator<ServerPlayerEntity> playerIterator = world.getPlayers().iterator();

            while(true) {
               LocalDifficulty localDifficulty;
               BlockPos blockPos2;
               BlockState blockState;
               FluidState fluidState;
               do {
                  BlockPos blockPos;
                  do {
                     ServerPlayerEntity serverPlayerEntity;
                   
                     do {
                        if (!playerIterator.hasNext()) {
                           return succesfulSpawnAttempts;
                        }

                        serverPlayerEntity = (ServerPlayerEntity)playerIterator.next();
                     } while(serverPlayerEntity.isSpectator());

                     blockPos = serverPlayerEntity.getBlockPos();
                     localDifficulty = world.getLocalDifficulty(blockPos);

                     ServerStatHandler serverStatHandler = serverPlayerEntity.getStatHandler();
                     timeSinceRest = MathHelper.clamp(
                        serverStatHandler.getStat(Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST)),
                        1,
                        Integer.MAX_VALUE
                     );
                  } while(random.nextInt(timeSinceRest) < 72000);

                  blockPos2 = blockPos.up(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
                  blockState = world.getBlockState(blockPos2);
                  fluidState = world.getFluidState(blockPos2);
               } while(!SpawnHelper.isClearForSpawn(world, blockPos2, blockState, fluidState, EntityType.PHANTOM));
               
               EntityData entityData = null;
               // Removed: Adding one to global difficulty, to limit how many phantoms spawn at once
               int spawnAmount = 1 + random.nextInt(localDifficulty.getGlobalDifficulty().getId()); 

               for(int m = 0; m < spawnAmount; ++m) {
                  PhantomEntity phantomEntity = (PhantomEntity)EntityType.PHANTOM.create(world, SpawnReason.NATURAL);
                  if (phantomEntity != null) {
                     phantomEntity.refreshPositionAndAngles(blockPos2, 0.0F, 0.0F);
                     entityData = phantomEntity.initialize(world, localDifficulty, SpawnReason.NATURAL, entityData);
                     world.spawnEntityAndPassengers(phantomEntity);
                     ++succesfulSpawnAttempts;
                  }
               }
            }
         }
      }
   }
}
