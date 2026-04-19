package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.ui.panel.AttributeEditorPanel;
import me.noramibu.itemeditor.ui.panel.BookEditorPanel;
import me.noramibu.itemeditor.ui.panel.ComponentsEditorPanel;
import me.noramibu.itemeditor.ui.panel.DisplayEditorPanel;
import me.noramibu.itemeditor.ui.panel.EnchantmentEditorPanel;
import me.noramibu.itemeditor.ui.panel.FlagsEditorPanel;
import me.noramibu.itemeditor.ui.panel.GeneralEditorPanel;
import me.noramibu.itemeditor.ui.panel.RawEditorPanel;
import me.noramibu.itemeditor.ui.panel.SpecialDataEditorPanel;
import me.noramibu.itemeditor.util.ItemEditorCapabilities;

import java.util.List;

public final class EditorModuleRegistry {

    private static final List<EditorModule> MODULES = List.of(
            new EditorModule(EditorCategory.GENERAL, session -> true, GeneralEditorPanel::new),
            new EditorModule(EditorCategory.COMPONENTS, session -> ItemEditorCapabilities.supportsComponents(session.originalStack(), session.registryAccess()), ComponentsEditorPanel::new),
            new EditorModule(EditorCategory.DISPLAY, session -> true, DisplayEditorPanel::new),
            new EditorModule(EditorCategory.ATTRIBUTES, session -> true, AttributeEditorPanel::new),
            new EditorModule(EditorCategory.ENCHANTMENTS, session -> true, EnchantmentEditorPanel::new),
            new EditorModule(EditorCategory.FLAGS, session -> true, FlagsEditorPanel::new),
            new EditorModule(EditorCategory.BOOK, session -> ItemEditorCapabilities.supportsBook(session.originalStack()), BookEditorPanel::new),
            new EditorModule(EditorCategory.RAW_EDITOR, session -> true, RawEditorPanel::new),
            new EditorModule(EditorCategory.SPECIAL_DATA, session -> ItemEditorCapabilities.supportsSpecialData(session.originalStack()), SpecialDataEditorPanel::new)
    );

    private EditorModuleRegistry() {
    }

    public static List<EditorModule> modules() {
        return MODULES;
    }
}
