package com.thecascadian.universalarmor.util;

import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class DynamicStatsCalculator {

  public static int getDurabilityMultiplier(Item item) {
    String name = BuiltInRegistries.ITEM.getKey(item).getPath();
    if (name.contains("netherite") || name.contains("obsidian")) return 37;
    if (
      name.contains("diamond") ||
      name.contains("steel") ||
      name.contains("titanium")
    ) return 33;
    if (
      name.contains("iron") ||
      name.contains("bronze") ||
      name.contains("copper") ||
      name.contains("tin")
    ) return 15;
    if (
      name.contains("gold") || name.contains("wood") || name.contains("leather")
    ) return 7;
    if (name.contains("stone") || name.contains("cobble")) return 10;

    Block block = Block.byItem(item);
    if (block != Blocks.AIR) {
      float hardness = block.defaultBlockState().getDestroySpeed(null, null);
      if (hardness < 0) return 40;
      if (hardness == 0) return 5;
      return Math.clamp((int) (hardness * 5), 5, 40);
    }

    Rarity rarity = item.getDefaultInstance().getRarity();
    return switch (rarity) {
      case EPIC -> 40;
      case RARE -> 30;
      case UNCOMMON -> 20;
      default -> 12;
    };
  }

  public static int getCompoundedDurability(
    ArmorPartsComponent parts,
    ArmorItem.Type type
  ) {
    float frameVal = getDurabilityMultiplier(parts.frame().value()) * 0.5f;
    float platingVal = getDurabilityMultiplier(parts.plating().value()) * 0.3f;
    float paddingVal = getDurabilityMultiplier(parts.padding().value()) * 0.1f;
    float attachVal =
      getDurabilityMultiplier(parts.attachment().value()) * 0.1f;

    int blendedMultiplier = Math.round(
      frameVal + platingVal + paddingVal + attachVal
    );
    int baseDurability = switch (type) {
      case HELMET -> 11;
      case CHESTPLATE -> 16;
      case LEGGINGS -> 15;
      case BOOTS -> 13;
      default -> 12;
    };

    float shapeMod = switch (parts.shape()) {
      case "heavy" -> 1.2f;
      case "light" -> 0.8f;
      default -> 1.0f;
    };

    return (int) (baseDurability * blendedMultiplier * shapeMod);
  }

  public static int getCompoundedDefense(
    ArmorPartsComponent parts,
    ArmorItem.Type type
  ) {
    float platingQual =
      (getDurabilityMultiplier(parts.plating().value()) / 30.0f) * 0.6f;
    float paddingQual =
      (getDurabilityMultiplier(parts.padding().value()) / 30.0f) * 0.2f;
    float frameQual =
      (getDurabilityMultiplier(parts.frame().value()) / 30.0f) * 0.1f;
    float attachQual =
      (getDurabilityMultiplier(parts.attachment().value()) / 30.0f) * 0.1f;

    float combinedQuality = platingQual + paddingQual + frameQual + attachQual;
    int baseDefense = switch (type) {
      case HELMET -> 3;
      case CHESTPLATE -> 8;
      case LEGGINGS -> 6;
      case BOOTS -> 3;
      default -> 1;
    };

    float shapeMod = switch (parts.shape()) {
      case "heavy" -> 1.5f;
      case "light" -> 0.7f;
      default -> 1.0f;
    };

    return Math.clamp(
      Math.round((baseDefense * combinedQuality) * shapeMod),
      1,
      10
    );
  }

  public static float getCompoundedToughness(ArmorPartsComponent parts) {
    int platingMult = getDurabilityMultiplier(parts.plating().value());
    int paddingMult = getDurabilityMultiplier(parts.padding().value());
    float blendedMult = (platingMult * 0.8f) + (paddingMult * 0.2f);

    float baseToughness = 0.0f;
    if (blendedMult >= 33) baseToughness = 3.0f;
    else if (blendedMult >= 20) baseToughness = 1.5f;

    float shapeMod = switch (parts.shape()) {
      case "heavy" -> 2.0f;
      case "light" -> 0.0f; // Light armor forfeits all toughness
      default -> 1.0f;
    };

    return baseToughness * shapeMod;
  }
}
