package dev.fiki.forgehax.main.mods;

import dev.fiki.forgehax.main.events.ClientTickEvent;
import dev.fiki.forgehax.main.Globals;
import dev.fiki.forgehax.main.util.command.Setting;
import dev.fiki.forgehax.main.util.mod.Category;
import dev.fiki.forgehax.main.util.mod.ToggleMod;
import dev.fiki.forgehax.main.util.mod.loader.RegisterMod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

@RegisterMod
public class FPSLock extends ToggleMod {
  
  private final Setting<Integer> defaultFps =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("default-fps")
          .description("default FPS to revert to")
          .defaultTo(Globals.getGameSettings().framerateLimit)
          .min(1)
          .build();
  
  private final Setting<Integer> fps =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("fps")
          .description("FPS to use when the world is loaded. Set to 0 to disable.")
          .min(0)
          .defaultTo(0)
          .build();
  private final Setting<Integer> menu_fps =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("menu-fps")
          .description("FPS when the GUI is opened. Set to 0 to disable.")
          .min(0)
          .defaultTo(60)
          .build();
  
  private final Setting<Integer> no_focus_fps =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("no-focus-fps")
          .description("FPS when the game window doesn't have focus. Set to 0 to disable.")
          .min(0)
          .defaultTo(3)
          .build();
  
  public FPSLock() {
    super(
        Category.MISC,
        "FPSLock",
        false,
        "Lock the fps to a lower-than-allowed value, and restore when disabled");
  }
  
  private int getFps() {
    if (no_focus_fps.get() > 0
        && GLFW.glfwGetWindowAttrib(Globals.getMainWindow().getHandle(), GLFW.GLFW_FOCUSED) == GLFW.GLFW_FALSE) {
      return no_focus_fps.get();
    } else if (Globals.MC.currentScreen != null) {
      return menu_fps.get() > 0 ? menu_fps.get() : defaultFps.get();
    } else {
      return fps.get() > 0 ? fps.get() : defaultFps.get();
    }
  }
  
  @Override
  protected void onDisabled() {
    Globals.getGameSettings().framerateLimit = defaultFps.get();
  }
  
  @SubscribeEvent
  void onTick(ClientTickEvent.Pre event) {
    Globals.getGameSettings().framerateLimit = getFps();
  }
}