package org.dpdns.pisekpiskovec.storagetower.network.packet;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record StorageItemUpdatePacket(int windowId, List<ItemStack> items) {

    public static void encode(StorageItemUpdatePacket packet, FriendlyByteBuf buffer) {
        buffer.writeInt(packet.windowId);
        buffer.writeInt(packet.items.size());
        for (int i = 0; i < packet.items.size(); i++) {
            ItemStack stack = packet.items.get(i);
            if (stack.isEmpty()) buffer.writeBoolean(false);
            else {
                buffer.writeBoolean(true);
                buffer.writeResourceLocation(stack.getItem().builtInRegistryHolder().key().location()); // Write item ID
                buffer.writeInt(stack.getCount());
                CompoundTag tag = stack.getTag();
                buffer.writeNbt(tag);

            }
        }
    }

    public static StorageItemUpdatePacket decode(FriendlyByteBuf buffer) {
        int windowId = buffer.readInt();
        int size = buffer.readInt();
        List<ItemStack> items = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            boolean hasItem = buffer.readBoolean();
            if (!hasItem) {
                items.add(ItemStack.EMPTY);
            } else {
                var itemLocation = buffer.readResourceLocation();
                var item = BuiltInRegistries.ITEM.get(itemLocation);
                int count = buffer.readInt();
                CompoundTag tag = buffer.readNbt();

                ItemStack stack = new ItemStack(item, count);
                if (tag != null) stack.setTag(tag);
                items.add(stack);
            }
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
        for (int i = 0; i < Math.min(10, packet.items.size()); i++) {
            ItemStack item = packet.items.get(i);
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        if (mc.player.containerMenu.containerId == packet.windowId) {
            if (mc.player.containerMenu instanceof StorageControllerMenu menu) {
                menu.updateClientItems(packet.items);
            } else if (mc.player.containerMenu instanceof StorageCraftingControllerMenu menu) {
                menu.updateClientItems(packet.items);
            }
        }
    }
}
