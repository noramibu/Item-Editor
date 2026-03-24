package me.noramibu.itemeditor.editor;

public record ValidationMessage(Severity severity, String message) {

    public static ValidationMessage error(String message) {
        return new ValidationMessage(Severity.ERROR, message);
    }

    public static ValidationMessage warning(String message) {
        return new ValidationMessage(Severity.WARNING, message);
    }

    public enum Severity {
        ERROR,
        WARNING,
        INFO
    }
}
