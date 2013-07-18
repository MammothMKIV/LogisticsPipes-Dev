package logisticspipes.interfaces.routing;

import net.minecraftforge.fluids.FluidStack;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.LiquidIdentifier;

public interface IRequestLiquid {
	IRouter getRouter();
	void sendFailed(LiquidIdentifier value1, Integer value2);
	float canSink(FluidStack f); //returns the amount actually sunk (ie, free space for the liquid)
}
