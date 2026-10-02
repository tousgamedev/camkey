package com.tous.camkey.command;

public record CommandResult(boolean success, String translationKey, Object[] args) {

    public static CommandResult success(String translationKey, Object... args) {
        return new CommandResult(true, translationKey, args);
    }

    public static CommandResult failure(String translationKey, Object... args) {
        return new CommandResult(false, translationKey, args);
    }
}
