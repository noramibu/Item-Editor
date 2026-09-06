package me.noramibu.itemeditor.util;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;

public final class RegistryUtil {

    private RegistryUtil() {}

    public static String resourceId(Identifier resource, String prefix, String suffix) {
        String path = resource.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(suffix) || path.length() < prefix.length() + suffix.length())
            return null;
        String value = path.substring(prefix.length(), path.length() - suffix.length());
        return value.isBlank() ? null : resource.getNamespace() + ":" + value;
    }

    public static <T> Holder<T> resolveHolder(Registry<T> registry, String rawId) {
        Identifier identifier = Identifier.tryParse(rawId);
        if (identifier == null) {
            return null;
        }
        return registry.get(identifier).orElse(null);
    }

    public static <T> Optional<Holder<T>> resolveOptionalHolder(
            Registry<T> registry, String rawId, String label, List<ValidationMessage> messages) {
        if (rawId.isBlank()) {
            return Optional.empty();
        }

        Holder<T> holder = resolveHolder(registry, rawId);
        if (holder == null) {
            messages.add(ValidationMessage.error(ItemEditorText.str("validation.registry_missing", label, rawId)));
            return Optional.empty();
        }
        return Optional.of(holder);
    }

    public static <T> List<String> ids(Registry<T> registry) {
        return registry.keySet().stream()
                .map(Identifier::toString)
                .sorted(Comparator.naturalOrder())
                .toList();
    }
}
