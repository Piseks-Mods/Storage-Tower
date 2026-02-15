package org.dpdns.pisekpiskovec.storagetower.screen.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.dpdns.pisekpiskovec.storagetower.block.entity.StorageCraftingControllerBlockEntity;
import org.dpdns.pisekpiskovec.storagetower.network.ModNetworking;
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.network.packet.StorageItemUpdatePacket;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;

import java.util.ArrayList;
import java.util.List;

public class StorageCraftingControllerMenu extends AbstractContainerMenu {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private final CraftingContainer craftingContainer;
    private final ResultContainer resultContainer;
    private final ContainerLevelAccess access;
    private final Player player;
    private String searchFilter = "";
    private List<ItemStack> clientItems = new ArrayList<>(); // Client-side cache
    private int updateCooldown = 0; // Track last update to avoid spam

    public StorageCraftingControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER_CRAFTING.get(), id);
        this.blockEntity = (StorageCraftingControllerBlockEntity) entity;
        this.craftingContainer = new TransientCraftingContainer(this, 3, 3);
        this.resultContainer = new ResultContainer();
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.player = playerInv.player;

        this.addSlot(new ResultSlot(this.player, this.craftingContainer, this.resultContainer, 0, 143, 73)); // Crafting result slot

        // Crafting grid
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(this.craftingContainer, col + row * 3, 53 + col * 18, 58 + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 126 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 184));
        }
    }

    public void setSearchFilter(String searchFilter) {
        this.searchFilter = searchFilter;
        updateClientItemList();
    }

    public String getSearchFilter() {
        return searchFilter;
    }

    public void updateClientItems(List<ItemStack> items) {
        this.clientItems = new ArrayList<>(items);
    }

    public List<ItemStack> getClientItems() {
        return clientItems;
    }

    public void clickItemGrid(int gridSlot, int button, ClickType clickType, Player player) {
        if (player.level().isClientSide) return;

        TowerNetwork network = blockEntity.getTower();
        if (network == null) return;

        List<ItemStack> serverItems = network.getAllItems(searchFilter);
        if (gridSlot < 0 || gridSlot >= serverItems.size()) return;

        ItemStack displayStack = serverItems.get(gridSlot);
        if (displayStack.isEmpty()) return;

        if (clickType == ClickType.PICKUP) {
            if (button == 0) { // Left click
                int amount = Math.min(displayStack.getCount(), displayStack.getMaxStackSize());
                ItemStack extracted = network.extractItem(displayStack, amount, false);
                if (!extracted.isEmpty()) player.containerMenu.setCarried(extracted);
            } else if (button == 1) { // Right click
                ItemStack extracted = network.extractItem(displayStack, 1, false);
                if (!extracted.isEmpty()) {
                    ItemStack carried = player.containerMenu.getCarried();
                    if (carried.isEmpty()) player.containerMenu.setCarried(extracted);
                    else if (ItemStack.isSameItemSameTags(carried, extracted)) carried.grow(1);
                }
            }
        } else if (clickType == ClickType.QUICK_MOVE) {
            if (button == 0) { // Shift+Left click
                int amount = Math.min(displayStack.getCount(), displayStack.getMaxStackSize());
                ItemStack extracted = network.extractItem(displayStack, amount, false);
                if (!extracted.isEmpty()) if (!player.getInventory().add(extracted)) player.drop(extracted, false);
            } else if (button == 1) { // Shift+Right click
                ItemStack extracted = network.extractItem(displayStack, 1, false);
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) player.drop(extracted, false);
                }
            }
        }
        updateClientItemList();
    }

    @Override
    public void slotsChanged(Container pContainer) {
        this.access.execute((level, pos) -> {
            slotChangedCraftingGrid(this, level, this.player, this.craftingContainer, this.resultContainer);
        });
    }

    protected static void slotChangedCraftingGrid(AbstractContainerMenu menu, Level level, Player player, CraftingContainer craftingContainer, ResultContainer resultContainer) {
        if (!level.isClientSide) {
            ItemStack result = ItemStack.EMPTY;
            var optional = level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, craftingContainer, level);

            if (optional.isPresent()) {
                CraftingRecipe recipe = optional.get();
                if (resultContainer.setRecipeUsed(level, (ServerPlayer) player, recipe))
                    result = recipe.assemble(craftingContainer, level.registryAccess());
            }

            resultContainer.setItem(0, result);
            menu.setRemoteSlot(0, result);
            menu.broadcastChanges();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(pIndex);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemStack = slotStack.copy();

            if (pIndex == 0) { // Result slot
                this.access.execute((level, pos) -> {
                    slotStack.getItem().onCraftedBy(slotStack, level, pPlayer);
                });

                if (!this.moveItemStackTo(slotStack, 10, 46, true)) return ItemStack.EMPTY;
                slot.onQuickCraft(slotStack, itemStack);
            } else if (pIndex >= 1 && pIndex < 10) { // From crafting grid
                if (!this.moveItemStackTo(slotStack, 10, 46, false)) return ItemStack.EMPTY;
            } else if (pIndex >= 10 && pIndex < 46) { // From player inventory
                if (!this.moveItemStackTo(slotStack, 1, 10, false)) {
                    // Try inserting to tower network
                    TowerNetwork network = blockEntity.getTower();
                    if (network != null) {
                        ItemStack remaining = network.insertItem(slotStack, false);
                        slotStack.setCount(remaining.getCount());
                        if (remaining.isEmpty()) slot.set(ItemStack.EMPTY);
                        else slot.setChanged();
                        updateClientItemList();
                        return itemStack;
                    }

                    if (pIndex < 37) if (!this.moveItemStackTo(slotStack, 37, 46, false)) return ItemStack.EMPTY;
                    else if (!this.moveItemStackTo(slotStack, 10, 37, false)) return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();
            if (slotStack.getCount() == itemStack.getCount()) return ItemStack.EMPTY;
            slot.onTake(pPlayer, slotStack);
            if (pIndex == 0) pPlayer.drop(slotStack, false);
        }
        return itemStack;
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

    private void updateClientItemList() {
        if (blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) return;

        TowerNetwork network = blockEntity.getTower();
        if (network != null && network.isValid()) {
            List<ItemStack> items = network.getAllItems(searchFilter);
            for (Player player : blockEntity.getLevel().players()) {
                if (player instanceof ServerPlayer serverPlayer && player.containerMenu == this) {
                    ModNetworking.sendToPlayer(new StorageItemUpdatePacket(this.containerId, items), serverPlayer);
                }
            }
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return blockEntity != null && !blockEntity.isRemoved() && pPlayer.distanceToSqr(blockEntity.getBlockPos().getX() + 0.5, blockEntity.getBlockPos().getY() + 0.5, blockEntity.getBlockPos().getZ() + 0.5) <= 64;
    }

    @Override
    public void removed(Player pPlayer) {
        super.removed(pPlayer);
        this.access.execute((level, pos) -> {
            this.clearContainer(pPlayer, this.craftingContainer);
        });
    }

    public StorageCraftingControllerBlockEntity getBlockEntity() {
        return blockEntity;
    }
}
