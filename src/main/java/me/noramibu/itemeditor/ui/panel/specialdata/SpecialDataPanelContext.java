package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;

public record SpecialDataPanelContext(ItemEditorScreen screen) {
    private static final int PICK_BUTTON_WIDTH_MIN = 70;
    private static final int PICK_BUTTON_WIDTH_MAX = 132;
    private static final int COLOR_INPUT_FIELD_WIDTH = 140;

    public List<EditorSearchDialog.Target> itemActionSearchTargets(
            EditorCategory category,
            List<String> path,
            String scope,
            Runnable expand,
            ItemStack stack,
            boolean editable) {
        var fields = new ArrayList<SpecialDataSearch.Field>();
        fields.add(ItemAction.PICK);
        fields.add(ItemAction.PICK_FROM_STORAGE);
        if (stack != null && !stack.isEmpty()) {
            fields.add(ItemAction.REMOVE);
            if (editable) fields.add(ItemAction.EDIT_STACK);
        }
        return SpecialDataSearch.targets(
                this, category, path, scope, expand, fields.toArray(SpecialDataSearch.Field[]::new));
    }

    public ItemStack originalStack() {
        return this.screen.session().originalStack();
    }

    public FlowLayout entityNameEditor(String value, String prefix, Consumer<String> setter) {
        var editor = StyledTextFieldSection.create(
                screen,
                RichTextDocument.fromMarkup(value),
                Sizing.fill(100),
                UiFactory.fixed(54),
                ItemEditorText.str(prefix + ".placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str(prefix + ".color_title"),
                ItemEditorText.str(prefix + ".gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1 ? ItemEditorText.str(prefix + ".single_line") : null,
                document -> mutate(() -> setter.accept(document.toMarkup())));
        return UiFactory.subCard()
                .child(UiFactory.title(NameField.CUSTOM_NAME.text()).shadow(false))
                .child(UiFactory.framedEditorCard()
                        .child(editor.toolbar())
                        .child(editor.editor())
                        .child(editor.validation()));
    }

    public FlowLayout itemSummary(ItemStack stack, Component label, boolean overlay) {
        FlowLayout row = UiFactory.row();
        if (stack != null && !stack.isEmpty())
            row.child(UIComponents.item(stack).showOverlay(overlay).setTooltipFromStack(true));
        return row.child(UiFactory.muted(label).horizontalSizing(Sizing.expand(100)));
    }

    public FlowLayout itemActions(ItemStack stack, Consumer<ItemStack> setter, Runnable onEdit) {
        Consumer<ItemStack> select = selected -> setter.accept(selected.copy());
        var remove = UiFactory.negativeButton(
                ItemAction.REMOVE.text(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> mutateRefresh(() -> select.accept(ItemStack.EMPTY)));
        remove.active(stack != null && !stack.isEmpty());
        ButtonComponent edit = onEdit == null
                ? null
                : UiFactory.button(
                        ItemAction.EDIT_STACK.text(), UiFactory.ButtonTextPreset.STANDARD, button -> onEdit.run());
        if (edit != null) edit.active(stack != null && !stack.isEmpty());
        return UiFactory.actionButtonRow(false, itemPickButton(select), storagePickButton(select), edit, remove);
    }

    public ItemEditorState.SpecialData special() {
        return this.screen.session().state().special;
    }

    public int panelWidthHint() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

    public static <T extends Comparable<T>> List<String> propertyValues(Property<T> property, boolean sorted) {
        var values = property.getPossibleValues().stream().map(property::getName);
        return (sorted ? values.sorted() : values).toList();
    }

    public boolean isCompactPanel(int widthThreshold) {
        return LayoutModeUtil.isCompactWidth(this.panelWidthHint(), widthThreshold);
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

    public List<String> equipmentAssetIds() {
        return this.resourceIds("equipment", "equipment/", ".json");
    }

    public List<String> waypointStyleIds() {
        return this.resourceIds("waypoint_style", "waypoint_style/", ".json");
    }

    public List<String> cameraOverlayIds() {
        return this.resourceIds("textures/misc", "textures/", ".png");
    }

    public List<String> tooltipStyleIds() {
        Set<String> sprites = new HashSet<>(
                this.resourceIds("textures/gui/sprites/tooltip", "textures/gui/sprites/tooltip/", ".png"));
        return sprites.stream()
                .filter(id -> id.endsWith("_background"))
                .map(id -> id.substring(0, id.length() - "_background".length()))
                .filter(id -> sprites.contains(id + "_frame"))
                .sorted()
                .toList();
    }

    private List<String> resourceIds(String directory, String pathPrefix, String suffix) {
        try {
            return this.screen
                    .session()
                    .minecraft()
                    .getResourceManager()
                    .listResources(directory, id -> id.getPath().endsWith(suffix))
                    .keySet()
                    .stream()
                    .map(id -> RegistryUtil.resourceId(id, pathPrefix, suffix))
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
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
            ButtonComponent anchor, List<T> values, Function<T, String> labelMapper, Consumer<T> selectionConsumer) {
        this.screen.openDropdown(anchor, values, labelMapper, selectionConsumer);
    }

    public <T> void openClearableDropdown(
            ButtonComponent anchor,
            Component clearLabel,
            Runnable clearAction,
            List<T> values,
            Function<T, String> labelMapper,
            Consumer<T> selectionConsumer) {
        this.screen.openClearableDropdown(anchor, clearLabel, clearAction, values, labelMapper, selectionConsumer);
    }

    public void openSearchablePicker(
            String title,
            String body,
            List<String> values,
            Function<String, String> labelMapper,
            Consumer<String> selectionConsumer) {
        this.screen.openSearchablePickerDialog(title, body, values, labelMapper, selectionConsumer);
    }

    public ButtonComponent storagePickButton(Consumer<ItemStack> selectionConsumer) {
        return UiFactory.button(
                ItemAction.PICK_FROM_STORAGE.text().copy().withColor(UiColors.PICKER),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> {
                    double panelScroll = this.screen.panelScrollOffset();
                    this.screen
                            .session()
                            .minecraft()
                            .setScreen(new StorageScreen(StorageScreenMode.SELECT, this.screen, stack -> {
                                this.mutateRefresh(() -> selectionConsumer.accept(stack));
                                this.screen.preservePanelScrollOnNextBuild(panelScroll);
                                this.screen.restorePanelScroll(panelScroll);
                            }));
                });
    }

    public ButtonComponent itemPickButton(Consumer<ItemStack> selectionConsumer) {
        return this.itemPickButton(ItemEditorText.str("dialog.picked_item.title"), selectionConsumer);
    }

    public ButtonComponent itemPickButton(String pickerTitle, Consumer<ItemStack> selectionConsumer) {
        return UiFactory.button(
                ItemAction.PICK.text().copy().withColor(UiColors.PICKER),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> this.openSearchablePicker(
                        pickerTitle,
                        "",
                        this.itemIdsWithoutAir(),
                        id -> id,
                        id -> this.screen.choosePickedItem(
                                new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))),
                                stack -> this.mutateRefresh(() -> selectionConsumer.accept(stack)))));
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
            Runnable remove) {
        return UiFactory.reorderableSubCard(
                title,
                canMoveUp,
                moveUp == null ? null : () -> this.mutateRefresh(moveUp),
                canMoveDown,
                moveDown == null ? null : () -> this.mutateRefresh(moveDown),
                remove == null ? null : () -> this.mutateRefresh(remove));
    }

    public FlowLayout colorInputWithPicker(
            String initialValue,
            Consumer<String> setter,
            Supplier<String> currentValueSupplier,
            String pickerTitle,
            int fallbackColor) {
        int rowGap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        boolean compactLayout = this.panelWidthHint() < COLOR_INPUT_FIELD_WIDTH + PICK_BUTTON_WIDTH_MIN + rowGap;
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(initialValue, this.bindText(setter))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(COLOR_INPUT_FIELD_WIDTH)));

        int selectedColor = ValidationUtil.parseHexColorOrDefault(currentValueSupplier.get(), fallbackColor);
        ButtonComponent pickButton = UiFactory.button(
                ItemAction.PICK.text().copy().withColor(selectedColor),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> this.screen.openUnifiedColorPickerDialog(
                        pickerTitle,
                        UnifiedColorPickerDialog.Options.plainColor(
                                ValidationUtil.parseHexColorOrDefault(currentValueSupplier.get(), fallbackColor)),
                        result -> this.mutateRefresh(() -> setter.accept(
                                ValidationUtil.toHex(result.colors().getFirst())))));
        pickButton.tooltip(
                List.of(Component.literal(ValidationUtil.toHex(selectedColor)).withColor(selectedColor)));
        if (compactLayout) {
            pickButton.horizontalSizing(Sizing.fill(100));
        } else {
            int buttonWidth = Math.clamp(this.panelWidthHint() / 3, PICK_BUTTON_WIDTH_MIN, PICK_BUTTON_WIDTH_MAX);
            buttonWidth = Math.clamp(buttonWidth, 1, Math.max(1, this.panelWidthHint()));
            pickButton.horizontalSizing(Sizing.fixed(buttonWidth));
        }
        row.child(pickButton);
        return row;
    }

    enum NameField implements SpecialDataSearch.Field {
        CUSTOM_NAME("common.custom_name");

        private final String key;

        NameField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    enum ItemAction implements SpecialDataSearch.Field {
        REMOVE("common.remove"),
        EDIT_STACK("special.entity.item.edit_stack"),
        PICK_FROM_STORAGE("common.pick_from_storage"),
        PICK("common.pick");

        private final String key;

        ItemAction(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
