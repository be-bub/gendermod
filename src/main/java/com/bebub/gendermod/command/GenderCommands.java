package com.bebub.genderbub.command;

import com.bebub.genderbub.GenderMod;
import com.bebub.genderbub.GenderGameplayEvents;
import com.bebub.genderbub.config.GenderConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod.EventBusSubscriber(modid = GenderMod.MOD_ID)
public class GenderCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        
        dispatcher.register(Commands.literal("genderbub")
            .then(Commands.literal("reload")
                .requires(source -> source.hasPermission(2))
                .executes(GenderCommands::reload))
            .then(Commands.literal("reset")
                .requires(source -> source.hasPermission(2))
                .executes(GenderCommands::reset))
        );
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        try {
            Path configPath = GenderConfig.getConfigPath();
            if (configPath == null || !Files.exists(configPath) || Files.size(configPath) == 0) {
                ctx.getSource().sendFailure(Component.literal("Config missing, creating new one..."));
                GenderConfig.resetToDefault();
                ctx.getSource().sendSuccess(() -> Component.literal("New config created!"), true);
                return 1;
            }
            
            GenderConfig.reload();
            GenderGameplayEvents.reloadConfig();
            ctx.getSource().sendSuccess(() -> Component.literal("Config reloaded!"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to reload config: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int reset(CommandContext<CommandSourceStack> ctx) {
        try {
            GenderConfig.resetToDefault();
            GenderGameplayEvents.reloadConfig();
            ctx.getSource().sendSuccess(() -> Component.literal("Config reset to default!"), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(Component.literal("Failed to reset config: " + e.getMessage()));
            return 0;
        }
    }
}