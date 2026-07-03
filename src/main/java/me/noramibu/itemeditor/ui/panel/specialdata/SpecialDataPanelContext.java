package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.UnifiedColorPickerDialog;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreenMode;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public record SpecialDataPanelContext(ItemEditorScreen screen) {
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 430;
    private static final int PICK_BUTTON_WIDTH_MIN = 70;
    private static final int PICK_BUTTON_WIDTH_MAX = 132;
    private static final int PICK_BUTTON_TEXT_MIN = 20;
    private static final int PICK_BUTTON_TEXT_RESERVE = 10;
    private static final int COLOR_INPUT_FIELD_WIDTH = 140;

    public ItemStack originalStack() {
        return this.screen.session().originalStack();
    }

    public ItemEditorState.SpecialData special() {
        return this.screen.session().state().special;
    }

    public int panelWidthHint() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

    public double guiScale() {
        return this.screen.session().minecraft().getWindow().getGuiScale();
    }

    public boolean isCompactPanel(int widthThreshold) {
        return LayoutModeUtil.isCompactPanel(this.guiScale(), this.panelWidthHint(), widthThreshold);
    }

    public <T> List<String> registryIds(ResourceKey<? extends Registry<T>> registryKey) {
        Registry<T> registry = this.screen.session().registryAccess().lookupOrThrow(registryKey);
        return RegistryUtil.ids(registry);
    }

    public List<String> itemIdsWithoutAir() {
        return this.registryIds(Registries.ITEM).stream()
                .filter(id -> !"minecraft:air".equals(id))
                .toList();
    }

    public <T> List<String> optionalRegistryIds(ResourceKey<? extends Registry<T>> registryKey) {
        try {
            return this.registryIds(registryKey);
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    public <T> List<String> registryTagIds(ResourceKey<? extends Registry<T>> registryKey, String prefix) {
        Registry<T> registry = this.screen.session().registryAccess().lookupOrThrow(registryKey);
        return registry.getTags()
                .map(tag -> prefix + tag.key().location())
                .sorted()
                .toList();
    }

    public <T> void swapEntries(List<T> entries, int left, int right) {
        if (left < 0 || right < 0 || left >= entries.size() || right >= entries.size()) {
            return;
        }
        Collections.swap(entries, left, right);
    }

    public void rebuildPreview() {
        this.screen.session().rebuildPreview();
    }

    public void mutate(Runnable mutation) {
        mutation.run();
        this.rebuildPreview();
    }

    public void mutateRefresh(Runnable mutation) {
        this.mutate(mutation);
        this.screen.refreshCurrentPanel();
    }

    public Consumer<String> bindText(Consumer<String> setter) {
        return value -> this.mutate(() -> setter.accept(value));
    }

    public Consumer<Boolean> bindToggle(Consumer<Boolean> setter) {
        return value -> this.mutate(() -> setter.accept(value));
    }

    public <T> void openDropdown(
            ButtonComponent anchor,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> selectionConsumer
    ) {
        this.screen.openDropdown(anchor, values, labelMapper, selectionConsumer);
    }

    public <T> void openClearableDropdown(
            ButtonComponent anchor,
            Component clearLabel,
            Runnable clearAction,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> selectionConsumer
    ) {
        this.screen.openClearableDropdown(anchor, clearLabel, clearAction, values, labelMapper, selectionConsumer);
    }

    public void openSearchablePicker(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> selectionConsumer
    ) {
        this.screen.openSearchablePickerDialog(title, body, values, labelMapper, selectionConsumer);
    }

    public ButtonComponent storagePickButton(Consumer<ItemStack> selectionConsumer) {
        return UiFactory.button(
                ItemEditorText.tr("common.pick_from_storage").copy().withColor(UiColors.PICKER),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> {
                    double panelScroll = this.screen.panelScrollOffset();
                    this.screen.session().minecraft().setScreen(new StorageScreen(
                            1,
                            "",
                            StorageSortMode.REGULAR,
                            StorageScreenMode.PICK_FOR_EDIT,
                            this.screen,
                            stack -> {
                                this.mutateRefresh(() -> selectionConsumer.accept(stack));
                                this.screen.preservePanelScrollOnNextBuild(panelScroll);
                                this.screen.restorePanelScroll(panelScroll);
                            }
                    ));
                }
        );
    }

    public FlowLayout createRemovableCard(Component title, Runnable removeAction) {
        return UiFactory.removableSubCard(title, () -> this.mutateRefresh(removeAction));
    }

    public FlowLayout createReorderableCard(
            Component title,
            boolean canMoveUp,
            Runnable moveUp,
            boolean canMoveDown,
            Runnable moveDown,
            Runnable remove
    ) {
        return UiFactory.reorderableSubCard(
                title,
                canMoveUp,
                moveUp == null ? null : () -> this.mutateRefresh(moveUp),
                canMoveDown,
                moveDown == null ? null : () -> this.mutateRefresh(moveDown),
                remove == null ? null : () -> this.mutateRefresh(remove)
        );
    }

    public FlowLayout colorInputWithPicker(
            String initialValue,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            int fallbackColor
    ) {
        boolean compactLayout = this.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(initialValue, this.bindText(setter)).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(COLOR_INPUT_FIELD_WIDTH)));

        int selectedColor = ValidationUtil.parseHexColorOrDefault(currentValueSupplier.get(), fallbackColor);
        ButtonComponent pickButton = UiFactory.button(
                Component.literal(ItemEditorText.str("common.pick")).withColor(selectedColor), UiFactory.ButtonTextPreset.STANDARD,
                button -> this.screen.openUnifiedColorPickerDialog(
                        pickerTitle,
                        UnifiedColorPickerDialog.Options.plainColor(ValidationUtil.parseHexColorOrDefault(currentValueSupplier.get(), fallbackColor)),
                        result -> this.mutateRefresh(() -> setter.accept(ValidationUtil.toHex(result.colors().getFirst())))
                )
        );
        pickButton.tooltip(List.of(Component.literal(ValidationUtil.toHex(selectedColor)).withColor(selectedColor)));
        if (compactLayout) {
            pickButton.horizontalSizing(Sizing.fill(100));
        } else {
            int buttonWidth = Math.clamp(
                    this.panelWidthHint() / 3,
                    PICK_BUTTON_WIDTH_MIN,
                    PICK_BUTTON_WIDTH_MAX
            );
            buttonWidth = Math.clamp(buttonWidth, 1, Math.max(1, this.panelWidthHint()));
            Component fullText = Component.literal(ItemEditorText.str("common.pick")).withColor(selectedColor);
            Component fitted = UiFactory.fitToWidth(
                    fullText,
                    Math.max(PICK_BUTTON_TEXT_MIN, buttonWidth - UiFactory.scaledPixels(PICK_BUTTON_TEXT_RESERVE))
            );
            pickButton.setMessage(fitted);
            pickButton.horizontalSizing(Sizing.fixed(buttonWidth));
        }
        row.child(pickButton);
        return row;
    }

}
