package com.thecascadian.universalarmor.client;

import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import net.minecraft.resources.ResourceLocation;

public class ClientArmorTextureHelper {

  public static ResourceLocation getTexture(
    ArmorPartsComponent parts,
    boolean inner
  ) {
    return DynamicArmorTextureManager.getOrCreateCompiledTexture(parts, inner);
  }
}
