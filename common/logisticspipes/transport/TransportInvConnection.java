package logisticspipes.transport;

import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class TransportInvConnection extends PipeTransportLogistics {
	
	public TransportInvConnection() {}

	@Override
	protected boolean isItemExitable(ItemStack stack) {
		return true;
	}
	
	@Override
	protected void insertedItemStack(IRoutedItem routed, TileEntity tile) {
		if(tile instanceof IInventory) {
			((PipeItemsInvSysConnector)this.container.pipe).handleItemEnterInv(routed,tile);
		}
	}
}
