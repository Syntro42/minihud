package fi.dy.masa.minihud.mixin;

import com.google.common.collect.Lists;
import fi.dy.masa.minihud.renderer.OverlayRendererBeaconRange;
import net.minecraft.block.entity.BeaconBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public class MixinBeaconBlockEntity
{
    @Shadow private int level;
    @Shadow private List<BeaconBlockEntity.BeamSegment> beamSegments;

    private int levelsPre;
    private boolean blocked;

    @Inject(method = "updateLevel", at = @At("HEAD"))
    private void onUpdateSegmentsPre(int x, int y, int z, CallbackInfo ci)
    {
        this.levelsPre = this.level;
    }

    @Inject(method = "updateLevel", at = @At("RETURN"))
    private void onUpdateSegmentsPost(int x, int y, int z, CallbackInfo ci)
    {
        if (this.level != this.levelsPre)
        {
            OverlayRendererBeaconRange.setNeedsUpdate();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void onBeaconBlocked(CallbackInfo ci)
    {
        if (this.beamSegments.isEmpty() && blocked == false)
        {
            OverlayRendererBeaconRange.setNeedsUpdate();
            blocked = true;
        }
        else if (this.beamSegments.isEmpty() == false && blocked)
        {
            OverlayRendererBeaconRange.setNeedsUpdate();
            blocked = false;
        }
    }

    @Inject(method = "markRemoved", at = @At("RETURN"))
    private void onBeaconRemoved(CallbackInfo ci)
    {
        OverlayRendererBeaconRange.setNeedsUpdate();
    }
}
