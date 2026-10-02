package com.thecascadian.universalarmor.inventory;

import com.thecascadian.universalarmor.component.ArmorPartsComponent;
import com.thecascadian.universalarmor.registry.ModRegistries;
import com.thecascadian.universalarmor.util.DynamicStatsCalculator;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class ArmoryStationMenu extends AbstractContainerMenu {

  public static final String[] SHAPES = { "standard", "heavy", "light" };
  public static final String[] PART_NAMES = {
    "frame",
    "padding",
    "plating",
    "attachment",
  };
  public static final String[] PIECE_NAMES = {
    "helmet",
    "chestplate",
    "leggings",
    "boots",
  };

  private static final ResourceLocation[] EMPTY_SLOTS = {
    InventoryMenu.EMPTY_ARMOR_SLOT_HELMET,
    InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE,
    InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS,
    InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS,
  };

  public static final int INPUTS = 4;
  public static final int OUTPUT_START = INPUTS;
  public static final int INV_START = INPUTS + 4;
  public static final int INV_END = INV_START + 36;

  // Resource costs required for [Helmet, Chest, Leggings, Boots] across [Frame, Padding, Plating, Attachment]
  private static final int[][] COSTS = {
    { 1, 1, 2, 1 },
    { 2, 2, 3, 1 },
    { 2, 2, 2, 1 },
    { 1, 1, 1, 1 },
  };

  private final IItemHandler inputHandler;
  private final SimpleContainer outputContainer = new SimpleContainer(4);
  private final DataSlot shapeIndexSlot = DataSlot.standalone();

  public ArmoryStationMenu(
    int containerId,
    Inventory playerInv,
    RegistryFriendlyByteBuf extraData
  ) {
    this(containerId, playerInv, new ItemStackHandler(INPUTS));
  }

  public ArmoryStationMenu(
    int containerId,
    Inventory playerInv,
    IItemHandler inputHandler
  ) {
    super(ModRegistries.ARMORY_STATION_MENU.get(), containerId);
    this.inputHandler = inputHandler;
    addDataSlot(shapeIndexSlot);

    // One dedicated, labelled slot per armor component
    for (int i = 0; i < INPUTS; i++) {
      addSlot(
        new SlotItemHandler(inputHandler, i, 11, 25 + i * 20) {
          @Override
          public void setChanged() {
            super.setChanged();
            setupResult();
          }
        }
      );
    }

    // Output column: helmet, chestplate, leggings, boots
    for (int i = 0; i < 4; i++) {
      Slot out = new ArmoryResultSlot(
        this,
        outputContainer,
        i,
        215,
        25 + i * 20,
        i
      );
      out.setBackground(InventoryMenu.BLOCK_ATLAS, EMPTY_SLOTS[i]);
      addSlot(out);
    }

    for (int i = 0; i < 3; ++i) {
      for (int j = 0; j < 9; ++j) {
        this.addSlot(
          new Slot(playerInv, j + i * 9 + 9, 40 + j * 18, 150 + i * 18)
        );
      }
    }
    for (int k = 0; k < 9; ++k) {
      this.addSlot(new Slot(playerInv, k, 40 + k * 18, 208));
    }

    setupResult();
  }

  public int getShapeIndex() {
    return shapeIndexSlot.get();
  }

  public void setShapeIndex(int index) {
    shapeIndexSlot.set(index);
    setupResult();
  }

  /** Input slot that supplies each component; empty slots fall back to an earlier one. */
  public int[] resolveSources() {
    if (inputHandler.getStackInSlot(0).isEmpty()) return null;
    int p = inputHandler.getStackInSlot(1).isEmpty() ? 0 : 1;
    int pl = inputHandler.getStackInSlot(2).isEmpty() ? 0 : 2;
    int a = inputHandler.getStackInSlot(3).isEmpty() ? p : 3;
    return new int[] { 0, p, pl, a };
  }

  /** Items needed from each input slot to craft the given piece. */
  public int[] requirements(int piece) {
    int[] src = resolveSources();
    if (src == null) return null;
    int[] req = new int[INPUTS];
    for (int c = 0; c < 4; c++) req[src[c]] += COSTS[piece][c];
    return req;
  }

  public static int[] costsFor(int piece) {
    return COSTS[piece];
  }

  public ItemStack getInputStack(int slot) {
    return inputHandler.getStackInSlot(slot);
  }

  public boolean hasOutput() {
    for (int i = 0; i < 4; i++) if (!outputContainer.getItem(i).isEmpty()) {
      return true;
    }
    return false;
  }

  public ArmorPartsComponent getPreviewParts() {
    int[] src = resolveSources();
    if (src == null) return null;
    return new ArmorPartsComponent(
      holder(src[0]),
      holder(src[1]),
      holder(src[2]),
      holder(src[3]),
      SHAPES[Math.max(0, Math.min(SHAPES.length - 1, getShapeIndex()))]
    );
  }

  private Holder<Item> holder(int slot) {
    return BuiltInRegistries.ITEM.wrapAsHolder(
      inputHandler.getStackInSlot(slot).getItem()
    );
  }

  public void consumeIngredients(int pieceIndex) {
    int[] req = requirements(pieceIndex);
    if (req == null) return;
    for (int i = 0; i < INPUTS; i++) {
      if (req[i] > 0) inputHandler.extractItem(i, req[i], false);
    }
    setupResult();
  }

  private void setupResult() {
    ArmorPartsComponent parts = getPreviewParts();
    for (int piece = 0; piece < 4; piece++) {
      int[] req = requirements(piece);
      boolean canCraft = req != null;
      if (canCraft) for (int i = 0; i < INPUTS; i++) {
        if (req[i] > 0 && inputHandler.getStackInSlot(i).getCount() < req[i]) {
          canCraft = false;
          break;
        }
      }
      outputContainer.setItem(
        piece,
        canCraft ? createArmor(piece, parts) : ItemStack.EMPTY
      );
    }
  }

  /** Builds an armor piece with baked stats; also used for the client preview. */
  public static ItemStack createArmor(int piece, ArmorPartsComponent parts) {
    String currentShape = parts.shape();

    Item resultItem = switch (piece) {
      case 0 -> ModRegistries.UNIVERSAL_HELMET.get();
      case 1 -> ModRegistries.UNIVERSAL_CHESTPLATE.get();
      case 2 -> ModRegistries.UNIVERSAL_LEGGINGS.get();
      case 3 -> ModRegistries.UNIVERSAL_BOOTS.get();
      default -> throw new IllegalStateException();
    };

    ItemStack result = new ItemStack(resultItem);
    result.set(ModRegistries.ARMOR_PARTS_COMPONENT.get(), parts);
    ArmorItem.Type slotType = ((ArmorItem) result.getItem()).getType();

    result.set(
      DataComponents.MAX_DAMAGE,
      DynamicStatsCalculator.getCompoundedDurability(parts, slotType)
    );
    result.set(DataComponents.DAMAGE, 0);

    EquipmentSlotGroup targetSlot = switch (slotType) {
      case HELMET -> EquipmentSlotGroup.HEAD;
      case CHESTPLATE -> EquipmentSlotGroup.CHEST;
      case LEGGINGS -> EquipmentSlotGroup.LEGS;
      case BOOTS -> EquipmentSlotGroup.FEET;
      default -> EquipmentSlotGroup.ANY;
    };

    ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
    builder.add(
      Attributes.ARMOR,
      new AttributeModifier(
        ResourceLocation.fromNamespaceAndPath(
          "universalarmor",
          "armor." + slotType.getName()
        ),
        DynamicStatsCalculator.getCompoundedDefense(parts, slotType),
        AttributeModifier.Operation.ADD_VALUE
      ),
      targetSlot
    );

    float toughness = DynamicStatsCalculator.getCompoundedToughness(parts);
    if (toughness > 0) builder.add(
      Attributes.ARMOR_TOUGHNESS,
      new AttributeModifier(
        ResourceLocation.fromNamespaceAndPath(
          "universalarmor",
          "armor_toughness." + slotType.getName()
        ),
        toughness,
        AttributeModifier.Operation.ADD_VALUE
      ),
      targetSlot
    );

    if ("heavy".equals(currentShape)) builder.add(
      Attributes.MOVEMENT_SPEED,
      new AttributeModifier(
        ResourceLocation.fromNamespaceAndPath(
          "universalarmor",
          "heavy_penalty." + slotType.getName()
        ),
        -0.03,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
      ),
      targetSlot
    );
    else if ("light".equals(currentShape)) builder.add(
      Attributes.MOVEMENT_SPEED,
      new AttributeModifier(
        ResourceLocation.fromNamespaceAndPath(
          "universalarmor",
          "light_bonus." + slotType.getName()
        ),
        0.03,
        AttributeModifier.Operation.ADD_MULTIPLIED_BASE
      ),
      targetSlot
    );

    result.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    return result;
  }

  @Override
  public ItemStack quickMoveStack(Player player, int index) {
    ItemStack itemstack = ItemStack.EMPTY;
    Slot slot = this.slots.get(index);
    if (slot != null && slot.hasItem()) {
      ItemStack itemstack1 = slot.getItem();
      itemstack = itemstack1.copy();

      if (index >= OUTPUT_START && index < INV_START) {
        if (
          !this.moveItemStackTo(itemstack1, INV_START, INV_END, true)
        ) return ItemStack.EMPTY;
        slot.onQuickCraft(itemstack1, itemstack);
      } else if (index >= INV_START) {
        if (
          !this.moveItemStackTo(itemstack1, 0, INPUTS, false)
        ) return ItemStack.EMPTY;
      } else {
        if (
          !this.moveItemStackTo(itemstack1, INV_START, INV_END, false)
        ) return ItemStack.EMPTY;
      }

      if (itemstack1.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
      else slot.setChanged();
      if (itemstack1.getCount() == itemstack.getCount()) return ItemStack.EMPTY;
      slot.onTake(player, itemstack1);
    }
    return itemstack;
  }

  @Override
  public boolean stillValid(Player player) {
    return true;
  }
}
