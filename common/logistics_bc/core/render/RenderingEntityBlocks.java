package logistics_bc.core.render;

import logistics_bc.lp_BuildCraftCore;
import logistics_bc.core.IInventoryRenderer;
import logistics_bc.core.utils.Utils;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import java.util.HashMap;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import org.lwjgl.opengl.GL11;

public class RenderingEntityBlocks implements ISimpleBlockRenderingHandler {

	private static final ResourceLocation BLOCK_TEXTURE = new ResourceLocation("/terrain.png");

	public static class EntityRenderIndex {

		public EntityRenderIndex(Block block, int damage) {
			this.block = block;
			this.damage = damage;
		}

		@Override
		public int hashCode() {
			return block.hashCode() + damage;
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof EntityRenderIndex))
				return false;

			EntityRenderIndex i = (EntityRenderIndex) o;

			return i.block == block && i.damage == damage;
		}
		Block block;
		int damage;
	}
	public static HashMap<EntityRenderIndex, IInventoryRenderer> blockByEntityRenders = new HashMap<EntityRenderIndex, IInventoryRenderer>();

	@Override
	public void renderInventoryBlock(Block block, int metadata, int modelID, RenderBlocks renderer) {

		if (block.getRenderType() == lp_BuildCraftCore.blockByEntityModel) {

			EntityRenderIndex index = new EntityRenderIndex(block, metadata);
			if (blockByEntityRenders.containsKey(index)) {
				blockByEntityRenders.get(index).inventoryRender(-0.5, -0.5, -0.5, 0, 0);
			}

		} 
	}

	@Override
	public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {

		if (block.getRenderType() == lp_BuildCraftCore.blockByEntityModel) {
			// renderblocks.renderStandardBlock(block, i, j, k);
		} 

		return true;
	}

	@Override
	public boolean shouldRender3DInInventory() {
		return true;
	}

	@Override
	public int getRenderId() {
		return lp_BuildCraftCore.blockByEntityModel;
	}

	/* LEGACY PIPE RENDERING and quarry frames! */
	private void legacyPipeRender(RenderBlocks renderblocks, IBlockAccess iblockaccess, int i, int j, int k, Block block, int l) {
		float minSize = Utils.pipeMinPos;
		float maxSize = Utils.pipeMaxPos;

		block.setBlockBounds(minSize, minSize, minSize, maxSize, maxSize, maxSize);
		renderblocks.setRenderBoundsFromBlock(block);
		renderblocks.renderStandardBlock(block, i, j, k);	

		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);
	}
}
