package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.core.VerticalAlignment;
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
import me.noramibu.itemeditor.editor.ItemEditorFieldReset;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.CompactFieldLayout;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.UnifiedColorPickerDialog;
import me.noramibu.itemeditor.ui.panel.PanelBindings;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreenMode;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.ui.util.UiColors;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.properties.Property;

public record SpecialDataPanelContext(ItemEditorScreen screen) {
    static final SpecialDataSearch.Field CUSTOM_NAME_FIELD = () -> "common.custom_name";
    private static final int PICK_BUTTON_WIDTH_MIN = 70;
    private static final int PICK_BUTTON_WIDTH_MAX = 132;
    private static final int COLOR_INPUT_FIELD_WIDTH = 140;

    public List<EditorSearchDialog.Target> itemActionSearchTargets(
            EditorCategory category, List<String> path, String scope, Runnable expand, ItemStack stack) {
        var fields = new ArrayList<SpecialDataSearch.Field>();
        fields.add(ItemAction.PICK);
        if (stack != null && !stack.isEmpty()) {
            fields.add(ItemAction.REMOVE);
            fields.add(ItemAction.EDIT_STACK);
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
                .child(UiFactory.title(CUSTOM_NAME_FIELD.text()).shadow(false))
                .child(UiFactory.framedEditorCard()
                        .child(editor.toolbar())
                        .child(editor.editor())
                        .child(editor.validation()));
    }

    public FlowLayout itemRow(
            Supplier<ItemStack> current, Consumer<ItemStack> setter, Runnable removeAction, Component contextTitle) {
        ItemStack stack = current.get();
        boolean empty = stack == null || stack.isEmpty();
        int availableWidth =
                Math.max(1, this.panelWidthHint() - UiFactory.scaleProfile().padding() * 4);
        int actionWidth = Math.min(availableWidth, UiFactory.scaledPixels(180));
        int gap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        boolean compact = availableWidth < UiFactory.scaledPixels(240) + actionWidth + gap;
        FlowLayout row = compact ? UiFactory.column() : UiFactory.row();
        Component name = empty
                ? ItemEditorText.tr("common.item_empty")
                : stack.getHoverName().copy().append(" x" + stack.getCount());
        int summaryWidth =
                Math.clamp(availableWidth - (compact ? 0 : actionWidth + gap), 1, UiFactory.scaledPixels(240));
        int width = Math.max(1, summaryWidth - UiFactory.scaledPixels(36));
        var label = UIComponents.label(UiFactory.fitToWidth(name, width));
        label.tooltip(List.of(name));
        FlowLayout summary = UiFactory.row();
        summary.verticalAlignment(VerticalAlignment.CENTER);
        FlowLayout slot = UiFactory.row();
        slot.horizontalSizing(Sizing.fixed(24));
        slot.verticalSizing(Sizing.fixed(24));
        slot.padding(Insets.of(4));
        slot.surface(Surface.flat(0xFF202020).and(Surface.outline(0xFF888888)));
        slot.child(UIComponents.item(empty ? ItemStack.EMPTY : stack)
                .showOverlay(true)
                .setTooltipFromStack(true));
        summary.child(slot);
        FlowLayout details = UiFactory.column().gap(2);
        details.child(label);
        Component detail = empty
                ? ItemEditorText.tr("common.pick_item_hint")
                : Component.literal(
                        BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        var subtitle = UiFactory.muted(UiFactory.fitToWidth(detail, width));
        subtitle.tooltip(List.of(detail));
        details.child(subtitle);
        summary.child(details.horizontalSizing(Sizing.expand(100)));
        row.child(summary.horizontalSizing(Sizing.fixed(summaryWidth)));
        if (!compact) row.verticalAlignment(VerticalAlignment.CENTER);
        ButtonComponent remove = removeAction == null
                ? null
                : UiFactory.negativeButton(
                        Component.literal("x"),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> this.mutateRefresh(removeAction));
        if (remove != null) {
            remove.active(!empty);
            remove.tooltip(List.of(ItemAction.REMOVE.text()));
        }
        FlowLayout actions = UiFactory.packedActionButtonRow(
                this.itemEditButton(current, setter, contextTitle),
                this.itemPickButton(ItemEditorText.str("dialog.picked_item.title"), setter, contextTitle),
                remove);
        actions.horizontalSizing(compact ? Sizing.fill(100) : Sizing.fixed(actionWidth));
        row.child(actions);
        return row;
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

    private void openStoragePicker(Consumer<ItemStack> selectionConsumer, Component contextTitle) {
        double panelScroll = this.screen.panelScrollOffset();
        this.screen
                .session()
                .minecraft()
                .setScreenAndShow(new StorageScreen(
                        StorageScreenMode.SELECT,
                        this.screen,
                        this.screen.contextualSelection(
                                stack -> {
                                    this.mutateRefresh(() -> selectionConsumer.accept(stack));
                                    this.screen.preservePanelScrollOnNextBuild(panelScroll);
                                    this.screen.restorePanelScroll(panelScroll);
                                },
                                contextTitle)));
    }

    public ButtonComponent itemEditButton(
            Supplier<ItemStack> current, Consumer<ItemStack> setter, Component contextTitle) {
        var edit = UiFactory.button(
                ItemEditorText.tr("common.edit").copy().withStyle(ChatFormatting.YELLOW),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> {
                    ItemStack stack = current.get();
                    if (stack != null && !stack.isEmpty()) {
                        this.screen.openNestedEditor(
                                stack, null, edited -> this.mutate(() -> setter.accept(edited)), contextTitle);
                    }
                });
        this.updateItemEditButton(edit, current.get());
        return edit;
    }

    public void updateItemEditButton(ButtonComponent button, ItemStack stack) {
        boolean available = stack != null && !stack.isEmpty();
        button.active(available);
        button.tooltip(
                List.of(ItemEditorText.tr(available ? "common.item_edit.tooltip" : "common.item_edit.empty.tooltip")));
    }

    public ButtonComponent itemPickButton(String pickerTitle, Consumer<ItemStack> selectionConsumer) {
        return this.itemPickButton(pickerTitle, selectionConsumer, Component.literal(pickerTitle));
    }

    private ButtonComponent itemPickButton(
            String pickerTitle, Consumer<ItemStack> selectionConsumer, Component contextTitle) {
        return this.itemPickButton(pickerTitle, this.itemIdsWithoutAir(), selectionConsumer, contextTitle);
    }

    public ButtonComponent itemPickButton(
            String pickerTitle, List<String> itemIds, Consumer<ItemStack> selectionConsumer, Component contextTitle) {
        var pick = UiFactory.button(
                ItemAction.PICK.text().copy().withColor(UiColors.PICKER),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> this.screen.chooseItemSource(
                        () -> this.openSearchablePicker(
                                pickerTitle,
                                "",
                                itemIds,
                                id -> id,
                                id -> this.screen.choosePickedItem(
                                        new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id))),
                                        this.screen.contextualSelection(
                                                stack -> this.mutateRefresh(() -> selectionConsumer.accept(stack)),
                                                contextTitle))),
                        () -> this.openStoragePicker(selectionConsumer, contextTitle)));
        pick.tooltip(List.of(ItemEditorText.tr("common.item_pick.tooltip")));
        return pick;
    }

    public TextBoxComponent boundTextBox(String key) {
        return PanelBindings.textBox(this.screen, key, this::mutate);
    }

    public FlowLayout boundIdField(String key, List<String> entries, int width) {
        var binding = ItemEditorFieldReset.text(ItemEditorText.key(key));
        var state = this.screen.session().state();
        return AdvancedItemSpecialDataSection.compactIdField(
                this,
                ItemEditorText.tr(key),
                binding.read().apply(state),
                value -> binding.write().accept(state, value),
                entries,
                ItemEditorText.str(key),
                width);
    }

    public FlowLayout boundTextField(String key, int width) {
        return CompactFieldLayout.compactField(ItemEditorText.tr(key), this.boundTextBox(key), width + 40);
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

    enum ItemAction implements SpecialDataSearch.Field {
        REMOVE("common.remove"),
        EDIT_STACK("special.entity.item.edit_stack"),
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
