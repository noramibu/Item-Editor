package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.RichTextAreaComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.ui.util.TriStateBooleanUi;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

public final class GeneralEditorPanel implements EditorPanel {
    private static final int COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD = 760;
    private static final int STACK_COLUMNS_WIDTH_THRESHOLD = 720;
    private static final int STACK_ROW_MARGIN_TOP = 2;
    private static final int FIELD_LABEL_WIDTH_MIN = 72;
    private static final int FIELD_LABEL_DYNAMIC_MIN = 88;
    private static final int FIELD_LABEL_COMPACT_RESERVE = 56;
    private static final int FIELD_LABEL_REGULAR_RESERVE = 42;
    private static final int RARITY_BUTTON_WIDTH = 140;
    private static final int DURABILITY_NUMERIC_FIELD_WIDTH = 96;
    private static final int DURABILITY_LABEL_WIDTH_CURRENT = 140;
    private static final int DURABILITY_LABEL_WIDTH_MAX = 120;
    private static final int DURABILITY_LABEL_WIDTH_REPAIR = 110;
    private static final int ITEM_MODEL_ID_LABEL_WIDTH = 190;
    private static final int ITEM_MODEL_VALUE_LABEL_WIDTH = 170;
    private static final int ITEM_MODEL_PICK_BUTTON_WIDTH_MIN = 64;
    private static final int ITEM_MODEL_PICK_BUTTON_WIDTH_MAX = 116;
    private static final int ITEM_MODEL_PICK_BUTTON_WIDTH_DIVISOR = 7;
    private static final int ITEM_MODEL_STACK_PICK_THRESHOLD = 420;
    private static final int ITEM_MODEL_VALUE_ROW_STACK_THRESHOLD = 720;
    private static final int ITEM_MODEL_ID_INPUT_MIN_WIDTH = 140;
    private static final int ITEM_MODEL_TEXTBOX_HEIGHT_REDUCTION = 2;
    private static final int ITEM_MODEL_TEXTBOX_MIN_HEIGHT = 14;
    private static final int UNBOUNDED_TEXT_LIMIT = Integer.MAX_VALUE;
    private static final int CUSTOM_NAME_EDITOR_HEIGHT = 54;
    private static final int ADVENTURE_COUNT_HINT_WIDTH = 60;
    private static final int ADVENTURE_EMPTY_HINT_WIDTH = 320;
    private static final String SYMBOL_MOVE_UP = "^";
    private static final String SYMBOL_MOVE_DOWN = "v";
    private static final String SYMBOL_REMOVE = "x";
    private static final String SYMBOL_ADD = "+";
    private static final int ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH = 36;
    private static final int ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH = 42;
    private static final int ADVENTURE_STACK_CONTROLS_WIDTH_THRESHOLD = 520;
    private static final int ADVENTURE_ENTRY_LABEL_MIN_WIDTH = 120;
    private static final int ADVENTURE_ENTRY_LABEL_INLINE_RESERVE = 220;
    private static final int ENTRY_ACTION_BUTTON_SIZE_BASE = 32;
    private static final int ENTRY_ACTION_BUTTON_SIZE_MIN = 26;

    private enum Group {
        IDENTITY("general.identity.title"),
        DURABILITY("general.durability.title"),
        VISUAL("general.visual_overrides.title"),
        MODEL("general.item_model.title"),
        ADVENTURE("general.adventure.title");

        private final DocumentPanelSearch.Label descriptor;

        Group(String key) {
            this.descriptor = new DocumentPanelSearch.Label(key);
        }

        Component label() {
            return descriptor.label();
        }

        void expand(ItemEditorState state) {
            switch (this) {
                case IDENTITY -> state.uiGeneralIdentityCollapsed = false;
                case DURABILITY -> state.uiGeneralDurabilityCollapsed = false;
                case VISUAL -> state.uiGeneralVisualOverridesCollapsed = false;
                case MODEL -> state.uiGeneralItemModelCollapsed = false;
                case ADVENTURE -> state.uiGeneralAdventureCollapsed = false;
            }
        }
    }

