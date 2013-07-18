package logisticspipes.interfaces.routing;

import java.util.List;

import buildcraft.api.transport.IPipeTile;

import net.minecraft.tileentity.TileEntity;
import logistics_bc.transport.EntityData;

public interface ISpecialTileConnection {
	public boolean init();
	public boolean isType(TileEntity tile);
	public List<IPipeTile> getConnections(IPipeTile tile);
	public boolean needsInformationTransition();
	public void transmit(TileEntity tile, EntityData data);
}
