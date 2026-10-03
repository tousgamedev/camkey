package com.tous.camkey.session;

/**
 * A translation key plus its arguments, usable as an argument to a {@link CommandResult} so that
 * nested text (e.g. each entry of a list) is translated too, without this package depending on
 * Minecraft's text classes.
 */
public record Translatable(String key, Object... args) {
}
