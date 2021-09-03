package fi.dy.masa.minihud.mixin;

import fi.dy.masa.minihud.renderer.OverlayRendererBeaconRange;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(BeaconBlockEntity.class)
public class MixinBeaconBlockEntity
{
    private static int levelsPre;
    private static boolean blocked;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void onUpdateSegmentsPre(World world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity, CallbackInfo ci)
    {
        levelsPre = ((IMixinBeaconBlockEntity) blockEntity).getLevel();
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void onUpdateSegmentsPost(World world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity, CallbackInfo ci)
    {
        if (((IMixinBeaconBlockEntity) blockEntity).getLevel() != levelsPre)
        {
            OverlayRendererBeaconRange.setNeedsUpdate();
        }
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private static void onBeaconBlocked(World world, BlockPos pos, BlockState state, BeaconBlockEntity blockEntity, CallbackInfo ci)
    {
        if (((IMixinBeaconBlockEntity) blockEntity).getBeamSegments().isEmpty() && blocked == false)
        {
            OverlayRendererBeaconRange.setNeedsUpdate();
            blocked = true;
        }
        else if (((IMixinBeaconBlockEntity) blockEntity).getBeamSegments().isEmpty() == false && blocked)
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
