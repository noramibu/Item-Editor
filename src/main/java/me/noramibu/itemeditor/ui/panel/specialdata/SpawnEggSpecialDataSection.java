package me.noramibu.itemeditor.ui.panel.specialdata;

import static me.noramibu.itemeditor.ui.panel.specialdata.AdvancedItemSpecialDataSection.valueOrDefault;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import me.noramibu.itemeditor.editor.EditorCategory;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.service.EntityTagFields;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

public final class SpawnEggSpecialDataSection {
    public static List<EditorSearchDialog.Target> searchTargets(SpecialDataPanelContext context) {
        var result = new ArrayList<>(SpecialDataSearch.targets(
                context,
                EditorCategory.SPECIAL_DATA,
                "special.spawn_egg.title",
                "spawn-egg",
                () -> {},
                Field.values()));
        var special = context.special();
        var path = List.of(ItemEditorText.str("special.spawn_egg.title"));
        result.addAll(EntityTagFieldsUi.searchTargets(
                context, special.spawnEggEntity, path.getFirst(), "spawn-egg", () -> {}));
        result.addAll(EntitySpawnDataUi.searchTargets(context, special.spawnEggEntity, path, "spawn-egg", () -> {}));
        var type = resolveSelectedEntityType(context, special.spawnEggEntity.entityId);
        if (ItemEditorCapabilities.supportsVillagerTrades(type)) {
            if (ItemEditorCapabilities.supportsVillagerData(type)) {
                result.addAll(SpecialDataSearch.targets(
                        context, EditorCategory.SPECIAL_DATA, path, "spawn-egg", () -> {}, VillagerField.values()));
            }
            result.addAll(SpecialDataSearch.targets(
                    context, EditorCategory.SPECIAL_DATA, path, "spawn-egg", () -> {}, TradeListField.ADD_TRADE));
            if (!special.spawnEggVillagerTrades.isEmpty()) {
                result.addAll(SpecialDataSearch.targets(
                        context,
                        EditorCategory.SPECIAL_DATA,
                        path,
                        "spawn-egg",
                        () -> {},
                        TradeListField.CLEAR_ALL,
                        TradeListField.EXPAND_ALL,
                        TradeListField.COLLAPSE_ALL));
            }
            for (int index = 0; index < special.spawnEggVillagerTrades.size(); index++) {
                var trade = special.spawnEggVillagerTrades.get(index);
                result.addAll(SpecialDataSearch.targets(
                        context,
                        EditorCategory.SPECIAL_DATA,
                        List.of(path.getFirst(), ItemEditorText.str("special.spawn_egg.villager.trade", index + 1)),
                        SpecialDataSearch.scope("villager-trade", trade),
                        () -> trade.uiCollapsed = false,
                        TradeField.values()));
            }
        }
        return result;
    }

    private static final int ENTITY_ID_FIELD_WIDTH = 280;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;
    private static final int ACTION_BUTTON_WIDTH_MAX = 176;
    private static final int ACTION_BUTTON_ROW_RESERVE = 12;

