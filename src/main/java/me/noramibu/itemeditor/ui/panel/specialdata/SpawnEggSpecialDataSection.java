package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.text.RichTextDocument;
import me.noramibu.itemeditor.ui.component.StyledTextFieldSection;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class SpawnEggSpecialDataSection {

    private static final int VILLAGER_ID_WIDTH = 220;
    private static final int ENTITY_ID_FIELD_WIDTH = 280;
    private static final int HEALTH_FIELD_WIDTH = 120;
    private static final int VILLAGER_LEVEL_FIELD_WIDTH = 90;
    private static final int TRADE_ITEM_ID_FIELD_WIDTH = 180;
    private static final int TRADE_ITEM_COUNT_FIELD_WIDTH = 54;
    private static final int TRADE_SHORT_FIELD_WIDTH = 95;
    private static final int NAME_EDITOR_HEIGHT = 54;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;
    private static final String ACTION_COPY = "Copy";
    private static final String ACTION_RESET_FLAGS = "Reset Flags";
    private static final String ACTION_DISPLAY_PRESET = "Display Preset";
    private static final String ACTION_DUPLICATE_LAST = "Duplicate Last";
    private static final String ACTION_CLEAR_ALL = "Clear All";
    private static final String ACTION_SWAP_BUY_SELL = "Swap Buy/Sell";
    private static final String ACTION_RESET_TRADE = "Reset Trade";
    private static final int ACTION_BUTTON_WIDTH_MIN = 76;
    private static final int ACTION_BUTTON_WIDTH_MAX = 176;
    private static final int ACTION_BUTTON_ROW_RESERVE = 12;
    private static final int QUICK_BUTTON_WIDTH_MIN = 64;
    private static final int QUICK_BUTTON_WIDTH_MAX = 132;
    private static final int QUICK_BUTTON_ROW_RESERVE = 20;

    private SpawnEggSpecialDataSection() {
    }

    public static boolean supports(ItemStack stack) {
        return ItemEditorCapabilities.supportsSpawnEggData(stack);
    }

    public static FlowLayout build(SpecialDataPanelContext context) {
        ItemEditorState.SpecialData special = context.special();
        FlowLayout section = UiFactory.section(ItemEditorText.tr("special.spawn_egg.title"), Component.empty());
        section.child(buildEntityCard(context, special));
        section.child(buildNameCard(context, special));
        section.child(buildFlagsCard(context, special));
        section.child(buildValuesCard(context, special));
        if (isVillagerSelected(context, special.spawnEggEntityId)) {
            section.child(buildVillagerCard(context, special));
        }
        return section;
    }

    private static FlowLayout buildEntityCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.entity")).shadow(false));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        int entityActionWidth = resolveButtonWidth(context, 3, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
        row.child(
                UiFactory.textBox(
                                special.spawnEggEntityId,
                                value -> updateSelectedEntity(context, special, value)
                        )
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(ENTITY_ID_FIELD_WIDTH))
        );
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.openSearchablePicker(
                        ItemEditorText.str("special.spawn_egg.entity"),
                        "",
                        entityTypeIds(context),
                        id -> id,
                        id -> setSelectedEntity(context, special, id)
                )
        );
        pickButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(entityActionWidth));
        row.child(pickButton);
        card.child(row);

        FlowLayout quickRow = compactLayout ? UiFactory.column() : UiFactory.row();
        quickRow.gap(6);
        int quickButtonWidth = resolveButtonWidth(context, 6, QUICK_BUTTON_WIDTH_MIN, QUICK_BUTTON_WIDTH_MAX, QUICK_BUTTON_ROW_RESERVE);
        for (String quickEntityId : Arrays.asList(
                "minecraft:villager",
                "minecraft:zombie",
                "minecraft:skeleton",
                "minecraft:creeper",
                "minecraft:cow",
                "minecraft:pig"
        )) {
            ButtonComponent quickButton = UiFactory.button(Component.literal(shortEntityName(quickEntityId)), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    setSelectedEntity(context, special, quickEntityId)
            );
            quickButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(quickButtonWidth));
            quickButton.tooltip(List.of(Component.literal(quickEntityId)));
            quickRow.child(quickButton);
        }
        card.child(quickRow);
        return card;
    }

    private static void updateSelectedEntity(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            String rawValue
    ) {
        boolean wasVillager = isVillagerSelected(context, special.spawnEggEntityId);
        String normalized = IdFieldNormalizer.normalize(rawValue);
        context.mutate(() -> special.spawnEggEntityId = normalized);
        boolean isVillager = isVillagerSelected(context, special.spawnEggEntityId);
        if (wasVillager != isVillager) {
            context.screen().refreshCurrentPanel();
        }
    }

    private static void setSelectedEntity(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            String entityId
    ) {
        String normalized = IdFieldNormalizer.normalize(entityId);
        boolean wasVillager = isVillagerSelected(context, special.spawnEggEntityId);
        context.mutate(() -> special.spawnEggEntityId = normalized);
        boolean isVillager = isVillagerSelected(context, special.spawnEggEntityId);
        if (wasVillager != isVillager) {
            context.screen().refreshCurrentPanel();
        } else {
            context.screen().session().rebuildPreview();
        }
    }

    private static String shortEntityName(String entityId) {
        int separator = entityId.indexOf(':');
        String path = separator >= 0 ? entityId.substring(separator + 1) : entityId;
        return switch (path) {
            case "villager" -> "Villager";
            case "zombie" -> "Zombie";
            case "skeleton" -> "Skeleton";
            case "creeper" -> "Creeper";
            case "cow" -> "Cow";
            case "pig" -> "Pig";
            default -> path;
        };
    }

    private static boolean isVillagerSelected(SpecialDataPanelContext context, String rawEntityId) {
        EntityType<?> selectedType = resolveSelectedEntityType(context, rawEntityId);
        return selectedType == EntityType.VILLAGER;
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
        if (stack.getItem() instanceof SpawnEggItem spawnEggItem) {
            return spawnEggItem.getType(stack);
        }
        return null;
    }

    private static FlowLayout buildNameCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.name")).shadow(false));

        StyledTextFieldSection.BoundEditor nameSection = StyledTextFieldSection.create(
                context.screen(),
                RichTextDocument.fromMarkup(special.spawnEggCustomName),
                Sizing.fill(100),
                UiFactory.fixed(NAME_EDITOR_HEIGHT),
                ItemEditorText.str("special.spawn_egg.name.placeholder"),
                StyledTextFieldSection.StylePreset.name(),
                ItemEditorText.str("special.spawn_egg.name.color_title"),
                ItemEditorText.str("special.spawn_egg.name.gradient_title"),
                "",
                "",
                null,
                document -> document.logicalLineCount() > 1
                        ? ItemEditorText.str("special.spawn_egg.name.single_line")
                        : null,
                document -> context.mutate(() -> special.spawnEggCustomName = TextComponentUtil.serializeEditorDocument(document))
        );

        FlowLayout frame = UiFactory.framedEditorCard();
        frame.child(nameSection.toolbar());
        frame.child(nameSection.editor());
        frame.child(nameSection.validation());
        card.child(frame);
        return card;
    }

    private static FlowLayout buildFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.flags")).shadow(false));

        FlowLayout presets = compactLayout ? UiFactory.column() : UiFactory.row();
        presets.gap(6);
        int flagsActionWidth = resolveButtonWidth(context, 2, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
        ButtonComponent resetFlags = UiFactory.button(Component.literal(ACTION_RESET_FLAGS), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    special.spawnEggNoAi = false;
                    special.spawnEggSilent = false;
                    special.spawnEggNoGravity = false;
                    special.spawnEggGlowing = false;
                    special.spawnEggInvulnerable = false;
                    special.spawnEggPersistenceRequired = false;
                    special.spawnEggCustomNameVisible = false;
                })
        );
        resetFlags.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(flagsActionWidth));
        presets.child(resetFlags);
        ButtonComponent displayPreset = UiFactory.button(Component.literal(ACTION_DISPLAY_PRESET), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    special.spawnEggNoAi = true;
                    special.spawnEggSilent = true;
                    special.spawnEggNoGravity = true;
                    special.spawnEggInvulnerable = true;
                    special.spawnEggPersistenceRequired = true;
                })
        );
        displayPreset.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(flagsActionWidth));
        presets.child(displayPreset);
        card.child(presets);

        FlowLayout rowA = compactLayout ? UiFactory.column() : UiFactory.row();
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.no_ai"),
                special.spawnEggNoAi,
                value -> context.mutateRefresh(() -> special.spawnEggNoAi = value)
        ));
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.silent"),
                special.spawnEggSilent,
                value -> context.mutateRefresh(() -> special.spawnEggSilent = value)
        ));
        rowA.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.no_gravity"),
                special.spawnEggNoGravity,
                value -> context.mutateRefresh(() -> special.spawnEggNoGravity = value)
        ));
        card.child(rowA);

        FlowLayout rowB = compactLayout ? UiFactory.column() : UiFactory.row();
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.glowing"),
                special.spawnEggGlowing,
                value -> context.mutateRefresh(() -> special.spawnEggGlowing = value)
        ));
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.invulnerable"),
                special.spawnEggInvulnerable,
                value -> context.mutateRefresh(() -> special.spawnEggInvulnerable = value)
        ));
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.persistent"),
                special.spawnEggPersistenceRequired,
                value -> context.mutateRefresh(() -> special.spawnEggPersistenceRequired = value)
        ));
        rowB.child(UiFactory.checkbox(
                ItemEditorText.tr("special.spawn_egg.name_visible"),
                special.spawnEggCustomNameVisible,
                value -> context.mutateRefresh(() -> special.spawnEggCustomNameVisible = value)
        ));
        card.child(rowB);
        return card;
    }

    private static FlowLayout buildValuesCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.values")).shadow(false));

        card.child(UiFactory.field(
                ItemEditorText.tr("special.spawn_egg.health"),
                Component.empty(),
                UiFactory.textBox(
                        special.spawnEggHealth,
                        context.bindText(value -> special.spawnEggHealth = value)
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(HEALTH_FIELD_WIDTH))
        ));
        return card;
    }

    private static FlowLayout buildVillagerCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.title")).shadow(false));

        FlowLayout villagerDataRow = compactLayout ? UiFactory.column() : UiFactory.row();
        villagerDataRow.child(idFieldWithPicker(
                context,
                ItemEditorText.tr("special.spawn_egg.villager.type"),
                special.spawnEggVillagerTypeId,
                value -> special.spawnEggVillagerTypeId = value,
                villagerTypeIds(context),
                ItemEditorText.str("special.spawn_egg.villager.select_type")
        ));
        villagerDataRow.child(idFieldWithPicker(
                context,
                ItemEditorText.tr("special.spawn_egg.villager.profession"),
                special.spawnEggVillagerProfessionId,
                value -> special.spawnEggVillagerProfessionId = value,
                villagerProfessionIds(context),
                ItemEditorText.str("special.spawn_egg.villager.select_profession")
        ));
        villagerDataRow.child(UiFactory.field(
                ItemEditorText.tr("special.spawn_egg.villager.level"),
                Component.empty(),
                UiFactory.textBox(
                        special.spawnEggVillagerLevel,
                        context.bindText(value -> special.spawnEggVillagerLevel = value)
                ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(VILLAGER_LEVEL_FIELD_WIDTH))
        ));
        card.child(villagerDataRow);

        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.trades")).shadow(false));
        FlowLayout actions = compactLayout ? UiFactory.column() : UiFactory.row();
        actions.gap(6);
        int villagerActionCount = special.spawnEggVillagerTrades.isEmpty() ? 2 : 3;
        int villagerActionWidth = resolveButtonWidth(context, villagerActionCount, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
        ButtonComponent addTrade = UiFactory.button(ItemEditorText.tr("special.spawn_egg.villager.add_trade"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> special.spawnEggVillagerTrades.add(new ItemEditorState.VillagerTradeDraft()))
        );
        addTrade.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(villagerActionWidth));
        actions.child(addTrade);
        ButtonComponent duplicateLast = UiFactory.button(Component.literal(ACTION_DUPLICATE_LAST), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    if (special.spawnEggVillagerTrades.isEmpty()) {
                        return;
                    }
                    ItemEditorState.VillagerTradeDraft source = special.spawnEggVillagerTrades.getLast();
                    special.spawnEggVillagerTrades.add(copyTrade(source));
                })
        );
        duplicateLast.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(villagerActionWidth));
        actions.child(duplicateLast);
        if (!special.spawnEggVillagerTrades.isEmpty()) {
            ButtonComponent clearTrades = UiFactory.button(Component.literal(ACTION_CLEAR_ALL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(special.spawnEggVillagerTrades::clear)
            );
            clearTrades.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(villagerActionWidth));
            actions.child(clearTrades);
        }
        card.child(actions);

        if (special.spawnEggVillagerTrades.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.spawn_egg.villager.trades_empty")));
            return card;
        }

        List<String> itemIds = itemIds(context);
        for (int index = 0; index < special.spawnEggVillagerTrades.size(); index++) {
            int currentIndex = index;
            ItemEditorState.VillagerTradeDraft trade = special.spawnEggVillagerTrades.get(index);
            FlowLayout tradeCard = context.createReorderableCard(
                    ItemEditorText.tr("special.spawn_egg.villager.trade", index + 1),
                    currentIndex > 0,
                    () -> swapEntries(special.spawnEggVillagerTrades, currentIndex, currentIndex - 1),
                    currentIndex < special.spawnEggVillagerTrades.size() - 1,
                    () -> swapEntries(special.spawnEggVillagerTrades, currentIndex, currentIndex + 1),
                    () -> special.spawnEggVillagerTrades.remove(currentIndex)
            );

            FlowLayout summaryRow = compactLayout ? UiFactory.column() : UiFactory.row();
            summaryRow.gap(6);
            int summaryActionWidth = resolveButtonWidth(context, 2, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
            summaryRow.child(UiFactory.muted(Component.literal(tradeSummary(trade))));
            ButtonComponent collapseToggle = UiFactory.button(LayoutModeUtil.sectionToggleText(trade.uiCollapsed), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> trade.uiCollapsed = !trade.uiCollapsed)
            );
            collapseToggle.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(summaryActionWidth));
            summaryRow.child(collapseToggle);
            ButtonComponent duplicateTrade = UiFactory.button(Component.literal(ACTION_COPY), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> special.spawnEggVillagerTrades.add(currentIndex + 1, copyTrade(trade)))
            );
            duplicateTrade.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(summaryActionWidth));
            summaryRow.child(duplicateTrade);
            tradeCard.child(summaryRow);

            if (trade.uiCollapsed) {
                card.child(tradeCard);
                continue;
            }

            tradeCard.child(tradeItemField(
                    context,
                    ItemEditorText.tr("special.spawn_egg.villager.trade.buy"),
                    trade.buy,
                    itemIds,
                    ItemEditorText.str("special.spawn_egg.villager.trade.buy")
            ));
            tradeCard.child(tradeItemField(
                    context,
                    ItemEditorText.tr("special.spawn_egg.villager.trade.buy_b"),
                    trade.buyB,
                    itemIds,
                    ItemEditorText.str("special.spawn_egg.villager.trade.buy_b")
            ));
            tradeCard.child(tradeItemField(
                    context,
                    ItemEditorText.tr("special.spawn_egg.villager.trade.sell"),
                    trade.sell,
                    itemIds,
                    ItemEditorText.str("special.spawn_egg.villager.trade.sell")
            ));
            ButtonComponent swapItems = UiFactory.button(Component.literal(ACTION_SWAP_BUY_SELL), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> swapBuyAndSell(trade))
            );
            int tradeActionWidth = resolveButtonWidth(context, 1, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
            swapItems.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(tradeActionWidth));
            tradeCard.child(swapItems);

            FlowLayout valuesRow = compactLayout ? UiFactory.column() : UiFactory.row();
            valuesRow.child(shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.max_uses"), trade.maxUses, value -> trade.maxUses = value));
            valuesRow.child(shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.uses"), trade.uses, value -> trade.uses = value));
            valuesRow.child(shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.xp"), trade.villagerXp, value -> trade.villagerXp = value));
            valuesRow.child(shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.price_multiplier"), trade.priceMultiplier, value -> trade.priceMultiplier = value));
            valuesRow.child(shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.demand"), trade.demand, value -> trade.demand = value));
            valuesRow.child(shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.special_price"), trade.specialPrice, value -> trade.specialPrice = value));
            tradeCard.child(valuesRow);

            tradeCard.child(UiFactory.checkbox(
                    ItemEditorText.tr("special.spawn_egg.villager.trade.reward_exp"),
                    trade.rewardExp,
                    value -> context.mutate(() -> trade.rewardExp = value)
            ));

            ButtonComponent resetTrade = UiFactory.button(Component.literal(ACTION_RESET_TRADE), UiFactory.ButtonTextPreset.STANDARD,  button ->
                    context.mutateRefresh(() -> resetTrade(trade))
            );
            resetTrade.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(tradeActionWidth));
            tradeCard.child(resetTrade);

            card.child(tradeCard);
        }

        return card;
    }

    private static List<String> entityTypeIds(SpecialDataPanelContext context) {
        Registry<EntityType<?>> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE);
        return RegistryUtil.ids(registry);
    }

    private static List<String> villagerTypeIds(SpecialDataPanelContext context) {
        Registry<?> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.VILLAGER_TYPE);
        return RegistryUtil.ids(registry);
    }

    private static List<String> villagerProfessionIds(SpecialDataPanelContext context) {
        Registry<?> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.VILLAGER_PROFESSION);
        return RegistryUtil.ids(registry);
    }

    private static List<String> itemIds(SpecialDataPanelContext context) {
        Registry<Item> registry = context.screen().session().registryAccess().lookupOrThrow(Registries.ITEM);
        List<String> ids = new ArrayList<>(RegistryUtil.ids(registry));
        ids.removeIf(id -> Objects.equals(id, "minecraft:air"));
        return ids;
    }

    private static FlowLayout tradeItemField(
            SpecialDataPanelContext context,
            Component label,
            ItemEditorState.TradeStackDraft stackDraft,
            List<String> itemIds,
            String pickerTitle
    ) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout container = UiFactory.column();
        container.gap(2);
        container.child(UiFactory.muted(label));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(UiFactory.textBox(
                stackDraft.itemId,
                value -> context.mutate(() -> stackDraft.itemId = IdFieldNormalizer.normalize(value))
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(TRADE_ITEM_ID_FIELD_WIDTH)));
        row.child(UiFactory.textBox(
                stackDraft.count,
                context.bindText(value -> stackDraft.count = value)
        ).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(TRADE_ITEM_COUNT_FIELD_WIDTH)));
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        itemIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> stackDraft.itemId = id)
                )
        );
        int pickerButtonWidth = resolveButtonWidth(context, 4, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
        pickButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(pickerButtonWidth));
        row.child(pickButton);
        container.child(row);
        return container;
    }

    private static FlowLayout idFieldWithPicker(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter,
            List<String> values,
            String pickerTitle
    ) {
        return UiFactory.field(
                label,
                Component.empty(),
                idTextWithPicker(context, value, setter, values, pickerTitle)
        );
    }

    private static FlowLayout idTextWithPicker(
            SpecialDataPanelContext context,
            String value,
            Consumer<String> setter,
            List<String> entries,
            String pickerTitle
    ) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        row.child(
                UiFactory.textBox(value, text -> context.mutate(() -> setter.accept(IdFieldNormalizer.normalize(text))))
                .horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(VILLAGER_ID_WIDTH))
        );
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        entries,
                        id -> id,
                        id -> context.mutateRefresh(() -> setter.accept(id))
                )
        );
        int pickerButtonWidth = resolveButtonWidth(context, 3, ACTION_BUTTON_WIDTH_MIN, ACTION_BUTTON_WIDTH_MAX, ACTION_BUTTON_ROW_RESERVE);
        pickButton.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(pickerButtonWidth));
        row.child(pickButton);
        return row;
    }

    private static FlowLayout shortField(
            SpecialDataPanelContext context,
            Component label,
            String value,
            Consumer<String> setter
    ) {
        boolean compactLayout = isCompactLayout(context);
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter)).horizontalSizing(compactLayout ? Sizing.fill(100) : UiFactory.fixed(TRADE_SHORT_FIELD_WIDTH))
        );
    }

    private static boolean isCompactLayout(SpecialDataPanelContext context) {
        return LayoutModeUtil.isCompactPanel(context.guiScale(), context.panelWidthHint(), COMPACT_LAYOUT_WIDTH_THRESHOLD);
    }

    private static <T> void swapEntries(List<T> drafts, int left, int right) {
        if (left < 0 || right < 0 || left >= drafts.size() || right >= drafts.size()) {
            return;
        }
        T draft = drafts.get(left);
        drafts.set(left, drafts.get(right));
        drafts.set(right, draft);
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
        String buyId = trade.buy.itemId == null || trade.buy.itemId.isBlank() ? "?" : trade.buy.itemId;
        String buyCount = trade.buy.count == null || trade.buy.count.isBlank() ? "1" : trade.buy.count;
        String sellId = trade.sell.itemId == null || trade.sell.itemId.isBlank() ? "?" : trade.sell.itemId;
        String sellCount = trade.sell.count == null || trade.sell.count.isBlank() ? "1" : trade.sell.count;
        return buyCount + "x " + buyId + " -> " + sellCount + "x " + sellId;
    }

    private static int resolveButtonWidth(
            SpecialDataPanelContext context,
            int buttonCount,
            int minWidth,
            int maxWidth,
            int rowReserve
    ) {
        int contentWidth = Math.max(1, context.panelWidthHint());
        int preferred = Math.max(
                minWidth,
                Math.min(
                        maxWidth,
                        (contentWidth - UiFactory.scaledPixels(rowReserve)) / Math.max(1, buttonCount)
                )
        );
        return Math.max(1, Math.min(contentWidth, preferred));
    }
}
