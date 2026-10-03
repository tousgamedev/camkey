package com.tous.camkey.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import com.tous.camkey.CamKey;
import com.tous.camkey.capture.CameraCapture;
import com.tous.camkey.client.CamKeySessionHolder;
import com.tous.camkey.client.ResultText;
import com.tous.camkey.model.Keyframe;
import com.tous.camkey.session.CommandResult;

@EventBusSubscriber(modid = CamKey.MODID, value = Dist.CLIENT)
public final class CamKeyCommands {

    private static final String DURATION_ARG = "duration";

    /** A command action that needs the typed duration, already converted to seconds. */
    @FunctionalInterface
    private interface DurationAction {
        int run(CommandContext<CommandSourceStack> context, double seconds);
    }

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
                                .executes(CamKeyCommands::executePlayDefaultDuration)
                                .then(durationArgument(CamKeyCommands::executePlay))))
                .then(Commands.literal("playactive")
                        .executes(CamKeyCommands::executePlayActiveDefaultDuration)
                        .then(durationArgument(CamKeyCommands::executePlayActive)))
                .then(Commands.literal("delete")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(CamKeyCommands::executeDelete)))
                .then(Commands.literal("list")
                        .executes(CamKeyCommands::executeList)));
    }

    /**
     * {@code <duration> [second|seconds|minute|minutes]} — a bare number is seconds, matching the
     * spec's own example ({@code /camkey play intro 10 seconds}).
     */
    private static RequiredArgumentBuilder<CommandSourceStack, Double> durationArgument(DurationAction action) {
        RequiredArgumentBuilder<CommandSourceStack, Double> duration =
                Commands.argument(DURATION_ARG, DoubleArgumentType.doubleArg());
        duration.executes(context -> action.run(context, DoubleArgumentType.getDouble(context, DURATION_ARG)));
        for (DurationUnit unit : DurationUnit.values()) {
            for (String word : unit.words()) {
                duration.then(Commands.literal(word).executes(context ->
                        action.run(context, unit.toSeconds(DoubleArgumentType.getDouble(context, DURATION_ARG)))));
            }
        }
        return duration;
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

    private static int executePlay(CommandContext<CommandSourceStack> context, double seconds) {
        String name = StringArgumentType.getString(context, "name");
        return report(context.getSource(), CamKeySessionHolder.session().play(name, seconds));
    }

    private static int executePlayDefaultDuration(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return report(context.getSource(), CamKeySessionHolder.session().play(name));
    }

    private static int executePlayActive(CommandContext<CommandSourceStack> context, double seconds) {
        return report(context.getSource(), CamKeySessionHolder.session().playActive(seconds));
    }

    private static int executePlayActiveDefaultDuration(CommandContext<CommandSourceStack> context) {
        return report(context.getSource(), CamKeySessionHolder.session().playActive());
    }

    private static int executeDelete(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        return report(context.getSource(), CamKeySessionHolder.session().deleteLast(name));
    }

    private static int executeList(CommandContext<CommandSourceStack> context) {
        return report(context.getSource(), CamKeySessionHolder.session().list());
    }

    private static int report(CommandSourceStack source, CommandResult result) {
        if (result.success()) {
            source.sendSuccess(() -> ResultText.toComponent(result), false);
            return Command.SINGLE_SUCCESS;
        }
        source.sendFailure(ResultText.toComponent(result));
        return 0;
    }
}
