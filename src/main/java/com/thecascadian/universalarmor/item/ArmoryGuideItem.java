package com.thecascadian.universalarmor.item;

import com.thecascadian.universalarmor.client.ClientGuideHelper;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

public class ArmoryGuideItem extends Item {

  public ArmoryGuideItem(Properties properties) {
    super(properties);
  }

  @Override
  public InteractionResultHolder<ItemStack> use(
    Level level,
    Player player,
    InteractionHand hand
  ) {
    ItemStack stack = player.getItemInHand(hand);
    if (level.isClientSide) ClientGuideHelper.open();
    return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
  }
}
