
package cz.pisekpiskovec.storagetower.gui;

import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.inventory.container.Slot;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Container;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.client.gui.ScreenManager;

import java.util.stream.Stream;
import java.util.function.Supplier;
import java.util.Map;
import java.util.HashMap;
import java.util.AbstractMap;

import cz.pisekpiskovec.storagetower.procedures.StorageControlerUpdateProcedure;
import cz.pisekpiskovec.storagetower.procedures.StorageControlerUpProcedure;
import cz.pisekpiskovec.storagetower.procedures.StorageControlerSyncProcedure;
import cz.pisekpiskovec.storagetower.procedures.StorageControlerOpenManagerProcedure;
import cz.pisekpiskovec.storagetower.procedures.StorageControlerDownProcedure;
import cz.pisekpiskovec.storagetower.procedures.StorageControlerCloseManagerProcedure;
import cz.pisekpiskovec.storagetower.PiseksStorageTowerModElements;
import cz.pisekpiskovec.storagetower.PiseksStorageTowerMod;

@PiseksStorageTowerModElements.ModElement.Tag
public class StorageControlerGUIGui extends PiseksStorageTowerModElements.ModElement {
	public static HashMap guistate = new HashMap();
	private static ContainerType<GuiContainerMod> containerType = null;

	public StorageControlerGUIGui(PiseksStorageTowerModElements instance) {
		super(instance, 5);
		elements.addNetworkMessage(ButtonPressedMessage.class, ButtonPressedMessage::buffer, ButtonPressedMessage::new,
				ButtonPressedMessage::handler);
		elements.addNetworkMessage(GUISlotChangedMessage.class, GUISlotChangedMessage::buffer, GUISlotChangedMessage::new,
				GUISlotChangedMessage::handler);
		containerType = new ContainerType<>(new GuiContainerModFactory());
		FMLJavaModLoadingContext.get().getModEventBus().register(new ContainerRegisterHandler());
		MinecraftForge.EVENT_BUS.register(this);
	}

