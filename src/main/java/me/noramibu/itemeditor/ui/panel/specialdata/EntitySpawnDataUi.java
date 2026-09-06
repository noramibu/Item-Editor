package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.editor.text.RichTextStyle;
import me.noramibu.itemeditor.service.EntitySpawnDataUtil;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.PickerFieldFactory;
import me.noramibu.itemeditor.ui.component.RichTextHorizontalScrollbarComponent;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.component.UnifiedColorPickerDialog;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import me.noramibu.itemeditor.util.ValidationUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

final class EntitySpawnDataUi {
    static List<EditorSearchDialog.Target> searchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            List<String> path,
            String scope,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        boolean living = EntitySpawnDataUtil.supportsStatusEffects(draft.entityId);
        if (EntitySpawnDataUtil.isItemEntity(draft.entityId)) {
            result.addAll(SpecialDataSearch.targets(
                    context, EditorCategory.SPECIAL_DATA, path, scope, expand, ItemField.values()));
            result.addAll(SpecialDataSearch.targets(
                    context, EditorCategory.SPECIAL_DATA, path, scope, expand, HealthField.HEALTH));
            result.addAll(context.itemActionSearchTargets(
                    EditorCategory.SPECIAL_DATA, path, scope, expand, draft.itemEntityStack, true));
        } else if (EntitySpawnDataUtil.isDisplayEntity(draft.entityId)) {
            result.addAll(displayAxisTargets(context, draft, path, scope, expand));
            result.addAll(SpecialDataSearch.targets(
                    context,
                    EditorCategory.SPECIAL_DATA,
                    path,
                    scope,
                    () -> {
                        expand.run();
                        draft.uiDisplayTransformCollapsed = false;
                    },
                    TransformField.values()));
            result.addAll(SpecialDataSearch.targets(
                    context,
                    EditorCategory.SPECIAL_DATA,
                    path,
                    scope,
                    () -> {
                        expand.run();
                        draft.uiDisplayRenderingCollapsed = false;
                    },
                    RenderingField.values()));
            switch (EntitySpawnDataUtil.displayType(draft.entityId)) {
                case "text" -> {
                    result.addAll(SpecialDataSearch.targets(
                            context, EditorCategory.SPECIAL_DATA, path, scope, expand, TextField.values()));
                }
                case "item" -> {
                    result.addAll(SpecialDataSearch.targets(
                            context, EditorCategory.SPECIAL_DATA, path, scope, expand, DisplayItemField.values()));
                    result.addAll(context.itemActionSearchTargets(
                            EditorCategory.SPECIAL_DATA, path, scope, expand, draft.displayItemStack, false));
                }
                case "block" -> {
                    result.addAll(SpecialDataSearch.targets(
                            context, EditorCategory.SPECIAL_DATA, path, scope, expand, BlockField.values()));
                    String blockId = EntitySpawnDataUtil.displayValue(draft, "block", "minecraft:air");
                    Identifier id = IdFieldNormalizer.parse(blockId);
                    Block block = id == null
                            ? null
                            : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
                    if (block != null) {
                        for (Property<?> property : block.getStateDefinition().getProperties()) {
                            result.add(searchTarget(
                                    context,
                                    path,
                                    property.getName(),
                                    property.getName(),
                                    scope,
                                    "display-property:" + property.getName(),
                                    expand));
                        }
                    }
                }
                default -> {}
            }
        } else if (living) {
            result.addAll(healthSearchTargets(context, draft.entityId, draft.attributes, path, scope, expand));
            result.addAll(attributeSearchTargets(
                    context,
                    draft.entityId,
                    draft.attributes,
                    path,
                    scope,
                    () -> {
                        expand.run();
                        draft.uiAttributesCollapsed = false;
                    },
                    Set.of()));
            result.addAll(equipmentSearchTargets(context, draft.equipment, EquipmentSlot.VALUES, true, path, expand));
        }
        if (living || !draft.effects.isEmpty()) {
            Runnable showEffects = () -> {
                expand.run();
                draft.uiEffectsCollapsed = false;
            };
            var effectPath = new ArrayList<>(path);
            effectPath.add(ItemEditorText.str("special.entity.effects"));
            result.addAll(SpecialDataSearch.targets(
                    context, EditorCategory.SPECIAL_DATA, effectPath, scope, showEffects, EffectField.values()));
            result.addAll(PotionSpecialDataSection.effectSearchTargets(
                    context, draft.effects, EditorCategory.SPECIAL_DATA, effectPath, showEffects));
        }
        return result;
    }

    static List<EditorSearchDialog.Target> healthSearchTargets(
            SpecialDataPanelContext context,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            List<String> path,
            String scope,
            Runnable expand) {
        var fields = new ArrayList<SpecialDataSearch.Field>();
        fields.add(HealthField.HEALTH);
        String maxHealthId = EntitySpawnDataUtil.maxHealthAttributeId();
        if (EntitySpawnDataUtil.defaultAttributeValues(
                                context.screen().session().registryAccess(), entityId)
                        .containsKey(maxHealthId)
                || !attributeBase(attributes, maxHealthId).isBlank()) {
            fields.add(HealthField.MAX_HEALTH);
        }
        return SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                path,
                scope,
                expand,
                fields.toArray(SpecialDataSearch.Field[]::new));
    }

    static List<EditorSearchDialog.Target> attributeSearchTargets(
            SpecialDataPanelContext context,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            List<String> path,
            String scope,
            Runnable expand,
            Set<String> excludedIds) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        for (String id : attributeIds(context, entityId, attributes, excludedIds)) {
            var attributePath = new ArrayList<>(path);
            attributePath.add(ItemEditorText.str("category.attributes.title"));
            result.add(searchTarget(
                    context,
                    attributePath,
                    attributeLabel(context, id).getString(),
                    id,
                    scope,
                    "entity-attribute:" + id,
                    expand));
        }
        return result;
    }

    private static Set<String> attributeIds(
            SpecialDataPanelContext context,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            Set<String> excludedIds) {
        var ids = new LinkedHashSet<>(EntitySpawnDataUtil.defaultAttributeValues(
                        context.screen().session().registryAccess(), entityId)
                .keySet());
        attributes.stream().map(attribute -> attribute.attributeId).forEach(ids::add);
        ids.remove(EntitySpawnDataUtil.maxHealthAttributeId());
        ids.removeAll(excludedIds);
        return ids;
    }

    static List<EditorSearchDialog.Target> equipmentSearchTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntityEquipmentDraft draft,
            List<EquipmentSlot> slots,
            boolean editDropChances,
            List<String> path,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        for (EquipmentSlot slot : slots) {
            String scope = SpecialDataSearch.scope("equipment", draft) + "-" + slot.getSerializedName();
            Runnable show = () -> {
                expand.run();
                draft.uiCollapsed = false;
            };
            var slotPath = new ArrayList<>(path);
            slotPath.add(ItemEditorText.str("common.equipment"));
            slotPath.add(ItemEditorText.str(equipmentSlotKey(slot)));
            result.addAll(context.itemActionSearchTargets(
                    EditorCategory.SPECIAL_DATA, slotPath, scope, show, draft.stack(slot), false));
            if (editDropChances) {
                result.addAll(SpecialDataSearch.targets(
                        context, EditorCategory.SPECIAL_DATA, slotPath, scope, show, EquipmentField.values()));
            }
        }
        return result;
    }

    private static EditorSearchDialog.Target searchTarget(
            SpecialDataPanelContext context,
            List<String> path,
            String label,
            String terms,
            String scope,
            String field,
            Runnable expand) {
        var fullPath = new ArrayList<String>();
        fullPath.add(SpecialDataSearch.categoryTitle(context, EditorCategory.SPECIAL_DATA));
        fullPath.addAll(path);
        fullPath.add(label);
        return new EditorSearchDialog.Target(fullPath, terms, () -> {
            expand.run();
            context.screen()
                    .revealSearchTarget(EditorCategory.SPECIAL_DATA, new EditorSearchDialog.Location(scope, field));
        });
    }

    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int DISPLAY_PICK_BUTTON_WIDTH = 96;
    private static final int EQUIPMENT_SLOT_LABEL_WIDTH = 72;
    private static final int EQUIPMENT_SUMMARY_RESERVE = 112;

    private EntitySpawnDataUi() {}

    static UIComponent nameEditor(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            Component label,
            String placeholderKey,
            String colorTitleKey,
            String gradientTitleKey) {
        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(draft.customName),
                Sizing.fill(100),
                UiFactory.fixed(NAME_EDITOR_HEIGHT),
                ItemEditorText.str(placeholderKey),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str(colorTitleKey),
                ItemEditorText.str(gradientTitleKey),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.spawn_egg.name.single_line")
                        : null,
                document ->
                        context.mutate(() -> draft.customName = TextComponentUtil.serializeEditorDocument(document)));

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        return UiFactory.field(label, Component.empty(), frame);
    }

    static FlowLayout flags(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, EntityTagFieldsUi tags) {
        FlowLayout group = UiFactory.column().gap(2);
        boolean expanded = draft.uiExpandedTagGroups.contains("flags");
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("special.entity.flags")).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.collapseToggleButton(
                !expanded,
                () -> context.mutateRefresh(() -> {
                    if (expanded) draft.uiExpandedTagGroups.remove("flags");
                    else draft.uiExpandedTagGroups.add("flags");
                })));
        group.child(header);
        if (!expanded) return group;

        addPackedRows(
                group,
                Math.max(1, context.panelWidthHint() - UiFactory.scaleProfile().padding() * 4),
                EntityTagFieldsUi.FLAGS.stream()
                        .map(option -> flag(context, option.key(), option.read().test(draft), value -> option.write()
                                .accept(draft, value)))
                        .toArray(UIComponent[]::new));
        group.child(tags.fields("flags"));
        return group;
    }

    static FlowLayout effects(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        Registry<MobEffect> registry =
                context.screen().session().registryAccess().lookupOrThrow(Registries.MOB_EFFECT);
        FlowLayout group = UiFactory.column().gap(2);
        FlowLayout header = UiFactory.row();
        LabelComponent title =
                UiFactory.title(ItemEditorText.tr("special.entity.effects")).shadow(false);
        title.tooltip(List.of(ItemEditorText.tr("special.entity.effects.tooltip")));
        header.child(title.horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.muted(ItemEditorText.tr("special.entity.effects.summary", draft.effects.size())));
        header.child(UiFactory.collapseToggleButton(
                draft.uiEffectsCollapsed,
                () -> context.mutateRefresh(() -> draft.uiEffectsCollapsed = !draft.uiEffectsCollapsed)));
        group.child(header);
        if (draft.uiEffectsCollapsed) {
            return group;
        }
        if (draft.effects.isEmpty()) {
            group.child(UiFactory.muted(ItemEditorText.tr("special.entity.effects.empty")));
        }
        group.child(PotionSpecialDataSection.buildEffectsEditor(
                context, draft.effects, RegistryUtil.ids(registry), EffectField.ADD.text()));
        return group;
    }

    static FlowLayout details(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, EntityTagFieldsUi tags) {
        FlowLayout result = UiFactory.column();
        boolean living = EntitySpawnDataUtil.supportsStatusEffects(draft.entityId);
        if (EntitySpawnDataUtil.isItemEntity(draft.entityId)) result.child(itemEntity(context, draft));
        else if (EntitySpawnDataUtil.isDisplayEntity(draft.entityId)) result.child(displayEntity(context, draft));
        else if (living) {
            result.child(UiFactory.title(ItemEditorText.tr("special.entity.values")));
            result.child(health(
                    context,
                    draft.health,
                    value -> draft.health = value,
                    draft.entityId,
                    draft.attributes,
                    context.isCompactPanel(620)));
            result.child(tags.fields("values"));
            result.child(attributes(context, draft));
            FlowLayout equipment = equipment(context, draft.equipment, EquipmentSlot.VALUES, true);
            if (!draft.equipment.uiCollapsed) equipment.child(tags.fields("equipment"));
            result.child(equipment);
        }
        if (living || !draft.effects.isEmpty()) result.child(effects(context, draft));
        return result;
    }

    static UIComponent health(
            SpecialDataPanelContext context,
            String health,
            Consumer<String> healthSetter,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            boolean compactLayout) {
        int columns = compactLayout ? 1 : 2;
        int fieldWidth = Math.max(1, context.panelWidthHint() / columns);
        int fieldFill = compactLayout ? 100 : 49;
        Component healthLabel = HealthField.HEALTH.text();
        Component maxHealthLabel = HealthField.MAX_HEALTH.text();
        int labelWidth = Math.max(textWidth(healthLabel), textWidth(maxHealthLabel));
        FlowLayout fields = compactLayout ? UiFactory.column() : UiFactory.row();
        FlowLayout healthField = compactValueField(
                healthLabel, UiFactory.textBox(health, context.bindText(healthSetter)), fieldWidth, labelWidth);
        healthField.horizontalSizing(Sizing.fill(fieldFill));
        fields.child(healthField);

        String maxHealthId = EntitySpawnDataUtil.maxHealthAttributeId();
        Map<String, Double> defaults = EntitySpawnDataUtil.defaultAttributeValues(
                context.screen().session().registryAccess(), entityId);
        String maxHealth = attributeBase(attributes, maxHealthId);
        if (defaults.containsKey(maxHealthId) || !maxHealth.isBlank()) {
            FlowLayout maxHealthField = compactValueField(
                    maxHealthLabel,
                    attributeInput(context, attributes, maxHealthId, defaults.get(maxHealthId)),
                    fieldWidth,
                    labelWidth);
            maxHealthField.horizontalSizing(Sizing.fill(fieldFill));
            fields.child(maxHealthField);
        }
        return fields;
    }

    static FlowLayout itemEntity(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.title(ItemField.TITLE.text()).shadow(false));

        group.child(context.itemSummary(
                draft.itemEntityStack, itemEntitySummary(draft.itemEntityStack, draft.itemEntityCount), true));
        group.child(context.itemActions(
                draft.itemEntityStack,
                stack -> setItemEntityStack(draft, stack),
                () -> openItemEntityEditor(context, draft)));

        boolean compact = context.isCompactPanel(620);
        int fieldWidth = Math.max(1, context.panelWidthHint() / (compact ? 1 : 4));
        int fieldFill = compact ? 100 : 24;
        Component countLabel = ItemField.COUNT.text();
        Component healthLabel = HealthField.HEALTH.text();
        Component ageLabel = ItemField.AGE.text();
        Component pickupDelayLabel = ItemField.PICKUP_DELAY.text();
        int labelWidth = Math.max(
                textWidth(countLabel),
                Math.max(textWidth(healthLabel), Math.max(textWidth(ageLabel), textWidth(pickupDelayLabel))));
        FlowLayout values = compact ? UiFactory.column() : UiFactory.row();
        values.child(compactValueField(
                        countLabel,
                        UiFactory.textBox(
                                draft.itemEntityCount, context.bindText(value -> draft.itemEntityCount = value)),
                        fieldWidth,
                        labelWidth)
                .horizontalSizing(Sizing.fill(fieldFill)));
        values.child(compactValueField(
                        healthLabel,
                        UiFactory.textBox(draft.health, context.bindText(value -> draft.health = value)),
                        fieldWidth,
                        labelWidth)
                .horizontalSizing(Sizing.fill(fieldFill)));
        values.child(compactValueField(
                        ageLabel,
                        UiFactory.textBox(draft.itemEntityAge, context.bindText(value -> draft.itemEntityAge = value)),
                        fieldWidth,
                        labelWidth)
                .horizontalSizing(Sizing.fill(fieldFill)));
        values.child(compactValueField(
                        pickupDelayLabel,
                        UiFactory.textBox(
                                draft.itemEntityPickupDelay,
                                context.bindText(value -> draft.itemEntityPickupDelay = value)),
                        fieldWidth,
                        labelWidth)
                .horizontalSizing(Sizing.fill(fieldFill)));
        group.child(values);

        group.child(playerUuidField(
                context, ItemField.OWNER.text(), draft.itemEntityOwner, value -> draft.itemEntityOwner = value));
        group.child(playerUuidField(
                context, ItemField.THROWER.text(), draft.itemEntityThrower, value -> draft.itemEntityThrower = value));
        return group;
    }

    private static FlowLayout playerUuidField(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter) {
        Map<String, String> players = new LinkedHashMap<>(onlinePlayers(context));
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.column()
                        .child(UiFactory.textBox(value, context.bindText(setter))
                                .horizontalSizing(Sizing.fill(100)))
                        .child(UiFactory.pickerButton(
                                ItemEditorText.tr("common.pick"), DISPLAY_PICK_BUTTON_WIDTH, button -> context.screen()
                                        .openPlayerUuidPicker(
                                                ItemEditorText.str("special.entity.item.player_picker"),
                                                players,
                                                uuid -> context.mutateRefresh(() -> setter.accept(uuid))))));
    }

    static Map<String, String> onlinePlayers(SpecialDataPanelContext context) {
        var connection = context.screen().session().minecraft().getConnection();
        if (connection == null) {
            return Map.of();
        }
        Map<String, String> players = new LinkedHashMap<>();
        connection.getOnlinePlayers().stream()
                .sorted(Comparator.comparing(info -> info.getProfile().name()))
                .forEach(info -> players.put(
                        info.getProfile().id().toString(), info.getProfile().name()));
        return players;
    }

    private static void openItemEntityEditor(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        if (draft.itemEntityStack == null || draft.itemEntityStack.isEmpty()) {
            return;
        }
        int count = Math.clamp(
                ValidationUtil.parseIntOrDefault(draft.itemEntityCount, draft.itemEntityStack.getCount()), 1, 99);
        editStack(context, draft.itemEntityStack.copyWithCount(count), stack -> setItemEntityStack(draft, stack));
    }

    static void editStack(SpecialDataPanelContext context, ItemStack stack, Consumer<ItemStack> setter) {
        context.screen().openNestedEditor(stack, null, edited -> context.mutate(() -> setter.accept(edited)));
    }

    static FlowLayout displayEntity(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.title(ItemEditorText.tr("special.entity.display.title"))
                .shadow(false));
        group.child(displayTransform(context, draft));
        group.child(displayRendering(context, draft));
        switch (EntitySpawnDataUtil.displayType(draft.entityId)) {
            case "block" -> group.child(blockDisplay(context, draft));
            case "item" -> group.child(itemDisplay(context, draft));
            case "text" -> group.child(textDisplay(context, draft));
            default -> {}
        }
        return group;
    }

    private static FlowLayout displayTransform(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(displayHeader(
                context,
                "special.entity.display.transform",
                draft.uiDisplayTransformCollapsed,
                value -> draft.uiDisplayTransformCollapsed = value));
        if (draft.uiDisplayTransformCollapsed) {
            return group;
        }

        for (TransformField field : TransformField.values()) {
            group.child(displayVector(context, draft, field));
        }
        return group;
    }

    private static FlowLayout displayRendering(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(displayHeader(
                context,
                "special.entity.display.rendering",
                draft.uiDisplayRenderingCollapsed,
                value -> draft.uiDisplayRenderingCollapsed = value));
        if (draft.uiDisplayRenderingCollapsed) {
            return group;
        }

        group.child(displayDropdown(
                context,
                draft,
                "billboard",
                "fixed",
                RenderingField.BILLBOARD.key(),
                List.of("fixed", "vertical", "horizontal", "center")));
        addDisplayFields(
                group,
                context,
                draft,
                List.of(
                        displayField("interpolation_duration", "0", RenderingField.INTERPOLATION_DURATION.key()),
                        displayField("start_interpolation", "0", RenderingField.START_INTERPOLATION.key()),
                        displayField("teleport_duration", "0", RenderingField.TELEPORT_DURATION.key()),
                        displayField("view_range", "1", RenderingField.VIEW_RANGE.key()),
                        displayField("shadow_radius", "0", RenderingField.SHADOW_RADIUS.key()),
                        displayField("shadow_strength", "1", RenderingField.SHADOW_STRENGTH.key()),
                        displayField("width", "0", RenderingField.WIDTH.key()),
                        displayField("height", "0", RenderingField.HEIGHT.key()),
                        displayField("glow_color_override", "-1", RenderingField.GLOW_COLOR.key())));
        group.child(UiFactory.muted(ItemEditorText.tr("special.entity.display.brightness")));
        addDisplayFields(
                group,
                context,
                draft,
                List.of(
                        displayField("brightness.block", "", RenderingField.BLOCK_LIGHT.key()),
                        displayField("brightness.sky", "", RenderingField.SKY_LIGHT.key())));
        return group;
    }

    private static FlowLayout blockDisplay(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.muted(ItemEditorText.tr("special.entity.display.block_state")));
        String blockId = EntitySpawnDataUtil.displayValue(draft, "block", "minecraft:air");
        ButtonComponent storagePick = context.storagePickButton(stack -> {
            if (stack.getItem() instanceof BlockItem blockItem) {
                setDisplayBlock(
                        draft,
                        BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
            }
        });
        group.child(PickerFieldFactory.searchableTextField(
                context,
                BlockField.BLOCK.text(),
                blockId,
                value -> setDisplayBlock(draft, value),
                DISPLAY_PICK_BUTTON_WIDTH,
                ItemEditorText.str("special.entity.display.block_picker"),
                "",
                context.registryIds(Registries.BLOCK),
                id -> id,
                id -> context.mutateRefresh(() -> setDisplayBlock(draft, id)),
                storagePick));

        Identifier identifier = IdFieldNormalizer.parse(blockId);
        Block block = identifier == null
                ? null
                : BuiltInRegistries.BLOCK.getOptional(identifier).orElse(null);
        if (block == null) {
            return group;
        }
        Map<String, String> selected = EntitySpawnDataUtil.parseDisplayProperties(
                EntitySpawnDataUtil.displayValue(draft, "block_properties", ""));
        List<Property<?>> properties =
                new ArrayList<>(block.getStateDefinition().getProperties());
        properties.sort(Comparator.comparing(Property::getName));
        for (Property<?> property : properties) {
            group.child(blockProperty(context, draft, block.defaultBlockState(), property, selected));
        }
        return group;
    }

    private static FlowLayout itemDisplay(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(UiFactory.muted(DisplayItemField.ITEM.text()));

        FlowLayout summary = UiFactory.row();
        if (draft.displayItemStack != null && !draft.displayItemStack.isEmpty()) {
            summary.child(
                    UIComponents.item(draft.displayItemStack).showOverlay(true).setTooltipFromStack(true));
        }
        summary.child(
                UiFactory.muted(displayItemSummary(draft.displayItemStack)).horizontalSizing(Sizing.expand(100)));
        group.child(summary);

        ButtonComponent remove = UiFactory.negativeButton(
                ItemEditorText.tr("common.remove"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> draft.displayItemStack = ItemStack.EMPTY));
        remove.active(draft.displayItemStack != null && !draft.displayItemStack.isEmpty());
        group.child(UiFactory.actionButtonRow(
                context.itemPickButton(stack -> draft.displayItemStack = stack.copy()),
                context.storagePickButton(stack -> draft.displayItemStack = stack.copy()),
                remove));
        group.child(displayDropdown(
                context,
                draft,
                "item_display",
                "none",
                DisplayItemField.ITEM_CONTEXT.key(),
                Arrays.stream(ItemDisplayContext.values())
                        .map(ItemDisplayContext::getSerializedName)
                        .toList()));
        return group;
    }

    private static FlowLayout textDisplay(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        FlowLayout group = UiFactory.column().gap(2);
        group.child(displayTextEditor(context, draft));
        addDisplayFields(
                group,
                context,
                draft,
                List.of(
                        displayField("line_width", "200", TextField.LINE_WIDTH.key()),
                        displayField("background", "1073741824", TextField.BACKGROUND.key()),
                        displayField("text_opacity", "-1", TextField.TEXT_OPACITY.key())));
        group.child(displayDropdown(
                context, draft, "alignment", "center", TextField.ALIGNMENT.key(), List.of("left", "center", "right")));
        addPackedRows(
                group,
                Math.max(1, context.panelWidthHint() - UiFactory.scaleProfile().padding() * 4),
                displayFlag(context, draft, "shadow", TextField.TEXT_SHADOW.key()),
                displayFlag(context, draft, "see_through", TextField.SEE_THROUGH.key()),
                displayFlag(context, draft, "default_background", TextField.DEFAULT_BACKGROUND.key()));
        return group;
    }

    private static UIComponent displayTextEditor(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        StyledTextFieldSection.BoundEditor editor = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(EntitySpawnDataUtil.displayValue(draft, "text", "")),
                Sizing.fill(100),
                Sizing.fixed(Math.clamp(context.screen().height / 2, 86, 320)),
                ItemEditorText.str("special.entity.display.text_placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("special.entity.display.text_color_title"),
                ItemEditorText.str("special.entity.display.text_gradient_title"),
                "",
                "",
                null,
                document -> null,
                document -> context.mutate(
                        () -> draft.displayValues.put("text", TextComponentUtil.serializeEditorDocument(document))));
        editor.editor()
                .lineWrapWidthOverride(Math.max(
                        2,
                        ValidationUtil.parseIntOrDefault(
                                EntitySpawnDataUtil.displayValue(draft, "line_width", "200"), 200)));
        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(editor.toolbar());
        ButtonComponent imageArtButton = UiFactory.button(
                TextField.BUTTON.text(), UiFactory.ButtonTextPreset.STANDARD, ignored -> context.screen()
                        .openTextDisplayImageArtDialog(
                                ValidationUtil.parseIntOrDefault(
                                        EntitySpawnDataUtil.displayValue(draft, "background", "1073741824"),
                                        1073741824),
                                (lines, append) -> applyTextDisplayArt(context, draft, lines, append)));
        imageArtButton.horizontalSizing(Sizing.fill(100));
        imageArtButton.tooltip(List.of(ItemEditorText.tr("special.entity.display.image_art.tooltip")));
        frame.child(imageArtButton);
        frame.child(editor.editor());
        frame.child(new RichTextHorizontalScrollbarComponent(Sizing.fill(100), editor.editor()));
        frame.child(editor.validation());
        return UiFactory.field(TextField.TEXT.text(), Component.empty(), frame);
    }

    private static void applyTextDisplayArt(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            List<Component> lines,
            boolean append) {
        context.mutateRefresh(() -> {
            RichTextDocument art = RichTextDocument.fromLines(lines);
            RichTextDocument document = append
                    ? RichTextDocument.fromMarkup(EntitySpawnDataUtil.displayValue(draft, "text", ""))
                    : RichTextDocument.empty();
            if (!document.isEmpty() && !document.plainText().endsWith("\n") && !art.isEmpty()) {
                document.replace(document.length(), document.length(), "\n", RichTextStyle.EMPTY);
            }
            document.replace(document.length(), document.length(), art);
            draft.displayValues.put("text", TextComponentUtil.serializeEditorDocument(document));

            int artWidth = lines.stream()
                    .mapToInt(Minecraft.getInstance().font::width)
                    .max()
                    .orElse(1);
            int currentWidth =
                    ValidationUtil.parseIntOrDefault(EntitySpawnDataUtil.displayValue(draft, "line_width", "200"), 200);
            draft.displayValues.put(
                    "line_width", Integer.toString(append ? Math.max(currentWidth, artWidth) : artWidth));
        });
    }

    private static FlowLayout displayHeader(
            SpecialDataPanelContext context, String titleKey, boolean collapsed, Consumer<Boolean> setter) {
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.muted(ItemEditorText.tr(titleKey)).horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.collapseToggleButton(
                collapsed, () -> context.mutateRefresh(() -> setter.accept(!collapsed))));
        return header;
    }

    private static FlowLayout displayVector(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, TransformField vector) {
        FlowLayout group = UiFactory.column().gap(1);
        group.child(UiFactory.muted(vector.text()));
        addDisplayFields(group, context, draft, displayVectorFields(vector));
        return group;
    }

    static List<DisplayField> displayVectorFields(TransformField vector) {
        List<String> axes = List.of("X", "Y", "Z", "W");
        var fields = new ArrayList<DisplayField>();
        for (int index = 0; index < vector.defaults.size(); index++) {
            fields.add(new DisplayField(
                    vector.name().toLowerCase(Locale.ROOT) + "."
                            + axes.get(index).toLowerCase(Locale.ROOT),
                    vector.defaults.get(index),
                    Component.literal(axes.get(index)),
                    0.1));
        }
        return List.copyOf(fields);
    }

    static List<EditorSearchDialog.Target> displayAxisTargets(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            List<String> path,
            String scope,
            Runnable expand) {
        var result = new ArrayList<EditorSearchDialog.Target>();
        Runnable show = () -> {
            expand.run();
            draft.uiDisplayTransformCollapsed = false;
        };
        for (TransformField vector : TransformField.values()) {
            var vectorPath = new ArrayList<>(path);
            vectorPath.add(vector.text().getString());
            for (DisplayField axis : displayVectorFields(vector)) {
                result.add(new SpecialDataSearch.Control("display-value:" + axis.key(), axis.label(), axis.key())
                        .target(context, EditorCategory.SPECIAL_DATA, vectorPath, scope, show));
                var axisPath = new ArrayList<>(vectorPath);
                axisPath.add(axis.label().getString());
                for (int direction : List.of(-1, 1)) {
                    result.add(displayStepControl(axis.key(), direction)
                            .target(context, EditorCategory.SPECIAL_DATA, axisPath, scope, show));
                }
            }
        }
        return result;
    }

    static SpecialDataSearch.Control displayStepControl(String key, int direction) {
        return new SpecialDataSearch.Control(
                "display-value:" + key + (direction < 0 ? ":decrease" : ":increase"),
                Component.literal(direction < 0 ? "-" : "+"),
                key + (direction < 0 ? " decrease" : " increase"));
    }

    private static void addDisplayFields(
            FlowLayout parent,
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            List<DisplayField> fields) {
        int availableWidth =
                Math.max(1, context.panelWidthHint() - UiFactory.scaleProfile().padding() * 4);
        int labelWidth = fields.stream()
                .map(DisplayField::label)
                .mapToInt(EntitySpawnDataUi::textWidth)
                .max()
                .orElse(1);
        int columns = Math.clamp(availableWidth / Math.max(210, labelWidth + 140), 1, Math.min(4, fields.size()));
        int fieldFill = Math.max(1, (100 - columns) / columns);
        int fieldWidth = Math.max(1, availableWidth * fieldFill / 100);
        for (int offset = 0; offset < fields.size(); offset += columns) {
            FlowLayout row = UiFactory.row();
            int end = Math.min(fields.size(), offset + columns);
            int rowFill = Math.max(1, (100 - (end - offset)) / (end - offset));
            for (int index = offset; index < end; index++) {
                DisplayField field = fields.get(index);
                TextBoxComponent input = UiFactory.textBox(
                        EntitySpawnDataUtil.displayValue(draft, field.key(), field.fallback()),
                        context.bindText(value -> draft.displayValues.put(field.key(), value)));
                input.id("display-value:" + field.key());
                row.child(displayValueField(
                                field.label(),
                                input,
                                fieldWidth,
                                labelWidth,
                                field.fallback(),
                                field.step(),
                                "background".equals(field.key())
                                        ? () -> openBackgroundPicker(context, draft, field)
                                        : null)
                        .horizontalSizing(Sizing.fill(end - offset == columns ? fieldFill : rowFill)));
            }
            parent.child(row);
        }
    }

    private static FlowLayout displayValueField(
            Component label,
            TextBoxComponent input,
            int availableWidth,
            int preferredLabelWidth,
            String fallback,
            double step,
            Runnable pickerAction) {
        int gap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        int labelWidth = Math.clamp(availableWidth / 2, 1, preferredLabelWidth);
        LabelComponent labelComponent = UiFactory.muted(UiFactory.fitToWidth(label, labelWidth), labelWidth);
        if (label.getContents() instanceof TranslatableContents text) {
            labelComponent.id(text.getKey());
        }
        labelComponent.horizontalSizing(Sizing.fixed(labelWidth));
        ButtonComponent decrement = UiFactory.button(
                displayStepControl(input.id().substring("display-value:".length()), -1)
                        .label(),
                UiFactory.ButtonTextPreset.TINY,
                button -> input.text(adjustDisplayValue(input.getValue(), fallback, -step)));
        ButtonComponent increment = UiFactory.button(
                displayStepControl(input.id().substring("display-value:".length()), 1)
                        .label(),
                UiFactory.ButtonTextPreset.TINY,
                button -> input.text(adjustDisplayValue(input.getValue(), fallback, step)));

        decrement.id(displayStepControl(input.id().substring("display-value:".length()), -1)
                .id());
        increment.id(displayStepControl(input.id().substring("display-value:".length()), 1)
                .id());
        FlowLayout field = UiFactory.row().gap(gap);
        field.child(labelComponent);
        field.child(decrement);
        field.child(input.horizontalSizing(Sizing.expand(100)));
        if (pickerAction != null) {
            int color =
                    ValidationUtil.parseIntOrDefault(input.getValue(), ValidationUtil.parseIntOrDefault(fallback, 0));
            ButtonComponent picker = UiFactory.button(
                    ItemEditorText.tr("common.pick"),
                    UiFactory.ButtonTextPreset.COMPACT,
                    ignored -> pickerAction.run());
            picker.tooltip(List.of(Component.literal(ValidationUtil.toArgbHex(color))));
            field.child(picker);
        }
        field.child(increment);
        return field;
    }

    private static void openBackgroundPicker(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, DisplayField field) {
        int initial = ValidationUtil.parseIntOrDefault(
                EntitySpawnDataUtil.displayValue(draft, field.key(), field.fallback()),
                ValidationUtil.parseIntOrDefault(field.fallback(), 0));
        context.screen()
                .openUnifiedColorPickerDialog(
                        field.label().getString(),
                        UnifiedColorPickerDialog.Options.argbColor(initial),
                        result -> context.mutateRefresh(() -> draft.displayValues.put(
                                field.key(), Integer.toString(result.colors().getFirst()))));
    }

    private static String adjustDisplayValue(String raw, String fallback, double delta) {
        String value = raw == null || raw.isBlank() ? fallback : raw;
        BigDecimal number;
        try {
            number = new BigDecimal(value == null || value.isBlank() ? "0" : value.trim());
        } catch (NumberFormatException ignored) {
            number = new BigDecimal(fallback == null || fallback.isBlank() ? "0" : fallback);
        }
        return number.add(BigDecimal.valueOf(delta)).stripTrailingZeros().toPlainString();
    }

    private static DisplayField displayField(String key, String fallback, String labelKey) {
        double step =
                switch (key) {
                    case "interpolation_duration",
                            "start_interpolation",
                            "teleport_duration",
                            "glow_color_override",
                            "brightness.block",
                            "brightness.sky",
                            "line_width",
                            "background",
                            "text_opacity" -> 1.0;
                    default -> 0.1;
                };
        return new DisplayField(key, fallback, ItemEditorText.tr(labelKey), step);
    }

    private static FlowLayout displayDropdown(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            String key,
            String fallback,
            String labelKey,
            List<String> values) {
        String selected = EntitySpawnDataUtil.displayValue(draft, key, fallback);
        ButtonComponent button = UiFactory.button(
                Component.literal(selected),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openDropdown(
                        anchor,
                        values,
                        value -> value,
                        value -> context.mutateRefresh(() -> draft.displayValues.put(key, value))));
        button.horizontalSizing(Sizing.fill(100));
        return UiFactory.field(ItemEditorText.tr(labelKey), Component.empty(), button);
    }

    private static UIComponent displayFlag(
            SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft, String key, String labelKey) {
        return flag(
                context,
                labelKey,
                EntitySpawnDataUtil.displayFlag(draft, key),
                value -> draft.displayValues.put(key, Boolean.toString(value)));
    }

    private static UIComponent blockProperty(
            SpecialDataPanelContext context,
            ItemEditorState.EntitySpawnDraft draft,
            BlockState defaultState,
            Property<?> property,
            Map<String, String> selected) {
        List<String> values = SpecialDataPanelContext.propertyValues(property, true);
        String current = selected.getOrDefault(property.getName(), propertyValue(defaultState, property));
        ButtonComponent button = UiFactory.button(
                Component.literal(current),
                UiFactory.ButtonTextPreset.STANDARD,
                anchor -> context.openDropdown(
                        anchor,
                        values,
                        value -> value,
                        value -> context.mutateRefresh(() -> setDisplayProperty(draft, property.getName(), value))));
        button.horizontalSizing(Sizing.fill(100));
        return UiFactory.field(Component.literal(property.getName()), Component.empty(), button)
                .id("display-property:" + property.getName());
    }

    private static void setDisplayBlock(ItemEditorState.EntitySpawnDraft draft, String blockId) {
        draft.displayValues.put("block", IdFieldNormalizer.normalize(blockId));
        draft.displayValues.remove("block_properties");
    }

    private static void setDisplayProperty(ItemEditorState.EntitySpawnDraft draft, String key, String value) {
        Map<String, String> properties = new LinkedHashMap<>(EntitySpawnDataUtil.parseDisplayProperties(
                EntitySpawnDataUtil.displayValue(draft, "block_properties", "")));
        properties.put(key, value);
        draft.displayValues.put("block_properties", EntitySpawnDataUtil.serializeDisplayProperties(properties));
    }

    private static <T extends Comparable<T>> String propertyValue(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    record DisplayField(String key, String fallback, Component label, double step) {}

    static FlowLayout attributes(SpecialDataPanelContext context, ItemEditorState.EntitySpawnDraft draft) {
        return attributes(
                context,
                draft.entityId,
                draft.attributes,
                draft.uiAttributesCollapsed,
                () -> context.mutateRefresh(() -> draft.uiAttributesCollapsed = !draft.uiAttributesCollapsed),
                Set.of());
    }

    static FlowLayout attributes(
            SpecialDataPanelContext context,
            String entityId,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            boolean collapsed,
            Runnable toggle,
            Set<String> excludedIds) {
        FlowLayout group = UiFactory.column().gap(2);
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("category.attributes.title"))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        header.child(UiFactory.collapseToggleButton(collapsed, toggle));
        group.child(header);
        if (collapsed) {
            return group;
        }

        Map<String, Double> defaults = EntitySpawnDataUtil.defaultAttributeValues(
                context.screen().session().registryAccess(), entityId);
        Set<String> attributeIds = attributeIds(context, entityId, attributes, excludedIds);
        if (attributeIds.isEmpty()) {
            group.child(UiFactory.muted(ItemEditorText.tr("common.none")));
            return group;
        }

        int columns = context.isCompactPanel(620) ? 1 : 2;
        int fieldWidth = Math.max(1, context.panelWidthHint() / columns);
        int fieldFill = columns == 1 ? 100 : 49;
        List<String> ids = List.copyOf(attributeIds);
        List<Component> labels =
                ids.stream().map(id -> attributeLabel(context, id)).toList();
        int labelWidth =
                labels.stream().mapToInt(EntitySpawnDataUi::textWidth).max().orElse(1);
        for (int offset = 0; offset < ids.size(); offset += columns) {
            FlowLayout row = UiFactory.row();
            for (int index = offset; index < Math.min(ids.size(), offset + columns); index++) {
                String attributeId = ids.get(index);
                row.child(compactValueField(
                                labels.get(index),
                                attributeInput(context, attributes, attributeId, defaults.get(attributeId)),
                                fieldWidth,
                                labelWidth)
                        .horizontalSizing(Sizing.fill(fieldFill)));
            }
            group.child(row);
        }
        return group;
    }

    static boolean attributeSchemaChanged(
            SpecialDataPanelContext context, String previousEntityId, String nextEntityId) {
        return !EntitySpawnDataUtil.defaultAttributeValues(
                        context.screen().session().registryAccess(), previousEntityId)
                .equals(EntitySpawnDataUtil.defaultAttributeValues(
                        context.screen().session().registryAccess(), nextEntityId));
    }

    static FlowLayout equipment(
            SpecialDataPanelContext context,
            ItemEditorState.EntityEquipmentDraft draft,
            List<EquipmentSlot> slots,
            boolean editDropChances) {
        FlowLayout group = UiFactory.column().gap(2);
        FlowLayout header = UiFactory.row();
        header.child(UiFactory.title(ItemEditorText.tr("common.equipment"))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        header.child(
                UiFactory.muted(ItemEditorText.tr("special.entity.equipment.summary", equippedCount(draft, slots))));
        header.child(UiFactory.collapseToggleButton(
                draft.uiCollapsed, () -> context.mutateRefresh(() -> draft.uiCollapsed = !draft.uiCollapsed)));
        group.child(header);
        if (draft.uiCollapsed) {
            return group;
        }

        for (EquipmentSlot slot : slots) {
            group.child(equipmentSlot(context, draft, slot, editDropChances));
        }
        return group;
    }

    private static FlowLayout equipmentSlot(
            SpecialDataPanelContext context,
            ItemEditorState.EntityEquipmentDraft draft,
            EquipmentSlot slot,
            boolean editDropChances) {
        ItemStack stack = draft.stack(slot);
        FlowLayout group = UiFactory.column().gap(1);
        group.id(SpecialDataSearch.scope("equipment", draft) + "-" + slot.getSerializedName());
        FlowLayout valueRow = UiFactory.row();
        valueRow.child(UiFactory.muted(ItemEditorText.tr(equipmentSlotKey(slot)), EQUIPMENT_SLOT_LABEL_WIDTH)
                .horizontalSizing(Sizing.fixed(EQUIPMENT_SLOT_LABEL_WIDTH)));
        if (!stack.isEmpty()) {
            valueRow.child(UIComponents.item(stack).showOverlay(false).setTooltipFromStack(true));
        }
        Component fullSummary = equipmentSummary(stack);
        int summaryWidth = Math.max(1, context.panelWidthHint() - UiFactory.scaledPixels(EQUIPMENT_SUMMARY_RESERVE));
        LabelComponent summary = UiFactory.muted(UiFactory.fitToWidth(fullSummary, summaryWidth), summaryWidth);
        summary.tooltip(List.of(fullSummary));
        summary.horizontalSizing(Sizing.expand(100));
        valueRow.child(summary);
        group.child(valueRow);

        FlowLayout actions = context.itemActions(stack, picked -> draft.set(slot, picked), null);
        if (editDropChances) {
            FlowLayout actionRow = UiFactory.row();
            actions.horizontalSizing(Sizing.fill(59));
            actionRow.child(actions);
            Component dropChanceLabel = EquipmentField.DROP_CHANCE.text();
            actionRow.child(compactValueField(
                            dropChanceLabel,
                            UiFactory.textBox(
                                    draft.dropChance(slot),
                                    context.bindText(value -> draft.setDropChance(slot, value))),
                            Math.max(1, context.panelWidthHint() * 39 / 100),
                            textWidth(dropChanceLabel))
                    .horizontalSizing(Sizing.fill(39)));
            group.child(actionRow);
        } else {
            group.child(actions);
        }
        return group;
    }

    private static void setItemEntityStack(ItemEditorState.EntitySpawnDraft draft, ItemStack stack) {
        draft.itemEntityStack = stack == null ? ItemStack.EMPTY : stack.copy();
        draft.itemEntityCount = Integer.toString(Math.max(1, draft.itemEntityStack.getCount()));
    }

    private static Component equipmentSummary(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemEditorText.tr("common.none");
        }
        return stack.getHoverName()
                .copy()
                .append(Component.literal(" | " + BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    private static Component itemEntitySummary(ItemStack stack, String count) {
        return stack == null || stack.isEmpty()
                ? ItemEditorText.tr("special.entity.item.stack_required")
                : equipmentSummary(stack).copy().append(Component.literal(" x" + count));
    }

    private static Component displayItemSummary(ItemStack stack) {
        return stack == null || stack.isEmpty()
                ? ItemEditorText.tr("common.none")
                : equipmentSummary(stack).copy().append(Component.literal(" x" + stack.getCount()));
    }

    private static Component attributeLabel(SpecialDataPanelContext context, String rawId) {
        Identifier identifier = IdFieldNormalizer.parse(rawId);
        if (identifier == null) {
            return Component.literal(rawId);
        }
        Registry<Attribute> registry =
                context.screen().session().registryAccess().lookupOrThrow(Registries.ATTRIBUTE);
        return registry.getOptional(identifier)
                .map(attribute -> Component.translatable(attribute.getDescriptionId()))
                .orElseGet(() -> Component.literal(rawId));
    }

    private static TextBoxComponent attributeInput(
            SpecialDataPanelContext context,
            List<ItemEditorState.EntityAttributeDraft> attributes,
            String attributeId,
            Double defaultValue) {
        TextBoxComponent input = UiFactory.textBox(
                attributeBase(attributes, attributeId),
                context.bindText(value -> setAttributeBase(attributes, attributeId, value)));
        if (defaultValue != null) {
            input.setHint(Component.literal(ValidationUtil.trimTrailingZeros(defaultValue)));
        }
        input.id("entity-attribute:" + attributeId);
        return input;
    }

    private static FlowLayout compactValueField(
            Component label, TextBoxComponent input, int availableWidth, int preferredLabelWidth) {
        int gap = Math.max(1, UiFactory.scaleProfile().tightSpacing());
        int inputWidth = Math.clamp(availableWidth / 2, 1, Math.clamp(availableWidth / 3, 48, 96));
        int labelWidth = Math.clamp(availableWidth - inputWidth - gap, 1, preferredLabelWidth);
        Component fittedLabel = UiFactory.fitToWidth(label, labelWidth);
        LabelComponent labelComponent = UiFactory.muted(fittedLabel, labelWidth);
        if (input.id() != null) {
            labelComponent.id(input.id());
        } else if (label.getContents() instanceof TranslatableContents text) {
            labelComponent.id(text.getKey());
        }
        labelComponent.horizontalSizing(Sizing.fixed(labelWidth));
        if (!fittedLabel.getString().equals(label.getString())) {
            labelComponent.tooltip(List.of(label));
        }

        FlowLayout field = UiFactory.row().gap(gap);
        field.child(labelComponent);
        field.child(input.horizontalSizing(Sizing.fixed(inputWidth)));
        return field;
    }

    private static int textWidth(Component text) {
        return Minecraft.getInstance().font.width(text) + 4;
    }

    private static String attributeBase(List<ItemEditorState.EntityAttributeDraft> attributes, String attributeId) {
        return attributes.stream()
                .filter(attribute -> attributeId.equals(attribute.attributeId))
                .map(attribute -> attribute.baseValue)
                .findFirst()
                .orElse("");
    }

    private static void setAttributeBase(
            List<ItemEditorState.EntityAttributeDraft> attributes, String attributeId, String value) {
        String normalized = value == null ? "" : value.trim();
        ItemEditorState.EntityAttributeDraft existing = attributes.stream()
                .filter(attribute -> attributeId.equals(attribute.attributeId))
                .findFirst()
                .orElse(null);
        if (normalized.isBlank()) {
            attributes.removeIf(attribute -> attributeId.equals(attribute.attributeId));
        } else if (existing != null) {
            existing.baseValue = normalized;
        } else {
            ItemEditorState.EntityAttributeDraft attribute = new ItemEditorState.EntityAttributeDraft();
            attribute.attributeId = attributeId;
            attribute.baseValue = normalized;
            attributes.add(attribute);
        }
    }

    private static int equippedCount(ItemEditorState.EntityEquipmentDraft draft, List<EquipmentSlot> slots) {
        return (int) slots.stream().filter(slot -> !draft.stack(slot).isEmpty()).count();
    }

    private static String equipmentSlotKey(EquipmentSlot slot) {
        return switch (slot) {
            case MAINHAND -> "special.entity.equipment.slot.mainhand";
            case OFFHAND -> "special.entity.equipment.slot.offhand";
            case FEET -> "special.entity.equipment.slot.feet";
            case LEGS -> "special.entity.equipment.slot.legs";
            case CHEST -> "special.entity.equipment.slot.chest";
            case HEAD -> "special.entity.equipment.slot.head";
            case BODY -> "special.entity.equipment.slot.body";
            case SADDLE -> "special.entity.equipment.slot.saddle";
        };
    }

    private static UIComponent flag(
            SpecialDataPanelContext context, String labelKey, boolean checked, Consumer<Boolean> setter) {
        Component label = ItemEditorText.tr(labelKey);
        int width = textWidth(label)
                + UiFactory.scaleProfile().controlHeight()
                + UiFactory.scaleProfile().tightSpacing();
        return UiFactory.checkbox(label, checked, value -> context.mutateRefresh(() -> setter.accept(value)))
                .horizontalSizing(Sizing.fixed(width));
    }

    private static void addPackedRows(FlowLayout group, int availableWidth, UIComponent... components) {
        int gap = Math.max(1, UiFactory.scaleProfile().spacing());
        int usedWidth = 0;
        FlowLayout row = UiFactory.row();
        for (UIComponent component : components) {
            int width = Math.min(availableWidth, component.horizontalSizing().get().value);
            int requiredWidth = usedWidth == 0 ? width : gap + width;
            if (usedWidth > 0 && usedWidth + requiredWidth > availableWidth) {
                group.child(row);
                row = UiFactory.row();
                usedWidth = 0;
                requiredWidth = width;
            }
            component.horizontalSizing(Sizing.fixed(width));
            row.child(component);
            usedWidth += requiredWidth;
        }
        group.child(row);
    }

    private enum HealthField implements SpecialDataSearch.Field {
        HEALTH("special.entity.health"),
        MAX_HEALTH("special.entity.max_health");

        private final String key;

        HealthField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum ItemField implements SpecialDataSearch.Field {
        TITLE("special.entity.item.title"),
        COUNT("common.count"),
        AGE("special.bucket.age"),
        PICKUP_DELAY("special.entity.item.pickup_delay"),
        OWNER("special.entity.item.owner"),
        THROWER("special.entity.item.thrower");

        private final String key;

        ItemField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum EffectField implements SpecialDataSearch.Field {
        ADD("special.entity.effects.add");

        private final String key;

        EffectField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum EquipmentField implements SpecialDataSearch.Field {
        DROP_CHANCE("special.entity.equipment.drop_chance");

        private final String key;

        EquipmentField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    enum TransformField implements SpecialDataSearch.Field {
        TRANSLATION("special.entity.display.translation", List.of("0", "0", "0")),
        SCALE("special.entity.display.scale", List.of("1", "1", "1")),
        LEFT_ROTATION("special.entity.display.left_rotation", List.of("0", "0", "0", "1")),
        RIGHT_ROTATION("special.entity.display.right_rotation", List.of("0", "0", "0", "1"));

        private final String key;

        private final List<String> defaults;

        TransformField(String key, List<String> defaults) {
            this.key = key;
            this.defaults = defaults;
        }

        public String key() {
            return key;
        }
    }

    private enum RenderingField implements SpecialDataSearch.Field {
        BILLBOARD("special.entity.display.billboard"),
        INTERPOLATION_DURATION("special.entity.display.interpolation_duration"),
        START_INTERPOLATION("special.entity.display.start_interpolation"),
        TELEPORT_DURATION("special.entity.display.teleport_duration"),
        VIEW_RANGE("special.entity.display.view_range"),
        SHADOW_RADIUS("special.entity.display.shadow_radius"),
        SHADOW_STRENGTH("special.entity.display.shadow_strength"),
        WIDTH("special.entity.display.width"),
        HEIGHT("special.entity.display.height"),
        GLOW_COLOR("special.entity.display.glow_color"),
        BLOCK_LIGHT("special.entity.display.block_light"),
        SKY_LIGHT("special.entity.display.sky_light");

        private final String key;

        RenderingField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum BlockField implements SpecialDataSearch.Field {
        BLOCK("special.entity.display.block");

        private final String key;

        BlockField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum DisplayItemField implements SpecialDataSearch.Field {
        ITEM("special.entity.display.item"),
        ITEM_CONTEXT("special.entity.display.item_context");

        private final String key;

        DisplayItemField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum TextField implements SpecialDataSearch.Field {
        TEXT("special.entity.display.text"),
        LINE_WIDTH("special.entity.display.line_width"),
        BACKGROUND("special.entity.display.background"),
        TEXT_OPACITY("special.entity.display.text_opacity"),
        ALIGNMENT("special.entity.display.alignment"),
        TEXT_SHADOW("special.entity.display.text_shadow"),
        SEE_THROUGH("special.entity.display.see_through"),
        DEFAULT_BACKGROUND("special.entity.display.default_background"),
        BUTTON("display.lore.image_art.button");

        private final String key;

        TextField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
