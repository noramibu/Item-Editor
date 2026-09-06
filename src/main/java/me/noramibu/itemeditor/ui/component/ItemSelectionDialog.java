package me.noramibu.itemeditor.ui.component;

import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Sizing;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.world.item.ItemStack;

public final class ItemSelectionDialog {
    private ItemSelectionDialog() {}

    public static FlowLayout create(ItemStack stack, Runnable use, Runnable edit, Runnable cancel) {
        int width = DialogUiUtil.dialogWidth(360);
        FlowLayout overlay = DialogUiUtil.overlay();
        FlowLayout dialog = UiFactory.centeredCard(width);
        dialog.child(
                UiFactory.title(ItemEditorText.tr("dialog.picked_item.title")).horizontalSizing(Sizing.fill(100)));
        dialog.child(UIComponents.item(stack).showOverlay(true).setTooltipFromStack(true));
        dialog.child(UiFactory.muted(stack.getHoverName()).horizontalSizing(Sizing.fill(100)));
        dialog.child(DialogUiUtil.footerRowByDivisor(
                width,
                DialogUiUtil.compactButtons(width, 320),
                72,
                120,
                3,
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.cancel"), button -> cancel.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("common.edit"), button -> edit.run()),
                new DialogUiUtil.FooterAction(ItemEditorText.tr("dialog.picked_item.use"), button -> use.run())));
        overlay.child(dialog);
        return overlay;
    }
}
