package logistics_bc.transport;

import logistics_bc.transport.render.PipeItemRenderer;
import logistics_bc.transport.render.PipeWorldRenderer;
import logistics_bc.transport.render.RenderPipe;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.client.registry.RenderingRegistry;

public class TransportProxyClient extends TransportProxy {
	public final static PipeItemRenderer pipeItemRenderer = new PipeItemRenderer();
	public final static PipeWorldRenderer pipeWorldRenderer = new PipeWorldRenderer();
//	public final static FacadeItemRenderer facadeItemRenderer = new FacadeItemRenderer();
//	public final static PlugItemRenderer plugItemRenderer = new PlugItemRenderer();

	@Override
	public void registerTileEntities() {
		super.registerTileEntities();
		RenderPipe rp = new RenderPipe();
		ClientRegistry.bindTileEntitySpecialRenderer(lp_TileGenericPipe.class, rp);
	}

	@Override
	public void registerRenderers() {

		TransportProxy.pipeModel = RenderingRegistry.getNextAvailableRenderId();

		RenderingRegistry.registerBlockHandler(pipeWorldRenderer);
	}
	
	@Override
	public void setIconProviderFromPipe(ItemPipe item, Pipe dummyPipe) {
		item.setPipesIcons(dummyPipe.getIconProvider());
	}
}
