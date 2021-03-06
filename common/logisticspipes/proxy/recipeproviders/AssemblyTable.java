package logisticspipes.proxy.recipeproviders;

import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import buildcraft.api.recipes.AssemblyRecipe;
import buildcraft.silicon.TileAssemblyTable;

public class AssemblyTable implements ICraftingRecipeProvider {
	@Override
	public boolean canOpenGui(TileEntity tile) {
		return (tile instanceof TileAssemblyTable);
	}

	@Override
	public boolean importRecipe(TileEntity tile, SimpleInventory inventory) {
		if (!(tile instanceof TileAssemblyTable))
			return false;

		TileAssemblyTable table = (TileAssemblyTable) tile;

		//current pipe inputs/outputs
		ItemStack[] inputs = new ItemStack[inventory.getSizeInventory() - 2];
		for(int i = 0; i< inventory.getSizeInventory() - 2; i++)
			inputs[i] = inventory.getStackInSlot(i);
		ItemStack output = inventory.getStackInSlot(inventory.getSizeInventory() - 2);

		//see if there's a recipe planned in the table that matches the current pipe settings, if yes take the next, otherwise take the first
		AssemblyRecipe firstRecipe = null;
		AssemblyRecipe nextRecipe = null;
		boolean takeNext = false;
		for (AssemblyRecipe r : AssemblyRecipe.assemblyRecipes) {
			if(table.isPlanned(r)) {
				if(firstRecipe == null) {
					firstRecipe = r;
				}
				if(takeNext) {
					nextRecipe = r;
					break;
				}
				if(output != null && ItemStack.areItemStacksEqual(output, r.output)) {
					if(r.canBeDone(inputs)) {
						takeNext = true;
					}
				}
			}
		}
		if(nextRecipe == null)
			nextRecipe = firstRecipe;
		if(nextRecipe == null)
			return false;

		// Import
		inventory.setInventorySlotContents(inventory.getSizeInventory() - 2, nextRecipe.output);
		for (int i = 0; i < inventory.getSizeInventory() - 2; i++) {
			if(i < nextRecipe.input.length) {
				inventory.setInventorySlotContents(i, nextRecipe.input[i]);
			} else {
				inventory.setInventorySlotContents(i, null);
			}
		}

		// Compact
		for (int i = 0; i < inventory.getSizeInventory() - 2; i++) {
			final ItemStack stackInSlot = inventory.getStackInSlot(i);
			if (stackInSlot == null) {
				continue;
			}
			final ItemIdentifier itemInSlot = ItemIdentifier.get(stackInSlot);
			for (int j = i + 1; j < inventory.getSizeInventory() - 2; j++) {
				final ItemStack stackInOtherSlot = inventory.getStackInSlot(j);
				if (stackInOtherSlot == null) {
					continue;
				}
				if (itemInSlot == ItemIdentifier.get(stackInOtherSlot)) {
					stackInSlot.stackSize += stackInOtherSlot.stackSize;
					inventory.setInventorySlotContents(j, null);
				}
			}
		}

		for (int i = 0; i < inventory.getSizeInventory() - 2; i++) {
			if (inventory.getStackInSlot(i) != null) {
				continue;
			}
			for (int j = i + 1; j < inventory.getSizeInventory() - 2; j++) {
				if (inventory.getStackInSlot(j) == null) {
					continue;
				}
				inventory.setInventorySlotContents(i, inventory.getStackInSlot(j));
				inventory.setInventorySlotContents(j, null);
				break;
			}
		}
		return true;
	}
}
