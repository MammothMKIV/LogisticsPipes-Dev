package logistics_bc.transport.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import logistics_bc.core.network.PacketCoordinates;
import logistics_bc.core.network.PacketIds;
import logistics_bc.core.network.PacketSlotChange;
import logistics_bc.core.network.PacketUpdate;
import logistics_bc.transport.PipeTransportItems;
import logistics_bc.transport.lp_TileGenericPipe;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.packet.Packet250CustomPayload;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import cpw.mods.fml.common.network.IPacketHandler;
import cpw.mods.fml.common.network.Player;

public class PacketHandlerTransport implements IPacketHandler {

	@Override
	public void onPacketData(INetworkManager manager, Packet250CustomPayload packet2, Player player) {
		DataInputStream data = new DataInputStream(new ByteArrayInputStream(packet2.data));
		try {
			// NetClientHandler net = (NetClientHandler) network.getNetHandler();

			int packetID = data.read();

			PacketUpdate packet = new PacketUpdate();
			switch (packetID) {
			case PacketIds.PIPE_LIQUID:
				PacketLiquidUpdate packetLiquid = new PacketLiquidUpdate();
				packetLiquid.readData(data);
				break;
			case PacketIds.PIPE_DESCRIPTION:
				PipeRenderStatePacket descPacket = new PipeRenderStatePacket();
				descPacket.readData(data);
				onPipeDescription((EntityPlayer) player, descPacket);
				break;
			case PacketIds.PIPE_CONTENTS:
				PacketPipeTransportContent packetC = new PacketPipeTransportContent();
				packetC.readData(data);
				onPipeContentUpdate((EntityPlayer) player, packetC);
				break;
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	

	/**
	 * Handles a pipe description packet. (Creates the pipe object client side if needed.)
	 * 
	 * @param descPacket
	 */
	private void onPipeDescription(EntityPlayer player, PipeRenderStatePacket descPacket) {
		World world = player.worldObj;

		if (!world.blockExists(descPacket.posX, descPacket.posY, descPacket.posZ))
			return;

		TileEntity entity = world.getBlockTileEntity(descPacket.posX, descPacket.posY, descPacket.posZ);
		if (entity == null)
			return;
		// entity = new TileGenericPipeProxy();
		// world.setBlockTileEntity(descPacket.posX, descPacket.posY, descPacket.posZ, entity);

		if (!(entity instanceof lp_TileGenericPipe))
			return;

		lp_TileGenericPipe tile = (lp_TileGenericPipe) entity;
		tile.handleDescriptionPacket(descPacket);
	}

	/**
	 * Updates items in a pipe.
	 * 
	 * @param packet
	 */
	private void onPipeContentUpdate(EntityPlayer player, PacketPipeTransportContent packet) {
		World world = player.worldObj;

		if (!world.blockExists(packet.posX, packet.posY, packet.posZ))
			return;

		TileEntity entity = world.getBlockTileEntity(packet.posX, packet.posY, packet.posZ);
		if (!(entity instanceof lp_TileGenericPipe))
			return;

		lp_TileGenericPipe pipe = (lp_TileGenericPipe) entity;
		if (pipe.pipe == null)
			return;

		if (!(pipe.pipe.transport instanceof PipeTransportItems))
			return;

		((PipeTransportItems) pipe.pipe.transport).handleItemPacket(packet);
	}

	
	/**
	 * Retrieves pipe at specified coordinates if any.
	 * 
	 * @param world
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private lp_TileGenericPipe getPipe(World world, int x, int y, int z) {
		if (!world.blockExists(x, y, z))
			return null;

		TileEntity tile = world.getBlockTileEntity(x, y, z);
		if (!(tile instanceof lp_TileGenericPipe))
			return null;

		return (lp_TileGenericPipe) tile;
	}


}
