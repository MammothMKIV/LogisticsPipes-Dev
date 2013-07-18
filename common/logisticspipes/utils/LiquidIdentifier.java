package logisticspipes.utils;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import logisticspipes.LogisticsPipes;
import net.minecraft.item.Item;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.google.common.base.Objects;

public class LiquidIdentifier {

	private static class ItemKey implements Comparable<ItemKey>{

		public int itemID;
		public int itemDamage;
		
		public ItemKey(int id, int d){
			itemID=id;
			itemDamage=d;
		}
		
		@Override 
		public boolean equals(Object that){
			if (!(that instanceof ItemKey))
				return false;
			ItemKey i = (ItemKey)that;
			return this.itemID== i.itemID && this.itemDamage == i.itemDamage;
			
		}
		
		@Override
		public int hashCode(){
			//1000001 chosen because 1048576 is 2^20, moving the bits for the item ID to the top of the integer
			// not exactly 2^20 was chosen so that when the has is used mod power 2, there arn't repeated collisions on things with the same damage id.
			//return ((itemID)*1000001)+itemDamage;
			return Objects.hashCode(itemID, itemDamage);
		}
		
		@Override
		public int compareTo(ItemKey o) {
			if(itemID==o.itemID)
				return itemDamage-o.itemDamage;
			return itemID-o.itemID;
		}
	}
	
	private final static ConcurrentHashMap<ItemKey, LiquidIdentifier> _liquidIdentifierCache = new ConcurrentHashMap<ItemKey, LiquidIdentifier>();
	
	private final static ConcurrentSkipListSet<ItemKey> _liquidIdentifierKeyQueue = new ConcurrentSkipListSet<ItemKey>();
	
	public final int itemId;
	public final int itemMeta;
	public final String name;
	
	private LiquidIdentifier(int itemId, int itemMeta, String name) {
		this.itemId = itemId;
		this.itemMeta = itemMeta;
		if(name == null) {
			name = "";
		}
		this.name = name;
		ItemKey key = new ItemKey(itemId, itemMeta);
		_liquidIdentifierCache.put(key, this);
		if(this.isVaild()) {
			_liquidIdentifierKeyQueue.add(key);
		}
	}
	
	public String getName() {
		return name;
	}
	
	public ItemIdentifier getItemIdentifier() {
		return ItemIdentifier.get(itemId, itemMeta, null);
	}
	
	public static LiquidIdentifier get(FluidStack stack) {
		if(stack.extra != null) {
			LogisticsPipes.log.warning("Found FluidStack with NBT tag. LP doesn't know how to handle it.");
			new Exception().printStackTrace();
		}
		return get(stack.fluidID, stack.itemMeta);
	}
	
	public static LiquidIdentifier get(FluidStack stack, String name) {
		if(stack.extra != null) {
			LogisticsPipes.log.warning("Found FluidStack with NBT tag. LP doesn't know how to handle it.");
			new Exception().printStackTrace();
		}
		return get(stack.fluidID, stack.itemMeta, name);
	}
	
	public static LiquidIdentifier get(int itemID, int itemMeta) {
		return get(itemID, itemMeta, "");
	}
	
	public static LiquidIdentifier get(int itemID, int itemMeta, String name) {
		if(_liquidIdentifierCache.containsKey(new ItemKey(itemID, itemMeta))) {
			return _liquidIdentifierCache.get(new ItemKey(itemID, itemMeta));
		}
		return new LiquidIdentifier(itemID, itemMeta, name);
	}
	
	public FluidStack makeFluidStack(int amount) {
		return new FluidStack(itemId, amount, itemMeta);
	}
	
	public int getFreeSpaceInsideTank(IFluidHandler container, ForgeDirection dir) {
		int free = 0;
		IFluidTank[] tanks = container.getTanks(dir);
		if(tanks != null && tanks.length > 0) {
			for(int i=0;i<tanks.length;i++) {
				free += getFreeSpaceInsideTank(tanks[i]);
			}
		} else {
			IFluidTank tank = container.getTank(dir, this.makeFluidStack(0));
			if(tank != null) {
				free += getFreeSpaceInsideTank(tank);
			}
		}
		return free;
	}
	
	public int getFreeSpaceInsideTank(IFluidTank tank) {
		FluidStack liquid = tank.getFluid();
		if(liquid == null || liquid.fluidID <= 0) {
			return tank.getCapacity();
		}
		if(get(liquid) == this) {
			return tank.getCapacity() - liquid.amount;
		}
		return 0;
	}
	
	private static boolean init = false;
	public static void initFromForge(boolean flag) {
		if(init) return;
		Map<String, FluidStack> liquids = FluidRegistry.getRegisteredFluids().getFluids();
		for(Entry<String, FluidStack> name: liquids.entrySet()) {
			get(name.getValue(), name.getKey());
		}
		if(flag) {
			init = true;
		}
	}
	
	@Override
	public String toString() {
		return name + "/" + itemId + ":" + itemMeta;
	}
	
	public boolean isVaild() {
		return Item.itemsList.length > itemId && Item.itemsList[itemId] != null;
	}
	
	public LiquidIdentifier next() {
		ItemKey key = new ItemKey(itemId, itemMeta);
		if(!_liquidIdentifierKeyQueue.contains(key)) return first();
		key = _liquidIdentifierKeyQueue.higher(key);
		if(key == null) {
			return null;
		}
		return _liquidIdentifierCache.get(key);
	}
	
	public LiquidIdentifier prev() {
		ItemKey key = new ItemKey(itemId, itemMeta);
		if(!_liquidIdentifierKeyQueue.contains(key)) return last();
		key = _liquidIdentifierKeyQueue.lower(key);
		if(key == null) {
			return null;
		}
		return _liquidIdentifierCache.get(key);
	}
	
	public static LiquidIdentifier first() {
		ItemKey key = _liquidIdentifierKeyQueue.first();
		if(key == null) {
			return null;
		}
		return _liquidIdentifierCache.get(key);
	}
	
	public static LiquidIdentifier last() {
		ItemKey key = _liquidIdentifierKeyQueue.last();
		if(key == null) {
			return null;
		}
		return _liquidIdentifierCache.get(key);
	}
}
