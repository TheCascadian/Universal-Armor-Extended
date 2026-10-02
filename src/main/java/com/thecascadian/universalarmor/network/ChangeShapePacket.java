package com.thecascadian.universalarmor.network;

import com.thecascadian.universalarmor.UniversalArmorMod;
import com.thecascadian.universalarmor.inventory.ArmoryStationMenu;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ChangeShapePacket(int shapeIndex) implements CustomPacketPayload {
  public static final Type<ChangeShapePacket> TYPE = new Type<>(
    ResourceLocation.fromNamespaceAndPath(
      UniversalArmorMod.MOD_ID,
      "change_shape"
    )
  );

  public static final StreamCodec<
    RegistryFriendlyByteBuf,
    ChangeShapePacket
  > STREAM_CODEC = StreamCodec.composite(
    ByteBufCodecs.INT,
    ChangeShapePacket::shapeIndex,
    ChangeShapePacket::new
  );

  @Override
  public Type<? extends CustomPacketPayload> type() {
    return TYPE;
  }

  public static void handleData(
    final ChangeShapePacket payload,
    final IPayloadContext context
  ) {
    context.enqueueWork(() -> {
      Player player = context.player();
      if (player.containerMenu instanceof ArmoryStationMenu menu) {
        menu.setShapeIndex(payload.shapeIndex());
      }
    });
  }
}
