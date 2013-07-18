/**
 * Copyright (c) SpaceToad, 2011
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logistics_bc.transport.pipes;

import logistics_bc.core.IDropControlInventory;
import logistics_bc.transport.lp_TileGenericPipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;

public class PipeLogic implements IDropControlInventory {

	public int xCoord;
	public int yCoord;
	public int zCoord;
	public World worldObj;
	public lp_TileGenericPipe container;

	public void setPosition(int xCoord, int yCoord, int zCoord) {
		this.xCoord = xCoord;
		this.yCoord = yCoord;
		this.zCoord = zCoord;
	}

	public void setWorld(World worldObj) {
		this.worldObj = worldObj;
	}

	public void setTile(lp_TileGenericPipe tile) {
		this.container = tile;
	}

	/* SAVING & LOADING */
	public void writeToNBT(NBTTagCompound nbttagcompound) {
	}

	public void readFromNBT(NBTTagCompound nbttagcompound) {
	}

	/* PIPE LOGIC */
	public void initialize() {
	}

	public void updateEntity() {
	}

	public boolean blockActivated(EntityPlayer entityplayer) {
		return false;
	}

	public boolean isPipeConnected(TileEntity tile) {
		return true;
	}

	public void onNeighborBlockChange(int blockId) {
	}

	public void onBlockPlaced() {
	}

	public boolean inputOpen(ForgeDirection from) {
		return true;
	}

	public boolean outputOpen(ForgeDirection to) {
		return true;
	}

	/* IDROPCONTROLINVENTORY */
	@Override
	public boolean doDrop() {
		return true;
	}
}
