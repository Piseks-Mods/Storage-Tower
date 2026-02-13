package org.dpdns.pisekpiskovec.storagetower.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ClickType;
import net.minecraftforge.network.NetworkEvent;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

import java.util.function.Supplier;

public class GridClickPacket {
    private final int gridSlot;
    private final int button;
    private final ClickType clickType;

    public GridClickPacket(int gridSlot, int button, ClickType clickType) {
        this.gridSlot = gridSlot;
        this.button = button;
        this.clickType = clickType;
    }

    public static void encode(GridClickPacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.gridSlot);
        buffer.writeInt(packet.button);
        buffer.writeEnum(packet.clickType);
    }

    public static GridClickPacket decode(FriendlyByteBuf buffer) {
        int gridSlot = buffer.readInt();
        int button = buffer.readInt();
        ClickType clickType = buffer.readEnum(ClickType.class);
        return new GridClickPacket(gridSlot, button, clickType);
    }

    public static void handle(GridClickPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                if (player.containerMenu instanceof StorageControllerMenu menu)
                    menu.clickItemGrid(packet.gridSlot, packet.button, packet.clickType, player);
                else if (player.containerMenu instanceof StorageCraftingControllerMenu menu)
                    menu.clickItemGrid(packet.gridSlot, packet.button, packet.clickType, player);
            }
        });
        context.setPacketHandled(true);
    }
}
