package com.thecascadian.universalarmor.network;

import com.thecascadian.universalarmor.UniversalArmorMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(
  modid = UniversalArmorMod.MOD_ID,
  bus = EventBusSubscriber.Bus.MOD
)
public class ModNetwork {

  @SubscribeEvent
  public static void register(final RegisterPayloadHandlersEvent event) {
    final PayloadRegistrar registrar = event.registrar(
      UniversalArmorMod.MOD_ID
    );
    registrar.playToServer(
      ChangeShapePacket.TYPE,
      ChangeShapePacket.STREAM_CODEC,
      ChangeShapePacket::handleData
    );
  }
}