    private enum Field {
        NAME("general.identity.custom_name.placeholder", Group.IDENTITY, "minecraft:custom_name customName item_name"),
        NAME_COLOR("general.identity.custom_name.color_title", Group.IDENTITY, "color shadow"),
        NAME_GRADIENT("general.identity.custom_name.gradient_title", Group.IDENTITY, "gradient"),
        RENDER("common.render_objects", Group.IDENTITY, "uiRenderObjectsInCustomName"),
        COUNT("general.stack_count", Group.IDENTITY, "count"),
        MAX_STACK("special.advanced.component_tweaks.max_stack_size", Group.IDENTITY, "minecraft:max_stack_size"),
        RARITY("general.rarity", Group.IDENTITY, "minecraft:rarity"),
        DAMAGE("general.current_damage", Group.DURABILITY, "minecraft:damage currentDamage"),
        MAX_DAMAGE("general.max_damage", Group.DURABILITY, "minecraft:max_damage"),
        REPAIR("general.repair_cost", Group.DURABILITY, "minecraft:repair_cost"),
        UNBREAKABLE("general.unbreakable", Group.DURABILITY, "minecraft:unbreakable"),
        GLINT("general.glint_override.enable", Group.VISUAL, "minecraft:enchantment_glint_override glintOverride"),
        MODEL_ID("general.item_model.id", Group.MODEL, "minecraft:item_model itemModelId"),
        MODEL_PICK("common.pick", Group.MODEL, "minecraft:item_model picker"),
        MODEL_FLOAT("general.item_model.float", Group.MODEL, "minecraft:custom_model_data floats customModelFloat"),
        MODEL_STRING("general.item_model.string", Group.MODEL, "minecraft:custom_model_data strings customModelString"),
        MODEL_COLOR("general.item_model.color", Group.MODEL, "minecraft:custom_model_data colors customModelColor"),
        MODEL_FLAGS("general.item_model.flag_value", Group.MODEL, "minecraft:custom_model_data flags customModelFlags");

        private final DocumentPanelSearch.Label descriptor;
        private final Group group;

        Field(String key, Group group, String aliases) {
            this.descriptor = new DocumentPanelSearch.Label(key, aliases);
            this.group = group;
        }

        Component label() {
            return descriptor.label();
        }

        boolean applicable(Capabilities capabilities) {
            return switch (this) {
                case DAMAGE, MAX_DAMAGE, UNBREAKABLE -> capabilities.durability();
                case REPAIR -> capabilities.repairCost();
                default -> true;
            };
        }
    }

    private enum AdventureField {
        BREAK("general.adventure.can_break"),
        PLACE("general.adventure.can_place_on"),
        ANY("general.adventure.any_block"),
        TOOLTIP("general.adventure.show_tooltip"),
        ADD("general.adventure.add_block"),
        CLEAR("general.adventure.clear"),
        UP("common.up"),
        DOWN("common.down"),
        REMOVE("common.remove");

        private final DocumentPanelSearch.Label descriptor;

        AdventureField(String key) {
            this.descriptor = new DocumentPanelSearch.Label(key);
        }

        Component label() {
            return descriptor.label();
        }
    }

    record Capabilities(boolean durability, boolean repairCost, boolean canBreak, boolean canPlaceOn) {
        static Capabilities from(ItemEditorScreen screen) {
            ItemStack stack = screen.session().originalStack();
            RegistryAccess registries = screen.session().registryAccess();
            return new Capabilities(
                    ItemEditorCapabilities.supportsDurability(stack),
                    ItemEditorCapabilities.supportsRepairCost(stack),
                    ItemEditorCapabilities.supportsComponent(stack, registries, "minecraft:can_break"),
                    ItemEditorCapabilities.supportsComponent(stack, registries, "minecraft:can_place_on"));
        }

        boolean adventure(boolean breaking) {
            return breaking ? canBreak : canPlaceOn;
        }
    }

    static boolean adventureEntriesVisible(boolean anyBlock) {
        return !anyBlock;
    }

    static boolean hasPrevious(int index) {
        return index > 0;
    }

    static boolean hasNext(int index, int size) {
        return index < size - 1;
    }

    private static String adventureEntryScope(AdventureField list, int index) {
        return list.descriptor.fullKey() + "-entry-" + index;
    }

    @Override
    public List<EditorSearchDialog.Target> searchTargets() {
        return searchTargets(this.screen, this.screen.session().state(), Capabilities.from(this.screen));
    }

    static List<EditorSearchDialog.Target> searchTargets(
            ItemEditorScreen screen, ItemEditorState state, Capabilities capabilities) {
        var targets = new ArrayList<EditorSearchDialog.Target>();
        var category = EditorCategory.GENERAL;
        for (Group group : Group.values()) {
            if (group == Group.DURABILITY && !capabilities.durability() && !capabilities.repairCost()) continue;
            if (group == Group.ADVENTURE && !capabilities.canBreak() && !capabilities.canPlaceOn()) continue;
            targets.add(DocumentPanelSearch.target(
                    screen,
                    category,
                    List.of(),
                    group.label().getString(),
                    group.descriptor.terms(),
                    "",
                    group.descriptor.fullKey(),
                    () -> group.expand(state)));
        }
        for (Field field : Field.values()) {
            if (!field.applicable(capabilities) || field == Field.NAME_COLOR || field == Field.NAME_GRADIENT) continue;
            String anchor =
                    switch (field) {
                        case NAME -> Group.IDENTITY.descriptor.fullKey();
                        default -> field.descriptor.fullKey();
                    };
            targets.add(DocumentPanelSearch.target(
                    screen,
                    category,
                    List.of(field.group.label().getString()),
                    field.label().getString(),
                    field.descriptor.terms(),
                    "",
                    anchor,
                    () -> field.group.expand(state)));
        }
        addAdventureTargets(targets, screen, state, capabilities, true);
        addAdventureTargets(targets, screen, state, capabilities, false);
        return List.copyOf(targets);
    }

