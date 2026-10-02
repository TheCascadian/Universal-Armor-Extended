package com.thecascadian.universalarmor.block;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thecascadian.universalarmor.block.entity.ArmoryStationBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class ArmoryStationBlock extends BaseEntityBlock {

  public ArmoryStationBlock(Properties properties) {
    super(properties);
  }

  @Override
  public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
    return new ArmoryStationBlockEntity(pos, state);
  }

  @Override
  protected InteractionResult useWithoutItem(
    BlockState state,
    Level level,
    BlockPos pos,
    Player player,
    BlockHitResult hitResult
  ) {
    if (!level.isClientSide) {
      BlockEntity entity = level.getBlockEntity(pos);
      if (entity instanceof ArmoryStationBlockEntity station) {
        player.openMenu(station, pos);
      }
    }
    return InteractionResult.SUCCESS;
  }

  @Override
  public void onRemove(
    BlockState state,
    Level level,
    BlockPos pos,
    BlockState newState,
    boolean movedByPiston
  ) {
    if (!state.is(newState.getBlock())) {
      BlockEntity blockEntity = level.getBlockEntity(pos);
      if (blockEntity instanceof ArmoryStationBlockEntity station) {
        Containers.dropContents(level, pos, station.getInventory());
        level.updateNeighbourForOutputSignal(pos, this);
      }
      super.onRemove(state, level, pos, newState, movedByPiston);
    }
  }

  @Override
  protected MapCodec<? extends BaseEntityBlock> codec() {
    return RecordCodecBuilder.mapCodec(instance ->
      instance.group(propertiesCodec()).apply(instance, ArmoryStationBlock::new)
    );
  }

  @Override
  public RenderShape getRenderShape(BlockState state) {
    return RenderShape.MODEL;
  }
}
