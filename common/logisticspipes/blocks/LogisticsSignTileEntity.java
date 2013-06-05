package logisticspipes.blocks;

import java.util.ArrayList;
import java.util.List;

import buildcraft.api.transport.IPipeTile;

import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.OrientationsUtil;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.ForgeDirection;
//import buildcraft.transport.TileGenericPipe;

public class LogisticsSignTileEntity extends TileEntity {
	
	private boolean init = false;
	
	public LogisticsSignTileEntity() {}
	
	@Override
	public void updateEntity() {
		if(!init) {
    		init = true;
    		if(!MainProxy.isClient(worldObj)) {
    			PipeItemsCraftingLogistics crafting = getAttachedSignOwnerPipe();
    			if(crafting != null) {
    				ForgeDirection dir = OrientationsUtil.getOrientationOfTilewithPipe(crafting.transport, this);
    				crafting.setCraftingSign(dir, true, null);
    				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
    			} else {
    				for(CoreRoutedPipe pipe:getNearRoutingPipes()) {
    	    			if(pipe instanceof PipeItemsCraftingLogistics) {
    	    				ForgeDirection dir = OrientationsUtil.getOrientationOfTilewithPipe(((PipeItemsCraftingLogistics)pipe).transport, this);
    	    				((PipeItemsCraftingLogistics)pipe).setCraftingSign(dir, true, null);
    	    				worldObj.setBlockToAir(xCoord, yCoord, zCoord);
        	    			break;
    	    			}
    	    		}
    			}
    		}
    	}
	}
	
	public PipeItemsCraftingLogistics getAttachedSignOwnerPipe() {
		for(CoreRoutedPipe pipe:this.getNearRoutingPipes()) {
			if(pipe instanceof PipeItemsCraftingLogistics) {
				if(((PipeItemsCraftingLogistics)pipe).isAttachedSign(this)) {
					return (PipeItemsCraftingLogistics)pipe;
				}
			}
		}
		return null;
	}
	
	private CoreRoutedPipe[] getNearRoutingPipes() {
		List<CoreRoutedPipe> list = new ArrayList<CoreRoutedPipe>();
		TileEntity tile = worldObj.getBlockTileEntity(xCoord + 1,yCoord,zCoord);
		if(tile instanceof IPipeTile && ((IPipeTile)tile).getPipe() instanceof CoreRoutedPipe) {
			list.add((CoreRoutedPipe) ((IPipeTile)tile).getPipe());
		}
		tile = worldObj.getBlockTileEntity(xCoord - 1,yCoord,zCoord);
		if(tile instanceof IPipeTile && ((IPipeTile)tile).getPipe() instanceof CoreRoutedPipe) {
			list.add((CoreRoutedPipe) ((IPipeTile)tile).getPipe());
		}
		tile = worldObj.getBlockTileEntity(xCoord,yCoord,zCoord + 1);
		if(tile instanceof IPipeTile && ((IPipeTile)tile).getPipe() instanceof CoreRoutedPipe) {
			list.add((CoreRoutedPipe) ((IPipeTile)tile).getPipe());
		}
		tile = worldObj.getBlockTileEntity(xCoord,yCoord,zCoord - 1);
		if(tile instanceof IPipeTile && ((IPipeTile)tile).getPipe() instanceof CoreRoutedPipe) {
			list.add((CoreRoutedPipe) ((IPipeTile)tile).getPipe());
		}
		return list.toArray(new CoreRoutedPipe[]{});
	}

	@Override
	public void func_85027_a(CrashReportCategory par1CrashReportCategory) {
		super.func_85027_a(par1CrashReportCategory);
		par1CrashReportCategory.addCrashSection("LP-Version", LogisticsPipes.VERSION);
	}
}
