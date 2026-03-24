package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ItemEditorState;
import me.noramibu.itemeditor.util.TextComponentUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class DisplayPreviewApplier extends AbstractPreviewApplierSupport implements ItemPreviewApplier {

    @Override
    public void apply(ItemPreviewApplyContext context) {
        if (this.sameLore(context.state().loreLines, context.baselineState().loreLines)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.LORE);
            return;
        }

        if (context.state().loreLines.isEmpty()) {
            this.clearToPrototype(context.previewStack(), DataComponents.LORE);
            return;
        }

        List<Component> lore = new ArrayList<>();
        for (ItemEditorState.LoreLineDraft line : context.state().loreLines) {
            Component component = TextComponentUtil.applyLineStyle(TextComponentUtil.parseMarkup(line.rawText), line.style, context.messages());
            lore.add(this.withPlainBaseline(component));
        }
        context.previewStack().set(DataComponents.LORE, new ItemLore(lore));
    }

    private boolean sameLore(List<ItemEditorState.LoreLineDraft> current, List<ItemEditorState.LoreLineDraft> baseline) {
        return this.sameList(current, baseline, (left, right) -> Objects.equals(left.rawText, right.rawText)
                && Objects.equals(left.style.colorHex, right.style.colorHex)
                && left.style.bold == right.style.bold
                && left.style.italic == right.style.italic
                && left.style.underlined == right.style.underlined
                && left.style.strikethrough == right.style.strikethrough
                && left.style.obfuscated == right.style.obfuscated);
    }
}
