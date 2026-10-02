package com.thecascadian.universalarmor.registry;

import com.thecascadian.universalarmor.UniversalArmorMod;
import com.thecascadian.universalarmor.block.ArmoryStationBlock;
import com.thecascadian.universalarmor.block.entity.ArmoryStationBlockEntity;
import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import com.thecascadian.universalarmor.inventory.ArmoryStationMenu;
import com.thecascadian.universalarmor.item.UniversalArmorItem;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistries {

  public static final DeferredRegister.Blocks BLOCKS =
    DeferredRegister.createBlocks(UniversalArmorMod.MOD_ID);
  public static final DeferredRegister.Items ITEMS =
    DeferredRegister.createItems(UniversalArmorMod.MOD_ID);
  public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
    DeferredRegister.create(
      Registries.BLOCK_ENTITY_TYPE,
      UniversalArmorMod.MOD_ID
    );
  public static final DeferredRegister<MenuType<?>> MENUS =
    DeferredRegister.create(Registries.MENU, UniversalArmorMod.MOD_ID);
  public static final DeferredRegister<
    DataComponentType<?>
  > DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(
    Registries.DATA_COMPONENT_TYPE,
    UniversalArmorMod.MOD_ID
  );
  public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
    DeferredRegister.create(
      Registries.CREATIVE_MODE_TAB,
      UniversalArmorMod.MOD_ID
    );

  public static final DeferredHolder<Block, ArmoryStationBlock> ARMORY_STATION =
    BLOCKS.register("armory_station", () ->
      new ArmoryStationBlock(
        BlockBehaviour.Properties.of()
          .mapColor(MapColor.METAL)
          .requiresCorrectToolForDrops()
          .strength(5.0F, 6.0F)
          .sound(SoundType.METAL)
      )
    );
  public static final DeferredHolder<Item, BlockItem> ARMORY_STATION_ITEM =
    ITEMS.register("armory_station", () ->
      new BlockItem(ARMORY_STATION.get(), new Item.Properties())
    );

  public static final DeferredHolder<Item, Item> ARMORY_GUIDE = ITEMS.register(
    "armory_guide",
    () -> new com.thecascadian.universalarmor.item.ArmoryGuideItem(
      new Item.Properties().stacksTo(1)
    )
  );

  public static final DeferredHolder<
    BlockEntityType<?>,
    BlockEntityType<ArmoryStationBlockEntity>
  > ARMORY_STATION_BE = BLOCK_ENTITIES.register("armory_station", () ->
    BlockEntityType.Builder.of(
      ArmoryStationBlockEntity::new,
      ARMORY_STATION.get()
    ).build(null)
  );
  public static final DeferredHolder<
    MenuType<?>,
    MenuType<ArmoryStationMenu>
  > ARMORY_STATION_MENU = MENUS.register("armory_station", () ->
    IMenuTypeExtension.create((windowId, inv, data) ->
      new ArmoryStationMenu(windowId, inv, data)
    )
  );

  public static final DeferredHolder<
    DataComponentType<?>,
    DataComponentType<ArmorPartsComponent>
  > ARMOR_PARTS_COMPONENT = DATA_COMPONENT_TYPES.register("parts", () ->
    DataComponentType.<ArmorPartsComponent>builder()
      .persistent(ArmorPartsComponent.CODEC)
      .networkSynchronized(ArmorPartsComponent.STREAM_CODEC)
      .build()
  );

  public static final DeferredHolder<
    Item,
    UniversalArmorItem
  > UNIVERSAL_HELMET = ITEMS.register("universal_helmet", () ->
    new UniversalArmorItem(
      ArmorItem.Type.HELMET,
      new Item.Properties().durability(11)
    )
  );
  public static final DeferredHolder<
    Item,
    UniversalArmorItem
  > UNIVERSAL_CHESTPLATE = ITEMS.register("universal_chestplate", () ->
    new UniversalArmorItem(
      ArmorItem.Type.CHESTPLATE,
      new Item.Properties().durability(16)
    )
  );
  public static final DeferredHolder<
    Item,
    UniversalArmorItem
  > UNIVERSAL_LEGGINGS = ITEMS.register("universal_leggings", () ->
    new UniversalArmorItem(
      ArmorItem.Type.LEGGINGS,
      new Item.Properties().durability(15)
    )
  );
  public static final DeferredHolder<Item, UniversalArmorItem> UNIVERSAL_BOOTS =
    ITEMS.register("universal_boots", () ->
      new UniversalArmorItem(
        ArmorItem.Type.BOOTS,
        new Item.Properties().durability(13)
      )
    );

  public static final DeferredHolder<
    CreativeModeTab,
    CreativeModeTab
  > ARMORY_TAB = CREATIVE_MODE_TABS.register("armory_tab", () ->
    CreativeModeTab.builder()
      .title(Component.translatable("itemGroup.universalarmor"))
      .icon(() -> new ItemStack(ARMORY_STATION_ITEM.get()))
      .displayItems((parameters, output) -> {
        output.accept(ARMORY_STATION_ITEM.get());
        output.accept(ARMORY_GUIDE.get());
        ArmorPartsComponent defaults = ArmorPartsComponent.defaultParts();
        for (int piece = 0; piece < 4; piece++) {
          output.accept(ArmoryStationMenu.createArmor(piece, defaults));
        }
      })
      .build()
  );
}
