package logisticspipes.pipes.basic.liquid;

import logisticspipes.transport.PipeLiquidTransportLogistics;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidHandler;
import logistics_bc.core.IMachine;
import logistics_bc.transport.BlockGenericPipe;
import logistics_bc.transport.Pipe;
import logistics_bc.transport.PipeTransportLiquids;
import logistics_bc.transport.TileGenericPipe;

public class LogisitcsLiquidConnectionTransport extends PipeTransportLiquids {
	@Override
	public boolean canPipeConnect(TileEntity tile, ForgeDirection side) {
		if (tile instanceof TileGenericPipe) {
			Pipe pipe2 = ((TileGenericPipe) tile).pipe;
			if (BlockGenericPipe.isValid(pipe2) && !(pipe2.transport instanceof PipeTransportLiquids || pipe2.transport instanceof PipeLiquidTransportLogistics))
				return false;
		}

		if (tile instanceof IFluidHandler) {
			IFluidHandler liq = (IFluidHandler) tile;

			if (liq.getTanks(side) != null && liq.getTanks(side).length > 0)
				return true;
		}

		return tile instanceof TileGenericPipe || (tile instanceof IMachine && ((IMachine) tile).manageLiquids());
	}
}
