package com.brandon3055.spawnmanager.network;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import com.brandon3055.brandonscore.utils.LogHelperBC;
import com.brandon3055.spawnmanager.SpawnManager;
import com.brandon3055.spawnmanager.SpawnDataManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.util.text.TextComponentString;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

public class ServerPacketHandler implements ICustomPacketHandler.IServerPacketHandler {

    private static Set<WeakReference<EntityPlayer>> playerCache = new HashSet<>();

    @Override
    public void handlePacket(PacketCustom packet, EntityPlayerMP sender, INetHandlerPlayServer handler) {
        playerCache = new HashSet<>();
        playerCache.removeIf(reference -> reference.get() == null || reference.get() == sender);
        switch (packet.getType()) {
            case 1: //Data Request Received
                if (SpawnManager.proxy.isOp(sender)) {
                    playerCache.add(new WeakReference<>(sender));
                    sendDataToListeners(false);
                }
                else {
                    new PacketCustom(SpawnManager.NET_CHANNEL, 2).sendToPlayer(sender);
                }
                break;
            case 2: //Data Update Received.
                if (SpawnManager.proxy.isOp(sender)) {
                    playerCache.add(new WeakReference<>(sender));
                    SpawnDataManager.deSerialize(packet);
                    sendDataToListeners(packet.readBoolean());
                }
                else {
                    //Yea i know this is kinda pointless because anyone who can figure out how to send an illegal packet will no doubt see this. But why not.
                    sender.sendMessage(new TextComponentString("I already told you. You dont have permission to use this feature! Attempting to hack your way past my security checks will only get you banned."));
                    LogHelperBC.warn("User: " + sender + ", may have tried to send an illegal packet to the server in an attempt to modify the spawn list via SpawnManager.");
                }
                break;
            case 3:
                if (SpawnManager.proxy.isOp(sender)) {
                    playerCache.add(new WeakReference<>(sender));
                    SpawnDataManager.resetEntity(packet.readResourceLocation());
                    sendDataToListeners(packet.readBoolean());
                }
                break;
        }
    }

    private void sendDataToListeners(boolean requiresReload) {
        PacketCustom packet = new PacketCustom(SpawnManager.NET_CHANNEL, 1); //Data Response
        SpawnDataManager.serialize(packet);
        packet.writeBoolean(requiresReload);

        for (WeakReference<EntityPlayer> ref : playerCache) {
            EntityPlayer player = ref.get();
            if (player != null) {
                packet.sendToPlayer(player);
            }
        }
    }
}