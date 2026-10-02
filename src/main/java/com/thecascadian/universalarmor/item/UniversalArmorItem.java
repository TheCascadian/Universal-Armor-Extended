package com.thecascadian.universalarmor.item;

import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import com.thecascadian.universalarmor.registry.ModRegistries;
import java.util.EnumMap;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;

public class UniversalArmorItem extends ArmorItem {

  private static final ArmorMaterial DYNAMIC_ARMOR_MATERIAL = new ArmorMaterial(
    Util.make(new EnumMap<>(ArmorItem.Type.class), map -> {
      map.put(ArmorItem.Type.HELMET, 1);
      map.put(ArmorItem.Type.CHESTPLATE, 1);
      map.put(ArmorItem.Type.LEGGINGS, 1);
      map.put(ArmorItem.Type.BOOTS, 1);
    }),
    10,
    SoundEvents.ARMOR_EQUIP_GENERIC,
    () -> Ingredient.EMPTY,
    List.of(
      new ArmorMaterial.Layer(
        ResourceLocation.fromNamespaceAndPath("universalarmor", "universal"),
        "",
        false
      )
    ),
    0.0f,
    0.0f
  );

  public UniversalArmorItem(ArmorItem.Type type, Properties properties) {
    super(Holder.direct(DYNAMIC_ARMOR_MATERIAL), type, properties);
  }

  @Override
  public ResourceLocation getArmorTexture(
    ItemStack stack,
    Entity entity,
    EquipmentSlot slot,
    ArmorMaterial.Layer layer,
    boolean innerModel
  ) {
    ArmorPartsComponent comp = stack.get(
      ModRegistries.ARMOR_PARTS_COMPONENT.get()
    );
    if (comp == null) comp = ArmorPartsComponent.defaultParts();
    if (comp != null) {
      return com.thecascadian.universalarmor.client.ClientArmorTextureHelper.getTexture(
        comp,
        innerModel
      );
    }
    return super.getArmorTexture(stack, entity, slot, layer, innerModel);
  }

  @Override
  public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
    ArmorPartsComponent comp = toRepair.get(
      ModRegistries.ARMOR_PARTS_COMPONENT.get()
    );
    if (comp != null) return (
      comp.plating().value() == repair.getItem() ||
      comp.frame().value() == repair.getItem()
    );
    return super.isValidRepairItem(toRepair, repair);
  }

  @Override
  public void appendHoverText(
    ItemStack stack,
    Item.TooltipContext context,
    List<Component> tooltipComponents,
    TooltipFlag tooltipFlag
  ) {
    ArmorPartsComponent comp = stack.get(
      ModRegistries.ARMOR_PARTS_COMPONENT.get()
    );
    if (comp != null) {
      tooltipComponents.add(
        Component.translatable(
          "tooltip.universalarmor.shape",
          Component.translatable("shape.universalarmor." + comp.shape())
        ).withStyle(ChatFormatting.GOLD)
      );
      tooltipComponents.add(Component.empty());
      tooltipComponents.add(
        Component.translatable("tooltip.universalarmor.components").withStyle(
          ChatFormatting.GRAY
        )
      );
      tooltipComponents.add(
        Component.translatable(
          "tooltip.universalarmor.frame",
          comp.frame().value().getDescription()
        ).withStyle(ChatFormatting.DARK_GRAY)
      );
      tooltipComponents.add(
        Component.translatable(
          "tooltip.universalarmor.padding",
          comp.padding().value().getDescription()
        ).withStyle(ChatFormatting.DARK_GRAY)
      );
      tooltipComponents.add(
        Component.translatable(
          "tooltip.universalarmor.plating",
          comp.plating().value().getDescription()
        ).withStyle(ChatFormatting.DARK_GRAY)
      );
      tooltipComponents.add(
        Component.translatable(
          "tooltip.universalarmor.attachment",
          comp.attachment().value().getDescription()
        ).withStyle(ChatFormatting.DARK_GRAY)
      );
    }
    super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
  }

  @Override
  public Component getName(ItemStack stack) {
    ArmorPartsComponent comp = stack.get(
      ModRegistries.ARMOR_PARTS_COMPONENT.get()
    );
    if (comp != null) {
      return Component.translatable(
        "item.universalarmor.dynamic_format_detailed",
        Component.translatable("shape.universalarmor." + comp.shape()),
        comp.plating().value().getDescription(),
        Component.translatable(this.getDescriptionId() + ".generic")
      );
    }
    return super.getName(stack);
  }
}
