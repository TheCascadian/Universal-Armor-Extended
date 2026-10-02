package com.thecascadian.universalarmor.client;

import com.thecascadian.universalarmor.client.screen.ArmoryGuideScreen;
import net.minecraft.client.Minecraft;

public class ClientGuideHelper {

  public static void open() {
    Minecraft mc = Minecraft.getInstance();
    mc.setScreen(new ArmoryGuideScreen(mc.screen));
  }
}
