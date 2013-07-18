package logisticspipes.pipes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import buildcraft.transport.PipeTransportFluids;
import buildcraft.transport.TileGenericPipe; // for connection

import logisticspipes.interfaces.routing.ILiquidProvider;
import logisticspipes.interfaces.routing.IRequestLiquid;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.logisticspipes.IRoutedItem.TransportMode;
import logisticspipes.pipes.basic.liquid.LiquidRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.request.LiquidRequestTreeNode;
import logisticspipes.routing.LiquidLogisticsPromise;
import logisticspipes.routing.LogisticsLiquidOrderManager;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.PipeLiquidTransportLogistics;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.LiquidIdentifier;
import logisticspipes.utils.Pair;
import logisticspipes.utils.Pair3;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import logistics_bc.transport.PipeTransportLiquids;
import logistics_bc.transport.lp_TileGenericPipe;

public class PipeLiquidProvider extends LiquidRoutedPipe implements ILiquidProvider {
	
	LogisticsLiquidOrderManager manager = new LogisticsLiquidOrderManager();
	
	public PipeLiquidProvider(int itemID) {
		super(itemID);
	}

	@Override
	public void enabledUpdateEntity() {
		if (!manager.hasOrders() || container.worldObj.getWorldTime() % 6 != 0) return;
		
		Pair3<LiquidIdentifier, Integer, IRequestLiquid> order = manager.getFirst();
		int amountToSend = Math.min(order.getValue2(), 5000);
		for(Pair<TileEntity, ForgeDirection> pair:getAdjacentTanks(false)) {
			if(amountToSend <= 0) break;
			FluidTankInfo[] tanks = ((IFluidHandler)pair.getValue1()).getTankInfo(pair.getValue2().getOpposite());
			for(FluidTankInfo tank:tanks) {
				FluidStack liquid;
				if((liquid = tank.fluid) != null) {
					if(order.getValue1() == LiquidIdentifier.get(liquid)) {
						int amount = Math.min(liquid.amount, amountToSend);
//						amount = Math.min(amountToSend, order.getValue3().getRouter().getPipe().transport.the amount you can sink()
						amountToSend -= amount;
						FluidStack drained = ((IFluidHandler)pair.getValue1()).drain(pair.getValue2(), amount, false);
						if(order.getValue1() == LiquidIdentifier.get(drained)) {
							drained = ((IFluidHandler)pair.getValue1()).drain(pair.getValue2(), amount, true);
							ItemStack stack = SimpleServiceLocator.logisticsLiquidManager.getFluidContainer(drained);
							IRoutedItem item = SimpleServiceLocator.buildCraftProxy.CreateRoutedItem(stack, container.worldObj);
							item.setDestination(order.getValue3().getRouter().getSimpleID());
							item.setTransportMode(TransportMode.Active);
							this.queueRoutedItem(item, pair.getValue2());
							manager.sendAmount(amount);
							if(amountToSend <= 0) break;
						}
					}
				}
			}
		}
		if(amountToSend > 0) {
			manager.sendFailed();
		}
	}

	@Override
	public Map<LiquidIdentifier, Integer> getAvailableLiquids() {
		Map<LiquidIdentifier, Integer> map = new HashMap<LiquidIdentifier, Integer>();
		for(Pair<TileEntity, ForgeDirection> pair:getAdjacentTanks(false)) {
			FluidTankInfo[] tanks = ((IFluidHandler)pair.getValue1()).getTankInfo(pair.getValue2().getOpposite());
			for(FluidTankInfo tank:tanks) {
				FluidStack liquid;
				if((liquid = tank.fluid) != null && liquid.fluidID != 0) {
					LiquidIdentifier ident = LiquidIdentifier.get(liquid);
					if(map.containsKey(ident)) {
						map.put(ident, map.get(ident) + tank.fluid.amount);
					} else {						
						map.put(ident, tank.fluid.amount);
					}
				}
			}
		}
		//Reduce Ordered
		for(Pair3<LiquidIdentifier, Integer, IRequestLiquid> pair: manager.getAll()) {
			if(map.containsKey(pair.getValue1())) {
				int result = map.get(pair.getValue1()) - pair.getValue2();
				if(result > 0) {
					map.put(pair.getValue1(), result);
				} else {
					map.remove(pair.getValue1());
				}
			}
		}
		return map;
	}
	
	@Override
	public boolean disconnectPipe(TileEntity tile, ForgeDirection dir) {
		return tile instanceof TileGenericPipe && ((buildcraft.transport.TileGenericPipe)tile).pipe != null && ((buildcraft.transport.TileGenericPipe)tile).pipe.transport instanceof PipeTransportFluids;
	}
	
	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_LIQUID_PROVIDER;
	}

	@Override
	public void canProvide(LiquidRequestTreeNode request, int donePromises) {
		if(request.isDone()) return;
		int containedAmount = 0;
		for(Pair<TileEntity, ForgeDirection> pair:getAdjacentTanks(false)) {
			IFluidTank[] tanks = ((IFluidHandler)pair.getValue1()).getTankInfo(pair.getValue2().getOpposite());
			for(IFluidTank tank:tanks) {
				FluidStack liquid;
				if((liquid = tank.getFluid()) != null) {
					if(request.getFluid() == LiquidIdentifier.get(liquid)) {
						containedAmount += liquid.amount;
					}
				}
			}
		}
		LiquidLogisticsPromise promise = new LiquidLogisticsPromise();
		promise.liquid = request.getFluid();
		promise.amount = Math.min(request.amountLeft(), containedAmount - donePromises);
		promise.sender = this;
		if(promise.amount > 0) {
			request.addPromise(promise);
		}
	}

	@Override
	public void fullFill(LiquidLogisticsPromise promise, IRequestLiquid destination) {
		manager.add(promise, destination);
	}

	@Override
	public boolean canInsertToTanks() {
		return true;
	}

	@Override
	public boolean canInsertFromSideToTanks() {
		return true;
	}


	@Override //work in progress, currently not active code.
	public Set<ItemIdentifier> getSpecificInterests() {
		Set<ItemIdentifier> l1 = new TreeSet<ItemIdentifier>();;
		for(Pair<TileEntity, ForgeDirection> pair:getAdjacentTanks(false)) {
			IFluidTank[] tanks = ((IFluidHandler)pair.getValue1()).getTankInfo(pair.getValue2().getOpposite());
			for(IFluidTank tank:tanks) {
				FluidStack liquid;
				if((liquid = tank.getFluid()) != null && liquid.fluidID != 0) {
					LiquidIdentifier ident = LiquidIdentifier.get(liquid);
					l1.add(ident.getItemIdentifier());
				}
			}
		}
		return l1;
	}

}
