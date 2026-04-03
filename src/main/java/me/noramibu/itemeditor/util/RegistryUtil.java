package me.noramibu.itemeditor.util;

import me.noramibu.itemeditor.editor.ValidationMessage;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class RegistryUtil {

    private RegistryUtil() {
    }

    public static <T> Holder<T> resolveHolder(Registry<T> registry, String rawId) {
        ResourceLocation identifier = ResourceLocation.tryParse(rawId);
        if (identifier == null) {
            return null;
        }
        return registry.get(identifier).orElse(null);
    }

    public static <T> Optional<Holder<T>> resolveOptionalHolder(Registry<T> registry, String rawId, String label, List<ValidationMessage> messages) {
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
        return registry.keySet().stream().map(ResourceLocation::toString).sorted(Comparator.naturalOrder()).toList();
    }
}