    private static void addAdventureTargets(
            List<EditorSearchDialog.Target> targets,
            ItemEditorScreen screen,
            ItemEditorState state,
            Capabilities capabilities,
            boolean breaking) {
        if (!capabilities.adventure(breaking)) return;
        AdventureField list = breaking ? AdventureField.BREAK : AdventureField.PLACE;
        List<String> entries = breaking ? state.canBreakBlockIds : state.canPlaceOnBlockIds;
        boolean anyBlock = breaking ? state.canBreakAnyBlock : state.canPlaceOnAnyBlock;
        var category = EditorCategory.GENERAL;
        var parents = List.of(Group.ADVENTURE.label().getString(), list.label().getString());
        Runnable expand = () -> Group.ADVENTURE.expand(state);
        Runnable expandEntries = () -> {
            expand.run();
            if (breaking) state.canBreakCollapsed = false;
            else state.canPlaceOnCollapsed = false;
        };
        String aliases = breaking ? "minecraft:can_break" : "minecraft:can_place_on";
        targets.add(DocumentPanelSearch.target(
                screen,
                category,
                List.of(Group.ADVENTURE.label().getString()),
                list.label().getString(),
                list.descriptor.terms() + " " + aliases,
                "",
                list.descriptor.fullKey(),
                expandEntries));
        for (AdventureField field :
                List.of(AdventureField.ANY, AdventureField.TOOLTIP, AdventureField.ADD, AdventureField.CLEAR)) {
            if (field == AdventureField.CLEAR && entries.isEmpty()) continue;
            targets.add(DocumentPanelSearch.target(
                    screen,
                    category,
                    parents,
                    field.label().getString(),
                    field.descriptor.terms() + " " + aliases,
                    list.descriptor.fullKey(),
                    field.descriptor.fullKey(),
                    expand));
        }
        if (!adventureEntriesVisible(anyBlock)) return;
        for (int index = 0; index < entries.size(); index++) {
            String scope = adventureEntryScope(list, index);
            String entry = entries.get(index);
            String entryLabel = (index + 1) + ": " + entry;
            targets.add(DocumentPanelSearch.target(
                    screen, category, parents, entryLabel, aliases + " " + entry, scope, scope, expandEntries));
            var entryParents = new ArrayList<>(parents);
            entryParents.add(entryLabel);
            for (AdventureField action : List.of(AdventureField.UP, AdventureField.DOWN, AdventureField.REMOVE)) {
                if (action == AdventureField.UP && !hasPrevious(index)) continue;
                if (action == AdventureField.DOWN && !hasNext(index, entries.size())) continue;
                targets.add(DocumentPanelSearch.target(
                        screen,
                        category,
                        entryParents,
                        action.label().getString(),
                        action.descriptor.terms() + " " + aliases + " " + entry,
                        scope,
                        action.descriptor.fullKey() + ".action",
                        expandEntries));
            }
        }
    }

    private final ItemEditorScreen screen;

    public GeneralEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        Capabilities capabilities = Capabilities.from(this.screen);
        boolean compactLayout =
                LayoutModeUtil.isCompactWidth(this.availableContentWidth(), COMPACT_LAYOUT_CONTENT_WIDTH_THRESHOLD);
        boolean supportsDurability = capabilities.durability();
        boolean supportsRepairCost = capabilities.repairCost();
        boolean supportsCanBreak = capabilities.canBreak();
        boolean supportsCanPlaceOn = capabilities.canPlaceOn();

        FlowLayout root = UiFactory.column();
        UiFactory.appendFillChild(root, this.buildIdentitySection(state, compactLayout));

        if (supportsDurability || supportsRepairCost) {
            UiFactory.appendFillChild(
                    root, this.buildDurabilitySection(state, supportsDurability, supportsRepairCost, compactLayout));
        }

