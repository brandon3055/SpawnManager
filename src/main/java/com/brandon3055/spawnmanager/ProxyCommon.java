package com.brandon3055.spawnmanager;

import codechicken.lib.packet.PacketCustom;
import com.brandon3055.spawnmanager.network.ServerPacketHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import static com.brandon3055.spawnmanager.SpawnManager.NET_CHANNEL;

/**
 * Created by brandon3055 on 10/10/18.
 */
public class ProxyCommon {

    public void preInit(FMLPreInitializationEvent event) {
        PacketCustom.assignHandler(NET_CHANNEL, new ServerPacketHandler());
    }

    public void loadComplete() {
        SMConfig.initialize();
        SpawnDataManager.applySpawnConfig();
    }

    public boolean isOp(EntityPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) return false;

        for (String str : server.getPlayerList().getOppedPlayerNames()) {
            if (player.getName().equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }
}
