package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;

final class DisplayPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        ItemLore originalLore = context.originalStack().get(DataComponents.LORE);
        if (this.sameLoreAsOriginal(context.state().loreLines, originalLore)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.LORE);
            return;
        }

        if (context.state().loreLines.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.LORE);
            return;
        }

        List<Component> lore = new ArrayList<>();
        for (int index = 0; index < context.state().loreLines.size(); index++) {
            ItemEditorState.LoreLineDraft line = context.state().loreLines.get(index);
            if (this.canReuseStoredOriginalLine(line)) {
                lore.add(line.originalComponent.copy());
            } else if (this.canReuseOriginalLine(line, originalLore, index)) {
                lore.add(originalLore.lines().get(index));
            } else {
                lore.add(this.rebuiltLoreComponent(line, context.messages()));
            }
        }
        context.previewStack().set(DataComponents.LORE, new ItemLore(lore));
    }

    private boolean sameLoreAsOriginal(List<ItemEditorState.LoreLineDraft> lines, ItemLore originalLore) {
        if (originalLore == null || lines.size() != originalLore.lines().size()) {
            return false;
        }
        for (int index = 0; index < lines.size(); index++) {
            if (!this.sameDraftAsComponent(lines.get(index), originalLore.lines().get(index))) {
                return false;
            }
        }
        return true;
    }

    private boolean canReuseStoredOriginalLine(ItemEditorState.LoreLineDraft line) {
        if (line.originalComponent == null) {
            return false;
        }
        return this.sameDraftAsComponent(line, line.originalComponent);
    }

    private boolean canReuseOriginalLine(ItemEditorState.LoreLineDraft line, ItemLore originalLore, int index) {
        return originalLore != null
                && index < originalLore.lines().size()
                && this.sameDraftAsComponent(line, originalLore.lines().get(index));
    }

    private boolean sameDraftAsComponent(ItemEditorState.LoreLineDraft draft, Component component) {
        return TextComponentUtil.sameVisibleContent(this.rebuiltLoreComponent(draft, new ArrayList<>()), component);
    }

    private Component rebuiltLoreComponent(ItemEditorState.LoreLineDraft draft, List<ValidationMessage> messages) {
        Component compact = TextComponentUtil.compactStyleFlags(this.componentFromDraft(draft, messages));
        if (compact.getStyle().isItalic()) {
            return compact;
        }
        return compact.copy().withStyle(compact.getStyle().withItalic(false));
    }

    private Component componentFromDraft(ItemEditorState.LoreLineDraft draft, List<ValidationMessage> messages) {
        return TextComponentUtil.parseStyledLine(draft.rawText, draft.style, messages);
    }
}
