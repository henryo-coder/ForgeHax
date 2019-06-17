package com.matt.forgehax.asm;

import com.google.common.collect.Sets;
import com.matt.forgehax.asm.events.*;
import com.matt.forgehax.asm.events.EntityBlockSlipApplyEvent.Stage;
import com.matt.forgehax.asm.events.listeners.BlockModelRenderListener;
import com.matt.forgehax.asm.events.listeners.Listeners;
import com.matt.forgehax.asm.utils.MultiBoolean;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.multiplayer.PlayerController;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GLAllocation;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.IPacket;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
//import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.lwjgl.opengl.GL11;

public class ForgeHaxHooks implements ASMCommon {

  public static final FloatBuffer PROJECTION = GLAllocation.createDirectFloatBuffer(16);
  public static final FloatBuffer MODELVIEW = GLAllocation.createDirectFloatBuffer(16);

  /** static fields */
  public static boolean isSafeWalkActivated = false;

  public static boolean isNoSlowDownActivated = false;

  public static boolean isNoBoatGravityActivated = false;
  public static boolean isNoClampingActivated = false;
  public static boolean isBoatSetYawActivated = false;
  public static boolean isNotRowingBoatActivated = false;

  public static boolean doIncreaseTabListSize = false;

  /** static hooks */

  /** Convenient functions for firing events */
  public static void fireEvent_v(Event event) {
    MinecraftForge.EVENT_BUS.post(event);
  }

  public static boolean fireEvent_b(Event event) {
    return MinecraftForge.EVENT_BUS.post(event);
  }

  public static void setProjection() {
    PROJECTION.clear();
    MODELVIEW.clear();
    GlStateManager.getMatrix(GL11.GL_PROJECTION_MATRIX, PROJECTION);
    GlStateManager.getMatrix(GL11.GL_MODELVIEW_MATRIX, MODELVIEW);
  }

  /** onDrawBoundingBox */
  public static void onDrawBoundingBoxPost() {
    MinecraftForge.EVENT_BUS.post(new DrawBlockBoundingBoxEvent.Post());
  }


  /** onRenderBoat */
  public static float onRenderBoat(BoatEntity boat, float entityYaw) {
    RenderBoatEvent event = new RenderBoatEvent(boat, entityYaw);
    MinecraftForge.EVENT_BUS.post(event);
    return event.getYaw();
  }

  /** onSchematicaPlaceBlock */
  public static void onSchematicaPlaceBlock(ItemStack itemIn, BlockPos posIn, Vec3d vecIn) {
    MinecraftForge.EVENT_BUS.post(new SchematicaPlaceBlockEvent(itemIn, posIn, vecIn));
  }

  /** onHurtcamEffect */
  public static boolean onHurtcamEffect(float partialTicks) {
    return MinecraftForge.EVENT_BUS.post(new HurtCamEffectEvent(partialTicks));
  }

  /** onSendingPacket */

  public static boolean onSendingPacket(IPacket<?> packet) {
    return MinecraftForge.EVENT_BUS.post(new PacketEvent.Outgoing.Pre(packet));
  }

  /** onSentPacket */
  public static void onSentPacket(IPacket<?> packet) {
    MinecraftForge.EVENT_BUS.post(new PacketEvent.Outgoing.Post(packet));
  }

  /** onPreReceived */
  public static boolean onPreReceived(IPacket<?> packet) {
    return MinecraftForge.EVENT_BUS.post(new PacketEvent.Incoming.Pre(packet));
  }

  /** onPostReceived */
  public static void onPostReceived(IPacket<?> packet) {
    MinecraftForge.EVENT_BUS.post(new PacketEvent.Incoming.Post(packet));
  }

  /** onWaterMovement */
  public static boolean onWaterMovement(Entity entity, Vec3d moveDir) {
    return MinecraftForge.EVENT_BUS.post(new WaterMovementEvent(entity, moveDir));
  }

  /** onApplyCollisionMotion */
  public static boolean onApplyCollisionMotion(
      Entity entity, Entity collidedWithEntity, double x, double z) {
    return MinecraftForge.EVENT_BUS.post(new ApplyCollisionMotionEvent(entity, collidedWithEntity, x, 0.D, z));
  }

  /** onPutColorMultiplier */
  public static boolean SHOULD_UPDATE_ALPHA = false;
  public static float COLOR_MULTIPLIER_ALPHA = 150.f / 255.f;

