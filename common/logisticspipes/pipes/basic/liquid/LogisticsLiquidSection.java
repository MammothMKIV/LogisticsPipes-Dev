package logisticspipes.pipes.basic.liquid;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;;

public class LogisticsLiquidSection extends FluidTank {

	public LogisticsLiquidSection(int capacity) {
		super(capacity);
	}

	@Override
	public LogisticsLiquidSection readFromNBT(NBTTagCompound compoundTag) {
		setFluid(FluidStack.loadFluidStackFromNBT(compoundTag));
		return this;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound subTag) {
		if (this.getFluid() != null) {
			return this.getFluid().writeToNBT(subTag);
		}
		return null;
	}
}
