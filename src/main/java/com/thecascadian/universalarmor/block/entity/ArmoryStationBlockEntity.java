package com.thecascadian.universalarmor.block.entity;

import com.thecascadian.universalarmor.inventory.ArmoryStationMenu;
import com.thecascadian.universalarmor.registry.ModRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class ArmoryStationBlockEntity
  extends BlockEntity
  implements MenuProvider
{

  // One slot per armor component
  private final ItemStackHandler itemHandler = new ItemStackHandler(4) {
    @Override
    protected void onContentsChanged(int slot) {
      setChanged();
    }
  };

  public ArmoryStationBlockEntity(BlockPos pos, BlockState state) {
    super(ModRegistries.ARMORY_STATION_BE.get(), pos, state);
  }

  @Override
  public Component getDisplayName() {
    return Component.translatable("block.universalarmor.armory_station");
  }

  @Override
  public AbstractContainerMenu createMenu(
    int containerId,
    Inventory playerInv,
    Player player
  ) {
    return new ArmoryStationMenu(containerId, playerInv, itemHandler);
  }

  public SimpleContainer getInventory() {
    SimpleContainer container = new SimpleContainer(4);
    for (int i = 0; i < 4; i++) {
      container.setItem(i, itemHandler.getStackInSlot(i));
    }
    return container;
  }

  @Override
  protected void saveAdditional(
    CompoundTag tag,
    HolderLookup.Provider registries
  ) {
    super.saveAdditional(tag, registries);
    tag.put("inventory", itemHandler.serializeNBT(registries));
  }

  @Override
  protected void loadAdditional(
    CompoundTag tag,
    HolderLookup.Provider registries
  ) {
    super.loadAdditional(tag, registries);
    itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
  }
}
