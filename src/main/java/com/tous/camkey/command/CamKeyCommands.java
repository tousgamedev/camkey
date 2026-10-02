package com.tous.camkey.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import com.tous.camkey.CamKey;
import com.tous.camkey.capture.CameraCapture;
import com.tous.camkey.model.Keyframe;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class CamKeyCommands {

    private CamKeyCommands() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("camkey")
                .then(Commands.literal("add")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(CamKeyCommands::executeAdd)))
                .then(Commands.literal("use")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(CamKeyCommands::executeUse)))
                .then(Commands.literal("play")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .then(Commands.argument("seconds", DoubleArgumentType.doubleArg())
                                        .executes(CamKeyCommands::executePlay))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(CamKeyCommands::executeDelete)))
                .then(Commands.literal("list")
                        .executes(CamKeyCommands::executeList)));
    }

    private static int executeAdd(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Keyframe keyframe = CameraCapture.capture(camera);
        return report(context.getSource(), CamKeySessionHolder.session().add(name, keyframe));
    }

    private static int executeUse(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return report(context.getSource(), CamKeySessionHolder.session().use(name));
    }

    private static int executePlay(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        double seconds = DoubleArgumentType.getDouble(context, "seconds");
        return report(context.getSource(), CamKeySessionHolder.session().play(name, seconds));
    }

    private static int executeDelete(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return report(context.getSource(), CamKeySessionHolder.session().deleteLast(name));
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        return report(context.getSource(), CamKeySessionHolder.session().list());
    }

    private static int report(CommandSourceStack source, CommandResult result) {
        Component message = Component.translatable(result.translationKey(), result.args());
        if (result.success()) {
            source.sendSuccess(() -> message, false);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(message);
        return 0;
    }
}
