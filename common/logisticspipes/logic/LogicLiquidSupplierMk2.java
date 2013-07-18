package logisticspipes.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import logisticspipes.LogisticsPipes;
import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.interfaces.routing.IRequestLiquid;
import logisticspipes.interfaces.routing.IRequireReliableLiquidTransport;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.LiquidSupplierAmount;
import logisticspipes.pipes.PipeLiquidSupplierMk2;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestTree;
import logisticspipes.utils.AdjacentTile;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.LiquidIdentifier;
import logisticspipes.utils.SimpleInventory;
import logisticspipes.utils.WorldUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.IFluidHandler;
import logistics_bc.transport.lp_TileGenericPipe;
import cpw.mods.fml.common.network.Player;

public class LogicLiquidSupplierMk2 extends BaseRoutingLogic implements IRequireReliableLiquidTransport {

	private SimpleInventory dummyInventory = new SimpleInventory(1, "Liquid to keep stocked", 127);
	private int amount = 0;
	
	private final Map<LiquidIdentifier, Integer> _requestedItems = new HashMap<LiquidIdentifier, Integer>();
	
	private boolean _requestPartials = false;
	
	public IRoutedPowerProvider _power;

	public LogicLiquidSupplierMk2(){
		throttleTime = 100;
	}

	@Override
	public void throttledUpdateEntity() {
		if (MainProxy.isClient(container.worldObj)) return;
		super.throttledUpdateEntity();
		if(dummyInventory.getStackInSlot(0) == null) return;
		WorldUtil worldUtil = new WorldUtil(container.worldObj, container.xCoord, container.yCoord, container.zCoord);
		for (AdjacentTile tile :  worldUtil.getAdjacentTileEntities(true)){
			if (!(tile.tile instanceof IFluidHandler) || tile.tile instanceof lp_TileGenericPipe) continue;
			IFluidHandler container = (IFluidHandler) tile.tile;
			if (container.getTankInfo(ForgeDirection.UNKNOWN) == null || container.getTankInfo(ForgeDirection.UNKNOWN).length == 0) continue;
			
			//How much do I want?
			Map<LiquidIdentifier, Integer> wantLiquids = new HashMap<LiquidIdentifier, Integer>();
			wantLiquids.put(ItemIdentifier.get(dummyInventory.getStackInSlot(0)).getFluidIdentifier(), amount);

			//How much do I have?
			HashMap<LiquidIdentifier, Integer> haveLiquids = new HashMap<LiquidIdentifier, Integer>();
			
			FluidTankInfo[] result = container.getTankInfo(ForgeDirection.UNKNOWN);
			for (FluidTankInfo slot : result){
				if (slot.fluid == null || !wantLiquids.containsKey(LiquidIdentifier.get(slot.fluid))) continue;
				Integer liquidWant = haveLiquids.get(LiquidIdentifier.get(slot.fluid));
				if (liquidWant==null){
					haveLiquids.put(LiquidIdentifier.get(slot.fluid), slot.fluid.amount);
				} else {
					haveLiquids.put(LiquidIdentifier.get(slot.fluid), liquidWant +  slot.fluid.amount);
				}
			}
			
			//HashMap<Integer, Integer> needLiquids = new HashMap<Integer, Integer>();
			//Reduce what I have and what have been requested already
			for (Entry<LiquidIdentifier, Integer> liquidId: wantLiquids.entrySet()){
				Integer haveCount = haveLiquids.get(liquidId.getKey());
				if (haveCount != null){
					liquidId.setValue(liquidId.getValue() - haveCount);
				}
				for (Entry<LiquidIdentifier, Integer> requestedItem : _requestedItems.entrySet()){
					if(requestedItem.getKey() == liquidId.getKey()) {
						liquidId.setValue(liquidId.getValue() - requestedItem.getValue());
					}
				}
			}
			
			((PipeLiquidSupplierMk2)this.container.pipe).setRequestFailed(false);
			
			//Make request
			
			for (LiquidIdentifier need : wantLiquids.keySet()){
				int countToRequest = wantLiquids.get(need);
				if (countToRequest < 1) continue;
				
				if(!_power.useEnergy(11)) {
					break;
				}
				
				boolean success = false;

				if(_requestPartials) {
					countToRequest = RequestTree.requestLiquidPartial(need, countToRequest, (IRequestLiquid) this.container.pipe, null);
					if(countToRequest > 0) {
						success = true;
					}
				} else {
					success = RequestTree.requestLiquid(need, countToRequest, (IRequestLiquid) this.container.pipe, null)>0;
				}
				
				if (success){
					Integer currentRequest = _requestedItems.get(need);
					if(currentRequest==null) {
						_requestedItems.put(need, countToRequest);
					} else {
						_requestedItems.put(need, currentRequest + countToRequest);
					}
				} else{
					((PipeLiquidSupplierMk2)this.container.pipe).setRequestFailed(true);
				}
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);	
		dummyInventory.readFromNBT(nbttagcompound, "");
		_requestPartials = nbttagcompound.getBoolean("requestpartials");
		amount = nbttagcompound.getInteger("amount");
    }

	@Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
    	super.writeToNBT(nbttagcompound);
    	dummyInventory.writeToNBT(nbttagcompound, "");
    	nbttagcompound.setBoolean("requestpartials", _requestPartials);
    	nbttagcompound.setInteger("amount", amount);
    }
	
	private void decreaseRequested(LiquidIdentifier liquid, int remaining) {
		//see if we can get an exact match
		Integer count = _requestedItems.get(liquid);
		if (count != null) {
			_requestedItems.put(liquid, Math.max(0, count - remaining));
			remaining -= count;
		}
		if(remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		for(Entry<LiquidIdentifier, Integer> e : _requestedItems.entrySet()) {
			if(e.getKey().itemId == liquid.itemId && e.getKey().itemMeta == liquid.itemMeta) {
				int expected = e.getValue();
				e.setValue(Math.max(0, expected - remaining));
				remaining -= expected;
			}
			if(remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		LogisticsPipes.requestLog.info("liquid supplier got unexpected item " + liquid.toString());
	}

	@Override
	public void liquidLost(LiquidIdentifier item, int amount) {
		decreaseRequested(item, amount);
	}

	@Override
	public void liquidArrived(LiquidIdentifier item, int amount) {
		decreaseRequested(item, amount);
		delayThrottle();
	}

	@Override
	public void liquidNotInserted(LiquidIdentifier item, int amount) {}
	
	public boolean isRequestingPartials(){
		return _requestPartials;
	}
	
	public void setRequestingPartials(boolean value){
		_requestPartials = value;
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		if(MainProxy.isServer(entityplayer.worldObj)) {
			entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_LiquidSupplier_MK2_ID, worldObj, container.xCoord, container.yCoord, container.zCoord);
		}
	}

	@Override
	public void destroy() {}

	public IInventory getDummyInventory() {
		return dummyInventory;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		if(MainProxy.isClient(this.worldObj)) {
			this.amount = amount;
		}
	}

	public void changeLiquidAmount(int change, EntityPlayer player) {
		amount += change;
		if(amount <= 0) {
			amount = 0;
		}
//TODO 	MainProxy.sendPacketToPlayer(new PacketPipeInteger(NetworkConstants.LIQUID_SUPPLIER_LIQUID_AMOUNT, xCoord, yCoord, zCoord, amount).getPacket(), (Player)player);
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(LiquidSupplierAmount.class).setInteger(amount).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord), (Player)player);
	}
}
