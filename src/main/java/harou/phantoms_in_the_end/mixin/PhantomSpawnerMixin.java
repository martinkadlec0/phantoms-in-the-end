package harou.phantoms_in_the_end.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.spawner.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(PhantomSpawner.class)
public abstract class PhantomSpawnerMixin {
    /**
     * @author phantoms_in_the_end
     * @reason Completely override vanilla behavior to prevent all overworld phantom spawning
     */
    @Overwrite
    public void spawn(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals) {
        return; // Always return 0 (no phantoms spawned)
    }
}