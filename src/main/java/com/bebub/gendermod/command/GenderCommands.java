package com.bebub.gendermod.command;

import com.bebub.gendermod.GenderMod;
import com.bebub.gendermod.GenderGameplayEvents;
import com.bebub.gendermod.config.GenderConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GenderMod.MOD_ID)
public class GenderCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("gendermod")
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(GenderCommands::reload))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            GenderConfig.reload();
            GenderGameplayEvents.reloadConfig();
            ctx.getSource().sendSuccess(() -> 
                Component.literal("§a[GenderMod] Config reloaded!"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("§c[GenderMod] Failed to reload config!"));
            return 0;
        }
    }
}