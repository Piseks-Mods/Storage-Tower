package org.dpdns.pisekpiskovec.storagetower.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

import java.util.function.Supplier;

public class SearchFilterPacket {
    private final String filter;

    public SearchFilterPacket(String filter) {
        this.filter = filter;
    }

    public static void encode(SearchFilterPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.filter);
    }

    public static SearchFilterPacket decode(FriendlyByteBuf buffer) {
        String filter = buffer.readUtf();
        return new SearchFilterPacket(filter);
    }

    public static void handle(SearchFilterPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                if (player.containerMenu instanceof StorageControllerMenu menu) {
                    menu.setSearchFilter(packet.filter);
                } else if (player.containerMenu instanceof StorageCraftingControllerMenu menu) {
                    menu.setSearchFilter(packet.filter);
                }
            }
        });
        context.setPacketHandled(true);
    }
}
