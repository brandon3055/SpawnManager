package com.brandon3055.spawnmanager;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkCheckHandler;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;


@Mod(modid = SpawnManager.MODID, name = SpawnManager.MODNAME, version = SpawnManager.VERSION)
public class SpawnManager {
    public static final String MODID = "spawnmanager";
    public static final String MODNAME = "Spawn Manager";
    public static final String VERSION = "${mod_version}";
    public static final String NET_CHANNEL = "SpawnManagerCh";

    @Mod.Instance(SpawnManager.MODID)
    public static SpawnManager instance;

    @SidedProxy(clientSide = "com.brandon3055.spawnmanager.client.ProxyClient", serverSide = "com.brandon3055.spawnmanager.ProxyCommon")
    public static ProxyCommon proxy;

    @NetworkCheckHandler
    public boolean networkCheck(Map<String, String> map, Side side) {
        return true;
    }

    @Mod.EventHandler
    public void serverStart(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "spawnmanager_reload";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "/spawnmanager_reload - Reloads spawn config from disk";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
                SMConfig.load();
                SpawnDataManager.applySpawnConfig();
            }
        });
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void loadComplete(FMLLoadCompleteEvent event) {
        proxy.loadComplete();
    }
}