        UiFactory.appendFillChild(root, this.buildVisualOverridesSection(state));
        UiFactory.appendFillChild(root, this.buildItemModelSection(state, compactLayout));
        if (supportsCanBreak || supportsCanPlaceOn) {
            UiFactory.appendFillChild(
                    root, this.buildAdventureSection(state, supportsCanBreak, supportsCanPlaceOn, compactLayout));
        }
        return root;
    }

    private FlowLayout buildIdentitySection(ItemEditorState state, boolean compactLayout) {
        FlowLayout identity = this.collapsibleSection(
                Group.IDENTITY.label(),
                state.uiGeneralIdentityCollapsed,
                value -> state.uiGeneralIdentityCollapsed = value);
        if (state.uiGeneralIdentityCollapsed) {
            return identity;
        }

        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                this.screen,
                RichTextDocument.fromMarkup(state.customName),
                Sizing.fill(100),
                UiFactory.fixed(CUSTOM_NAME_EDITOR_HEIGHT),
                Field.NAME.label().getString(),
                StyledTextFieldSection.StylePreset.name(),
                Field.NAME_COLOR.label().getString(),
                Field.NAME_GRADIENT.label().getString(),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("general.identity.custom_name.single_line")
                        : null,
                document -> PanelBindings.text(this.screen, value -> state.customName = value)
                        .accept(TextComponentUtil.serializeEditorDocument(document)),
                compactLayout);
        this.applyRichEditorRenderMode(nameSection.editor(), state.uiRenderObjectsInCustomName);

        FlowLayout editorFrame = UiFactory.framedEditorCard();
        editorFrame.id(Field.NAME.descriptor.fullKey());
        editorFrame.child(nameSection.toolbar());
        editorFrame.child(UiFactory.checkbox(Field.RENDER.label(), state.uiRenderObjectsInCustomName, value -> {
            state.uiRenderObjectsInCustomName = value;
            this.applyRichEditorRenderMode(nameSection.editor(), value);
        }));
        editorFrame.child(nameSection.editor());
        editorFrame.child(nameSection.validation());
        identity.child(editorFrame);

        int defaultMaxStackSize = this.defaultMaxStackSize();
        String displayedMaxStackSize = state.special.maxStackSize.isBlank()
                ? Integer.toString(defaultMaxStackSize)
                : state.special.maxStackSize;

        boolean stackAsColumns = compactLayout && this.availableContentWidth() < STACK_COLUMNS_WIDTH_THRESHOLD;
        FlowLayout stackRow = stackAsColumns ? UiFactory.column() : UiFactory.row();
        stackRow.gap(Math.max(2, UiFactory.scaleProfile().tightSpacing()));
        stackRow.margins(Insets.top(STACK_ROW_MARGIN_TOP));

        FlowLayout stackCountField = this.compactField(
                Field.COUNT.label(),
                UiFactory.textBox(state.count, PanelBindings.text(this.screen, value -> state.count = value))
                        .horizontalSizing(Sizing.fill(100)),
                140,
                true);
        FlowLayout maxStackField = this.compactField(
                Field.MAX_STACK.label(),
                UiFactory.textBox(displayedMaxStackSize, PanelBindings.text(this.screen, value -> {
                            String trimmed = value == null ? "" : value.trim();
                            state.special.maxStackSize = trimmed.isBlank()
                                            || trimmed.equals(Integer.toString(defaultMaxStackSize))
                                    ? ""
                                    : value;
                        }))
                        .horizontalSizing(Sizing.fill(100)),
                180,
                true);

        stackRow.child(stackCountField);
        stackRow.child(maxStackField);
        if (!stackAsColumns) {
            this.distributeRowChildren(stackRow);
        }
        identity.child(stackRow);

        var rarityButton = UiFactory.button(
                state.rarity,
                UiFactory.ButtonTextPreset.STANDARD,
                button -> this.screen.openDropdown(
                        button,
                        Arrays.asList(Rarity.values()),
                        Rarity::name,
                        PanelBindings.value(this.screen, rarity -> state.rarity = rarity.name())));
        FlowLayout rarityRow = compactLayout ? UiFactory.column() : UiFactory.row();
        rarityRow.child(this.compactField(
                Field.RARITY.label(),
                rarityButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(RARITY_BUTTON_WIDTH)),
                150,
                compactLayout));
        identity.child(rarityRow);
        return identity;
    }

    private FlowLayout compactField(Component label, UIComponent input, int labelWidth, boolean compactLayout) {
        FlowLayout field = UiFactory.column();
        field.gap(2);
        int contentWidth = this.availableContentWidth();
        int dynamicCap = compactLayout
                ? Math.max(1, contentWidth - UiFactory.scaledPixels(FIELD_LABEL_COMPACT_RESERVE))
                : Math.max(1, contentWidth - UiFactory.scaledPixels(FIELD_LABEL_REGULAR_RESERVE));
        int preferredLabelWidth = Math.clamp(
                Math.max(FIELD_LABEL_DYNAMIC_MIN, dynamicCap),
                FIELD_LABEL_WIDTH_MIN,
                Math.max(FIELD_LABEL_WIDTH_MIN, labelWidth));
        int effectiveLabelWidth = Math.clamp(preferredLabelWidth, 1, Math.max(1, contentWidth));
        Component fittedLabel = UiFactory.fitToWidth(label, effectiveLabelWidth);
        LabelComponent labelComponent = UiFactory.muted(fittedLabel, effectiveLabelWidth);
        labelComponent.horizontalSizing(Sizing.fill(100));
        if (!fittedLabel.getString().equals(label.getString())) {
            labelComponent.tooltip(List.of(label));
        }
        labelComponent.margins(Insets.top(1));
        field.child(labelComponent);
        field.child(input);
        return field;
    }

    private FlowLayout collapsibleSection(Component title, boolean collapsed, Consumer<Boolean> collapsedSetter) {
        FlowLayout section = UiFactory.card();
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(title).horizontalSizing(Sizing.expand(100)));
        ButtonComponent toggle = UiFactory.button(
                LayoutModeUtil.sectionToggleText(collapsed),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> PanelBindings.mutateRefresh(this.screen, () -> collapsedSetter.accept(!collapsed)));
        toggle.horizontalSizing(Sizing.fixed(Math.max(
                ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH, UiFactory.scaledPixels(ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH))));
        header.child(toggle);
        section.child(header);
        return section;
    }

    private int defaultMaxStackSize() {
        ItemStack original = this.screen.session().originalStack();
        ItemStack base = new ItemStack(original.getItem());
        return Math.max(1, base.getMaxStackSize());
    }

    private FlowLayout buildDurabilitySection(
            ItemEditorState state, boolean supportsDurability, boolean supportsRepairCost, boolean compactLayout) {
        FlowLayout durability = this.collapsibleSection(
                Group.DURABILITY.label(),
                state.uiGeneralDurabilityCollapsed,
                value -> state.uiGeneralDurabilityCollapsed = value);
        if (state.uiGeneralDurabilityCollapsed) {
            return durability;
        }
        int numericWidth = DURABILITY_NUMERIC_FIELD_WIDTH;

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        if (supportsDurability) {
            row.child(this.compactField(
                    Field.DAMAGE.label(),
                    UiFactory.textBox(
                                    state.currentDamage,
                                    PanelBindings.text(this.screen, value -> state.currentDamage = value))
                            .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)),
                    DURABILITY_LABEL_WIDTH_CURRENT,
                    compactLayout));
            row.child(this.compactField(
                    Field.MAX_DAMAGE.label(),
                    UiFactory.textBox(
                                    state.maxDamage, PanelBindings.text(this.screen, value -> state.maxDamage = value))
                            .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)),
                    DURABILITY_LABEL_WIDTH_MAX,
                    compactLayout));
        }
        if (supportsRepairCost) {
            row.child(this.compactField(
                    Field.REPAIR.label(),
                    UiFactory.textBox(
                                    state.repairCost,
                                    PanelBindings.text(this.screen, value -> state.repairCost = value))
                            .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(numericWidth)),
                    DURABILITY_LABEL_WIDTH_REPAIR,
                    compactLayout));
        }
        if (!compactLayout) {
            this.distributeRowChildren(row);
        }
        durability.child(row);

        if (supportsDurability) {
            durability.child(UiFactory.checkbox(
                    Field.UNBREAKABLE.label(),
                    state.unbreakable,
                    PanelBindings.toggle(this.screen, value -> state.unbreakable = value)));
        }
        return durability;
    }

    private FlowLayout buildAdventureSection(
            ItemEditorState state, boolean supportsCanBreak, boolean supportsCanPlaceOn, boolean compactLayout) {
        FlowLayout adventure = this.collapsibleSection(
                Group.ADVENTURE.label(),
                state.uiGeneralAdventureCollapsed,
                value -> state.uiGeneralAdventureCollapsed = value);
        if (state.uiGeneralAdventureCollapsed) {
            return adventure;
        }
        List<String> blockIds = BuiltInRegistries.BLOCK.keySet().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();

        if (supportsCanBreak) {
            adventure.child(this.buildAdventureListCard(
                    AdventureField.BREAK,
                    state.canBreakBlockIds,
                    state.canBreakAnyBlock,
                    value -> state.canBreakAnyBlock = value,
                    state.canBreakShowInTooltip,
                    value -> {
                        state.canBreakShowInTooltip = value;
                        if (value) {
                            state.hiddenTooltipComponents.remove("minecraft:can_break");
                        } else {
                            state.hiddenTooltipComponents.add("minecraft:can_break");
                        }
                    },
                    blockIds,
                    state.canBreakCollapsed,
                    value -> state.canBreakCollapsed = value,
                    compactLayout));
        }
        if (supportsCanPlaceOn) {
            adventure.child(this.buildAdventureListCard(
                    AdventureField.PLACE,
                    state.canPlaceOnBlockIds,
                    state.canPlaceOnAnyBlock,
                    value -> state.canPlaceOnAnyBlock = value,
                    state.canPlaceOnShowInTooltip,
                    value -> {
                        state.canPlaceOnShowInTooltip = value;
                        if (value) {
                            state.hiddenTooltipComponents.remove("minecraft:can_place_on");
                        } else {
                            state.hiddenTooltipComponents.add("minecraft:can_place_on");
                        }
                    },
                    blockIds,
                    state.canPlaceOnCollapsed,
                    value -> state.canPlaceOnCollapsed = value,
                    compactLayout));
        }
        return adventure;
    }

    private FlowLayout buildAdventureListCard(
            AdventureField list,
            List<String> blockIds,
            boolean anyBlock,
            Consumer<Boolean> anyBlockUpdater,
            boolean showInTooltip,
            Consumer<Boolean> tooltipUpdater,
            List<String> allBlocks,
            boolean collapsed,
            Consumer<Boolean> collapsedSetter,
            boolean compactLayout) {
        FlowLayout card = UiFactory.subCard();
        card.id(list.descriptor.fullKey());
        Component title = list.label();
        int contentWidth = this.availableContentWidth();
        Runnable openAddBlockPicker = () -> this.screen.openSearchablePickerDialog(
                ItemEditorText.str("general.adventure.picker_title"),
                allBlocks,
                id -> id,
                selected -> PanelBindings.mutateRefresh(this.screen, () -> this.addUnique(blockIds, selected)));
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(title).shadow(false).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.muted(Component.literal("(" + blockIds.size() + ")"), ADVENTURE_COUNT_HINT_WIDTH));
        ButtonComponent headerAddButton = UiFactory.button(
                Component.literal(SYMBOL_ADD), UiFactory.ButtonTextPreset.STANDARD, button -> openAddBlockPicker.run());
        headerAddButton.horizontalSizing(Sizing.fixed(Math.max(
                ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH, UiFactory.scaledPixels(ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH))));
        headerAddButton.tooltip(List.of(AdventureField.ADD.label()));
        headerAddButton.active(!anyBlock);
        header.child(headerAddButton);
        ButtonComponent collapseToggle = UiFactory.button(
                LayoutModeUtil.sectionToggleText(collapsed),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> PanelBindings.mutateRefresh(this.screen, () -> collapsedSetter.accept(!collapsed)));
        collapseToggle.horizontalSizing(Sizing.fixed(Math.max(
                ADVENTURE_COLLAPSE_TOGGLE_MIN_WIDTH, UiFactory.scaledPixels(ADVENTURE_COLLAPSE_TOGGLE_BASE_WIDTH))));
        header.child(collapseToggle);
        card.child(header);
        card.child(UiFactory.checkbox(
                AdventureField.ANY.label(),
                anyBlock,
                value -> PanelBindings.mutateRefresh(this.screen, () -> anyBlockUpdater.accept(value))));

        boolean stackControls =
                compactLayout || contentWidth < UiFactory.scaledPixels(ADVENTURE_STACK_CONTROLS_WIDTH_THRESHOLD);
        FlowLayout controls = stackControls ? UiFactory.column() : UiFactory.row();
        controls.child(UiFactory.checkbox(
                AdventureField.TOOLTIP.label(), showInTooltip, PanelBindings.toggle(this.screen, tooltipUpdater)));

        ButtonComponent addButton = UiFactory.button(
                AdventureField.ADD.label(), UiFactory.ButtonTextPreset.STANDARD, button -> openAddBlockPicker.run());
        addButton.horizontalSizing(stackControls ? Sizing.fill(100) : Sizing.expand(100));
        addButton.active(!anyBlock);
        controls.child(addButton);

        if (!blockIds.isEmpty()) {
            ButtonComponent clearButton = UiFactory.button(
                    AdventureField.CLEAR.label(),
                    UiFactory.ButtonTextPreset.STANDARD,
                    button -> PanelBindings.mutateRefresh(this.screen, blockIds::clear));
            clearButton.horizontalSizing(stackControls ? Sizing.fill(100) : Sizing.expand(100));
            clearButton.active(!anyBlock);
            controls.child(clearButton);
        }

        if (!stackControls) {
            this.distributeRowChildren(controls);
        }
        card.child(controls);

        if (collapsed || !adventureEntriesVisible(anyBlock)) {
            return card;
        }

        if (blockIds.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("general.adventure.empty"), ADVENTURE_EMPTY_HINT_WIDTH));
            return card;
        }

        int effectiveContentWidth = Math.max(
                contentWidth,
                UiFactory.scaledPixels(ADVENTURE_ENTRY_LABEL_INLINE_RESERVE + ADVENTURE_ENTRY_LABEL_MIN_WIDTH));
        for (int index = 0; index < blockIds.size(); index++) {
            int currentIndex = index;
            String blockId = blockIds.get(index);
            FlowLayout row = UiFactory.row();
            row.id(adventureEntryScope(list, index));
            int labelWidth = Math.max(
                    ADVENTURE_ENTRY_LABEL_MIN_WIDTH,
                    effectiveContentWidth - UiFactory.scaledPixels(ADVENTURE_ENTRY_LABEL_INLINE_RESERVE));
            LabelComponent label =
                    UiFactory.muted(UiFactory.fitToWidth(Component.literal(blockId), labelWidth), labelWidth);
            label.tooltip(List.of(Component.literal(blockId)));
            label.horizontalSizing(Sizing.content());
            row.child(label);

            FlowLayout actionRow = UiFactory.row();
            if (hasPrevious(currentIndex)) {
                actionRow.child(this.actionButton(
                        Component.literal(SYMBOL_MOVE_UP),
                        AdventureField.UP.label(),
                        () -> PanelBindings.mutateRefresh(
                                this.screen, () -> this.swap(blockIds, currentIndex, currentIndex - 1))));
            }
            if (hasNext(currentIndex, blockIds.size())) {
                actionRow.child(this.actionButton(
                        Component.literal(SYMBOL_MOVE_DOWN),
                        AdventureField.DOWN.label(),
                        () -> PanelBindings.mutateRefresh(
                                this.screen, () -> this.swap(blockIds, currentIndex, currentIndex + 1))));
            }
            actionRow.child(this.actionButton(
                    Component.literal(SYMBOL_REMOVE),
                    AdventureField.REMOVE.label(),
                    () -> PanelBindings.mutateRefresh(this.screen, () -> blockIds.remove(currentIndex))));
            row.child(actionRow);
            card.child(row);
        }

        return card;
    }

    private ButtonComponent actionButton(Component label, Component tooltip, Runnable action) {
        Component message = tooltip.getContents() instanceof TranslatableContents text
                ? Component.translatableWithFallback(text.getKey() + ".action", label.getString())
                : label;
        ButtonComponent button =
                UiFactory.button(message, UiFactory.ButtonTextPreset.COMPACT, component -> action.run());
        int size = Math.max(ENTRY_ACTION_BUTTON_SIZE_MIN, UiFactory.scaledPixels(ENTRY_ACTION_BUTTON_SIZE_BASE));
        button.horizontalSizing(Sizing.fixed(size));
        button.tooltip(List.of(tooltip));
        return button;
    }

    private void addUnique(List<String> values, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!values.contains(value)) {
            values.add(value);
        }
    }

    private void swap(List<String> values, int left, int right) {
        if (left < 0 || right < 0 || left >= values.size() || right >= values.size()) {
            return;
        }
        String temp = values.get(left);
        values.set(left, values.get(right));
        values.set(right, temp);
    }

    private FlowLayout buildVisualOverridesSection(ItemEditorState state) {
        FlowLayout visual = this.collapsibleSection(
                Group.VISUAL.label(),
                state.uiGeneralVisualOverridesCollapsed,
                value -> state.uiGeneralVisualOverridesCollapsed = value);
        if (state.uiGeneralVisualOverridesCollapsed) {
            return visual;
        }
        ButtonComponent glintButton = UiFactory.actionToneButton(
                TriStateBooleanUi.label(state.glintOverride),
                UiFactory.ButtonTextPreset.STANDARD,
                TriStateBooleanUi.tone(state.glintOverride),
                button -> PanelBindings.mutateRefresh(
                        this.screen, () -> state.glintOverride = TriStateBooleanUi.next(state.glintOverride)));
        glintButton.horizontalSizing(Sizing.fill(100));
        visual.child(this.compactField(Field.GLINT.label(), glintButton, ITEM_MODEL_VALUE_LABEL_WIDTH, true));
        return visual;
    }

    private FlowLayout buildItemModelSection(ItemEditorState state, boolean compactLayout) {
        FlowLayout itemModel = this.collapsibleSection(
                Group.MODEL.label(),
                state.uiGeneralItemModelCollapsed,
                value -> state.uiGeneralItemModelCollapsed = value);
        if (state.uiGeneralItemModelCollapsed) {
            return itemModel;
        }
        itemModel.gap(Math.max(1, UiFactory.scaleProfile().tightSpacing()));
        int contentWidth = this.availableContentWidth();
        int pickItemModelWidth = Math.clamp(
                contentWidth / ITEM_MODEL_PICK_BUTTON_WIDTH_DIVISOR,
                ITEM_MODEL_PICK_BUTTON_WIDTH_MIN,
                ITEM_MODEL_PICK_BUTTON_WIDTH_MAX);
        int idRowGap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        int minIdInputWidth = UiFactory.scaledPixels(ITEM_MODEL_ID_INPUT_MIN_WIDTH);
        boolean stackPickButton = compactLayout
                || contentWidth < UiFactory.scaledPixels(ITEM_MODEL_STACK_PICK_THRESHOLD)
                || contentWidth < (pickItemModelWidth + minIdInputWidth + idRowGap);
        FlowLayout itemModelIdInputRow =
                stackPickButton ? UiFactory.column() : UiFactory.row().gap(idRowGap);
        TextBoxComponent itemModelIdInput = this.itemModelTextBox(
                state.itemModelId, PanelBindings.text(this.screen, value -> state.itemModelId = value));
        itemModelIdInput.setMaxLength(UNBOUNDED_TEXT_LIMIT);
        itemModelIdInput.horizontalSizing(stackPickButton ? Sizing.fill(100) : Sizing.expand(100));
        itemModelIdInputRow.child(itemModelIdInput);
        ButtonComponent pickItemModelButton = UiFactory.button(
                Field.MODEL_PICK.label(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> this.screen.openSearchablePickerDialog(
                        Field.MODEL_ID.label().getString(),
                        "",
                        BuiltInRegistries.ITEM.keySet().stream()
                                .map(Identifier::toString)
                                .sorted()
                                .toList(),
                        id -> id,
                        selected -> PanelBindings.mutateRefresh(this.screen, () -> state.itemModelId = selected)));
        pickItemModelButton.horizontalSizing(stackPickButton ? Sizing.fill(100) : UiFactory.fixed(pickItemModelWidth));
        pickItemModelButton.verticalSizing(UiFactory.fixed(this.itemModelControlHeight()));
        itemModelIdInputRow.child(pickItemModelButton);
        itemModel.child(this.compactField(
                Field.MODEL_ID.label(), itemModelIdInputRow, ITEM_MODEL_ID_LABEL_WIDTH, compactLayout));

        boolean stackValueFields =
                compactLayout || contentWidth < UiFactory.scaledPixels(ITEM_MODEL_VALUE_ROW_STACK_THRESHOLD);
        FlowLayout customModelValues = stackValueFields ? UiFactory.column() : UiFactory.row();
        customModelValues.child(this.compactField(
                Field.MODEL_FLOAT.label(),
                this.itemModelTextBox(
                                state.customModelFloat,
                                PanelBindings.text(this.screen, value -> state.customModelFloat = value))
                        .horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true));
        customModelValues.child(this.compactField(
                Field.MODEL_STRING.label(),
                this.itemModelTextBox(
                                state.customModelString,
                                PanelBindings.text(this.screen, value -> state.customModelString = value))
                        .horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true));
        if (!stackValueFields) {
            this.distributeRowChildren(customModelValues);
        }
        itemModel.child(customModelValues);
        itemModel.child(this.compactField(
                Field.MODEL_COLOR.label(),
                this.itemModelTextBox(
                                state.customModelColor,
                                PanelBindings.text(this.screen, value -> state.customModelColor = value))
                        .horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true));
        itemModel.child(this.compactField(
                Field.MODEL_FLAGS.label(),
                this.itemModelTextBox(
                                state.customModelFlags,
                                PanelBindings.text(this.screen, value -> state.customModelFlags = value))
                        .horizontalSizing(Sizing.fill(100)),
                ITEM_MODEL_VALUE_LABEL_WIDTH,
                true));
        return itemModel;
    }

    private TextBoxComponent itemModelTextBox(String value, Consumer<String> onChanged) {
        TextBoxComponent input = UiFactory.textBox(value, onChanged);
        input.verticalSizing(Sizing.fixed(this.itemModelControlHeight()));
        return input;
    }

    private int itemModelControlHeight() {
        return Math.max(
                ITEM_MODEL_TEXTBOX_MIN_HEIGHT,
                UiFactory.scaleProfile().controlHeight() - UiFactory.scaledPixels(ITEM_MODEL_TEXTBOX_HEIGHT_REDUCTION));
    }

    private void distributeRowChildren(FlowLayout row) {
        int childCount = row.children().size();
        if (childCount <= 1) {
            return;
        }
        int childWidth = Math.max(1, (100 - childCount) / childCount);
        for (UIComponent child : row.children()) {
            child.horizontalSizing(Sizing.fill(childWidth));
        }
    }

    private int availableContentWidth() {
        return Math.max(1, this.screen.editorContentWidthHint());
    }

    private void applyRichEditorRenderMode(RichTextAreaComponent editor, boolean renderStructured) {
        editor.structuredRenderMode(renderStructured);
    }
}
