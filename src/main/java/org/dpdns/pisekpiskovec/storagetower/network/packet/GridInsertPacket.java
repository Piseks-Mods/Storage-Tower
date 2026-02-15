package org.dpdns.pisekpiskovec.storagetower.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageControllerMenu;
import org.dpdns.pisekpiskovec.storagetower.screen.menu.StorageCraftingControllerMenu;

import java.util.function.Supplier;

public class GridInsertPacket {
    private final boolean insertAll; // true = insert all, false = insert 1

    public GridInsertPacket(boolean insertAll) {
        this.insertAll = insertAll;
    }

    public static void encode(GridInsertPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBoolean(packet.insertAll);
    }

    public static GridInsertPacket decode(FriendlyByteBuf buffer) {
        boolean insertAll = buffer.readBoolean();
        return new GridInsertPacket(insertAll);
    }

    public static void handle(GridInsertPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                TowerNetwork network = null;

                if (player.containerMenu instanceof StorageControllerMenu menu) {
                    network = menu.getBlockEntity().getTower();
                } else if (player.containerMenu instanceof StorageCraftingControllerMenu menu) {
                    network = menu.getBlockEntity().getTower();
                }

                if (network != null) {
                    ItemStack carried = player.containerMenu.getCarried();
                    if (!carried.isEmpty()) {
                        if (packet.insertAll) {
                            ItemStack remaining = network.insertItem(carried, false);
                            player.containerMenu.setCarried(remaining);
                        } else {
                            ItemStack single = carried.copy();
                            single.setCount(1);
                            ItemStack remaining = network.insertItem(single, false);
                            if (remaining.isEmpty()) {
                                carried.shrink(1);
                                if (carried.isEmpty()) player.containerMenu.setCarried(ItemStack.EMPTY);
                            }
                        }

                        if (player.containerMenu instanceof StorageControllerMenu menu) {
                            menu.updateClientItemList();
                        } else if (player.containerMenu instanceof StorageCraftingControllerMenu menu) {
                            menu.updateClientItemList();
                        }
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
