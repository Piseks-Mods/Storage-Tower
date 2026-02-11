package org.dpdns.pisekpiskovec.storagetower.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.network.packet.StorageItemUpdatePacket;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(StorageTower.MOD_ID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(StorageItemUpdatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(StorageItemUpdatePacket::decode).encoder(StorageItemUpdatePacket::encode).consumerMainThread(StorageItemUpdatePacket::handle).add();
    }

    public static void sentToPlayer(StorageItemUpdatePacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