	private static class ContainerRegisterHandler {
		@SubscribeEvent
		public void registerContainer(RegistryEvent.Register<ContainerType<?>> event) {
			event.getRegistry().register(containerType.setRegistryName("storage_controler_gui"));
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void initElements() {
		DeferredWorkQueue.runLater(() -> ScreenManager.registerFactory(containerType, StorageControlerGUIGuiWindow::new));
	}

	@SubscribeEvent
	public void onPlayerTick(TickEvent.PlayerTickEvent event) {
		PlayerEntity entity = event.player;
		if (event.phase == TickEvent.Phase.END && entity.openContainer instanceof GuiContainerMod) {
			World world = entity.world;
			double x = entity.getPosX();
			double y = entity.getPosY();
			double z = entity.getPosZ();

			StorageControlerSyncProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
	}

	public static class GuiContainerModFactory implements IContainerFactory {
		public GuiContainerMod create(int id, PlayerInventory inv, PacketBuffer extraData) {
			return new GuiContainerMod(id, inv, extraData);
		}
	}

	public static class GuiContainerMod extends Container implements Supplier<Map<Integer, Slot>> {
		World world;
		PlayerEntity entity;
		int x, y, z;
		private IItemHandler internal;
		private Map<Integer, Slot> customSlots = new HashMap<>();
		private boolean bound = false;

		public GuiContainerMod(int id, PlayerInventory inv, PacketBuffer extraData) {
			super(containerType, id);
			this.entity = inv.player;
			this.world = inv.player.world;
			this.internal = new ItemStackHandler(36);
			BlockPos pos = null;
			if (extraData != null) {
				pos = extraData.readBlockPos();
				this.x = pos.getX();
				this.y = pos.getY();
				this.z = pos.getZ();
			}
			if (pos != null) {
				if (extraData.readableBytes() == 1) { // bound to item
					byte hand = extraData.readByte();
					ItemStack itemstack;
					if (hand == 0)
						itemstack = this.entity.getHeldItemMainhand();
					else
						itemstack = this.entity.getHeldItemOffhand();
					itemstack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability -> {
						this.internal = capability;
						this.bound = true;
					});
				} else if (extraData.readableBytes() > 1) {
					extraData.readByte(); // drop padding
					Entity entity = world.getEntityByID(extraData.readVarInt());
					if (entity != null)
						entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability -> {
							this.internal = capability;
							this.bound = true;
						});
				} else { // might be bound to block
					TileEntity ent = inv.player != null ? inv.player.world.getTileEntity(pos) : null;
					if (ent != null) {
						ent.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).ifPresent(capability -> {
							this.internal = capability;
							this.bound = true;
						});
					}
				}
			}
			this.customSlots.put(0, this.addSlot(new SlotItemHandler(internal, 0, 7, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(0, 0, 0);
				}
			}));
			this.customSlots.put(1, this.addSlot(new SlotItemHandler(internal, 1, 25, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(1, 0, 0);
				}
			}));
			this.customSlots.put(2, this.addSlot(new SlotItemHandler(internal, 2, 43, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(2, 0, 0);
				}
			}));
			this.customSlots.put(3, this.addSlot(new SlotItemHandler(internal, 3, 61, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(3, 0, 0);
				}
			}));
			this.customSlots.put(4, this.addSlot(new SlotItemHandler(internal, 4, 79, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(4, 0, 0);
				}
			}));
			this.customSlots.put(5, this.addSlot(new SlotItemHandler(internal, 5, 97, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(5, 0, 0);
				}
			}));
			this.customSlots.put(6, this.addSlot(new SlotItemHandler(internal, 6, 115, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(6, 0, 0);
				}
			}));
			this.customSlots.put(7, this.addSlot(new SlotItemHandler(internal, 7, 133, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(7, 0, 0);
				}
			}));
			this.customSlots.put(8, this.addSlot(new SlotItemHandler(internal, 8, 151, 30) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(8, 0, 0);
				}
			}));
			this.customSlots.put(9, this.addSlot(new SlotItemHandler(internal, 9, 7, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(9, 0, 0);
				}
			}));
			this.customSlots.put(10, this.addSlot(new SlotItemHandler(internal, 10, 25, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(10, 0, 0);
				}
			}));
			this.customSlots.put(11, this.addSlot(new SlotItemHandler(internal, 11, 43, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(11, 0, 0);
				}
			}));
			this.customSlots.put(12, this.addSlot(new SlotItemHandler(internal, 12, 61, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(12, 0, 0);
				}
			}));
			this.customSlots.put(13, this.addSlot(new SlotItemHandler(internal, 13, 79, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(13, 0, 0);
				}
			}));
			this.customSlots.put(14, this.addSlot(new SlotItemHandler(internal, 14, 97, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(14, 0, 0);
				}
			}));
			this.customSlots.put(15, this.addSlot(new SlotItemHandler(internal, 15, 115, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(15, 0, 0);
				}
			}));
			this.customSlots.put(16, this.addSlot(new SlotItemHandler(internal, 16, 133, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(16, 0, 0);
				}
			}));
			this.customSlots.put(17, this.addSlot(new SlotItemHandler(internal, 17, 151, 48) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(17, 0, 0);
				}
			}));
			this.customSlots.put(18, this.addSlot(new SlotItemHandler(internal, 18, 7, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(18, 0, 0);
				}
			}));
			this.customSlots.put(19, this.addSlot(new SlotItemHandler(internal, 19, 25, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(19, 0, 0);
				}
			}));
			this.customSlots.put(20, this.addSlot(new SlotItemHandler(internal, 20, 43, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(20, 0, 0);
				}
			}));
			this.customSlots.put(21, this.addSlot(new SlotItemHandler(internal, 21, 61, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(21, 0, 0);
				}
			}));
			this.customSlots.put(22, this.addSlot(new SlotItemHandler(internal, 22, 79, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(22, 0, 0);
				}
			}));
			this.customSlots.put(23, this.addSlot(new SlotItemHandler(internal, 23, 97, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(23, 0, 0);
				}
			}));
			this.customSlots.put(24, this.addSlot(new SlotItemHandler(internal, 24, 115, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(24, 0, 0);
				}
			}));
			this.customSlots.put(25, this.addSlot(new SlotItemHandler(internal, 25, 133, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(25, 0, 0);
				}
			}));
			this.customSlots.put(26, this.addSlot(new SlotItemHandler(internal, 26, 151, 66) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(26, 0, 0);
				}
			}));
			this.customSlots.put(27, this.addSlot(new SlotItemHandler(internal, 27, 7, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(27, 0, 0);
				}
			}));
			this.customSlots.put(28, this.addSlot(new SlotItemHandler(internal, 28, 25, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(28, 0, 0);
				}
			}));
			this.customSlots.put(29, this.addSlot(new SlotItemHandler(internal, 29, 43, 84) {
			}));
			this.customSlots.put(30, this.addSlot(new SlotItemHandler(internal, 30, 61, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(30, 0, 0);
				}
			}));
			this.customSlots.put(31, this.addSlot(new SlotItemHandler(internal, 31, 79, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(31, 0, 0);
				}
			}));
			this.customSlots.put(32, this.addSlot(new SlotItemHandler(internal, 32, 97, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(32, 0, 0);
				}
			}));
			this.customSlots.put(33, this.addSlot(new SlotItemHandler(internal, 33, 115, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(33, 0, 0);
				}
			}));
			this.customSlots.put(34, this.addSlot(new SlotItemHandler(internal, 34, 133, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(34, 0, 0);
				}
			}));
			this.customSlots.put(35, this.addSlot(new SlotItemHandler(internal, 35, 151, 84) {
				@Override
				public void onSlotChanged() {
					super.onSlotChanged();
					GuiContainerMod.this.slotChanged(35, 0, 0);
				}
			}));
			int si;
			int sj;
			for (si = 0; si < 3; ++si)
				for (sj = 0; sj < 9; ++sj)
					this.addSlot(new Slot(inv, sj + (si + 1) * 9, 0 + 8 + sj * 18, 28 + 84 + si * 18));
			for (si = 0; si < 9; ++si)
				this.addSlot(new Slot(inv, si, 0 + 8 + si * 18, 28 + 142));

			StorageControlerOpenManagerProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}

		public Map<Integer, Slot> get() {
			return customSlots;
		}

		@Override
		public boolean canInteractWith(PlayerEntity player) {
			return true;
		}

		@Override
		public ItemStack transferStackInSlot(PlayerEntity playerIn, int index) {
			ItemStack itemstack = ItemStack.EMPTY;
			Slot slot = (Slot) this.inventorySlots.get(index);
			if (slot != null && slot.getHasStack()) {
				ItemStack itemstack1 = slot.getStack();
				itemstack = itemstack1.copy();
				if (index < 36) {
					if (!this.mergeItemStack(itemstack1, 36, this.inventorySlots.size(), true)) {
						return ItemStack.EMPTY;
					}
					slot.onSlotChange(itemstack1, itemstack);
				} else if (!this.mergeItemStack(itemstack1, 0, 36, false)) {
					if (index < 36 + 27) {
						if (!this.mergeItemStack(itemstack1, 36 + 27, this.inventorySlots.size(), true)) {
							return ItemStack.EMPTY;
						}
					} else {
						if (!this.mergeItemStack(itemstack1, 36, 36 + 27, false)) {
							return ItemStack.EMPTY;
						}
					}
					return ItemStack.EMPTY;
				}
				if (itemstack1.getCount() == 0) {
					slot.putStack(ItemStack.EMPTY);
				} else {
					slot.onSlotChanged();
				}
				if (itemstack1.getCount() == itemstack.getCount()) {
					return ItemStack.EMPTY;
				}
				slot.onTake(playerIn, itemstack1);
			}
			return itemstack;
		}

		@Override /** 
					* Merges provided ItemStack with the first avaliable one in the container/player inventor between minIndex (included) and maxIndex (excluded). Args : stack, minIndex, maxIndex, negativDirection. /!\ the Container implementation do not check if the item is valid for the slot
					*/
		protected boolean mergeItemStack(ItemStack stack, int startIndex, int endIndex, boolean reverseDirection) {
			boolean flag = false;
			int i = startIndex;
			if (reverseDirection) {
				i = endIndex - 1;
			}
			if (stack.isStackable()) {
				while (!stack.isEmpty()) {
					if (reverseDirection) {
						if (i < startIndex) {
							break;
						}
					} else if (i >= endIndex) {
						break;
					}
					Slot slot = this.inventorySlots.get(i);
					ItemStack itemstack = slot.getStack();
					if (slot.isItemValid(itemstack) && !itemstack.isEmpty() && areItemsAndTagsEqual(stack, itemstack)) {
						int j = itemstack.getCount() + stack.getCount();
						int maxSize = Math.min(slot.getSlotStackLimit(), stack.getMaxStackSize());
						if (j <= maxSize) {
							stack.setCount(0);
							itemstack.setCount(j);
							slot.putStack(itemstack);
							flag = true;
						} else if (itemstack.getCount() < maxSize) {
							stack.shrink(maxSize - itemstack.getCount());
							itemstack.setCount(maxSize);
							slot.putStack(itemstack);
							flag = true;
						}
					}
					if (reverseDirection) {
						--i;
					} else {
						++i;
					}
				}
			}
			if (!stack.isEmpty()) {
				if (reverseDirection) {
					i = endIndex - 1;
				} else {
					i = startIndex;
				}
				while (true) {
					if (reverseDirection) {
						if (i < startIndex) {
							break;
						}
					} else if (i >= endIndex) {
						break;
					}
					Slot slot1 = this.inventorySlots.get(i);
					ItemStack itemstack1 = slot1.getStack();
					if (itemstack1.isEmpty() && slot1.isItemValid(stack)) {
						if (stack.getCount() > slot1.getSlotStackLimit()) {
							slot1.putStack(stack.split(slot1.getSlotStackLimit()));
						} else {
							slot1.putStack(stack.split(stack.getCount()));
						}
						slot1.onSlotChanged();
						flag = true;
						break;
					}
					if (reverseDirection) {
						--i;
					} else {
						++i;
					}
				}
			}
			return flag;
		}

		@Override
		public void onContainerClosed(PlayerEntity playerIn) {
			super.onContainerClosed(playerIn);

			StorageControlerCloseManagerProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
			if (!bound && (playerIn instanceof ServerPlayerEntity)) {
				if (!playerIn.isAlive() || playerIn instanceof ServerPlayerEntity && ((ServerPlayerEntity) playerIn).hasDisconnected()) {
					for (int j = 0; j < internal.getSlots(); ++j) {
						playerIn.dropItem(internal.extractItem(j, internal.getStackInSlot(j).getCount(), false), false);
					}
				} else {
					for (int i = 0; i < internal.getSlots(); ++i) {
						playerIn.inventory.placeItemBackInInventory(playerIn.world,
								internal.extractItem(i, internal.getStackInSlot(i).getCount(), false));
					}
				}
			}
		}

		private void slotChanged(int slotid, int ctype, int meta) {
			if (this.world != null && this.world.isRemote()) {
				PiseksStorageTowerMod.PACKET_HANDLER.sendToServer(new GUISlotChangedMessage(slotid, x, y, z, ctype, meta));
				handleSlotAction(entity, slotid, ctype, meta, x, y, z);
			}
		}
	}

	public static class ButtonPressedMessage {
		int buttonID, x, y, z;

		public ButtonPressedMessage(PacketBuffer buffer) {
			this.buttonID = buffer.readInt();
			this.x = buffer.readInt();
			this.y = buffer.readInt();
			this.z = buffer.readInt();
		}

		public ButtonPressedMessage(int buttonID, int x, int y, int z) {
			this.buttonID = buttonID;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public static void buffer(ButtonPressedMessage message, PacketBuffer buffer) {
			buffer.writeInt(message.buttonID);
			buffer.writeInt(message.x);
			buffer.writeInt(message.y);
			buffer.writeInt(message.z);
		}

		public static void handler(ButtonPressedMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				PlayerEntity entity = context.getSender();
				int buttonID = message.buttonID;
				int x = message.x;
				int y = message.y;
				int z = message.z;
				handleButtonAction(entity, buttonID, x, y, z);
			});
			context.setPacketHandled(true);
		}
	}

	public static class GUISlotChangedMessage {
		int slotID, x, y, z, changeType, meta;

		public GUISlotChangedMessage(int slotID, int x, int y, int z, int changeType, int meta) {
			this.slotID = slotID;
			this.x = x;
			this.y = y;
			this.z = z;
			this.changeType = changeType;
			this.meta = meta;
		}

		public GUISlotChangedMessage(PacketBuffer buffer) {
			this.slotID = buffer.readInt();
			this.x = buffer.readInt();
			this.y = buffer.readInt();
			this.z = buffer.readInt();
			this.changeType = buffer.readInt();
			this.meta = buffer.readInt();
		}

		public static void buffer(GUISlotChangedMessage message, PacketBuffer buffer) {
			buffer.writeInt(message.slotID);
			buffer.writeInt(message.x);
			buffer.writeInt(message.y);
			buffer.writeInt(message.z);
			buffer.writeInt(message.changeType);
			buffer.writeInt(message.meta);
		}

		public static void handler(GUISlotChangedMessage message, Supplier<NetworkEvent.Context> contextSupplier) {
			NetworkEvent.Context context = contextSupplier.get();
			context.enqueueWork(() -> {
				PlayerEntity entity = context.getSender();
				int slotID = message.slotID;
				int changeType = message.changeType;
				int meta = message.meta;
				int x = message.x;
				int y = message.y;
				int z = message.z;
				handleSlotAction(entity, slotID, changeType, meta, x, y, z);
			});
			context.setPacketHandled(true);
		}
	}

	static void handleButtonAction(PlayerEntity entity, int buttonID, int x, int y, int z) {
		World world = entity.world;
		// security measure to prevent arbitrary chunk generation
		if (!world.isBlockLoaded(new BlockPos(x, y, z)))
			return;
		if (buttonID == 0) {

			StorageControlerUpProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (buttonID == 1) {

			StorageControlerDownProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
	}

	private static void handleSlotAction(PlayerEntity entity, int slotID, int changeType, int meta, int x, int y, int z) {
		World world = entity.world;
		// security measure to prevent arbitrary chunk generation
		if (!world.isBlockLoaded(new BlockPos(x, y, z)))
			return;
		if (slotID == 0 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 1 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 2 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 3 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 4 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 5 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 6 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 7 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 8 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 9 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 10 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 11 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 12 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 13 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 14 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 15 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 16 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 17 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 18 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 19 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 20 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 21 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 22 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 23 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 24 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 25 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 26 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 27 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 28 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 30 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 31 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 32 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 33 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 34 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
		if (slotID == 35 && changeType == 0) {

			StorageControlerUpdateProcedure.executeProcedure(Stream
					.of(new AbstractMap.SimpleEntry<>("world", world), new AbstractMap.SimpleEntry<>("x", x), new AbstractMap.SimpleEntry<>("y", y),
							new AbstractMap.SimpleEntry<>("z", z))
					.collect(HashMap::new, (_m, _e) -> _m.put(_e.getKey(), _e.getValue()), Map::putAll));
		}
	}
}
