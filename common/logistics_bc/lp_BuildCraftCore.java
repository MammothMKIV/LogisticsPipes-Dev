/**
 * BuildCraft is open-source. It is distributed under the terms of the
 * BuildCraft Open Source License. It grants rights to read, modify, compile
 * or run the code. It does *NOT* grant the right to redistribute this software
 * or its modifications in any form, binary or source, except if expressively
 * granted by the copyright holder.
 */

package logistics_bc;

import java.io.File;
import java.util.TreeMap;
import java.util.logging.Logger;

import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.core.IIconProvider;
import buildcraft.api.gates.ActionManager;

import logistics_bc.core.BlockIndex;
import logistics_bc.core.BuildCraftConfiguration;
import logistics_bc.core.CommandBuildCraft;
import logistics_bc.core.CoreIconProvider;
import logistics_bc.core.DefaultProps;
import logistics_bc.core.ItemBuildCraft;
import logistics_bc.core.TickHandlerCoreClient;
import logistics_bc.core.Version;
import logistics_bc.core.network.EntityIds;
import logistics_bc.core.network.PacketHandler;
import logistics_bc.core.network.PacketUpdate;
import logistics_bc.core.proxy.CoreProxy;
import logistics_bc.core.utils.Localization;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFluid;
import net.minecraft.command.CommandHandler;
import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.Property;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.Init;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.PostInit;
import cpw.mods.fml.common.Mod.PreInit;
import cpw.mods.fml.common.Mod.ServerStarting;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;

@Mod(name = "lp_BuildCraft", version = Version.VERSION, useMetadata = false, modid = "lp_BuildCraft|Core", dependencies = "required-after:Forge@[6.5.0.0,)")
@NetworkMod(channels = { DefaultProps.NET_CHANNEL_NAME }, packetHandler = PacketHandler.class, clientSideRequired = true, serverSideRequired = true)
public class lp_BuildCraftCore {
	public static enum RenderMode {
		Full, NoDynamic
	};

	public static RenderMode render = RenderMode.Full;

	public static boolean debugMode = false;
	public static boolean modifyWorld = false;
	public static boolean trackNetworkUsage = false;

	public static boolean dropBrokenBlocks = true; // Set to false to prevent the filler from dropping broken blocks.

	public static int itemLifespan = 1200;

	public static int updateFactor = 10;

	public static long longUpdateFactor = 40;

	public static BuildCraftConfiguration mainConfiguration;

	public static TreeMap<BlockIndex, PacketUpdate> bufferedDescriptions = new TreeMap<BlockIndex, PacketUpdate>();

	public static final int trackedPassiveEntityId = 156;

	public static boolean continuousCurrentModel;

	public static Block springBlock;

	public static Item woodenGearItem;
	public static Item stoneGearItem;
	public static Item ironGearItem;
	public static Item goldGearItem;
	public static Item diamondGearItem;
	public static Item wrenchItem;


    @SideOnly(Side.CLIENT)
    public static IIconProvider iconProvider;

	public static int blockByEntityModel;

	public static boolean loadDefaultRecipes = true;

	public static Logger bcLog = Logger.getLogger("Buildcraft");
	
//	public IIconProvider actionTriggerIconProvider = new ActionTriggerIconProvider();

	@Instance("lp_BuildCraft|Core")
	public static lp_BuildCraftCore instance;

