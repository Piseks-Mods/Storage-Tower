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
import org.dpdns.pisekpiskovec.storagetower.network.TowerNetwork;
import org.dpdns.pisekpiskovec.storagetower.screen.ModMenuTypes;

public class StorageCraftingControllerMenu extends AbstractContainerMenu {
    private final StorageCraftingControllerBlockEntity blockEntity;
    private final CraftingContainer craftingContainer;
    private final ResultContainer resultContainer;
    private final ContainerLevelAccess access;
    private final Player player;
    private static final int STORAGE_SLOTS = 3; // 3 vertical slots
    private final StorageDisplayContainer storageContainer;

    public StorageCraftingControllerMenu(int id, Inventory playerInv, BlockEntity entity) {
        super(ModMenuTypes.STORAGE_CONTROLLER_CRAFTING.get(), id);
        this.blockEntity = (StorageCraftingControllerBlockEntity) entity;
        this.craftingContainer = new TransientCraftingContainer(this, 3, 3);
        this.resultContainer = new ResultContainer();
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.player = playerInv.player;
        this.storageContainer = new StorageDisplayContainer(blockEntity);

        // Storage display slots
        for (int i = 0; i < 3; i++) {
            this.addSlot(new StorageDisplaySlot(storageContainer, i, 9, 17 + i * 18, this));
        }

        // Crafting result slot
        this.addSlot(new ResultSlot(this.player, this.craftingContainer, this.resultContainer, 0, 124, 35));

        // Crafting grid
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(this.craftingContainer, col + row * 3, 30 + col * 18, 17 + row * 18));
            }
        }

        // Player inventory
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Player hotbar
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInv, col, 8 + col * 18, 142));
        }
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
                if (resultContainer.setRecipeUsed(level, (ServerPlayer) player, recipe)) {
                    result = recipe.assemble(craftingContainer, level.registryAccess());
                }
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

            if (pIndex < STORAGE_SLOTS) {
                // From storage display to player inventory
                // This is a ghost slot, so we need to extract from the network manually
                TowerNetwork network = blockEntity.getTower();
                if (network != null && !slotStack.isEmpty()) {
                    // Extract the item from the network
                    ItemStack extracted = network.extractItem(slotStack, slotStack.getCount(), false);

                    if (!extracted.isEmpty()) {
                        if (!pPlayer.getInventory().add(extracted)) /* Try to add to player inventory */ {
                            pPlayer.drop(extracted, false); // If couldn't add, drop it
                        }
                    }

                    this.broadcastChanges();
                }
            } else if (pIndex == STORAGE_SLOTS) {
                // Result slot - craft the item
                this.access.execute((level, pos) -> {
                    slotStack.getItem().onCraftedBy(slotStack, level, pPlayer);
                });

                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 46, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onQuickCraft(slotStack, itemStack);
            } else if (pIndex >= STORAGE_SLOTS + 1 && pIndex < STORAGE_SLOTS + 10) {
                // From crafting grid to player inventory
                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 46, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (pIndex >= STORAGE_SLOTS + 10 && pIndex < STORAGE_SLOTS + 46) {
                // From player inventory
                // Try crafting grid first, then storage
                if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 1, STORAGE_SLOTS + 10, false)) {
                    // Try inserting into tower network
                    TowerNetwork network = blockEntity.getTower();
                    if (network != null) {
                        ItemStack remaining = network.insertItem(slotStack, false);
                        slotStack.setCount(remaining.getCount());
                        if (remaining.isEmpty()) {
                            slot.set(ItemStack.EMPTY);
                        } else {
                            slot.setChanged();
                        }
                        this.broadcastChanges();
                        return itemStack;
                    }

                    if (pIndex < STORAGE_SLOTS + 37) {
                        if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 37, STORAGE_SLOTS + 46, false)) {
                            return ItemStack.EMPTY;
                        }
                    } else if (!this.moveItemStackTo(slotStack, STORAGE_SLOTS + 10, STORAGE_SLOTS + 37, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }

            if (slotStack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemStack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(pPlayer, slotStack);
            if (pIndex == STORAGE_SLOTS) {
                pPlayer.drop(slotStack, false);
            }
        }

        return itemStack;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Update all ghost slots with current network data
        TowerNetwork network = blockEntity.getTower();
        if (network != null) {
            var items = network.getAllItems();
            for (int i = 0; i < STORAGE_SLOTS; i++) {
                ItemStack newStack = i < items.size() ? items.get(i) : ItemStack.EMPTY;
                this.setRemoteSlot(i, newStack);
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

    private static class StorageDisplayContainer implements Container {
        private final StorageCraftingControllerBlockEntity blockEntity;

        public StorageDisplayContainer(StorageCraftingControllerBlockEntity blockEntity) {
            this.blockEntity = blockEntity;
        }

        @Override
        public int getContainerSize() {
            return 3;
        }

        @Override
        public boolean isEmpty() {
            TowerNetwork network = blockEntity.getTower();
            return network == null || network.getAllItems().isEmpty();
        }

        @Override
        public ItemStack getItem(int pSlot) {
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                var items = network.getAllItems();
                if (pSlot < items.size()) {
                    return items.get(pSlot);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int pSlot, int pAmount) {
            TowerNetwork network = blockEntity.getTower();
            if (network != null) {
                var items = network.getAllItems();
                if (pSlot < items.size()) {
                    ItemStack stack = items.get(pSlot);
                    ItemStack filter = stack.copy();
                    return network.extractItem(filter, Math.min(pAmount, stack.getCount()), false);
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItemNoUpdate(int pSlot) {
            return removeItem(pSlot, 64);
        }

        @Override
        public void setItem(int pSlot, ItemStack pStack) {
            // Noop
        }

        @Override
        public void setChanged() {
            blockEntity.setChanged();
        }

        @Override
        public boolean stillValid(Player pPlayer) {
            return true;
        }

        @Override
        public void clearContent() {
            // Noop
        }
    }

    private static class StorageDisplaySlot extends Slot {
        private final AbstractContainerMenu menu;

        public StorageDisplaySlot(Container container, int index, int x, int y, AbstractContainerMenu menu) {
            super(container, index, x, y);
            this.menu = menu;
        }

        @Override
        public boolean mayPlace(ItemStack pStack) {
            return false; // Can't place items directly in display slots
        }

        @Override
        public boolean mayPickup(Player pPlayer) {
            return true;
        }

        @Override
        public ItemStack remove(int pAmount) {
            // Only process on server side to avoid double extraction
            if (this.container instanceof StorageDisplayContainer displayContainer) {
                if (displayContainer.blockEntity.getLevel() != null && displayContainer.blockEntity.getLevel().isClientSide) {
                    // On client, just return what we think we are removing
                    ItemStack displayStack = getItem();
                    if (!displayStack.isEmpty()) {
                        ItemStack result = displayStack.copy();
                        result.setCount(Math.min(pAmount, displayStack.getCount()));
                        return result;
                    }
                    return ItemStack.EMPTY;
                }
            }

            // Server side - do the actual extraction
            ItemStack displayStack = getItem();
            if (displayStack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            ItemStack extracted = this.container.removeItem(this.index, pAmount); // Extract from the network

            // Mark container as changed to trigger client sync
            if (!extracted.isEmpty()) {
                this.setChanged();
            }

            return extracted;
        }

        @Override
        public void onTake(Player pPlayer, ItemStack pStack) {
            this.setChanged();
            this.menu.broadcastChanges();
            super.onTake(pPlayer, pStack);
        }

        @Override
        public void set(ItemStack pStack) {
            // Noop
        }

        @Override
        public ItemStack safeInsert(ItemStack pStack) {
            return pStack; // Ghosts can't accept items
        }

        @Override
        public ItemStack safeInsert(ItemStack pStack, int pIncrement) {
            return pStack; // Ghosts can't accept items
        }
    }
}
