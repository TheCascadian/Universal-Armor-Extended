package com.thecascadian.universalarmor.client;

import com.thecascadian.universalarmor.UniversalArmorMod;
import com.thecascadian.universalarmor.client.screen.ArmoryStationScreen;
import com.thecascadian.universalarmor.registry.ModRegistries;
import com.thecascadian.universalarmor.client.model.HeavyArmorModel;
import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@EventBusSubscriber(
  modid = UniversalArmorMod.MOD_ID,
  bus = EventBusSubscriber.Bus.MOD,
  value = Dist.CLIENT
)
public class ModClientEvents {

  private static HeavyArmorModel heavyOuter;
  private static HeavyArmorModel heavyInner;

  @SubscribeEvent
  public static void registerLayers(
    EntityRenderersEvent.RegisterLayerDefinitions event
  ) {
    event.registerLayerDefinition(HeavyArmorModel.OUTER, HeavyArmorModel::createOuter);
    event.registerLayerDefinition(HeavyArmorModel.INNER, HeavyArmorModel::createInner);
  }

  @SubscribeEvent
  public static void resetModels(EntityRenderersEvent.AddLayers event) {
    heavyOuter = null;
    heavyInner = null;
  }

  @SubscribeEvent
  public static void registerClientExtensions(
    RegisterClientExtensionsEvent event
  ) {
    event.registerItem(
      new IClientItemExtensions() {
        @Override
        public BlockEntityWithoutLevelRenderer getCustomRenderer() {
          return ArmorItemRenderer.getInstance();
        }

        @Override
        public HumanoidModel<?> getHumanoidArmorModel(
          LivingEntity entity,
          ItemStack stack,
          EquipmentSlot slot,
          HumanoidModel<?> original
        ) {
          ArmorPartsComponent parts = stack.get(ModRegistries.ARMOR_PARTS_COMPONENT.get());
          if (parts == null || !"heavy".equals(parts.shape())) return original;
          var models = Minecraft.getInstance().getEntityModels();
          if (slot == EquipmentSlot.LEGS) {
            if (heavyInner == null) heavyInner = new HeavyArmorModel(models.bakeLayer(HeavyArmorModel.INNER));
            return heavyInner;
          }
          if (heavyOuter == null) heavyOuter = new HeavyArmorModel(models.bakeLayer(HeavyArmorModel.OUTER));
          return heavyOuter;
        }
      },
      ModRegistries.UNIVERSAL_HELMET.get(),
      ModRegistries.UNIVERSAL_CHESTPLATE.get(),
      ModRegistries.UNIVERSAL_LEGGINGS.get(),
      ModRegistries.UNIVERSAL_BOOTS.get()
    );
  }

  @SubscribeEvent
  public static void registerScreens(RegisterMenuScreensEvent event) {
    event.register(
      ModRegistries.ARMORY_STATION_MENU.get(),
      ArmoryStationScreen::new
    );
  }
}
