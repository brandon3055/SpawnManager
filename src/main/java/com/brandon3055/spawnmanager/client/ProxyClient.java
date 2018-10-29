package com.brandon3055.spawnmanager.client;

import codechicken.lib.packet.PacketCustom;
import com.brandon3055.brandonscore.lib.DelayedTask;
import com.brandon3055.spawnmanager.ProxyCommon;
import com.brandon3055.spawnmanager.network.ClientPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

import static com.brandon3055.spawnmanager.SpawnManager.NET_CHANNEL;

/**
 * Created by brandon3055 on 10/10/18.
 */
public class ProxyClient extends ProxyCommon {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        PacketCustom.assignHandler(NET_CHANNEL, new ClientPacketHandler());
        ClientCommandHandler.instance.registerCommand(new CommandBase() {
            @Override
            public String getName() {
                return "spawnmanager";
            }

            @Override
            public String getUsage(ICommandSender sender) {
                return "/spawnmanager";
            }

            @Override
            public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
                Minecraft mc = Minecraft.getMinecraft();
                DelayedTask.run(1, () -> mc.displayGuiScreen(new GuiCustomizeSpawns(mc.player)));
            }
        });
    }

    @Override
    public void loadComplete() {
        super.loadComplete();
    }

    @Override
    public boolean isOp(EntityPlayer player) {
        return player.capabilities.isCreativeMode;
    }
}
