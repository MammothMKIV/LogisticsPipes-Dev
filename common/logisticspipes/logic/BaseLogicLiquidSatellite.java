/**
 * Copyright (c) Krapht, 2011
 * 
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.logic;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.IRequestLiquid;
import logisticspipes.interfaces.routing.IRequireReliableLiquidTransport;
import logisticspipes.network.GuiIDs;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.satpipe.SatPipeNext;
import logisticspipes.network.packets.satpipe.SatPipePrev;
import logisticspipes.network.packets.satpipe.SatPipeSetID;
import logisticspipes.pipes.PipeLiquidSatellite;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.RequestTree;
import logisticspipes.utils.LiquidIdentifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import logistics_bc.core.network.TileNetworkData;
import cpw.mods.fml.common.network.Player;

public class BaseLogicLiquidSatellite extends BaseRoutingLogic implements IRequireReliableLiquidTransport {

	public static HashSet<BaseLogicLiquidSatellite> AllSatellites = new HashSet<BaseLogicLiquidSatellite>();

	// called only on server shutdown
	public static void cleanup() {
		AllSatellites.clear();
	}
	
	protected final Map<LiquidIdentifier, Integer> _lostItems = new HashMap<LiquidIdentifier, Integer>();

	@TileNetworkData
	public int satelliteId;

	public BaseLogicLiquidSatellite() {
		throttleTime = 40;
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		satelliteId = nbttagcompound.getInteger("satelliteid");
		ensureAllSatelliteStatus();
	}

	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		nbttagcompound.setInteger("satelliteid", satelliteId);
		super.writeToNBT(nbttagcompound);
	}

	protected int findId(int increment) {
		if(MainProxy.isClient(this.worldObj)) return satelliteId;
		int potentialId = satelliteId;
		boolean conflict = true;
		while (conflict) {
			potentialId += increment;
			if (potentialId < 0) {
				return 0;
			}
			conflict = false;
			for (final BaseLogicLiquidSatellite sat : AllSatellites) {
				if (sat.satelliteId == potentialId) {
					conflict = true;
					break;
				}
			}
		}
		return potentialId;
	}

	protected void ensureAllSatelliteStatus() {
		if(MainProxy.isClient()) return;
		if (satelliteId == 0 && AllSatellites.contains(this)) {
			AllSatellites.remove(this);
		}
		if (satelliteId != 0 && !AllSatellites.contains(this)) {
			AllSatellites.add(this);
		}
	}

	public void setNextId(EntityPlayer player) {
		satelliteId = findId(1);
		ensureAllSatelliteStatus();
		if (MainProxy.isClient(player.worldObj)) {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeNext.class).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord);
//TODO Must be handled manualy
			MainProxy.sendPacketToServer(packet);
		} else {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord);
//TODO Must be handled manualy
			MainProxy.sendPacketToPlayer(packet, (Player)player);
		}
		updateWatchers();
	}

	public void setPrevId(EntityPlayer player) {
		satelliteId = findId(-1);
		ensureAllSatelliteStatus();
		if (MainProxy.isClient(player.worldObj)) {
			final ModernPacket packet = PacketHandler
					.getPacket(SatPipePrev.class).setPosX(container.xCoord)
					.setPosY(container.yCoord).setPosZ(container.zCoord);
//TODO Must be handled manualy
			MainProxy.sendPacketToServer(packet);
		} else {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord);
//TODO Must be handled manualy
			MainProxy.sendPacketToPlayer(packet,(Player) player);
		}
		updateWatchers();
	}

	
	private void updateWatchers() {
		for(EntityPlayer player : ((PipeLiquidSatellite)this.container.pipe).localModeWatchers) {
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord);
//TODO Must be handled manualy
			MainProxy.sendPacketToPlayer(packet,(Player) player);
		}
	}
	

	@Override
	public void destroy() {
		if(MainProxy.isClient(this.worldObj)) return;
		if (AllSatellites.contains(this)) {
			AllSatellites.remove(this);
		}
	}

	@Override
	public void onWrenchClicked(EntityPlayer entityplayer) {
		if (MainProxy.isServer(entityplayer.worldObj)) {
			// Send the satellite id when opening gui
			final ModernPacket packet = PacketHandler.getPacket(SatPipeSetID.class).setSatID(satelliteId).setPosX(container.xCoord).setPosY(container.yCoord).setPosZ(container.zCoord);
//TODO Must be handled manualy
			MainProxy.sendPacketToPlayer(packet, (Player)entityplayer);
			entityplayer.openGui(LogisticsPipes.instance, GuiIDs.GUI_SatelitePipe_ID, worldObj, container.xCoord, container.yCoord, container.zCoord);
		}
	}

	@Override
	public void throttledUpdateEntity() {
		super.throttledUpdateEntity();
		if (_lostItems.isEmpty()) {
			return;
		}
		final Iterator<Entry<LiquidIdentifier, Integer>> iterator = _lostItems.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<LiquidIdentifier, Integer> stack = iterator.next();
			int received = RequestTree.requestLiquidPartial(stack.getKey(), stack.getValue(), (IRequestLiquid) this.getRoutedPipe(), null);
			
			if(received > 0) {
				if(received == stack.getValue()) {
					iterator.remove();
				} else {
					stack.setValue(stack.getValue() - received);
				}
			}
		}
	}

	public void setSatelliteId(int integer) {
		satelliteId = integer;
	}

	@Override
	public void liquidLost(LiquidIdentifier item, int amount) {
		if(_lostItems.containsKey(item)) {
			_lostItems.put(item, _lostItems.get(item) + amount);
		} else {
			_lostItems.put(item, amount);
		}
	}

	@Override
	public void liquidArrived(LiquidIdentifier item, int amount) {}

	@Override
	public void liquidNotInserted(LiquidIdentifier item, int amount) {
		this.liquidLost(item, amount);
	}
}
