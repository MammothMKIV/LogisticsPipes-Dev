package logistics_bc.transport;

import logistics_bc.lp_BuildCraftTransport;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.registry.GameRegistry;

public class TransportProxy {
	@SidedProxy(clientSide = "buildcraft.transport.TransportProxyClient", serverSide = "buildcraft.transport.TransportProxy")
	public static TransportProxy proxy;
	public static int pipeModel = -1;

	public void registerTileEntities() {
		GameRegistry.registerTileEntity(lp_TileGenericPipe.class, "net.minecraft.src.buildcraft.transport.GenericPipe");
	}

	public void registerRenderers() {
	}

	public void initIconProviders(lp_BuildCraftTransport instance){
		
	}

	public void setIconProviderFromPipe(ItemPipe item, Pipe dummyPipe) {
		
	}

}
