package me.noramibu.itemeditor.ui.panel;

import io.wispforest.owo.ui.core.UIComponent;
import java.util.List;
import me.noramibu.itemeditor.ui.component.EditorSearchDialog.Target;

public interface EditorPanel {

    UIComponent build();

    List<Target> searchTargets();
}
