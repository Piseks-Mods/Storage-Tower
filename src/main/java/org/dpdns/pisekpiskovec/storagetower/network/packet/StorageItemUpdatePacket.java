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
        System.out.println("PACKET: Creating packet with windowId=" + windowId + ", items=" + items.size());
    }

    public static void encode(StorageItemUpdatePacket packet, FriendlyByteBuf buffer) {
        System.out.println("=== PACKET ENCODE ===");
        System.out.println("ENCODE: Writing windowId=" + packet.windowId);
        buffer.writeInt(packet.windowId);
        System.out.println("ENCODE: Writing " + packet.items.size() + " items");
        buffer.writeInt(packet.items.size());
        for (int i = 0; i < packet.items.size(); i++) {
            ItemStack stack = packet.items.get(i);
            System.out.println("ENCODE: [" + i + "] " + stack.getHoverName().getString() + " x" + stack.getCount());
            buffer.writeItem(stack);
        }
        System.out.println("=== PACKET ENCODE DONE ===");
    }

    public static StorageItemUpdatePacket decode(FriendlyByteBuf buffer) {
        System.out.println("=== PACKET DECODE ===");
        int windowId = buffer.readInt();
        System.out.println("DECODE: Read windowId=" + windowId);
        int size = buffer.readInt();
        System.out.println("DECODE: Reading " + size + " items");
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buffer.readItem();
            items.add(stack);
            System.out.println("DECODE: [" + i + "] " + stack.getHoverName().getString() + " x" + stack.getCount());
        }
        System.out.println("=== PACKET DECODE DONE ===");
        return new StorageItemUpdatePacket(windowId, items);
    }

    public static void handle(StorageItemUpdatePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        System.out.println("=== PACKET HANDLE ===");
        NetworkEvent.Context context = contextSupplier.get();
        System.out.println("HANDLE: Direction=" + context.getDirection() + ", Side=" + context.getDirection().getReceptionSide());

        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                System.out.println("HANDLE: Processing on client side");
                handleClientSide(packet);
            } else {
                System.out.println("HANDLE: Not client side, ignoring");
            }
        });
        context.setPacketHandled(true);
        System.out.println("=== PACKET HANDLE DONE ===");
    }

    private static void handleClientSide(StorageItemUpdatePacket packet) {
        System.out.println("=== PACKET HANDLE CLIENT SIDE ===");
        System.out.println("CLIENT HANDLE: WindowId=" + packet.windowId);
        System.out.println("CLIENT HANDLE: Items=" + packet.items.size());

        for (int i = 0; i < Math.min(10, packet.items.size()); i++) {
            ItemStack item = packet.items.get(i);
            System.out.println("CLIENT HANDLE: [" + i + "] " + item.getHoverName().getString() + " x" + item.getCount());
        }

        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.player == null) {
            System.out.println("CLIENT HANDLE: Player is null!");
            return;
        }

        System.out.println("CLIENT HANDLE: Player container windowId=" + mc.player.containerMenu.containerId);

        if (mc.player.containerMenu.containerId == packet.windowId) {
            System.out.println("CLIENT HANDLE: WindowId matches!");
            if (mc.player.containerMenu instanceof org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu menu) {
                System.out.println("CLIENT HANDLE: Container is Storage Controller Menu, updating items");
                menu.updateClientItems(packet.items);
            } else if (mc.player.containerMenu instanceof org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu menu) {
                System.out.println("CLIENT HANDLE: Container is Storage Crafting Controller Menu, updating items");
                menu.updateClientItems(packet.items);
            } else {
                System.out.println("CLIENT HANDLE: Container type msmatch: " + mc.player.containerMenu.getClass().getName());
            }
        } else {
            System.out.println("CLIENT HANDLE: WindowId mismatch!");
        }
        System.out.println("=== PACKET HANDLE CLIENT SIDE DONE ===");
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public int getWindowId() {
        return windowId;
    }
}
