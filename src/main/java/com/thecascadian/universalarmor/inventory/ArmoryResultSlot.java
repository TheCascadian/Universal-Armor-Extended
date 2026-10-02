package com.thecascadian.universalarmor.inventory;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ArmoryResultSlot extends Slot {

  private final ArmoryStationMenu menu;
  private final int pieceIndex;

  public ArmoryResultSlot(
    ArmoryStationMenu menu,
    Container container,
    int slot,
    int x,
    int y,
    int pieceIndex
  ) {
    super(container, slot, x, y);
    this.menu = menu;
    this.pieceIndex = pieceIndex;
  }

  @Override
  public boolean mayPlace(ItemStack stack) {
    return false;
  }

  @Override
  public void onTake(Player player, ItemStack stack) {
    this.checkTakeAchievements(stack);
    menu.consumeIngredients(pieceIndex);
    super.onTake(player, stack);
  }
}
