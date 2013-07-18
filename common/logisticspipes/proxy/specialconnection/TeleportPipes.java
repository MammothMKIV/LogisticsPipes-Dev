package logisticspipes.proxy.specialconnection;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import buildcraft.api.transport.IPipeConnection;
import buildcraft.api.transport.IPipeEntry;
import buildcraft.api.transport.IPipeTile;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.routing.ISpecialPipedConnection;
import logistics_bc.transport.Pipe;
import logistics_bc.transport.lp_TileGenericPipe;

/** Support for teleport pipes **/
public class TeleportPipes implements ISpecialPipedConnection {

	private static Class<? extends Pipe> PipeItemTeleport;
	private static Method teleportPipeMethod;
	private static Object teleportManager;

	@Override
	@SuppressWarnings("unchecked")
	public boolean init() {
		try {
			try {
				PipeItemTeleport = (Class<? extends Pipe>) Class.forName("buildcraft.additionalpipes.pipes.PipeItemTeleport");
			} catch (Exception e) {
				PipeItemTeleport = (Class<? extends Pipe>) Class.forName("net.minecraft.src.buildcraft.additionalpipes.pipes.PipeItemTeleport");
			}
			teleportPipeMethod = PipeItemTeleport.getMethod("getConnectedPipes", boolean.class);
			LogisticsPipes.log.fine("Additional pipes detected, adding compatibility");
			return true;
		} catch (Exception e1) {
			try {
				PipeItemTeleport = (Class<? extends Pipe>) Class.forName("buildcraft.additionalpipes.pipes.PipeItemsTeleport");
				Class<?> tpmanager = Class.forName("buildcraft.additionalpipes.pipes.TeleportManager");
				teleportManager = tpmanager.getField("instance").get(null);
				teleportPipeMethod = tpmanager.getMethod("getConnectedPipes",Class.forName("buildcraft.additionalpipes.pipes.PipeTeleport"),boolean.class);
				LogisticsPipes.log.fine("Additional pipes detected, adding compatibility");
				return true;
			} catch (Exception e2) {
				LogisticsPipes.log.fine("Additional pipes not detected: " + e2.getMessage());
				return false;
			}
		}
	}


	@Override
	public boolean isType(IPipeConnection tile) {
		if(PipeItemTeleport.isAssignableFrom(tile.getPipe().getClass())) return true;
		return false;
	}

	@SuppressWarnings("unchecked")
	private LinkedList<? extends Pipe> getConnectedTeleportPipes(Pipe pipe) throws Exception {
		if (teleportManager != null) {
			return (LinkedList<? extends Pipe>) teleportPipeMethod.invoke(teleportManager, pipe, false);
		}
		return (LinkedList<? extends Pipe>) teleportPipeMethod.invoke(pipe, false);
	}
	
	@Override
	public List<IPipeTile> getConnections(IPipeTile tile) {
		List<IPipeTile> list = new ArrayList<IPipeTile>();
		try {
			LinkedList<? extends IPipeEntry> pipes = getConnectedTeleportPipes(tile.getPipe());
			for(IPipeEntry pipe : pipes) {
				list.add(pipe.getContainer());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return list;
	}
}
