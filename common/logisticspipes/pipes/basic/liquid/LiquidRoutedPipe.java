package logisticspipes.pipes.basic.liquid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.IRequireReliableLiquidTransport;
import logisticspipes.items.LogisticsLiquidContainer;
import logisticspipes.logic.BaseRoutingLogic;
import logisticspipes.logic.TemporaryLogic;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.PipeLiquidTransportLogistics;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.LiquidIdentifier;
import logisticspipes.utils.Pair;
import logisticspipes.utils.WorldUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import buildcraft.api.transport.IPipeEntry;
import logistics_bc.core.IMachine;
import logistics_bc.transport.EntityData;
import logistics_bc.transport.IItemTravelingHook;
import logistics_bc.transport.PipeTransportItems;
import logistics_bc.transport.lp_TileGenericPipe;

public abstract class LiquidRoutedPipe extends CoreRoutedPipe implements IItemTravelingHook {

	private WorldUtil worldUtil;
	
	public LiquidRoutedPipe(int itemID) {
		super(new PipeLiquidTransportLogistics(), new TemporaryLogic(), itemID);
		((PipeTransportItems) transport).travelHook = this;
		worldUtil = new WorldUtil(container.worldObj, getX(), getY(), getZ());
	}
	
	public LiquidRoutedPipe(BaseRoutingLogic logic, int itemID) {
		super(new PipeLiquidTransportLogistics(), logic, itemID);
		((PipeTransportItems) transport).travelHook = this;
		worldUtil = new WorldUtil(container.worldObj, getX(), getY(), getZ());
	}
	
	@Override
	public void setTile(TileEntity tile) {
		super.setTile(tile);
		worldUtil = new WorldUtil(container.worldObj, getX(), getY(), getZ());
	}

	@Override
	public boolean logisitcsIsPipeConnected(TileEntity tile) {
		if (tile instanceof IFluidHandler) {
			IFluidHandler liq = (IFluidHandler) tile;

			if (liq.getTankInfo(ForgeDirection.UNKNOWN) != null && liq.getTankInfo(ForgeDirection.UNKNOWN).length > 0)
				return true;
		}

		return tile instanceof IPipeEntry || (tile instanceof IMachine && ((IMachine) tile).manageFluids());
	}
	
	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getNonRoutedTexture(ForgeDirection connection) {
		if(isLiquidSidedTexture(connection)) {
			return Textures.LOGISTICSPIPE_LIQUID_TEXTURE;
		}
		return super.getNonRoutedTexture(connection);
	}
	
	private boolean isLiquidSidedTexture(ForgeDirection connection) {
		WorldUtil util = new WorldUtil(container.worldObj, getX(), getY(), getZ());
		TileEntity tile = util.getAdjacentTileEntitie(connection);
		if (tile instanceof IFluidHandler) {
			IFluidHandler liq = (IFluidHandler) tile;

			if (liq.getTankInfo(ForgeDirection.UNKNOWN) != null && liq.getTankInfo(ForgeDirection.UNKNOWN).length > 0)
				return true;
		}
		if(tile instanceof lp_TileGenericPipe) {
			return ((lp_TileGenericPipe)tile).pipe instanceof LogisticsLiquidConnectorPipe;
		}
		return false;
	}
	
	@Override
	public LogisticsModule getLogisticsModule() {
		return null;
	}
	
	/***
	 * @param flag  Weather to list a Nearby Pipe or not
	 */
	
	public final List<Pair<TileEntity,ForgeDirection>> getAdjacentTanks(boolean flag) {
		List<Pair<TileEntity,ForgeDirection>> tileList =  new ArrayList<Pair<TileEntity,ForgeDirection>>();
		for(ForgeDirection dir:ForgeDirection.VALID_DIRECTIONS) {
			TileEntity tile = worldUtil.getAdjacentTileEntitie(dir);
			if(!isConnectableTank(tile, dir, flag)) continue;
			tileList.add(new Pair<TileEntity,ForgeDirection>(tile, dir));
		}
		return tileList;
	}
	
	/***
	 * @param tile The connected TileEntity
	 * @param dir  The direction the TileEntity is in relative to the currect pipe
	 * @param flag Weather to list a Nearby Pipe or not
	 */
	