  public static int onPutColorMultiplier(float r, float g, float b, int buffer, boolean[] flag) {
    flag[0] = SHOULD_UPDATE_ALPHA;

    if (SHOULD_UPDATE_ALPHA) {
      if (ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) {
        int red = (int) ((float) (buffer & 255) * r);
        int green = (int) ((float) (buffer >> 8 & 255) * g);
        int blue = (int) ((float) (buffer >> 16 & 255) * b);
        int alpha = (int) (((float) (buffer >> 24 & 255) * COLOR_MULTIPLIER_ALPHA));
        buffer = alpha << 24 | blue << 16 | green << 8 | red;
      } else {
        int red = (int) ((float) (buffer >> 24 & 255) * r);
        int green = (int) ((float) (buffer >> 16 & 255) * g);
        int blue = (int) ((float) (buffer >> 8 & 255) * b);
        int alpha = (int) (((float) (buffer & 255) * COLOR_MULTIPLIER_ALPHA));
        buffer = red << 24 | green << 16 | blue << 8 | alpha;
      }
    }
    return buffer;
  }

  /** onPreRenderBlockLayer */
  public static boolean onPreRenderBlockLayer(BlockRenderLayer layer, double partialTicks) {
    return MinecraftForge.EVENT_BUS.post(new RenderBlockLayerEvent.Pre(layer, partialTicks));
  }

  /** onPostRenderBlockLayer */
  public static void onPostRenderBlockLayer(BlockRenderLayer layer, double partialTicks) {
    MinecraftForge.EVENT_BUS.post(new RenderBlockLayerEvent.Post(layer, partialTicks));
  }

  /** onSetupTerrain */
  public static boolean onSetupTerrain(Entity renderEntity, boolean playerSpectator) {
    SetupTerrainEvent event = new SetupTerrainEvent(renderEntity, playerSpectator);
    MinecraftForge.EVENT_BUS.post(event);
    return event.isCulling();
  }

  /** onComputeVisibility */
  @Deprecated
  public static void onComputeVisibility(VisGraph visGraph, SetVisibility setVisibility) {
    MinecraftForge.EVENT_BUS.post(new ComputeVisibilityEvent(visGraph, setVisibility));
  }

  /** onDoBlockCollisions */
  @Deprecated
  public static boolean onDoBlockCollisions(Entity entity, BlockPos pos, BlockState state) {
    return MinecraftForge.EVENT_BUS.post(new DoBlockCollisionsEvent(entity, pos, state));
  }

  /** isBlockFiltered */
  public static final Set<Class<? extends Block>> LIST_BLOCK_FILTER = Sets.newHashSet();

  public static boolean isBlockFiltered(Entity entity, BlockState state) {
    return entity instanceof PlayerEntity
        && LIST_BLOCK_FILTER.contains(state.getBlock().getClass());
  }

  /** onApplyClimbableBlockMovement */
  @Deprecated
  public static boolean onApplyClimbableBlockMovement(LivingEntity livingBase) {
    return MinecraftForge.EVENT_BUS.post(new ApplyClimbableBlockMovement(livingBase));
  }

  /** onRenderBlockInLayer */
  /*public static final HookReporter HOOK_onRenderBlockInLayer =
      newHookReporter()
          .hook("onRenderBlockInLayer")
          .dependsOn(TypesMc.Methods.Block_canRenderInLayer)
          .forgeEvent(RenderBlockInLayerEvent.class)
          .build();

  public static BlockRenderLayer onRenderBlockInLayer(
      Block block, IBlockState state, BlockRenderLayer layer, BlockRenderLayer compareToLayer) {
    if (HOOK_onRenderBlockInLayer.reportHook()) {
      RenderBlockInLayerEvent event =
          new RenderBlockInLayerEvent(block, state, layer, compareToLayer);
      MinecraftForge.EVENT_BUS.post(event);
      return event.getLayer();
    } else return layer;
  }*/

  /** onBlockRender */
  @Deprecated
  public static void onBlockRender(BlockPos pos, BlockState state, IWorldReader access, BufferBuilder buffer) {
      MinecraftForge.EVENT_BUS.post(new BlockRenderEvent(pos, state, access, buffer));
  }

  /** onBlockRenderInLoop */
  public static void onBlockRenderInLoop(
      ChunkRender renderChunk, Block block, BlockState state, BlockPos pos) {
    // faster hook
    for (BlockModelRenderListener listener : Listeners.BLOCK_MODEL_RENDER_LISTENER.getAll())
      listener.onBlockRenderInLoop(renderChunk, block, state, pos);
  }

  /** onPreBuildChunk */
  public static void onPreBuildChunk(ChunkRender renderChunk) {
    MinecraftForge.EVENT_BUS.post(new BuildChunkEvent.Pre(renderChunk));
  }

  /** onPostBuildChunk */
  public static void onPostBuildChunk(ChunkRender renderChunk) {
    // i couldn't place a post block render hook within the if label so I have to do this
    MinecraftForge.EVENT_BUS.post(new BuildChunkEvent.Post(renderChunk));
  }

