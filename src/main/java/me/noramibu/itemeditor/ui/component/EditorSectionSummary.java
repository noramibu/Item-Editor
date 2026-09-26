package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.LabelComponent;
import java.util.List;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class EditorSectionSummary {
    private EditorSectionSummary() {}

    public static LabelComponent label(Component text, int width) {
        int available = Math.max(1, width);
        LabelComponent label = UiFactory.muted(UiFactory.fitToWidth(text, available), available);
        label.tooltip(List.of(text));
        return label;
    }

    public static Component forSection(Component title, ItemEditorState state) {
        if (!(title.getContents() instanceof TranslatableContents translation)) return Component.empty();
        var special = state.special;
        return switch (translation.getKey()) {
            case "itemeditor.general.identity.title" ->
                values("general.stack_count", state.count, "general.rarity", state.rarity);
            case "itemeditor.general.durability.title" ->
                values(
                        "general.current_damage",
                        state.currentDamage,
                        "general.max_damage",
                        state.maxDamage,
                        "general.repair_cost",
                        state.repairCost);
            case "itemeditor.general.visual_overrides.title" ->
                values(
                        "general.glint_override.enable",
                        state.glintOverride.isBlank() ? ItemEditorText.str("common.unset") : state.glintOverride);
            case "itemeditor.general.item_model.title" ->
                values(
                        "general.item_model.id", state.itemModelId,
                        "general.item_model.float", state.customModelFloat,
                        "general.item_model.string", state.customModelString);
            case "itemeditor.general.adventure.title" ->
                values(
                        "general.adventure.can_break",
                                state.canBreakAnyBlock ? "*" : Integer.toString(state.canBreakBlockIds.size()),
                        "general.adventure.can_place_on",
                                state.canPlaceOnAnyBlock ? "*" : Integer.toString(state.canPlaceOnBlockIds.size()));
            case "itemeditor.special.advanced.food.title" ->
                values(
                        "special.advanced.food.nutrition", special.foodNutrition,
                        "special.advanced.food.saturation", special.foodSaturation,
                        "special.advanced.consumable.consume_seconds", special.consumableConsumeSeconds,
                        "special.advanced.use_cooldown.seconds", special.useCooldownSeconds);
            case "itemeditor.special.advanced.component_tweaks.naming_title" ->
                values(
                        "special.advanced.component_tweaks.item_name", special.itemName,
                        "special.advanced.component_tweaks.enchantable", special.enchantableValue,
                        "special.advanced.component_tweaks.tooltip_style", special.tooltipStyleId);
            case "itemeditor.special.advanced.crossbow.title" ->
                values("special.advanced.crossbow.title", Integer.toString(special.chargedProjectiles.size()));
            default -> Component.empty();
        };
    }

    public static Component values(String... pairs) {
        var result = Component.empty();
        for (int i = 0; i < pairs.length; i += 2) {
            String value = pairs[i + 1];
            if (value == null || value.isBlank()) continue;
            if (!result.getSiblings().isEmpty()) result.append(separator());
            result.append(ItemEditorText.tr(pairs[i]).copy().withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                    .append(value(value));
        }
        return result;
    }

    public static Component separator() {
        return Component.literal(" | ").withStyle(ChatFormatting.GRAY);
    }

    public static Component configuredCount(long count) {
        return ItemEditorText.tr("special.entity.effects.summary", count)
                .copy()
                .withStyle(count == 0 ? ChatFormatting.GRAY : ChatFormatting.AQUA);
    }

    public static Component value(String value) {
        boolean unset = value.isBlank()
                || value.equals("-")
                || value.equals("?")
                || value.equals(ItemEditorText.str("common.unset"))
                || value.equals(ItemEditorText.str("common.none"));
        return Component.literal(value)
                .withStyle(
                        unset
                                ? ChatFormatting.DARK_GRAY
                                : value.contains(":") ? ChatFormatting.AQUA : ChatFormatting.GREEN);
    }
}
