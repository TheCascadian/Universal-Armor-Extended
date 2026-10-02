package com.thecascadian.universalarmor.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;

public record ArmorPartsComponent(
  Holder<Item> frame,
  Holder<Item> padding,
  Holder<Item> plating,
  Holder<Item> attachment,
  String shape
) {
  public static final Codec<ArmorPartsComponent> CODEC =
    RecordCodecBuilder.create(instance ->
      instance
        .group(
          BuiltInRegistries.ITEM.holderByNameCodec()
            .fieldOf("frame")
            .forGetter(ArmorPartsComponent::frame),
          BuiltInRegistries.ITEM.holderByNameCodec()
            .fieldOf("padding")
            .forGetter(ArmorPartsComponent::padding),
          BuiltInRegistries.ITEM.holderByNameCodec()
            .fieldOf("plating")
            .forGetter(ArmorPartsComponent::plating),
          BuiltInRegistries.ITEM.holderByNameCodec()
            .fieldOf("attachment")
            .forGetter(ArmorPartsComponent::attachment),
          Codec.STRING.optionalFieldOf("shape", "standard").forGetter(
            ArmorPartsComponent::shape
          )
        )
        .apply(instance, ArmorPartsComponent::new)
    );

  public static final StreamCodec<
    RegistryFriendlyByteBuf,
    ArmorPartsComponent
  > STREAM_CODEC = StreamCodec.composite(
    ByteBufCodecs.holderRegistry(Registries.ITEM),
    ArmorPartsComponent::frame,
    ByteBufCodecs.holderRegistry(Registries.ITEM),
    ArmorPartsComponent::padding,
    ByteBufCodecs.holderRegistry(Registries.ITEM),
    ArmorPartsComponent::plating,
    ByteBufCodecs.holderRegistry(Registries.ITEM),
    ArmorPartsComponent::attachment,
    ByteBufCodecs.STRING_UTF8,
    ArmorPartsComponent::shape,
    ArmorPartsComponent::new
  );

  /** Plain iron set shown for stacks that carry no parts (creative menu, /give). */
  public static ArmorPartsComponent defaultParts() {
    Holder<Item> iron = BuiltInRegistries.ITEM.wrapAsHolder(
      net.minecraft.world.item.Items.IRON_INGOT
    );
    return new ArmorPartsComponent(iron, iron, iron, iron, "standard");
  }
}
