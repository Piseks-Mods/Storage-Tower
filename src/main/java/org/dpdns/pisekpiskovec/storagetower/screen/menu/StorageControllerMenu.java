package org.dpdns.pisekpiskovec.storagetower.screen.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.ModNetworking;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.network.packet.StorageItemUpdatePacket;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;

import java.util.ArrayList;
import java.util.List;

public class StorageControllerMenu extends AbstractContainerMenu {
    private final StorageControllerBlockEntity blockEntity;
    private String searchFilter = "";
    private List<ItemStack> clientItems = new ArrayList<>(); // Client-side cache
    private int updateCooldown = 0; // Track last update to avoid spam
    private int clientTotalSlots = 0;

    public StorageControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER.get(), id);
        this.blockEntity = (StorageControllerBlockEntity) entity;

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 122 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 180));
        }
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
        if (blockEntity.getLevel() != null && !blockEntity.getLevel().isClientSide) updateClientItemList();
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void updateClientItems(List<ItemStack> items, int totalSlots) {
        this.clientItems = new ArrayList<>(items);
        this.clientTotalSlots = totalSlots;
    }

    public int getClientTotalSlots() {
        return this.clientTotalSlots;
    }

    public List<ItemStack> getClientItems() {
        return clientItems;
    }

    public void clickItemGrid(int gridSlot, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide) return; // Server-side only

        TowerNetwork network = blockEntity.getTower();
        if (network == null) return;

        List<ItemStack> serverItems = network.getAllItems(searchFilter);
        if (gridSlot < 0 || gridSlot >= serverItems.size()) return;

        ItemStack displayStack = serverItems.get(gridSlot);
        if (displayStack.isEmpty()) return;

        if (clickType == ClickType.PICKUP) {
            if (button == 0) { // Left click - extract full stack
                int amount = Math.min(displayStack.getCount(), displayStack.getMaxStackSize());
                ItemStack extracted = network.extractItem(displayStack, amount, false);
                if (!extracted.isEmpty()) player.containerMenu.setCarried(extracted);
            } else if (button == 1) { // Right click - extract 1 item
                int amount = ((int) Math.ceil(Math.min(displayStack.getCount(), displayStack.getMaxStackSize()) / 2.0));
                ItemStack extracted = network.extractItem(displayStack, amount, false);
                if (!extracted.isEmpty()) {
                    ItemStack carried = player.containerMenu.getCarried();
                    if (carried.isEmpty()) player.containerMenu.setCarried(extracted);
                    else if (ItemStack.isSameItemSameTags(carried, extracted)) carried.grow(1);
                }
            }
        } else if (clickType == ClickType.QUICK_MOVE) {
            if (button == 0) { // Shift+Left click - full stack to inventory
                int amount = Math.min(displayStack.getCount(), displayStack.getMaxStackSize());
                ItemStack extracted = network.extractItem(displayStack, amount, false);
                if (!extracted.isEmpty()) if (!player.getInventory().add(extracted)) player.drop(extracted, false);
            } else if (button == 1) { // Shift+Right click - 1 item to inventory
                ItemStack extracted = network.extractItem(displayStack, 1, false);
                if (!extracted.isEmpty()) if (!player.getInventory().add(extracted)) player.drop(extracted, false);
            }
        }

        updateClientItemList();
    }

    @Override
    public void clicked(int pSlotId, int pButton, ClickType pClickType, Player pPlayer) {
        super.clicked(pSlotId, pButton, pClickType, pPlayer);
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        if (pPlayer.level().isClientSide) return ItemStack.EMPTY;

        Slot slot = this.slots.get(pIndex);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack slotStack = slot.getItem();
        ItemStack originalStack = slotStack.copy();

        // From player to storage
        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            ItemStack remaining = network.insertItem(slotStack, false);

            if (remaining.getCount() < originalStack.getCount()) {
                slotStack.setCount(remaining.getCount());
                if (remaining.isEmpty()) slot.set(ItemStack.EMPTY);
                else slot.setChanged();
                updateClientItemList();
                return originalStack;
            } else {
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;

        updateCooldown--;
        if (updateCooldown <= 0) {
            updateCooldown = 5;
            updateClientItemList();
        }
    }

    public void updateClientItemList() {
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;

        TowerNetwork network = blockEntity.getTower();
        if (network != null && network.isValid()) {
            List<ItemStack> items = network.getAllItems(searchFilter);
            int totalSlots = network.getTotalSlots();

            // Send to all players viewing this menu
            for (Player player : blockEntity.getLevel().players()) {
                if (player instanceof ServerPlayer serverPlayer && player.containerMenu == this) {
                    ModNetworking.sendToPlayer(new StorageItemUpdatePacket(this.containerId, items, totalSlots), serverPlayer);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return blockEntity != null && !blockEntity.isRemoved() && pPlayer.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }

    public StorageControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