	public final boolean isConnectableTank(TileEntity tile, ForgeDirection dir, boolean flag) {
		if(!(tile instanceof IFluidHandler)) return false;
		if(!this.canPipeConnect(tile, dir)) return false;
		if(tile instanceof lp_TileGenericPipe) {
			if(((lp_TileGenericPipe)tile).pipe instanceof LiquidRoutedPipe) return false;
			if(!flag) return false;
		}
		if(tile instanceof TileGenericPipe){
			if(((lp_TileGenericPipe)tile).pipe == null || !(((TileGenericPipe)tile).pipe.transport instanceof IFluidHandler)) return false;
		}
		return true;
	}
	
	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if(canInsertFromSideToTanks()) {
			int validDirections = 0;
			List<Pair<TileEntity,ForgeDirection>> list = getAdjacentTanks(true);
			for(Pair<TileEntity,ForgeDirection> pair:list) {
				if(pair.getValue1() instanceof TileGenericPipe) {
					if(((TileGenericPipe)pair.getValue1()).pipe instanceof CoreRoutedPipe) continue;
				}
				LogisticsLiquidSection tank = ((PipeLiquidTransportLogistics)this.transport).sideTanks[pair.getValue2().ordinal()];
				validDirections++;
				if(tank.getFluid() == null) continue;
				int filled = ((IFluidHandler)pair.getValue1()).fill(pair.getValue2().getOpposite(), tank.getFluid(), true);
				if(filled == 0) continue;
				FluidStack drain = tank.drain(filled, true);
				if(drain == null || filled != drain.amount) {
					if(LogisticsPipes.DEBUG) {
						throw new UnsupportedOperationException("Liquid Multiplication");
					}
				}
			}
			if(validDirections == 0) return;
			LogisticsLiquidSection tank = ((PipeLiquidTransportLogistics)this.transport).internalTank;
			FluidStack stack = tank.getFluid();
			if(stack == null) return;
			for(Pair<TileEntity,ForgeDirection> pair:list) {
				if(pair.getValue1() instanceof TileGenericPipe) {
					if(((TileGenericPipe)pair.getValue1()).pipe instanceof CoreRoutedPipe) continue;
				}
				LogisticsLiquidSection tankSide = ((PipeLiquidTransportLogistics)this.transport).sideTanks[pair.getValue2().ordinal()];
				stack = tank.getFluid();
				if(stack == null) continue;
				stack = stack.copy();
				int filled = tankSide.fill(stack , true);
				if(filled == 0) continue;
				FluidStack drain = tank.drain(filled, true);
				if(drain == null || filled != drain.amount) {
					if(LogisticsPipes.DEBUG) {
						throw new UnsupportedOperationException("Liquid Multiplication");
					}
				}
			}
		}
	}

	public int countOnRoute(LiquidIdentifier ident) {
		int amount = 0;
		for(Iterator<IRoutedItem> iter = _inTransitToMe.iterator();iter.hasNext();) {
			IRoutedItem next = iter.next();
			ItemStack item = next.getItemStack();
			if(item.getItem() instanceof LogisticsLiquidContainer) {
				FluidStack liquid = SimpleServiceLocator.logisticsLiquidManager.getFluidFromContainer(item);
				if(LiquidIdentifier.get(liquid) == ident) {
					amount += liquid.amount;
				}
			}
		}
		return amount;
	}

	public abstract boolean canInsertFromSideToTanks();
	
	public abstract boolean canInsertToTanks();
	
	/* IItemTravelingHook */

	@Override
	public void drop(PipeTransportItems pipe, EntityData data) {}

	@Override
	public void centerReached(PipeTransportItems pipe, EntityData data) {}

	@Override
	public void endReached(PipeTransportItems pipe, EntityData data, TileEntity tile) {
		((PipeTransportLogistics)pipe).markChunkModified(tile);
		if(canInsertToTanks() && MainProxy.isServer(container.worldObj)) {
			if(!(data.item instanceof IRoutedItem) || data.item.getItemStack() == null || !(data.item.getItemStack().getItem() instanceof LogisticsLiquidContainer)) return;
			if(this.getRouter().getSimpleID() != ((IRoutedItem)data.item).getDestination()) return;
			((PipeTransportItems)this.transport).scheduleRemoval(data.item);
			int filled = 0;
			FluidStack liquid = SimpleServiceLocator.logisticsLiquidManager.getFluidFromContainer(data.item.getItemStack());
			if(this.isConnectableTank(tile, data.output, false)) {
				List<Pair<TileEntity,ForgeDirection>> adjTanks = getAdjacentTanks(false);
				//Try to put liquid into all adjacent tanks.
				for (int i = 0; i < adjTanks.size(); i++) {
					Pair<TileEntity,ForgeDirection> pair = adjTanks.get(i);
					IFluidHandler tank = (IFluidHandler) pair.getValue1();
					ForgeDirection dir = pair.getValue2();
					filled = tank.fill(dir.getOpposite(), liquid, true);
					liquid.amount -= filled;
					if (liquid.amount != 0) continue;
					return;
				}
				//Try inserting the liquid into the pipe side tank
				filled = ((PipeLiquidTransportLogistics)this.transport).sideTanks[data.output.ordinal()].fill(liquid, true);
				if(filled == liquid.amount) return;
				liquid.amount -= filled;
			}
			//Try inserting the liquid into the pipe internal tank
			filled = ((PipeLiquidTransportLogistics)this.transport).internalTank.fill(liquid, true);
			if(filled == liquid.amount) return;
			//If liquids still exist,
			liquid.amount -= filled;

			if(logic instanceof IRequireReliableLiquidTransport) {
				((IRequireReliableLiquidTransport)logic).liquidNotInserted(LiquidIdentifier.get(liquid), liquid.amount);
			}
			
			IRoutedItem routedItem = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(SimpleServiceLocator.logisticsLiquidManager.getFluidContainer(liquid), worldObj);
			Pair<Integer, Integer> replies = SimpleServiceLocator.logisticsLiquidManager.getBestReply(liquid, this.getRouter(), routedItem.getJamList());
			int dest = replies.getValue1();
			routedItem.setDestination(dest);
			routedItem.setTransportMode(TransportMode.Passive);
			this.queueRoutedItem(routedItem, data.output.getOpposite());
		}
	}

	@Override
	public boolean isLiquidPipe() {
		return true;
	}
	
	public boolean sharesTankWith(LiquidRoutedPipe other){
		List<TileEntity> theirs = other.getAllTankTiles();
		for(TileEntity tile:this.getAllTankTiles()) {
			if(theirs.contains(tile)) {
				return true;
			}
		}
		return false;
	}
	
	public List<TileEntity> getAllTankTiles() {
		List<TileEntity> list = new ArrayList<TileEntity>();
		for(Pair<TileEntity, ForgeDirection> pair:getAdjacentTanks(false)) {
			list.addAll(SimpleServiceLocator.specialTankHandler.getBaseTileFor(pair.getValue1()));
		}
		return list;
	}
}
