package fi.dy.masa.minihud.mixin;

import net.minecraft.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public interface IMixinBeaconBlockEntity
{
    @Accessor("level")
    int getLevel();

    @Accessor("beamSegments")
    List<BeaconBlockEntity.BeamSegment> getBeamSegments();
}
