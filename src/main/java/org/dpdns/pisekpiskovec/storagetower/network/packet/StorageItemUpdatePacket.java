package org.dpdns.pisekpiskovec.storagetower.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StorageItemUpdatePacket {
    private final List<ItemStack> items;
    private final int windowId;

    public StorageItemUpdatePacket(int windowId, List<ItemStack> items) {
        this.windowId = windowId;
        this.items = items;
    }

    public static void encode(StorageItemUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.windowId);
        buffer.writeInt(packet.items.size());
        for (ItemStack stack : packet.items) {
            buffer.writeItem(stack);
        }
    }

    public static StorageItemUpdatePacket decode(FriendlyByteBuf buffer) {
        int windowId = buffer.readInt();
        int size = buffer.readInt();
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            items.add(buffer.readItem());
        }
        return new StorageItemUpdatePacket(windowId, items);
    }

    public static void handle(StorageItemUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                handleClientSide(packet);
            }
        });
        context.setPacketHandled(true);
    }

    private static void handleClientSide(StorageItemUpdatePacket packet) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player != null && mc.player.containerMenu.containerId == packet.windowId) {
            if (mc.player.containerMenu instanceof org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu menu) {
                menu.updateClientItems(packet.items);
            } else if (mc.player.containerMenu instanceof org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu menu) {
                menu.updateClientItems(packet.items);
            }
        }
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getWindowId() {
        return windowId;
    }
}
