package logisticspipes.pipes;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import cpw.mods.fml.common.network.Player;

import logisticspipes.LogisticsPipes;
import logisticspipes.config.Configs;
import logisticspipes.interfaces.ILogisticsModule;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.logic.TemporaryLogic;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.NetworkConstants;
import logisticspipes.network.packets.PacketPipeBitSet;
import logisticspipes.pipes.basic.RoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.SearchNode;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.SimpleInventory;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public class PipeItemsFirewall extends RoutedPipe {

	private IRouter[] routers = new IRouter[ForgeDirection.VALID_DIRECTIONS.length];
	private String[] routerIds = new String[ForgeDirection.VALID_DIRECTIONS.length];
	
	public SimpleInventory inv = new SimpleInventory(6 * 6, "Filter Inv", 1);
	private boolean blockProvider = false;
	private boolean blockCrafer = false;
	private boolean blockSorting = false;
	private boolean isBlocking = true;
	
	public PipeItemsFirewall(int itemID) {
		super(new TemporaryLogic(), itemID);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}
	
	@Override
	public boolean blockActivated(World world, int x, int y, int z, EntityPlayer entityplayer) {
		if(SimpleServiceLocator.buildCraftProxy.isWrenchEquipped(entityplayer)) {
			entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_FIREWALL, world, x, y, z);
			MainProxy.sendPacketToPlayer(new PacketPipeBitSet(NetworkConstants.FIREWALL_FLAG_SET, xCoord, yCoord, zCoord, getFlags()).getPacket(), (Player) entityplayer);
			return true;
		} else {
			return super.blockActivated(world, x, y, z, entityplayer);
		}
	}

	public void ignoreDisableUpdateEntity() {
		for(ForgeDirection dir: ForgeDirection.VALID_DIRECTIONS) {
			getRouter(dir).update(worldObj.getWorldTime() % Configs.LOGISTICS_DETECTION_FREQUENCY == _delayOffset || _initialInit);
		}
	}
	
	@Override
	public IRouter getRouter(ForgeDirection dir) {
		if(dir.ordinal() < routers.length) {
			if (routers[dir.ordinal()] == null){
				synchronized (routerIdLock) {
					if (routerIds[dir.ordinal()] == null || routerIds[dir.ordinal()].isEmpty()) {
						routerIds[dir.ordinal()] = UUID.randomUUID().toString();
					}
					UUID routerUUId=UUID.fromString(routerIds[dir.ordinal()]);
					routers[dir.ordinal()] = SimpleServiceLocator.routerManager.getOrCreateRouter(routerUUId, MainProxy.getDimensionForWorld(worldObj), xCoord, yCoord, zCoord,true);
//					routers[dir.ordinal()] = SimpleServiceLocator.routerManager.getOrCreateFirewallRouter(UUID.fromString(routerIds[dir.ordinal()]), MainProxy.getDimensionForWorld(worldObj), xCoord, yCoord, zCoord, dir);
				}
			}
			return routers[dir.ordinal()];
		}
		return super.getRouter();
	}
	
	public IRouter getRouter() {
		if (router == null){
			synchronized (routerIdLock) {
				if (routerId == null || routerId == ""){
					routerId = UUID.randomUUID().toString();
				}
				router = SimpleServiceLocator.routerManager.getOrCreateFirewallRouter(UUID.fromString(routerId), MainProxy.getDimensionForWorld(worldObj), xCoord, yCoord, zCoord, ForgeDirection.UNKNOWN);
			}
		}
		return router;
	}
	
	public ForgeDirection getRouterSide(IRouter router) {
		for(ForgeDirection dir: ForgeDirection.VALID_DIRECTIONS) {
			if(getRouter(dir) == router) {
				return dir;
			}
		}
		return ForgeDirection.UNKNOWN;
	}

	public boolean idIdforOtherSide(int id) {
		for(ForgeDirection dir: ForgeDirection.VALID_DIRECTIONS) {
			if(getRouter(dir).getSimpleID() == id) {
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		
		synchronized (routerIdLock) {
			for(int i=0;i<routerIds.length;i++) {
				nbttagcompound.setString("routerId" + i, routerIds[i]);
			}
		}
		inv.writeToNBT(nbttagcompound);
		nbttagcompound.setBoolean("blockProvider", blockProvider);
		nbttagcompound.setBoolean("blockCrafer", blockCrafer);
		nbttagcompound.setBoolean("blockSorting", blockSorting);
		nbttagcompound.setBoolean("isBlocking", isBlocking);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		
		synchronized (routerIdLock) {
			for(int i=0;i<routerIds.length;i++) {
				routerIds[i] = nbttagcompound.getString("routerId" + i);
			}
		}
		inv.readFromNBT(nbttagcompound);
		blockProvider = nbttagcompound.getBoolean("blockProvider");
		blockCrafer = nbttagcompound.getBoolean("blockCrafer");
		blockSorting = nbttagcompound.getBoolean("blockSorting");
		isBlocking = nbttagcompound.getBoolean("isBlocking");
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_FIREWALL_TEXTURE;
	}

	@Override
	public ILogisticsModule getLogisticsModule() {
		return null;
	}
	
	public List<SearchNode> getRouters(IRouter from) {
		List<SearchNode> list = new ArrayList<SearchNode>();
		for(ForgeDirection dir: ForgeDirection.VALID_DIRECTIONS) {
			if(getRouter(dir).equals(from)) continue;
			List<SearchNode> nodes = getRouter(dir).getIRoutersByCost();
			list.addAll(nodes);
		}
		Collections.sort(list);
		return list;
	}
	
	public IFilter getFilter(final UUID id) {
		return new IFilter() {
			@Override
			public boolean isBlocked() {
				return isBlocking;
			}

			@Override
			public List<ItemIdentifier> getFilteredItems() {
				return inv.getItems();
			}

			@Override
			public boolean blockProvider() {
				return blockProvider;
			}

			@Override
			public boolean blockCrafting() {
				return blockCrafer;
			}

			@Override
			public UUID getUUID() {
				return id;
			}

			@Override
			public boolean blockRouting() {
				return blockSorting;
			}
		};
	}

	public boolean isBlockProvider() {
		return blockProvider;
	}

	public void setBlockProvider(boolean blockProvider) {
		this.blockProvider = blockProvider;
		MainProxy.sendPacketToServer(new PacketPipeBitSet(NetworkConstants.FIREWALL_FLAG_SET, xCoord, yCoord, zCoord, getFlags()).getPacket());
	}

	public boolean isBlockCrafer() {
		return blockCrafer;
	}

	public void setBlockCrafer(boolean blockCrafer) {
		this.blockCrafer = blockCrafer;
		MainProxy.sendPacketToServer(new PacketPipeBitSet(NetworkConstants.FIREWALL_FLAG_SET, xCoord, yCoord, zCoord, getFlags()).getPacket());
	}

	public boolean isBlockSorting() {
		return blockSorting;
	}

	public void setBlockSorting(boolean blockSorting) {
		this.blockSorting = blockSorting;
		MainProxy.sendPacketToServer(new PacketPipeBitSet(NetworkConstants.FIREWALL_FLAG_SET, xCoord, yCoord, zCoord, getFlags()).getPacket());
	}

	public boolean isBlocking() {
		return isBlocking;
	}

	public void setBlocking(boolean isBlocking) {
		this.isBlocking = isBlocking;
		MainProxy.sendPacketToServer(new PacketPipeBitSet(NetworkConstants.FIREWALL_FLAG_SET, xCoord, yCoord, zCoord, getFlags()).getPacket());
	}
	
	private BitSet getFlags() {
		BitSet flags = new BitSet();
		flags.set(0, blockProvider);
		flags.set(1, blockCrafer);
		flags.set(2, blockSorting);
		flags.set(3, isBlocking);
		return flags;
	}
	
	public void setFlags(BitSet flags) {
		blockProvider = flags.get(0);
		blockCrafer = flags.get(1);
		blockSorting = flags.get(2);
		isBlocking = flags.get(3);
	}
}