  /** onDeleteGlResources */
  public static void onDeleteGlResources(ChunkRender renderChunk) {
    MinecraftForge.EVENT_BUS.post(new DeleteGlResourcesEvent(renderChunk));
  }

  /** onAddRenderChunk */
  public static void onAddRenderChunk(ChunkRender renderChunk, BlockRenderLayer layer) {
    MinecraftForge.EVENT_BUS.post(new AddRenderChunkEvent(renderChunk, layer));
  }

  /** onChunkUploaded */
  public static void onChunkUploaded(ChunkRender chunk, BufferBuilder buffer) {
    MinecraftForge.EVENT_BUS.post(new ChunkUploadedEvent(chunk, buffer));
  }

  /** onLoadRenderers */
  public static void onLoadRenderers(
      ViewFrustum viewFrustum, ChunkRenderDispatcher renderDispatcher) {
      MinecraftForge.EVENT_BUS.post(new LoadRenderersEvent(viewFrustum, renderDispatcher));
  }

  /** onWorldRendererDeallocated */
  /*public static final HookReporter HOOK_onWorldRendererDeallocated =
      newHookReporter()
          .hook("onWorldRendererDeallocated")
          .dependsOn(TypesMc.Methods.ChunkRenderWorker_freeRenderBuilder)
          .forgeEvent(WorldRendererDeallocatedEvent.class)
          .build();

  public static void onWorldRendererDeallocated(ChunkCompileTaskGenerator generator) {
    if (HOOK_onWorldRendererDeallocated.reportHook())
      MinecraftForge.EVENT_BUS.post(
          new WorldRendererDeallocatedEvent(generator, generator.getRenderChunk()));
  }*/

  /** shouldDisableCaveCulling */
  public static final MultiBoolean SHOULD_DISABLE_CAVE_CULLING = new MultiBoolean();

  public static boolean shouldDisableCaveCulling() {
    return SHOULD_DISABLE_CAVE_CULLING.isEnabled();
  }

  /** onUpdateWalkingPlayerPre */
  public static boolean onUpdateWalkingPlayerPre(ClientPlayerEntity localPlayer) {
    return MinecraftForge.EVENT_BUS.post(new LocalPlayerUpdateMovementEvent.Pre(localPlayer));
  }

  /** onUpdateWalkingPlayerPost */
  public static void onUpdateWalkingPlayerPost(ClientPlayerEntity localPlayer) {
    MinecraftForge.EVENT_BUS.post(new LocalPlayerUpdateMovementEvent.Post(localPlayer));
  }


  /** onLeftClickCounterSet */
  public static int onLeftClickCounterSet(int value, Minecraft minecraft) {
    LeftClickCounterUpdateEvent event = new LeftClickCounterUpdateEvent(minecraft, value);
    return MinecraftForge.EVENT_BUS.post(event) ? event.getCurrentValue() : event.getValue();
  }

  /** onSendClickBlockToController */
  public static boolean onSendClickBlockToController(Minecraft minecraft, boolean clicked) {
    BlockControllerProcessEvent event = new BlockControllerProcessEvent(minecraft, clicked);
    MinecraftForge.EVENT_BUS.post(event);
    return event.isLeftClicked();
  }

  /** onPlayerItemSync */
  public static void onPlayerItemSync(PlayerController playerControllerMP) {
    MinecraftForge.EVENT_BUS.post(new PlayerSyncItemEvent(playerControllerMP));
  }

  /** onPlayerBreakingBlock */
  public static void onPlayerBreakingBlock(PlayerController playerControllerMP, BlockPos pos, Direction facing) {
    MinecraftForge.EVENT_BUS.post(new PlayerDamageBlockEvent(playerControllerMP, pos, facing));
  }

  /** onPlayerAttackEntity */
  public static void onPlayerAttackEntity(PlayerController playerControllerMP, PlayerEntity attacker, Entity victim) {
    MinecraftForge.EVENT_BUS.post(new PlayerAttackEntityEvent(playerControllerMP, attacker, victim));
  }

  /** onPlayerStopUse */
  public static boolean onPlayerStopUse(
      PlayerController playerControllerMP, PlayerEntity player) {
    return MinecraftForge.EVENT_BUS.post(new ItemStoppedUsedEvent(playerControllerMP, player));
  }

  /** onPlayerStopUse */
  public static float onEntityBlockSlipApply(
      float defaultSlipperiness,
      LivingEntity entityLivingBase,
      BlockState blockStateUnder,
      int stage)
  {

    EntityBlockSlipApplyEvent event =
        new EntityBlockSlipApplyEvent(
            Stage.values()[stage], entityLivingBase, blockStateUnder, defaultSlipperiness);
    MinecraftForge.EVENT_BUS.post(event);
    return event.getSlipperiness();
  }
}
