/**
 * Copyright (c) SpaceToad, 2011 http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public License
 * 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package logistics_bc.core;

import buildcraft.api.power.IPowerReceptor;
import logistics_bc.core.network.ISynchronizedTile;
import logistics_bc.core.network.PacketPayload;
import logistics_bc.core.network.PacketPayloadArrays;
import logistics_bc.core.network.PacketTileUpdate;
import logistics_bc.core.network.PacketUpdate;
import logistics_bc.core.network.TilePacketWrapper;
import logistics_bc.core.proxy.CoreProxy;
import logistics_bc.core.utils.Utils;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public abstract class TileBuildCraft extends TileEntity implements ISynchronizedTile {

	@SuppressWarnings("rawtypes")
	private static Map<Class, TilePacketWrapper> updateWrappers = new HashMap<Class, TilePacketWrapper>();
	@SuppressWarnings("rawtypes")
	private static Map<Class, TilePacketWrapper> descriptionWrappers = new HashMap<Class, TilePacketWrapper>();
	private final TilePacketWrapper descriptionPacket;
	private final TilePacketWrapper updatePacket;
	private boolean init = false;

	public TileBuildCraft() {
		if (!updateWrappers.containsKey(this.getClass())) {
			updateWrappers.put(this.getClass(), new TilePacketWrapper(this.getClass()));
		}

		if (!descriptionWrappers.containsKey(this.getClass())) {
			descriptionWrappers.put(this.getClass(), new TilePacketWrapper(this.getClass()));
		}

		updatePacket = updateWrappers.get(this.getClass());
		descriptionPacket = descriptionWrappers.get(this.getClass());

	}

	@Override
	public void updateEntity() {
		if (!init && !isInvalid()) {
			initialize();
			init = true;
		}

		if (this instanceof IPowerReceptor) {
			IPowerReceptor receptor = ((IPowerReceptor) this);
			receptor.getPowerReceiver(null).update();
		}
	}

	@Override
	public void invalidate() {
		init = false;
		super.invalidate();
	}

	public void initialize() {
		Utils.handleBufferedDescription(this);
	}

	public void destroy() {
	}

	public void sendNetworkUpdate() {
		if (CoreProxy.proxy.isSimulating(worldObj)) {
			CoreProxy.proxy.sendToPlayers(getUpdatePacket(), worldObj, xCoord, yCoord, zCoord, DefaultProps.NETWORK_UPDATE_RANGE);
		}
	}

	@Override
	public Packet getDescriptionPacket() {
		return new PacketTileUpdate(this).getPacket();
	}

	@Override
	public PacketPayload getPacketPayload() {
		return updatePacket.toPayload(this);
	}

	@Override
	public Packet getUpdatePacket() {
		return new PacketTileUpdate(this).getPacket();
	}

	@Override
	public void handleDescriptionPacket(PacketUpdate packet) throws IOException {
		if (packet.payload instanceof PacketPayloadArrays)
			descriptionPacket.fromPayload(this, (PacketPayloadArrays) packet.payload);
	}

	@Override
	public void handleUpdatePacket(PacketUpdate packet) throws IOException {
		if (packet.payload instanceof PacketPayloadArrays)
			updatePacket.fromPayload(this, (PacketPayloadArrays) packet.payload);
	}

	@Override
	public void postPacketHandling(PacketUpdate packet) {
	}

	public boolean isInvNameLocalized() {
		// TODO Auto-generated method stub
		return false;
	}

	public World getWorld() {
		return worldObj;
	}
}
