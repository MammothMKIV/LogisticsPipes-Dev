package logisticspipes.transport;

import logisticspipes.pipes.PipeItemsCraftingLogisticsMk3;
import logistics_bc.transport.EntityData;

public class CraftingPipeMk3Transport extends PipeTransportLogistics {
	
	public PipeItemsCraftingLogisticsMk3 pipe;
	
	public CraftingPipeMk3Transport() {
		super();
	}

	@Override
	protected void reverseItem(EntityData data) {
		data.item.getItemStack().stackSize = pipe.inv.addCompressed(data.item.getItemStack());
		if(data.item.getItemStack().stackSize > 0) {
			super.reverseItem(data);
		}
	}
}
