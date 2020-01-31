package dev.fiki.forgehax.main.mods;

import dev.fiki.forgehax.common.events.packet.PacketInboundEvent;
import dev.fiki.forgehax.main.Globals;
import dev.fiki.forgehax.main.util.reflection.FastReflection;
import dev.fiki.forgehax.main.events.LocalPlayerUpdateEvent;
import dev.fiki.forgehax.main.util.command.Setting;
import dev.fiki.forgehax.main.util.mod.Category;
import dev.fiki.forgehax.main.util.mod.ToggleMod;
import dev.fiki.forgehax.main.util.mod.loader.RegisterMod;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.play.server.SPlaySoundEffectPacket;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Created on 9/2/2016 by fr1kin
 */
@RegisterMod
public class AutoFishMod extends ToggleMod {
  
  private int ticksCastDelay = 0;
  private int ticksHookDeployed = 0;
  
  private boolean previouslyHadRodEquipped = false;
  
  public final Setting<Integer> casting_delay =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("casting_delay")
          .description("Number of ticks to wait after casting the rod to attempt a recast")
          .defaultTo(20)
          .build();
  
  public final Setting<Double> max_sound_distance =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("max_sound_distance")
          .description(
              "Maximum distance between the splash sound and hook entity allowed (set to 0 to disable this feature)")
          .defaultTo(2.D)
          .build();
  
  public final Setting<Integer> fail_safe_time =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("fail_safe_time")
          .description(
              "Maximum amount of time (in ticks) allowed until the hook is pulled in (set to 0 to disable this feature)")
          .defaultTo(600)
          .build();
  
  public AutoFishMod() {
    super(Category.PLAYER, "AutoFish", false, "Auto fish");
  }
  
  private boolean isCorrectSplashPacket(SPlaySoundEffectPacket packet) {
    ClientPlayerEntity me = Globals.getLocalPlayer();
    return packet.getSound().equals(SoundEvents.ENTITY_FISHING_BOBBER_SPLASH)
        && (me != null
        && me.fishingBobber != null
        && (max_sound_distance.get() == 0
        || // disables this check
        (me.fishingBobber.getPositionVector()
            .distanceTo(new Vec3d(packet.getX(), packet.getY(), packet.getZ())) <= max_sound_distance.get())));
  }
  
  private void rightClick() {
    if (ticksCastDelay <= 0) { // to prevent the fishing rod from being spammed when in hand
      FastReflection.Methods.Minecraft_rightClickMouse.invoke(Globals.MC);
      ticksCastDelay = casting_delay.get();
    }
  }
  
  private void resetLocals() {
    ticksCastDelay = 0;
    ticksHookDeployed = 0;
    previouslyHadRodEquipped = false;
  }
  
  @Override
  public void onEnabled() {
    resetLocals();
  }
  
  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    ClientPlayerEntity me = Globals.getLocalPlayer();
    ItemStack heldStack = me.getHeldItemMainhand();
    
    // update tick delay if hook is deployed
    if (ticksCastDelay > casting_delay.get()) {
      ticksCastDelay = casting_delay.get(); // greater than current delay, set to the current delay
    } else if (ticksCastDelay > 0) {
      --ticksCastDelay;
    }
    
    // check if player is holding a fishing rod
    if (Items.FISHING_ROD.equals(heldStack.getItem())) { // item being held is a fishing rod {
      if (!previouslyHadRodEquipped) {
        ticksCastDelay = casting_delay.get();
        previouslyHadRodEquipped = true;
      } else if (me.fishingBobber == null) { // no hook is deployed
        // cast hook
        rightClick();
      } else { // hook is deployed and rod was not previously equipped
        // increment the number of ticks that the hook entity has existed
        ++ticksHookDeployed;
        
        if (fail_safe_time.get() != 0 && (ticksHookDeployed > fail_safe_time.get())) {
          rightClick(); // reel in hook if the fail safe time has passed
          resetLocals();
        }
      }
    } else {
      resetLocals();
    }
  }
  
  @SubscribeEvent
  public void onMouseEvent(InputEvent.MouseInputEvent event) {
    if (Globals.getGameSettings().keyBindUseItem.isKeyDown() && ticksHookDeployed > 0) {
      ticksCastDelay = casting_delay.get();
    }
  }
  
  @SubscribeEvent
  public void onPacketIncoming(PacketInboundEvent event) {
    if (event.getPacket() instanceof SPlaySoundEffectPacket) {
      SPlaySoundEffectPacket packet = (SPlaySoundEffectPacket) event.getPacket();
      if (isCorrectSplashPacket(packet)) {
        rightClick();
      }
    }
  }
}