package com.tous.camkey.session;

/**
 * Outcome of a user action, as a translation key plus arguments. Arguments may be plain values, a
 * {@link Translatable}, or a list of either (rendered comma-separated).
 */
public record CommandResult(boolean success, String translationKey, Object[] args) {

    public static CommandResult success(String translationKey, Object... args) {
        return new CommandResult(true, translationKey, args);
    }

    public static CommandResult failure(String translationKey, Object... args) {
        return new CommandResult(false, translationKey, args);
    }
}
