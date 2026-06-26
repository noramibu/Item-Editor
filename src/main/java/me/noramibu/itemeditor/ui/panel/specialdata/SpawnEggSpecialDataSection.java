package me.noramibu.itemeditor.ui.panel.specialdata;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.storage.StorageSortMode;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.StorageScreen;
import me.noramibu.itemeditor.ui.screen.StorageScreenMode;
import me.noramibu.itemeditor.ui.util.LayoutModeUtil;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
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
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class SpawnEggSpecialDataSection {

    private static final int VILLAGER_ID_WIDTH = 220;
    private static final int ENTITY_ID_FIELD_WIDTH = 280;
    private static final int COMPACT_LAYOUT_WIDTH_THRESHOLD = 620;
    private static final String ACTION_DUPLICATE = "Duplicate";
    private static final String ACTION_RESET_FLAGS = "Reset Flags";
    private static final String ACTION_DISPLAY_PRESET = "Display Preset";
    private static final String ACTION_CLEAR_ALL = "Clear All";
    private static final String ACTION_SWAP_BUY_SELL = "Swap Buy/Sell";
    private static final String ACTION_RESET_TRADE = "Reset Trade";
    private static final int ACTION_BUTTON_WIDTH_MAX = 176;
    private static final int ACTION_BUTTON_ROW_RESERVE = 12;

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
        if (isVillagerSelected(context, special.spawnEggEntity.entityId)) {
            section.child(buildVillagerCard(context, special));
        }
        return section;
    }

    private static FlowLayout buildEntityCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.entity")).shadow(false));

        FlowLayout row = compactLayout ? UiFactory.column() : UiFactory.row();
        int entityActionWidth = resolveButtonWidth(context, 3);
        row.child(
                UiFactory.textBox(
                                special.spawnEggEntity.entityId,
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
        return card;
    }

    private static void updateSelectedEntity(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            String rawValue
    ) {
        String previousEntityId = special.spawnEggEntity.entityId;
        boolean wasVillager = isVillagerSelected(context, special.spawnEggEntity.entityId);
        String normalized = IdFieldNormalizer.normalize(rawValue);
        context.mutate(() -> special.spawnEggEntity.entityId = normalized);
        boolean isVillager = isVillagerSelected(context, special.spawnEggEntity.entityId);
        if (wasVillager != isVillager || variantFieldsChanged(context, previousEntityId, normalized)) {
            context.screen().refreshCurrentPanel();
        }
    }

    private static void setSelectedEntity(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            String entityId
    ) {
        String previousEntityId = special.spawnEggEntity.entityId;
        String normalized = IdFieldNormalizer.normalize(entityId);
        boolean wasVillager = isVillagerSelected(context, special.spawnEggEntity.entityId);
        context.mutate(() -> special.spawnEggEntity.entityId = normalized);
        boolean isVillager = isVillagerSelected(context, special.spawnEggEntity.entityId);
        if (wasVillager != isVillager || variantFieldsChanged(context, previousEntityId, normalized)) {
            context.screen().refreshCurrentPanel();
        } else {
            context.screen().session().rebuildPreview();
        }
    }

    private static boolean variantFieldsChanged(
            SpecialDataPanelContext context,
            String previousEntityId,
            String nextEntityId
    ) {
        return !Objects.equals(
                variantFieldGroup(context, previousEntityId),
                variantFieldGroup(context, nextEntityId)
        );
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
                    "minecraft:wolf",
                    "minecraft:zombie_nautilus" -> entityId;
            default -> "";
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
        card.child(EntitySpawnDataUi.nameEditor(
                context,
                special.spawnEggEntity,
                ItemEditorText.tr("special.spawn_egg.name"),
                "special.spawn_egg.name.placeholder",
                "special.spawn_egg.name.color_title",
                "special.spawn_egg.name.gradient_title"
        ));
        return card;
    }

    private static FlowLayout buildFlagsCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.flags")).shadow(false));

        FlowLayout presets = compactLayout ? UiFactory.column() : UiFactory.row();
        presets.gap(6);
        int flagsActionWidth = resolveButtonWidth(context, 2);
        ButtonComponent resetFlags = UiFactory.button(Component.literal(ACTION_RESET_FLAGS), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    special.spawnEggEntity.noAi = false;
                    special.spawnEggEntity.silent = false;
                    special.spawnEggEntity.noGravity = false;
                    special.spawnEggEntity.glowing = false;
                    special.spawnEggEntity.invulnerable = false;
                    special.spawnEggEntity.persistenceRequired = false;
                    special.spawnEggEntity.customNameVisible = false;
                })
        );
        resetFlags.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(flagsActionWidth));
        presets.child(resetFlags);
        ButtonComponent displayPreset = UiFactory.button(Component.literal(ACTION_DISPLAY_PRESET), UiFactory.ButtonTextPreset.STANDARD,  button ->
                context.mutateRefresh(() -> {
                    special.spawnEggEntity.noAi = true;
                    special.spawnEggEntity.silent = true;
                    special.spawnEggEntity.noGravity = true;
                    special.spawnEggEntity.invulnerable = true;
                    special.spawnEggEntity.persistenceRequired = true;
                })
        );
        displayPreset.horizontalSizing(compactLayout ? Sizing.fill(100) : Sizing.fixed(flagsActionWidth));
        presets.child(displayPreset);
        card.child(presets);
        card.child(EntitySpawnDataUi.flags(context, special.spawnEggEntity, compactLayout ? 2 : 3));
        return card;
    }

    private static FlowLayout buildValuesCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.values")).shadow(false));
        card.child(EntitySpawnDataUi.health(context, special.spawnEggEntity, compactLayout));
        return card;
    }

    private static FlowLayout buildVillagerCard(SpecialDataPanelContext context, ItemEditorState.SpecialData special) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout card = UiFactory.subCard();
        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.title")).shadow(false));

        FlowLayout villagerDataRow = compactLayout ? UiFactory.column() : UiFactory.row();
        FlowLayout typeField = idFieldWithPicker(
                context,
                ItemEditorText.tr("special.spawn_egg.villager.type"),
                special.spawnEggVillagerTypeId,
                value -> special.spawnEggVillagerTypeId = value,
                villagerTypeIds(context),
                ItemEditorText.str("special.spawn_egg.villager.select_type")
        );
        typeField.horizontalSizing(Sizing.fill(compactLayout ? 100 : 40));
        villagerDataRow.child(typeField);
        FlowLayout professionField = idFieldWithPicker(
                context,
                ItemEditorText.tr("special.spawn_egg.villager.profession"),
                special.spawnEggVillagerProfessionId,
                value -> special.spawnEggVillagerProfessionId = value,
                villagerProfessionIds(context),
                ItemEditorText.str("special.spawn_egg.villager.select_profession")
        );
        professionField.horizontalSizing(Sizing.fill(compactLayout ? 100 : 40));
        villagerDataRow.child(professionField);
        FlowLayout levelField = UiFactory.field(
                ItemEditorText.tr("special.spawn_egg.villager.level"),
                Component.empty(),
                UiFactory.textBox(
                        special.spawnEggVillagerLevel,
                        context.bindText(value -> special.spawnEggVillagerLevel = value)
                )
        );
        levelField.horizontalSizing(Sizing.fill(compactLayout ? 100 : 14));
        villagerDataRow.child(levelField);
        card.child(villagerDataRow);

        card.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.trades")).shadow(false));
        ButtonComponent addTrade = UiFactory.button(ItemEditorText.tr("special.spawn_egg.villager.add_trade"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.spawnEggVillagerTrades.add(new ItemEditorState.VillagerTradeDraft()))
        );
        ButtonComponent clearTrades = null;
        if (!special.spawnEggVillagerTrades.isEmpty()) {
            clearTrades = UiFactory.button(Component.literal(ACTION_CLEAR_ALL), UiFactory.ButtonTextPreset.STANDARD, button ->
                    context.mutateRefresh(special.spawnEggVillagerTrades::clear)
            );
        }
        card.child(UiFactory.actionButtonRow(addTrade, clearTrades));

        if (special.spawnEggVillagerTrades.isEmpty()) {
            card.child(UiFactory.muted(ItemEditorText.tr("special.spawn_egg.villager.trades_empty")));
            return card;
        }

        ButtonComponent expandAll = UiFactory.button(ItemEditorText.tr("common.expand_all"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.spawnEggVillagerTrades.forEach(trade -> trade.uiCollapsed = false))
        );
        ButtonComponent collapseAll = UiFactory.button(ItemEditorText.tr("common.collapse_all"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.spawnEggVillagerTrades.forEach(trade -> trade.uiCollapsed = true))
        );
        card.child(UiFactory.actionButtonRow(expandAll, collapseAll));

        List<String> itemIds = itemIds(context);
        for (int index = 0; index < special.spawnEggVillagerTrades.size(); index++) {
            FlowLayout tradeCard = tradeCard(context, special, index);
            ItemEditorState.VillagerTradeDraft trade = special.spawnEggVillagerTrades.get(index);

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
            ButtonComponent swapItems = UiFactory.button(Component.literal(ACTION_SWAP_BUY_SELL), UiFactory.ButtonTextPreset.STANDARD, button ->
                    context.mutateRefresh(() -> swapBuyAndSell(trade))
            );
            ButtonComponent resetTrade = UiFactory.button(Component.literal(ACTION_RESET_TRADE), UiFactory.ButtonTextPreset.STANDARD, button ->
                    context.mutateRefresh(() -> resetTrade(trade))
            );
            tradeCard.child(UiFactory.actionButtonRow(swapItems, resetTrade));

            tradeCard.child(tradeValueRows(context, trade));

            tradeCard.child(UiFactory.checkbox(
                    ItemEditorText.tr("special.spawn_egg.villager.trade.reward_exp"),
                    trade.rewardExp,
                    value -> context.mutate(() -> trade.rewardExp = value)
            ));

            card.child(tradeCard);
        }

        return card;
    }

    private static FlowLayout tradeCard(
            SpecialDataPanelContext context,
            ItemEditorState.SpecialData special,
            int currentIndex
    ) {
        ItemEditorState.VillagerTradeDraft trade = special.spawnEggVillagerTrades.get(currentIndex);
        FlowLayout card = UiFactory.subCard();
        FlowLayout titleRow = UiFactory.row();
        titleRow.child(UiFactory.title(ItemEditorText.tr("special.spawn_egg.villager.trade", currentIndex + 1))
                .shadow(false)
                .horizontalSizing(Sizing.expand(100)));
        titleRow.child(UiFactory.collapseToggleButton(
                trade.uiCollapsed,
                () -> context.mutateRefresh(() -> trade.uiCollapsed = !trade.uiCollapsed)
        ));
        card.child(titleRow);
        card.child(UiFactory.muted(Component.literal(tradeSummary(trade))));

        ButtonComponent upButton = UiFactory.button(ItemEditorText.tr("common.up"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> swapEntries(special.spawnEggVillagerTrades, currentIndex, currentIndex - 1))
        );
        upButton.active(currentIndex > 0);
        ButtonComponent downButton = UiFactory.button(ItemEditorText.tr("common.down"), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> swapEntries(special.spawnEggVillagerTrades, currentIndex, currentIndex + 1))
        );
        downButton.active(currentIndex < special.spawnEggVillagerTrades.size() - 1);
        ButtonComponent duplicateButton = UiFactory.button(Component.literal(ACTION_DUPLICATE), UiFactory.ButtonTextPreset.COMPACT, button ->
                context.mutateRefresh(() -> special.spawnEggVillagerTrades.add(currentIndex + 1, copyTrade(trade)))
        );
        ButtonComponent removeButton = UiFactory.negativeButton(ItemEditorText.tr("common.remove"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.mutateRefresh(() -> special.spawnEggVillagerTrades.remove(currentIndex))
        );
        card.child(UiFactory.actionButtonRow(upButton, downButton, duplicateButton, removeButton));
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
                value -> context.mutate(() -> {
                    stackDraft.itemId = IdFieldNormalizer.normalize(value);
                    stackDraft.templateStack = ItemStack.EMPTY;
                })
        ).horizontalSizing(Sizing.fill(compactLayout ? 100 : 50)));
        row.child(UiFactory.textBox(
                stackDraft.count,
                context.bindText(value -> stackDraft.count = value)
        ).horizontalSizing(Sizing.fill(compactLayout ? 100 : 10)));
        ButtonComponent pickButton = UiFactory.button(ItemEditorText.tr("common.pick"), UiFactory.ButtonTextPreset.STANDARD, button ->
                context.openSearchablePicker(
                        pickerTitle,
                        "",
                        itemIds,
                        id -> id,
                        id -> context.mutateRefresh(() -> {
                            stackDraft.itemId = id;
                            stackDraft.templateStack = ItemStack.EMPTY;
                        })
                )
        );
        pickButton.horizontalSizing(Sizing.fill(compactLayout ? 100 : 15));
        row.child(pickButton);
        ButtonComponent inventoryPickButton = UiFactory.button(
                ItemEditorText.tr("special.spawn_egg.villager.trade.pick_inventory"),
                UiFactory.ButtonTextPreset.STANDARD,
                button -> {
                    double panelScroll = context.screen().panelScrollOffset();
                    context.screen().session().minecraft().setScreen(new StorageScreen(
                            1,
                            "",
                            StorageSortMode.REGULAR,
                            StorageScreenMode.PICK_FOR_EDIT,
                            context.screen(),
                            stack -> {
                                context.mutateRefresh(() -> setTradeStackFromInventory(stackDraft, stack));
                                context.screen().preservePanelScrollOnNextBuild(panelScroll);
                                context.screen().restorePanelScroll(panelScroll);
                            }
                    ));
                }
        );
        inventoryPickButton.horizontalSizing(Sizing.fill(compactLayout ? 100 : 25));
        row.child(inventoryPickButton);
        container.child(row);
        return container;
    }

    private static void setTradeStackFromInventory(
            ItemEditorState.TradeStackDraft stackDraft,
            ItemStack stack
    ) {
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
        int pickerButtonWidth = resolveButtonWidth(context, 3);
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
        return UiFactory.field(
                label,
                Component.empty(),
                UiFactory.textBox(value, context.bindText(setter))
        );
    }

    private static FlowLayout tradeValueRows(
            SpecialDataPanelContext context,
            ItemEditorState.VillagerTradeDraft trade
    ) {
        boolean compactLayout = isCompactLayout(context);
        FlowLayout rows = UiFactory.column();
        rows.child(tradeValueRow(
                compactLayout,
                shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.max_uses"), trade.maxUses, value -> trade.maxUses = value),
                shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.uses"), trade.uses, value -> trade.uses = value),
                shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.xp"), trade.villagerXp, value -> trade.villagerXp = value)
        ));
        rows.child(tradeValueRow(
                compactLayout,
                shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.price_multiplier"), trade.priceMultiplier, value -> trade.priceMultiplier = value),
                shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.demand"), trade.demand, value -> trade.demand = value),
                shortField(context, ItemEditorText.tr("special.spawn_egg.villager.trade.special_price"), trade.specialPrice, value -> trade.specialPrice = value)
        ));
        return rows;
    }

    private static FlowLayout tradeValueRow(boolean compactLayout, FlowLayout first, FlowLayout second, FlowLayout third) {
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
        String buyId = trade.buy.itemId == null || trade.buy.itemId.isBlank() ? "?" : trade.buy.itemId;
        String buyCount = trade.buy.count == null || trade.buy.count.isBlank() ? "1" : trade.buy.count;
        String sellId = trade.sell.itemId == null || trade.sell.itemId.isBlank() ? "?" : trade.sell.itemId;
        String sellCount = trade.sell.count == null || trade.sell.count.isBlank() ? "1" : trade.sell.count;
        return buyCount + "x " + buyId + " -> " + sellCount + "x " + sellId;
    }

    private static int resolveButtonWidth(
            SpecialDataPanelContext context,
            int buttonCount
    ) {
        int contentWidth = Math.max(1, context.panelWidthHint());
        int preferred = Math.min(
                ACTION_BUTTON_WIDTH_MAX,
                (contentWidth - UiFactory.scaledPixels(ACTION_BUTTON_ROW_RESERVE)) / Math.max(1, buttonCount)
        );
        return Math.clamp(preferred, 1, contentWidth);
    }
}
