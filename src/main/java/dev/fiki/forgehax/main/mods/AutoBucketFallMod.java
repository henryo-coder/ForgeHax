package dev.fiki.forgehax.main.mods;

import static dev.fiki.forgehax.main.util.entity.EntityUtils.isAboveWater;
import static net.minecraft.util.math.RayTraceResult.Type;

import dev.fiki.forgehax.main.Globals;
import dev.fiki.forgehax.main.util.command.Setting;
import dev.fiki.forgehax.main.util.entity.EntityUtils;
import dev.fiki.forgehax.main.util.entity.LocalPlayerInventory;
import dev.fiki.forgehax.main.util.mod.Category;
import dev.fiki.forgehax.main.util.mod.ToggleMod;
import dev.fiki.forgehax.main.util.mod.loader.RegisterMod;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.client.CPlayerPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Created by Babbaj on 9/4/2017. TODO: check all 4 collision box corners
 */
@RegisterMod
public class AutoBucketFallMod extends ToggleMod {
  
  public AutoBucketFallMod() {
    super(Category.PLAYER, "AutoBucket", false, "Automatically place bucket to reset fall damage");
  }
  
  public final Setting<Double> preHeight =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("PreHeight")
          .description("how far below to check before preparing")
          .defaultTo(10D)
          .build();
  public final Setting<Double> settingFallHeight =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("height")
          .description("minimum fall distance to work")
          .defaultTo(15D)
          .build();
  
  private ItemStack WATER_BUCKET = new ItemStack(Items.WATER_BUCKET);
  
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (Globals.getLocalPlayer() == null
        || Globals.getLocalPlayer().fallDistance < settingFallHeight.getAsDouble()
        || !Globals.getLocalPlayer().inventory.hasItemStack(WATER_BUCKET)
        || EntityUtils.isInWater(Globals.getLocalPlayer())
        || EntityUtils.isAboveWater(Globals.getLocalPlayer())) {
      return;
    }
    
    Vec3d playerPos = Globals.getLocalPlayer().getPositionVector();
    Vec3d rayTraceBucket = new Vec3d(playerPos.x, playerPos.y - 5, playerPos.z);
    Vec3d rayTracePre =
        new Vec3d(
            playerPos.x,
            playerPos.y - preHeight.getAsDouble(),
            playerPos.z); // find the ground before the player is ready to water bucket

    RayTraceContext ctx = new RayTraceContext(playerPos, rayTraceBucket,
        RayTraceContext.BlockMode.COLLIDER,
        RayTraceContext.FluidMode.NONE,
        Globals.getLocalPlayer());

    BlockRayTraceResult result = Globals.getWorld().rayTraceBlocks(ctx);

    ctx = new RayTraceContext(playerPos, rayTracePre,
        RayTraceContext.BlockMode.COLLIDER,
        RayTraceContext.FluidMode.NONE,
        Globals.getLocalPlayer());

    BlockRayTraceResult resultPre = Globals.getWorld().rayTraceBlocks(ctx);
    
    if (Type.BLOCK.equals(resultPre.getType())
        && !Globals.getWorld().getBlockState(resultPre.getPos()).getMaterial().isLiquid()) {
      // set the pitch early to not get cucked by ncp
      Globals.getLocalPlayer().prevRotationPitch = 90f;
      Globals.getLocalPlayer().rotationPitch = 90f;
      
      int bucketSlot = findBucketHotbar();
      if (bucketSlot == -1) {
        bucketSlot = findBucketInv();
      }
      if (bucketSlot > 8) {
        swap(bucketSlot, Globals.getLocalPlayer().inventory.currentItem); // move bucket from inventory to hotbar
      } else {
        Globals.getLocalPlayer().inventory.currentItem = bucketSlot;
      }
    }
    
    if (Type.BLOCK.equals(result.getType())
        && !Globals.getWorld().getBlockState(result.getPos()).getMaterial().isLiquid()) {
      Globals.sendNetworkPacket(new CPlayerPacket.RotationPacket(
          Globals.getLocalPlayer().rotationYaw,
          90,
          Globals.getLocalPlayer().onGround));

      // probably unnecessary but doing it anyways
      Globals.getLocalPlayer().prevRotationPitch = 90f;
      Globals.getLocalPlayer().rotationPitch = 90f;
      
      // printMessage("Attempted to place water bucket");
      Globals.getPlayerController().processRightClick(Globals.getLocalPlayer(), Globals.getWorld(), Hand.MAIN_HAND);
    }
  }
  
  private int findBucketInv() {
    return Globals.getLocalPlayer().inventory.getSlotFor(WATER_BUCKET); // find bucket in entire inventory
  }
  
  private int findBucketHotbar() {
    for (int i = 0; i < 9; i++) // iterate through hotbar slots
    {
      if (Globals.getLocalPlayer().inventory.getStackInSlot(i).getItem() == Items.WATER_BUCKET) {
        return i;
      }
    }
    return -1;
  }
  
  private void swap(final int slot, final int hotbarNum) {
    Globals.getPlayerController().windowClick(LocalPlayerInventory.getContainer().windowId,
        slot, hotbarNum, ClickType.SWAP, Globals.getLocalPlayer());
  }
}