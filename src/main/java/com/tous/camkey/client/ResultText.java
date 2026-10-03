package com.tous.camkey.client;

import java.util.Arrays;
import java.util.Collection;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;

import com.tous.camkey.session.CommandResult;
import com.tous.camkey.session.Translatable;

/**
 * Turns a {@link CommandResult} into chat text. Failures are shown in red.
 */
public final class ResultText {

    private static final Component LIST_SEPARATOR = Component.literal(", ");

    private ResultText() {
    }

    public static Component toComponent(CommandResult result) {
        MutableComponent message = translate(result.translationKey(), result.args());
        return result.success() ? message : message.withStyle(ChatFormatting.RED);
    }

    private static MutableComponent translate(String key, Object[] args) {
        return Component.translatable(key, Arrays.stream(args).map(ResultText::toArgument).toArray());
    }

    private static Object toArgument(Object arg) {
        if (arg instanceof Translatable translatable) {
            return translate(translatable.key(), translatable.args());
        }
        if (arg instanceof Collection<?> items) {
            return ComponentUtils.formatList(items, LIST_SEPARATOR, item -> asComponent(toArgument(item)));
        }
        return arg;
    }

    private static Component asComponent(Object value) {
        return value instanceof Component component ? component : Component.literal(String.valueOf(value));
    }
}
