package org.dpdns.pisekpiskovec.storagetower.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.dpdns.pisekpiskovec.storagetower.StorageTower;
import org.dpdns.pisekpiskovec.storagetower.network.packet.GridClickPacket;
import org.dpdns.pisekpiskovec.storagetower.network.packet.StorageItemUpdatePacket;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation(StorageTower.MOD_ID, "main"), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        CHANNEL.messageBuilder(StorageItemUpdatePacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(StorageItemUpdatePacket::decode).encoder(StorageItemUpdatePacket::encode).consumerMainThread(StorageItemUpdatePacket::handle).add(); // Server -> Client: Storage item updates
        CHANNEL.messageBuilder(GridClickPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(GridClickPacket::decode).encoder(GridClickPacket::encode).consumerMainThread(GridClickPacket::handle).add(); // Client -> Server: Grid Clicks
    }

    public static void sendToPlayer(StorageItemUpdatePacket packet, ServerPlayer player) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToServer(GridClickPacket packet) {
        CHANNEL.sendToServer(packet);
    }
}
