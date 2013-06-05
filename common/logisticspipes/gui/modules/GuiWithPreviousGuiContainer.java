package logisticspipes.gui.modules;

import logisticspipes.interfaces.IGuiIDHandlerProvider;
import logisticspipes.network.NetworkConstants;
import logisticspipes.network.packets.old.PacketPipeInteger;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.KraphtBaseGuiScreen;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import buildcraft.api.transport.IPipe;

public abstract class GuiWithPreviousGuiContainer extends KraphtBaseGuiScreen implements IGuiIDHandlerProvider {
	
	private int prevGuiID = -1;
	protected IPipe pipe;
	private GuiScreen prevGui;
	
	public GuiWithPreviousGuiContainer(Container par1Container, IPipe pipe2, GuiScreen prevGui) {
		super(par1Container);
		this.prevGui = prevGui;
		if(prevGui instanceof IGuiIDHandlerProvider) {
			this.prevGuiID = ((IGuiIDHandlerProvider)prevGui).getGuiID();
		}
		this.pipe = pipe2;
	}
	
	public GuiScreen getprevGui() {
		return prevGui;
	}
	
	@Override
	protected void keyTyped(char c, int i) {
		if(pipe == null) {
			super.keyTyped(c, i);
			return;
		}
		if (i == 1 || c == 'e') {
			if (prevGuiID != -1) {
				super.keyTyped(c,i);
				MainProxy.sendPacketToServer(new PacketPipeInteger(NetworkConstants.GUI_BACK_PACKET, pipe.getXPosition(), pipe.getYPosition(), pipe.getZPosition(), prevGuiID + 10000).getPacket());
			} else {
				super.keyTyped(c, i);
			}
		}
	}
}
