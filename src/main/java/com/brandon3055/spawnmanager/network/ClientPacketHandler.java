package com.brandon3055.spawnmanager.network;

import codechicken.lib.packet.ICustomPacketHandler;
import codechicken.lib.packet.PacketCustom;
import com.brandon3055.spawnmanager.client.GuiCustomizeSpawns;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.util.text.TextComponentString;

public class ClientPacketHandler implements ICustomPacketHandler.IClientPacketHandler {

    @Override
    public void handlePacket(PacketCustom packet, Minecraft mc, INetHandlerPlayClient handler) {
        switch (packet.getType()) {
            case 1: //Data Response Received
                GuiScreen screen = mc.currentScreen;
                if (screen instanceof GuiCustomizeSpawns) {
                    ((GuiCustomizeSpawns) screen).handleDataPacket(packet);
                }
                break;
            case 2: //Permission Denied Response
                mc.displayGuiScreen(null);
                mc.setIngameFocus();
                mc.player.sendMessage(new TextComponentString("You do not have permission to use that command!"));
                break;
        }
    }
}