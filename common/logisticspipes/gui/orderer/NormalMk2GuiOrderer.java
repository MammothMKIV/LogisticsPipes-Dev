package logisticspipes.gui.orderer;

import logisticspipes.gui.popup.GuiDiskPopup;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.DiskDropPacket;
import logisticspipes.network.packets.orderer.DiskRequestConectPacket;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.gui.SmallGuiButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.lwjgl.opengl.GL11;

public class NormalMk2GuiOrderer extends NormalGuiOrderer {
	
	public PipeItemsRequestLogisticsMk2 pipe;
	private SmallGuiButton Macrobutton;
	
	public NormalMk2GuiOrderer(PipeItemsRequestLogisticsMk2 RequestPipeMK2 ,EntityPlayer entityPlayer) {
		super(RequestPipeMK2.container.xCoord, RequestPipeMK2.container.yCoord, RequestPipeMK2.container.zCoord, MainProxy.getDimensionForWorld(RequestPipeMK2.container.worldObj), entityPlayer);
		pipe = RequestPipeMK2;
//TODO 	MainProxy.sendPacketToServer(new PacketCoordinates(NetworkConstants.DISK_REQUEST_CONTENT, pipe.xCoord, pipe.yCoord, pipe.zCoord).getPacket());
		MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskRequestConectPacket.class).setPosX(pipe.container.xCoord).setPosY(pipe.container.yCoord).setPosZ(pipe.container.zCoord));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void initGui() {
		super.initGui();
		buttonList.add(Macrobutton = new SmallGuiButton(12, right - 55, bottom - 60, 50, 10, "Disk"));
		Macrobutton.enabled = false;
	}
	
	@Override
	public void drawGuiContainerBackgroundLayer(float f, int i, int j) {
		super.drawGuiContainerBackgroundLayer(f, i, j);

		drawRect(right - 39, bottom - 47, right - 19, bottom - 27, Colors.Black);
		drawRect(right - 37, bottom - 45, right - 21, bottom - 29, Colors.DarkGrey);
		
		if(pipe.getDisk() != null) {
			renderItem.renderItemIntoGUI(fontRenderer, mc.renderEngine, pipe.getDisk(), right - 37, bottom - 45);
			Macrobutton.enabled = true;
		} else {
			Macrobutton.enabled = false;
		}
		
		//Click on Disk
		if(lastClickedx != -10000000 &&	lastClickedy != -10000000) {
			if (lastClickedx >= right - 39 && lastClickedx < right - 19 && lastClickedy >= bottom - 47 && lastClickedy < bottom - 27) {
//TODO 			MainProxy.sendPacketToServer(new PacketCoordinates(NetworkConstants.DISK_DROP, pipe.xCoord, pipe.yCoord, pipe.zCoord).getPacket());
				MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskDropPacket.class).setPosX(pipe.container.xCoord).setPosY(pipe.container.yCoord).setPosZ(pipe.container.zCoord));
				lastClickedx = -10000000;
				lastClickedy = -10000000;
			}
		}
		GL11.glDisable(2896 /*GL_LIGHTING*/);
	}
	
	@Override
	protected void mouseClicked(int i, int j, int k) {
		super.mouseClicked(i, j, k);
		if ((!clickWasButton && i >= right - 39 && i < right - 19 && j >= bottom - 47 && j < bottom - 27) || editsearch){
			if(!editsearchb) {
				editsearch = false;
			}
			selectedItem = null;
			lastClickedx = i;
			lastClickedy = j;
			lastClickedk = k;
		}
	}

	@Override
	protected void actionPerformed(GuiButton guibutton) {
		super.actionPerformed(guibutton);
		if (guibutton.id == 12) {
//TODO 		MainProxy.sendPacketToServer(new PacketCoordinates(NetworkConstants.DISK_REQUEST_CONTENT, pipe.xCoord, pipe.yCoord, pipe.zCoord).getPacket());
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskRequestConectPacket.class).setPosX(pipe.container.xCoord).setPosY(pipe.container.yCoord).setPosZ(pipe.container.zCoord));
			this.setSubGui(new GuiDiskPopup(this));
		}
	}
	
	public ItemStack getDisk() {
		return pipe.getDisk();
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {
		SimpleServiceLocator.thaumCraftProxy.renderAspectsDown(item.unsafeMakeNormalStack(1), guiLeft - 20, guiTop + 10, this);
	}
}
