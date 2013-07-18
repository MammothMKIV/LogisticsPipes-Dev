package logisticspipes.proxy.recipeproviders;

import logisticspipes.proxy.interfaces.ICraftingRecipeProvider;
import logisticspipes.utils.ItemIdentifier;
import logisticspipes.utils.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import logistics_bc.silicon.TileAdvancedCraftingTable;

public class AssemblyAdvancedWorkbench implements ICraftingRecipeProvider {
	@Override
	public boolean canOpenGui(TileEntity tile) {
		return (tile instanceof TileAdvancedCraftingTable);
	}

	@Override
	public boolean importRecipe(TileEntity tile, SimpleInventory inventory) {
		if (!(tile instanceof TileAdvancedCraftingTable))
			return false;

		TileAdvancedCraftingTable bench = (TileAdvancedCraftingTable) tile;
		ItemStack result = bench.getOutputSlot().getStackInSlot(0);

		if (result == null)
			return false;

		inventory.setInventorySlotContents(9, result);

		// Import
		for (int i = 0; i < bench.getCraftingSlots().getSizeInventory(); i++) {
			if (i >= inventory.getSizeInventory() - 2) {
				break;
			}
			final ItemStack newStack = bench.getCraftingSlots().getStackInSlot(i) == null ? null : bench.getCraftingSlots().getStackInSlot(i).copy();
			inventory.setInventorySlotContents(i, newStack);
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
