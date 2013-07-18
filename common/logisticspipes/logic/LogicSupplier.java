/** 
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public 
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.logic;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import buildcraft.api.transport.IPipeEntry;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.modules.SupplierPipeMode;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.RequestTree;
import logisticspipes.utils.AdjacentTile;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.ItemIdentifierStack;
import logisticspipes.utils.SimpleInventory;
import logisticspipes.utils.WorldUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.nbt.NBTTagCompound;
import logistics_bc.transport.lp_TileGenericPipe;
import cpw.mods.fml.common.network.Player;

public class LogicSupplier extends BaseRoutingLogic implements IRequireReliableTransport{
	
	private SimpleInventory dummyInventory = new SimpleInventory(9, "Items to keep stocked", 127);
	
	private final HashMap<ItemIdentifier, Integer> _requestedItems = new HashMap<ItemIdentifier, Integer>();
	
	private boolean _requestPartials = false;

	public boolean pause = false;
	
	public IRoutedPowerProvider _power;
	
	public LogicSupplier() {
		throttleTime = 100;
	}
	
	@Override
	public void destroy() {}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		//pause = true; //Pause until GUI is closed //TODO Find a way to handle this
		if(MainProxy.isServer(entityplayer.worldObj)) {
			//GuiProxy.openGuiSupplierPipe(entityplayer.inventory, dummyInventory, this);
			entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_SupplierPipe_ID, worldObj, container.xCoord, container.yCoord, container.zCoord);
//TODO 		MainProxy.sendPacketToPlayer(new PacketPipeInteger(NetworkConstants.SUPPLIER_PIPE_MODE_RESPONSE, xCoord, yCoord, zCoord, isRequestingPartials() ? 1 : 0).getPacket(), (Player)entityplayer);
			MainProxy.sendPacketToPlayer(PacketHandler.getPacket(SupplierPipeMode.class).setInteger(isRequestingPartials() ? 1 : 0).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord), (Player)entityplayer);
		}
	}
	
	/*** GUI ***/
	public SimpleInventory getDummyInventory() {
		return dummyInventory;
	}

	@Override
	public void throttledUpdateEntity() {
		
		if (!((CoreRoutedPipe)this.container.pipe).isEnabled()){
			return;
		}
		
		if(MainProxy.isClient(container.worldObj)) return;
		if (pause) return;
		super.throttledUpdateEntity();

		for(int amount : _requestedItems.values()) {
			if(amount > 0) {
				MainProxy.sendSpawnParticlePacket(Particles.VioletParticle, container.xCoord, container.yCoord, container.zCoord, container.worldObj, 2);
			}
		}

		WorldUtil worldUtil = new WorldUtil(container.worldObj, container.xCoord, container.yCoord, container.zCoord);
		for (AdjacentTile tile :  worldUtil.getAdjacentTileEntities(true)){
			if (tile.tile instanceof IPipeEntry) continue;
			if (!(tile.tile instanceof IInventory)) continue;
			
			IInventory inv = (IInventory) tile.tile;
			if (inv.getSizeInventory() < 1) continue;
			IInventoryUtil invUtil = SimpleServiceLocator.inventoryUtilFactory.getInventoryUtil(inv);
			
			//How many do I want?
			HashMap<ItemIdentifier, Integer> needed = new HashMap<ItemIdentifier, Integer>(dummyInventory.getItemsAndCount());
			
			//How many do I have?
			Map<ItemIdentifier, Integer> have = invUtil.getItemsAndCount();
			//How many do I have?
			HashMap<ItemIdentifier, Integer> haveUndamaged = new HashMap<ItemIdentifier, Integer>();
			for (Entry<ItemIdentifier, Integer> item : have.entrySet()){
				Integer n=haveUndamaged.get(item.getKey().getUndamaged());
				if(n==null)
					haveUndamaged.put(item.getKey().getUndamaged(), item.getValue());
				else
					haveUndamaged.put(item.getKey().getUndamaged(), item.getValue()+n);
			}
			
			//Reduce what I have and what have been requested already
			for (Entry<ItemIdentifier, Integer> item : needed.entrySet()){
				Integer haveCount = haveUndamaged.get(item.getKey().getUndamaged());
				if (haveCount != null){
					item.setValue(item.getValue() - haveCount);
					// so that 1 damaged item can't satisfy a request for 2 other damage values.
					haveUndamaged.put(item.getKey().getUndamaged(),haveCount - item.getValue());
				}
				Integer requestedCount =  _requestedItems.get(item.getKey());
				if (requestedCount!=null){
					item.setValue(item.getValue() - requestedCount);
				}
			}
			
			((PipeItemsSupplierLogistics)this.container.pipe).setRequestFailed(false);

			//Make request
			for (Entry<ItemIdentifier, Integer> need : needed.entrySet()){
				Integer amountRequested = need.getValue();
				if (amountRequested==null || amountRequested < 1) continue;
				int neededCount = amountRequested;
				if(!_power.useEnergy(10)) {
					break;
				}
				
				boolean success = false;

				if(_requestPartials) {
					neededCount = RequestTree.requestPartial(need.getKey().makeStack(neededCount), (IRequestItems) container.pipe);
					if(neededCount > 0) {
						success = true;
					}
				} else {
					success = RequestTree.request(need.getKey().makeStack(neededCount), (IRequestItems) container.pipe, null)>0;
				}
				
				if (success){
					Integer currentRequest = _requestedItems.get(need.getKey());
					if(currentRequest == null) {
						_requestedItems.put(need.getKey(), neededCount);
					} else {
						_requestedItems.put(need.getKey(), currentRequest + neededCount);
					}
				} else {
					((PipeItemsSupplierLogistics)this.container.pipe).setRequestFailed(true);
				}
				
			}
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);	
		dummyInventory.readFromNBT(nbttagcompound, "");
		_requestPartials = nbttagcompound.getBoolean("requestpartials");
    }

	@Override
    public void writeToNBT(NBTTagCompound nbttagcompound) {
    	super.writeToNBT(nbttagcompound);
    	dummyInventory.writeToNBT(nbttagcompound, "");
    	nbttagcompound.setBoolean("requestpartials", _requestPartials);
    }
	
	private void decreaseRequested(ItemIdentifierStack item) {
		int remaining = item.stackSize;
		//see if we can get an exact match
		Integer count = _requestedItems.get(item.getItem());
		if (count != null) {
			_requestedItems.put(item.getItem(), Math.max(0, count - remaining));
			remaining -= count;
		}
		if(remaining <= 0) {
			return;
		}
		//still remaining... was from fuzzyMatch on a crafter
		for(Entry<ItemIdentifier, Integer> e : _requestedItems.entrySet()) {
			if(e.getKey().itemID == item.getItem().itemID && e.getKey().itemDamage == item.getItem().itemDamage) {
				int expected = e.getValue();
				e.setValue(Math.max(0, expected - remaining));
				remaining -= expected;
			}
			if(remaining <= 0) {
				return;
			}
		}
		//we have no idea what this is, log it.
		LogisticsPipes.requestLog.info("supplier got unexpected item " + item.toString());
	}

	@Override
	public void itemLost(ItemIdentifierStack item) {
		decreaseRequested(item);
	}

	@Override
	public void itemArrived(ItemIdentifierStack item) {
		decreaseRequested(item);
		delayThrottle();
	}
	
	public boolean isRequestingPartials(){
		return _requestPartials;
	}
	
	public void setRequestingPartials(boolean value){
		_requestPartials = value;
	}
}
