package logisticspipes.pipes;

import java.util.LinkedList;

import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.PipeLiquidTransportLogistics;
import logisticspipes.utils.AdjacentTile;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import logistics_bc.transport.TileGenericPipe;

public class PipeLiquidExtractor extends PipeLiquidInsertion {

	private int[] liquidToExtract = new int[6];

	private static final int flowRate = 25;
	
	public PipeLiquidExtractor(int itemID) {
		super(itemID);
	}

	@Override
	public void enabledUpdateEntity() {
		super.enabledUpdateEntity();
		if(container.worldObj.getWorldTime() % 10 == 0) return;
		LinkedList<AdjacentTile> connected = this.getConnectedEntities();
		for(AdjacentTile tile:connected) {
			if(tile.tile instanceof IFluidHandler && !(tile.tile instanceof TileGenericPipe)) {
				extractFrom((IFluidHandler) tile.tile, tile.orientation);
			}
		}
	}
	
	private void extractFrom(IFluidHandler container, ForgeDirection side) {
		int i = side.ordinal();
		FluidStack contained = ((PipeLiquidTransportLogistics)this.transport).getTanks(side)[0].getFluid();
		int amountMissing = ((PipeLiquidTransportLogistics)this.transport).getSideCapacity() - (contained != null ? contained.amount : 0);
		if(liquidToExtract[i] < Math.min(200, amountMissing)) {
			if(this.useEnergy(2)) {
				liquidToExtract[i] += Math.min(200, amountMissing);
			}
		}
		FluidStack extracted = container.drain(side.getOpposite(), liquidToExtract[i] > flowRate ? flowRate : liquidToExtract[i], false);

		int inserted = 0;
		if (extracted != null) {
			inserted = ((PipeLiquidTransportLogistics) transport).fill(side, extracted, true);
			container.drain(side.getOpposite(), inserted, true);
		}
		liquidToExtract[i] -= inserted;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound) {
		super.writeToNBT(nbttagcompound);
		nbttagcompound.setIntArray("liquidToExtract", liquidToExtract);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound) {
		super.readFromNBT(nbttagcompound);
		liquidToExtract = nbttagcompound.getIntArray("liquidToExtract");
		if(liquidToExtract.length < 6) {
			liquidToExtract = new int[6];
		}
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_EXTRACTOR;
	}

}
