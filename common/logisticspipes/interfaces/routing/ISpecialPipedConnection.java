package logisticspipes.interfaces.routing;

import java.util.List;

import buildcraft.api.transport.IPipeTile;

import logistics_bc.transport.lp_TileGenericPipe;

public interface ISpecialPipedConnection {
	public boolean init();
	public boolean isType(IPipeTile tile);
	public List<IPipeTile> getConnections(IPipeTile tile);
}