    private SpawnEggSpecialDataSection() {}

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSpawnEggData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        EntityTagFieldsUi tags = new EntityTagFieldsUi(context, special.spawnEggEntity);
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.spawn_egg.title"), Component.empty());
        section.id("spawn-egg");
        section.child(buildEntityCard(context, special));
        section.child(EntitySpawnDataUi.absorbedItem(context, special.spawnEggEntity));
        section.child(buildNameCard(context, special));
        section.child(buildFlagsCard(context, special, tags));
        section.child(EntitySpawnDataUi.details(context, special.spawnEggEntity, tags));
        if (ItemEditorCapabilities.supportsVillagerTrades(
                resolveSelectedEntityType(context, special.spawnEggEntity.entityId))) {
            section.child(buildVillagerCard(context, special, tags));
        }
        section.child(tags.build());
        return section;
    }

    private static FlowLayout buildEntityCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(Field.ENTITY.text()).shadow(false));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        int entityActionWidth = resolveButtonWidth(context, 3);
        row.child(UiFactory.textBox(
                        special.spawnEggEntity.entityId, value -> updateSelectedEntity(context, special, value))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(ENTITY_ID_FIELD_WIDTH)));
        ButtonComponent pickButton = UiFactory.button(
                ItemEditorText.tr("common.pick"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.openSearchablePicker(
                        Field.ENTITY.text().getString(),
                        "",
                        context.registryIds(Registries.ENTITY_TYPE),
                        id -> id,
                        id -> setSelectedEntity(context, special, id)));
        pickButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(entityActionWidth));
        row.child(pickButton);
        card.child(row);
        return card;
    }

    private static void updateSelectedEntity(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, String rawValue) {
        String previousEntityId = special.spawnEggEntity.entityId;
        String normalized = IdFieldNormalizer.normalize(rawValue);
        context.mutate(() -> special.spawnEggEntity.entityId = normalized);
        if (variantFieldsChanged(context, previousEntityId, normalized)) {
            context.screen().refreshCurrentPanel();
        }
    }

    private static void setSelectedEntity(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, String entityId) {
        String previousEntityId = special.spawnEggEntity.entityId;
        String normalized = IdFieldNormalizer.normalize(entityId);
        context.mutate(() -> special.spawnEggEntity.entityId = normalized);
        if (variantFieldsChanged(context, previousEntityId, normalized)) {
            context.screen().refreshCurrentPanel();
        } else {
            context.screen().session().rebuildPreview();
        }
    }

    private static boolean variantFieldsChanged(
            SpecialDataPanelContext context, String previousEntityId, String nextEntityId) {
        if (nextEntityId == null
                || nextEntityId.isBlank()
                || resolveSelectedEntityType(context, nextEntityId) == null) {
            return false;
        }
        return previousEntityId == null
                || previousEntityId.isBlank()
                || resolveSelectedEntityType(context, previousEntityId) == null
                || EntitySpawnDataUi.attributeSchemaChanged(context, previousEntityId, nextEntityId)
                || !EntityTagFields.groups(previousEntityId).equals(EntityTagFields.groups(nextEntityId))
                || !Objects.equals(
                        variantFieldGroup(context, previousEntityId), variantFieldGroup(context, nextEntityId));
    }

    private static String variantFieldGroup(SpecialDataPanelContext context, String rawEntityId) {
        EntityType<?> selectedType = resolveSelectedEntityType(context, rawEntityId);
        if (selectedType == null) {
            return "";
        }

        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(selectedType).toString();
        return switch (entityId) {
            case "minecraft:axolotl",
                    "minecraft:cat",
                    "minecraft:chicken",
                    "minecraft:cow",
                    "minecraft:fox",
                    "minecraft:frog",
                    "minecraft:horse",
                    "minecraft:llama",
                    "minecraft:trader_llama",
                    "minecraft:mooshroom",
                    "minecraft:parrot",
                    "minecraft:pig",
                    "minecraft:rabbit",
                    "minecraft:salmon",
                    "minecraft:sheep",
                    "minecraft:shulker",
                    "minecraft:tropical_fish",
                    "minecraft:villager",
                    "minecraft:wandering_trader",
                    "minecraft:zombie_villager",
                    "minecraft:wolf",
                    "minecraft:zombie_nautilus",
                    "minecraft:item",
                    "minecraft:block_display",
                    "minecraft:item_display",
                    "minecraft:text_display" -> entityId;
            default -> "";
        };
    }

    private static EntityType<?> resolveSelectedEntityType(SpecialDataPanelContext context, String rawEntityId) {
        String normalized = rawEntityId == null ? "" : IdFieldNormalizer.normalize(rawEntityId);
        if (!normalized.isBlank()) {
            Identifier identifier = IdFieldNormalizer.parse(normalized);
            if (identifier != null) {
                return BuiltInRegistries.ENTITY_TYPE.getOptional(identifier).orElse(null);
            }
            return null;
        }

        ItemStack stack = context.originalStack();
        if (stack.getItem() instanceof SpawnEggItem) {
            return SpawnEggItem.getType(stack);
        }
        return null;
    }

    private static FlowLayout buildNameCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(EntitySpawnDataUi.nameEditor(
                context,
                special.spawnEggEntity,
                Field.CUSTOM_NAME.text(),
                "special.spawn_egg.name.placeholder",
                "special.spawn_egg.name.color_title",
                "special.spawn_egg.name.gradient_title"));
        return card;
    }

    private static FlowLayout buildFlagsCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, EntityTagFieldsUi tags) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();

        FlowLayout presets = compactLayout ? UiFactory.column() : UiFactory.row();
        presets.gap(6);
        int flagsActionWidth = resolveButtonWidth(context, 2);
        ButtonComponent resetFlags = UiFactory.button(
                Field.RESET_FLAGS.text(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> {
                    special.spawnEggEntity.noAi = false;
                    special.spawnEggEntity.silent = false;
                    special.spawnEggEntity.noGravity = false;
                    special.spawnEggEntity.glowing = false;
                    special.spawnEggEntity.invulnerable = false;
                    special.spawnEggEntity.persistenceRequired = false;
                    special.spawnEggEntity.customNameVisible = false;
                }));
        resetFlags.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(flagsActionWidth));
        presets.child(resetFlags);
        ButtonComponent displayPreset = UiFactory.button(
                Field.DISPLAY_PRESET.text(),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> context.mutateRefresh(() -> {
                    special.spawnEggEntity.noAi = true;
                    special.spawnEggEntity.silent = true;
                    special.spawnEggEntity.noGravity = true;
                    special.spawnEggEntity.invulnerable = true;
                    special.spawnEggEntity.persistenceRequired = true;
                }));
        displayPreset.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(flagsActionWidth));
        presets.child(displayPreset);
        card.child(presets);
        card.child(EntitySpawnDataUi.flags(context, special.spawnEggEntity, tags));
        return card;
    }

    private static FlowLayout buildVillagerCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, EntityTagFieldsUi tags) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(tags.fields("entity"));
        if (ItemEditorCapabilities.supportsVillagerData(
                resolveSelectedEntityType(context, special.spawnEggEntity.entityId))) {
            card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.title"))
                    .shadow(false));

            FlowLayout villagerDataRow = compactLayout ? UiFactory.column() : UiFactory.row();
            FlowLayout typeField = idFieldWithPicker(
                    context,
                    VillagerField.TYPE.text(),
                    special.spawnEggVillagerTypeId,
                    value -> special.spawnEggVillagerTypeId = value,
                    context.registryIds(Registries.VILLAGER_TYPE),
                    ItemEditorText.str("special.spawn_egg.villager.select_type"));
            typeField.horizontalSizing(Sizing.fill(compactLayout ? 100 : 40));
            villagerDataRow.child(typeField);
            FlowLayout professionField = idFieldWithPicker(
                    context,
                    VillagerField.PROFESSION.text(),
                    special.spawnEggVillagerProfessionId,
                    value -> special.spawnEggVillagerProfessionId = value,
                    context.registryIds(Registries.VILLAGER_PROFESSION),
                    ItemEditorText.str("special.spawn_egg.villager.select_profession"));
            professionField.horizontalSizing(Sizing.fill(compactLayout ? 100 : 40));
            villagerDataRow.child(professionField);
            FlowLayout levelField = UiFactory.field(
                    VillagerField.LEVEL.text(),
                    Component.empty(),
                    UiFactory.textBox(
                            special.spawnEggVillagerLevel,
                            context.bindText(value -> special.spawnEggVillagerLevel = value)));
            levelField.horizontalSizing(Sizing.fill(compactLayout ? 100 : 14));
            villagerDataRow.child(levelField);
            card.child(villagerDataRow);
        }

        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.trades"))
                .shadow(false));
        card.child(UiFactory.actionButtonRow(
                UiFactory.button(
                        TradeListField.ADD_TRADE.text(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.mutateRefresh(
                                () -> special.spawnEggVillagerTrades.add(new ItemEditorState.VillagerTradeDraft()))),
                special.spawnEggVillagerTrades.isEmpty()
                        ? null
                        : UiFactory.button(
                                TradeListField.CLEAR_ALL.text(),
                                UiFactory.ButtonTextPreset.STANDARD,
                                button -> context.mutateRefresh(special.spawnEggVillagerTrades::clear))));

        if (special.spawnEggVillagerTrades.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.spawn_egg.villager.trades_empty")));
            return card;
        }

        card.child(UiFactory.actionButtonRow(
                UiFactory.button(
                        TradeListField.EXPAND_ALL.text(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.mutateRefresh(
                                () -> special.spawnEggVillagerTrades.forEach(trade -> trade.uiCollapsed = false))),
                UiFactory.button(
                        TradeListField.COLLAPSE_ALL.text(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.mutateRefresh(
                                () -> special.spawnEggVillagerTrades.forEach(trade -> trade.uiCollapsed = true)))));

        List<String> itemIds = context.itemIdsWithoutAir();
        for (int index = 0; index < special.spawnEggVillagerTrades.size(); index++) {
            FlowLayout tradeCard = tradeCard(context, special, index);
            ItemEditorState.VillagerTradeDraft trade = special.spawnEggVillagerTrades.get(index);

            if (trade.uiCollapsed) {
                card.child(tradeCard);
                continue;
            }

            tradeCard.child(tradeItemField(
                    context,
                    TradeField.BUY.text(),
                    trade.buy,
                    itemIds,
                    TradeField.BUY.text().getString()));
            tradeCard.child(tradeItemField(
                    context,
                    TradeField.BUY_B.text(),
                    trade.buyB,
                    itemIds,
                    TradeField.BUY_B.text().getString()));
            tradeCard.child(tradeItemField(
                    context,
                    TradeField.SELL.text(),
                    trade.sell,
                    itemIds,
                    TradeField.SELL.text().getString()));
            tradeCard.child(UiFactory.actionButtonRow(
                    UiFactory.button(
                            TradeField.SWAP.text(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() -> swapBuyAndSell(trade))),
                    UiFactory.button(
                            TradeField.RESET.text(),
                            UiFactory.ButtonTextPreset.STANDARD,
                            button -> context.mutateRefresh(() -> resetTrade(trade)))));

            tradeCard.child(tradeValueRows(context, trade));

            tradeCard.child(UiFactory.checkbox(
                    TradeField.REWARD_EXP.text(),
                    trade.rewardExp,
                    value -> context.mutate(() -> trade.rewardExp = value)));

            card.child(tradeCard);
        }

        return card;
    }

    private static FlowLayout tradeCard(
            SpecialDataPanelContext context, ItemEditorState.SpecialData special, int currentIndex) {
        ItemEditorState.VillagerTradeDraft trade = special.spawnEggVillagerTrades.get(currentIndex);
        FlowLayout card = UiFactory.subCard();
        card.id(SpecialDataSearch.scope("villager-trade", trade));
        FlowLayout titleRow = UiFactory.row();
        titleRow.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.trade", currentIndex + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        titleRow.child(UiFactory.collapseToggleButton(
                trade.uiCollapsed, () -> context.mutateRefresh(() -> trade.uiCollapsed = !trade.uiCollapsed)));
        card.child(titleRow);
        card.child(UiFactory.muted(Component.literal(tradeSummary(trade))));

        ButtonComponent upButton = UiFactory.button(
                TradeField.UP.text(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(
                        () -> context.swapEntries(special.spawnEggVillagerTrades, currentIndex, currentIndex - 1)));
        upButton.active(currentIndex > 0);
        ButtonComponent downButton = UiFactory.button(
                TradeField.DOWN.text(),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.mutateRefresh(
                        () -> context.swapEntries(special.spawnEggVillagerTrades, currentIndex, currentIndex + 1)));
        downButton.active(currentIndex < special.spawnEggVillagerTrades.size() - 1);
        card.child(UiFactory.actionButtonRow(
                upButton,
                downButton,
                UiFactory.button(
                        TradeField.DUPLICATE.text(),
                        UiFactory.ButtonTextPreset.COMPACT,
                        button -> context.mutateRefresh(
                                () -> special.spawnEggVillagerTrades.add(currentIndex + 1, copyTrade(trade)))),
                UiFactory.negativeButton(
                        TradeField.REMOVE.text(),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.mutateRefresh(() -> special.spawnEggVillagerTrades.remove(currentIndex)))));
        return card;
    }

    private static FlowLayout tradeItemField(
            SpecialDataPanelContext context,
            Component label,
            ItemEditorState.TradeStackDraft stackDraft,
            List<String> itemIds,
            String pickerTitle) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout container = UiFactory.column();
        container.gap(2);
        container.child(UiFactory.muted(label));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(
                        stackDraft.itemId,
                        value -> context.mutate(() -> {
                            stackDraft.itemId = IdFieldNormalizer.normalize(value);
                            stackDraft.templateStack = ItemStack.EMPTY;
                        }))
                .horizontalSizing(Sizing.fill(compactLayout ? 100 : 50)));
        row.child(UiFactory.textBox(stackDraft.count, context.bindText(value -> stackDraft.count = value))
                .horizontalSizing(Sizing.fill(compactLayout ? 100 : 10)));
        row.child(UiFactory.button(
                        ItemEditorText.tr("common.pick"),
                        UiFactory.ButtonTextPreset.STANDARD,
                        button -> context.openSearchablePicker(
                                pickerTitle,
                                "",
                                itemIds,
                                id -> id,
                                id -> context.mutateRefresh(() -> {
                                    stackDraft.itemId = id;
                                    stackDraft.templateStack = ItemStack.EMPTY;
                                })))
                .horizontalSizing(Sizing.fill(compactLayout ? 100 : 15)));
        row.child(context.storagePickButton(stack -> setTradeStackFromInventory(stackDraft, stack))
                .horizontalSizing(Sizing.fill(compactLayout ? 100 : 25)));
        container.child(row);
        return container;
    }

    private static void setTradeStackFromInventory(ItemEditorState.TradeStackDraft stackDraft, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            stackDraft.itemId = "";
            stackDraft.count = "1";
            stackDraft.templateStack = ItemStack.EMPTY;
            return;
        }
        stackDraft.itemId = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        stackDraft.count = Integer.toString(stack.getCount());
        stackDraft.templateStack = stack.copy();
    }

    private static FlowLayout idFieldWithPicker(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> values,
            String pickerTitle) {
        return UiFactory.field(label, Component.empty(), idTextWithPicker(context, value, setter, values, pickerTitle));
    }

    private static FlowLayout idTextWithPicker(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle) {
        FlowLayout row = UiFactory.row();
        row.child(
                UiFactory.textBox(value, text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text))))
                        .horizontalSizing(Sizing.expand(100)));
        ButtonComponent pickButton = UiFactory.button(
                ItemEditorText.tr("common.pick"),
                UiFactory.ButtonTextPreset.COMPACT,
                button -> context.openSearchablePicker(
                        pickerTitle, "", entries, id -> id, id -> context.mutateRefresh(() -> setter.accept(id))));
        pickButton.horizontalSizing(UiFactory.fixed(52));
        row.child(pickButton);
        return row;
    }

    private static FlowLayout shortField(
            SpecialDataPanelContext context, Component label, String value, Consumer<String> setter) {
        return UiFactory.field(label, Component.empty(), UiFactory.textBox(value, context.bindText(setter)));
    }

    private static FlowLayout tradeValueRows(
            SpecialDataPanelContext context, ItemEditorState.VillagerTradeDraft trade) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout rows = UiFactory.column();
        rows.child(tradeValueRow(
                compactLayout,
                shortField(context, TradeField.MAX_USES.text(), trade.maxUses, value -> trade.maxUses = value),
                shortField(context, TradeField.USES.text(), trade.uses, value -> trade.uses = value),
                shortField(context, TradeField.XP.text(), trade.villagerXp, value -> trade.villagerXp = value)));
        rows.child(tradeValueRow(
                compactLayout,
                shortField(
                        context,
                        TradeField.PRICE_MULTIPLIER.text(),
                        trade.priceMultiplier,
                        value -> trade.priceMultiplier = value),
                shortField(context, TradeField.DEMAND.text(), trade.demand, value -> trade.demand = value),
                shortField(
                        context,
                        TradeField.SPECIAL_PRICE.text(),
                        trade.specialPrice,
                        value -> trade.specialPrice = value)));
        return rows;
    }

    private static FlowLayout tradeValueRow(
            boolean compactLayout, FlowLayout first, FlowLayout second, FlowLayout third) {
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        first.horizontalSizing(Sizing.fill(compactLayout ? 100 : 33));
        second.horizontalSizing(Sizing.fill(compactLayout ? 100 : 33));
        third.horizontalSizing(Sizing.fill(compactLayout ? 100 : 33));
        row.child(first);
        row.child(second);
        row.child(third);
        return row;
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return context.isCompactPanel(COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static ItemEditorState.VillagerTradeDraft copyTrade(ItemEditorState.VillagerTradeDraft source) {
        ItemEditorState.VillagerTradeDraft copy = new ItemEditorState.VillagerTradeDraft();
        copyTradeStack(source.buy, copy.buy);
        copyTradeStack(source.buyB, copy.buyB);
        copyTradeStack(source.sell, copy.sell);
        copy.maxUses = source.maxUses;
        copy.uses = source.uses;
        copy.villagerXp = source.villagerXp;
        copy.priceMultiplier = source.priceMultiplier;
        copy.demand = source.demand;
        copy.specialPrice = source.specialPrice;
        copy.rewardExp = source.rewardExp;
        copy.uiCollapsed = source.uiCollapsed;
        return copy;
    }

    private static void copyTradeStack(ItemEditorState.TradeStackDraft source, ItemEditorState.TradeStackDraft target) {
        target.itemId = source.itemId;
        target.count = source.count;
        target.templateStack = source.templateStack == null ? ItemStack.EMPTY : source.templateStack.copy();
    }

    private static void swapBuyAndSell(ItemEditorState.VillagerTradeDraft trade) {
        ItemEditorState.TradeStackDraft buyCopy = new ItemEditorState.TradeStackDraft();
        ItemEditorState.TradeStackDraft sellCopy = new ItemEditorState.TradeStackDraft();
        copyTradeStack(trade.buy, buyCopy);
        copyTradeStack(trade.sell, sellCopy);
        copyTradeStack(sellCopy, trade.buy);
        copyTradeStack(buyCopy, trade.sell);
    }

    private static void resetTrade(ItemEditorState.VillagerTradeDraft trade) {
        trade.buy.itemId = "";
        trade.buy.count = "1";
        trade.buyB.itemId = "";
        trade.buyB.count = "1";
        trade.sell.itemId = "";
        trade.sell.count = "1";
        trade.maxUses = "16";
        trade.uses = "0";
        trade.villagerXp = "1";
        trade.priceMultiplier = "0.05";
        trade.demand = "";
        trade.specialPrice = "";
        trade.rewardExp = true;
    }

    private static String tradeSummary(ItemEditorState.VillagerTradeDraft trade) {
        String buyId = valueOrDefault(trade.buy.itemId, "?");
        String buyCount = valueOrDefault(trade.buy.count, "1");
        String sellId = valueOrDefault(trade.sell.itemId, "?");
        String sellCount = valueOrDefault(trade.sell.count, "1");
        return buyCount + "x " + buyId + " -> " + sellCount + "x " + sellId;
    }

    private static int resolveButtonWidth(SpecialDataPanelContext context, int buttonCount) {
        int contentWidth = Math.max(1, context.panelWidthHint());
        int preferred = Math.min(
                ACTION_BUTTON_WIDTH_MAX,
                (contentWidth - UiFactory.scaledPixels(ACTION_BUTTON_ROW_RESERVE)) / Math.max(1, buttonCount));
        return Math.clamp(preferred, 1, contentWidth);
    }

    private enum Field implements SpecialDataSearch.Field {
        ENTITY("special.spawn_egg.entity"),
        CUSTOM_NAME("common.custom_name"),
        RESET_FLAGS("special.spawn_egg.reset_flags"),
        DISPLAY_PRESET("special.spawn_egg.display_preset");

        private final String key;

        Field(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum VillagerField implements SpecialDataSearch.Field {
        TYPE("special.spawn_egg.villager.type"),
        PROFESSION("special.spawn_egg.villager.profession"),
        LEVEL("special.spawn_egg.villager.level");

        private final String key;

        VillagerField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum TradeListField implements SpecialDataSearch.Field {
        ADD_TRADE("special.spawn_egg.villager.add_trade"),
        CLEAR_ALL("common.clear_all"),
        EXPAND_ALL("common.expand_all"),
        COLLAPSE_ALL("common.collapse_all");

        private final String key;

        TradeListField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }

    private enum TradeField implements SpecialDataSearch.Field {
        BUY("special.spawn_egg.villager.trade.buy"),
        BUY_B("special.spawn_egg.villager.trade.buy_b"),
        SELL("special.spawn_egg.villager.trade.sell"),
        SWAP("special.spawn_egg.villager.trade.swap"),
        RESET("special.spawn_egg.villager.trade.reset"),
        REWARD_EXP("special.spawn_egg.villager.trade.reward_exp"),
        MAX_USES("special.spawn_egg.villager.trade.max_uses"),
        USES("special.spawn_egg.villager.trade.uses"),
        XP("special.spawn_egg.villager.trade.xp"),
        PRICE_MULTIPLIER("special.spawn_egg.villager.trade.price_multiplier"),
        DEMAND("special.spawn_egg.villager.trade.demand"),
        SPECIAL_PRICE("special.spawn_egg.villager.trade.special_price"),
        UP("common.up"),
        DOWN("common.down"),
        DUPLICATE("common.duplicate"),
        REMOVE("common.remove");

        private final String key;

        TradeField(String key) {
            this.key = key;
        }

        public String key() {
            return key;
        }
    }
}
