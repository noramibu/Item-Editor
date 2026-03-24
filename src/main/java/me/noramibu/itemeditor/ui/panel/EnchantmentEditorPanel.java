package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.ui.component.UiFactory;
import me.noramibu.itemeditor.ui.screen.ItemEditorScreen;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RegistryUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Collections;
import java.util.List;

public final class EnchantmentEditorPanel implements EditorPanel {

    private final ItemEditorScreen screen;

    public EnchantmentEditorPanel(ItemEditorScreen screen) {
        this.screen = screen;
    }

    @Override
    public UIComponent build() {
        ItemEditorState state = this.screen.session().state();
        Registry<Enchantment> enchantmentRegistry = this.screen.session().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<String> enchantmentIds = RegistryUtil.ids(enchantmentRegistry);
        FlowLayout root = UiFactory.column();

        FlowLayout intro = UiFactory.section(
                ItemEditorText.tr("enchantments.title"),
                Component.empty()
        );
        intro.child(UiFactory.checkbox(ItemEditorText.tr("enchantments.unsafe"), state.unsafeEnchantments, PanelBindings.toggle(this.screen, value -> state.unsafeEnchantments = value)));
        root.child(intro);

        root.child(this.buildListSection(
                "enchantments.regular.title",
                "enchantments.regular.entry",
                state.enchantments,
                false,
                enchantmentIds
        ));
        root.child(this.buildListSection(
                "enchantments.stored.title",
                "enchantments.stored.entry",
                state.storedEnchantments,
                true,
                enchantmentIds
        ));
        return root;
    }

    private FlowLayout buildListSection(
            String titleKey,
            String entryKey,
            List<ItemEditorState.EnchantmentDraft> drafts,
            boolean stored,
            List<String> enchantmentIds
    ) {
        FlowLayout section = UiFactory.section(ItemEditorText.tr(titleKey), Component.empty());

        section.child(UiFactory.button(stored ? ItemEditorText.tr("enchantments.stored.add") : ItemEditorText.tr("enchantments.regular.add"), button -> {
            PanelBindings.mutateRefresh(this.screen, () -> drafts.add(new ItemEditorState.EnchantmentDraft()));
        }));

        for (int index = 0; index < drafts.size(); index++) {
            int currentIndex = index;
            ItemEditorState.EnchantmentDraft draft = drafts.get(currentIndex);
            FlowLayout card = UiFactory.reorderableSubCard(
                    ItemEditorText.tr(entryKey, index + 1),
                    currentIndex > 0,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(drafts, currentIndex, currentIndex - 1)),
                    currentIndex < drafts.size() - 1,
                    () -> PanelBindings.mutateRefresh(this.screen, () -> Collections.swap(drafts, currentIndex, currentIndex + 1)),
                    () -> PanelBindings.mutateRefresh(this.screen, () -> drafts.remove(currentIndex))
            );

            ButtonComponent enchantmentButton = UiFactory.pickerButton(
                    draft.enchantmentId.isBlank() ? ItemEditorText.tr("enchantments.select") : Component.literal(draft.enchantmentId),
                    -1,
                    button -> this.screen.openSearchablePickerDialog(
                            ItemEditorText.str("enchantments.entry.enchantment"),
                            "",
                            enchantmentIds,
                            id -> id,
                            id -> PanelBindings.mutate(this.screen, () -> draft.enchantmentId = id)
                    )
            );

            FlowLayout row = UiFactory.row();
            row.child(UiFactory.field(
                    ItemEditorText.tr("enchantments.entry.enchantment"),
                    Component.empty(),
                    enchantmentButton
            ));
            row.child(UiFactory.field(
                    ItemEditorText.tr("enchantments.entry.level"),
                    Component.empty(),
                    UiFactory.textBox(draft.level, PanelBindings.text(this.screen, value -> draft.level = value)).horizontalSizing(Sizing.fixed(100))
            ));
            card.child(row);
            section.child(card);
        }

        return section;
    }
}