	@PreInit
	public void loadConfiguration(FMLPreInitializationEvent evt) {

		Version.check();

		bcLog.setParent(FMLLog.getLogger());
		bcLog.info("Starting BuildCraft " + Version.getVersion());
		bcLog.info("Copyright (c) SpaceToad, 2011");
		bcLog.info("http://www.mod-buildcraft.com");

		mainConfiguration = new BuildCraftConfiguration(new File(evt.getModConfigurationDirectory(), "buildcraft/main.conf"));
		try {
			mainConfiguration.load();


			Property trackNetwork = lp_BuildCraftCore.mainConfiguration.get(Configuration.CATEGORY_GENERAL, "trackNetworkUsage", false);
			trackNetworkUsage = trackNetwork.getBoolean(false);


			Property lifespan = lp_BuildCraftCore.mainConfiguration.get(Configuration.CATEGORY_GENERAL, "itemLifespan", itemLifespan);
			lifespan.comment = "the lifespan in ticks of items dropped on the ground by pipes and machines, vanilla = 6000, default = 1200";
			itemLifespan = lifespan.getInt(itemLifespan);
			if (itemLifespan < 100) {
				itemLifespan = 100;
			}

			Property factor = lp_BuildCraftCore.mainConfiguration.get(Configuration.CATEGORY_GENERAL, "network.updateFactor", 10);
			factor.comment = "increasing this number will decrease network update frequency, useful for overloaded servers";
			updateFactor = factor.getInt(10);

			Property longFactor = lp_BuildCraftCore.mainConfiguration.get(Configuration.CATEGORY_GENERAL, "network.stateRefreshPeriod", 40);
			longFactor.comment = "delay between full client sync packets, increasing it saves bandwidth, decreasing makes for better client syncronization.";
			longUpdateFactor = longFactor.getInt(40);
			
			MinecraftForge.EVENT_BUS.register(this);

		} finally {
			if (mainConfiguration.hasChanged()) {
				mainConfiguration.save();
			}
		}
	}

	@Init
	public void initialize(FMLInitializationEvent evt) {
		// MinecraftForge.registerConnectionHandler(new ConnectionHandler());
//		ActionManager.registerTriggerProvider(new DefaultTriggerProvider());
//		ActionManager.registerActionProvider(new DefaultActionProvider());
/*
		if (lp_BuildCraftCore.loadDefaultRecipes) {
			loadRecipes();
		}*/

		CoreProxy.proxy.initializeRendering();
		CoreProxy.proxy.initializeEntityRendering();

		Localization.addLocalization("/lang/buildcraft/", DefaultProps.DEFAULT_LANGUAGE);

	}

	@PostInit
	public void postInit(FMLPostInitializationEvent event) {
/*		for (Block block : Block.blocksList) {
			if (block instanceof BlockFluid || block instanceof IPlantable) {
				BuildCraftAPI.softBlocks[block.blockID] = true;
			}
		}*/

		TickRegistry.registerTickHandler(new TickHandlerCoreClient(), Side.CLIENT);

	}

	@ServerStarting
	public void serverStarting(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandBuildCraft());
	}
	
	@ForgeSubscribe
	@SideOnly(Side.CLIENT)
	public void textureHook(TextureStitchEvent.Pre event){
		if ("items".equals(event.map.field_94253_b)){
			iconProvider = new CoreIconProvider();
			iconProvider.registerIcons(event.map);
		}
	}
/*
	public void loadRecipes() {
		GameRegistry.addRecipe(new ItemStack(wrenchItem), "I I", " G ", " I ", Character.valueOf('I'), Item.ingotIron, Character.valueOf('G'), stoneGearItem);
		GameRegistry.addRecipe(new ItemStack(woodenGearItem), " S ", "S S", " S ", Character.valueOf('S'), Item.stick);
		GameRegistry.addRecipe(new ItemStack(stoneGearItem), " I ", "IGI", " I ", Character.valueOf('I'), Block.cobblestone, Character.valueOf('G'),
				woodenGearItem);
		GameRegistry.addRecipe(new ItemStack(ironGearItem), " I ", "IGI", " I ", Character.valueOf('I'), Item.ingotIron, Character.valueOf('G'), stoneGearItem);
		GameRegistry.addRecipe(new ItemStack(goldGearItem), " I ", "IGI", " I ", Character.valueOf('I'), Item.ingotGold, Character.valueOf('G'), ironGearItem);
		GameRegistry.addRecipe(new ItemStack(diamondGearItem), " I ", "IGI", " I ", Character.valueOf('I'), Item.diamond, Character.valueOf('G'), goldGearItem);
	}*/
}
