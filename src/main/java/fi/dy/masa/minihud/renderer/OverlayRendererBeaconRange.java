package fi.dy.masa.minihud.renderer;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.config.RendererToggle;
import fi.dy.masa.minihud.mixin.IMixinBeaconBlockEntity;
import fi.dy.masa.minihud.mixin.IMixinWorld;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.*;
import net.minecraft.entity.Entity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockEntityTickInvoker;
import net.minecraft.world.chunk.Chunk;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.minecraft.world.Heightmap.Type.WORLD_SURFACE;

public class OverlayRendererBeaconRange extends OverlayRendererBase
{
    private static final Set<BlockPos> BEACON_POSITIONS = new HashSet<>();
    private static final Set<ChunkPos> BEACON_CHUNKS = new HashSet<>();

    private static boolean needsUpdate;

    public static void clear()
    {
        synchronized (BEACON_POSITIONS)
        {
            BEACON_CHUNKS.clear();
            BEACON_POSITIONS.clear();
        }
    }

    public static void setNeedsUpdate()
    {
        if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue() == false)
        {
            clear();
        }

        needsUpdate = true;
    }

    public static void checkNeedsUpdate(BlockPos pos, BlockState state)
    {
        synchronized (BEACON_POSITIONS)
        {
            if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue() &&
                    (state.getBlock() == Blocks.BEACON || BEACON_POSITIONS.contains(pos)))
            {
                setNeedsUpdate();
            }
        }
    }

    public static void checkNeedsUpdate(ChunkPos chunkPos)
    {
        synchronized (BEACON_POSITIONS)
        {
            if (RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue() &&
                    BEACON_CHUNKS.contains(chunkPos))
            {
                setNeedsUpdate();
            }
        }
    }

    @Override
    public boolean shouldRender(MinecraftClient mc)
    {
        return RendererToggle.OVERLAY_BEACON_RANGE.getBooleanValue();
    }

    @Override
    public boolean needsUpdate(Entity entity, MinecraftClient mc)
    {
        if (needsUpdate || this.lastUpdatePos == null)
        {
            return true;
        }

        return false;
    }

    @Override
    public void update(Vec3d cameraPos, Entity entity, MinecraftClient mc)
    {
        clear();

        RenderObjectBase renderQuads = this.renderObjects.get(0);
        RenderObjectBase renderLines = this.renderObjects.get(1);
        BUFFER_1.begin(renderQuads.getGlMode(), VertexFormats.POSITION_COLOR);
        BUFFER_2.begin(renderLines.getGlMode(), VertexFormats.POSITION_COLOR);

        synchronized (BEACON_POSITIONS)
        {
            this.renderBeaconRanges(entity.getEntityWorld(), cameraPos, BUFFER_1, BUFFER_2);
        }

        BUFFER_1.end();
        BUFFER_2.end();
        renderQuads.uploadData(BUFFER_1);
        renderLines.uploadData(BUFFER_2);

        needsUpdate = false;
    }

    @Override
    public void allocateGlResources()
    {
        this.allocateBuffer(VertexFormat.DrawMode.QUADS);
        this.allocateBuffer(VertexFormat.DrawMode.DEBUG_LINES);
    }

    protected static Color4f getColorForLevel(int level)
    {
        switch (level)
        {
            case 1: return Configs.Colors.BEACON_RANGE_LVL1_OVERLAY_COLOR.getColor();
            case 2: return Configs.Colors.BEACON_RANGE_LVL2_OVERLAY_COLOR.getColor();
            case 3: return Configs.Colors.BEACON_RANGE_LVL3_OVERLAY_COLOR.getColor();
            default: return Configs.Colors.BEACON_RANGE_LVL4_OVERLAY_COLOR.getColor();
        }
    }

    protected void renderBeaconRanges(World world, Vec3d cameraPos, BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        List<BlockEntityTickInvoker> blockEntities = ((IMixinWorld) world).getBlockEntities();

        for (BlockEntityTickInvoker blockEntity : blockEntities)
        {
            BlockPos pos = blockEntity.getPos();
            BlockEntity be = world.getBlockEntity(pos);

            if (be instanceof BeaconBlockEntity)
            {
                List<BeaconBlockEntity.BeamSegment> beamSegments = ((BeaconBlockEntity) be).getBeamSegments();
                int level = ((IMixinBeaconBlockEntity) be).getLevel();

                if (beamSegments.isEmpty())
                {
                    level = 0;
                }

                if (level >= 1 && level <= 4)
                {
                    this.renderBeaconBox(world, pos, cameraPos, level, getColorForLevel(level), bufferQuads, bufferLines);
                }
            }
        }
    }

    protected void renderBeaconBox(World world, BlockPos pos, Vec3d cameraPos, int level, Color4f color, BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();

        int range = level * 10 + 10;
        double minX = x - range - cameraPos.x;
        double minY = y - range - cameraPos.y;
        double minZ = z - range - cameraPos.z;
        double maxX = x + range + 1 - cameraPos.x;
        double maxY = this.getMaxHeight(world, pos, range) - cameraPos.y;
        double maxZ = z + range + 1 - cameraPos.z;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, bufferQuads);
        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 1f), bufferLines);

        BEACON_POSITIONS.add(pos);
        BEACON_CHUNKS.add(new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4));
    }

    protected int getMaxHeight(World world, BlockPos pos, int range)
    {
        final int minX = pos.getX() - range;
        final int minZ = pos.getZ() - range;
        final int maxX = pos.getX() + range;
        final int maxZ = pos.getZ() + range;

        final int minCX = minX >> 4;
        final int minCZ = minZ >> 4;
        final int maxCX = maxX >> 4;
        final int maxCZ = maxZ >> 4;
        int maxY = 0;

        for (int cz = minCZ; cz <= maxCZ; ++cz)
        {
            for (int cx = minCX; cx <= maxCX; ++cx)
            {
                final int xMin = Math.max(minX,  cx << 4      );
                final int zMin = Math.max(minZ,  cz << 4      );
                final int xMax = Math.min(maxX, (cx << 4) + 15);
                final int zMax = Math.min(maxZ, (cz << 4) + 15);
                Chunk chunk = world.getChunk(cx, cz);

                for (int z = zMin; z <= zMax; ++z)
                {
                    for (int x = xMin; x <= xMax; ++x)
                    {
                        int height = chunk.sampleHeightmap(WORLD_SURFACE, x, z);

                        if (height > maxY)
                        {
                            maxY = height;
                        }
                    }
                }
            }
        }

        return maxY + 4;
    }

    public static void renderBeaconBoxForPlayerIfHoldingItem(ClientPlayerEntity player, double dx, double dy, double dz)
    {
        Item item = player.getMainHandStack().getItem();

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON)
        {
            renderBeaconBoxForPlayer(player, dx, dy, dz);
            return;
        }

        item = player.getOffHandStack().getItem();

        if (item instanceof BlockItem && ((BlockItem) item).getBlock() == Blocks.BEACON)
        {
            renderBeaconBoxForPlayer(player, dx, dy, dz);
        }
    }

    private static void renderBeaconBoxForPlayer(ClientPlayerEntity player, double dx, double dy, double dz)
    {
        double x = Math.floor(player.getX()) - dx;
        double y = Math.floor(player.getY()) - dy;
        double z = Math.floor(player.getZ()) - dz;
        // Use the slot number as the level if sneaking
        int level = player.isSneaking() ? Math.min(4, player.getInventory().selectedSlot + 1) : 4;
        double range = level * 10 + 10;
        double minX = x - range;
        double minY = y - range;
        double minZ = z - range;
        double maxX = x + range + 1;
        double maxY = y + 4;
        double maxZ = z + range + 1;
        Color4f color = getColorForLevel(level);

        RenderSystem.disableTexture();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.polygonOffset(-3f, -3f);
        RenderSystem.enablePolygonOffset();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        fi.dy.masa.malilib.render.RenderUtils.setupBlend();
        fi.dy.masa.malilib.render.RenderUtils.color(1f, 1f, 1f, 1f);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 0.3f), buffer);

        tessellator.draw();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, Color4f.fromColor(color, 1f), buffer);

        tessellator.draw();

        RenderSystem.polygonOffset(0f, 0f);
        RenderSystem.disablePolygonOffset();
        RenderSystem.enableCull();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
