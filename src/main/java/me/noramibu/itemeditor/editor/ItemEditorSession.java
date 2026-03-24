package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.service.ItemApplyService;
import me.noramibu.itemeditor.service.ItemEditorStateMapper;
import me.noramibu.itemeditor.service.ItemPreviewService;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class ItemEditorSession {

    private final Minecraft minecraft;
    private final ItemStack originalStack;
    private final ItemEditorStateMapper stateMapper;
    private final ItemPreviewService previewService;
    private final ItemApplyService applyService;
    private final List<Runnable> listeners = new ArrayList<>();
    private final ItemEditorState baselineState;

    private ItemEditorState state;
    private final ItemStack cleanPreviewStack;
    private ItemStack previewStack;
    private List<ValidationMessage> messages = List.of();
    private boolean dirty;

    public ItemEditorSession(Minecraft minecraft, ItemStack originalStack) {
        this.minecraft = minecraft;
        this.originalStack = originalStack.copy();
        this.stateMapper = new ItemEditorStateMapper();
        this.previewService = new ItemPreviewService();
        this.applyService = new ItemApplyService();
        this.baselineState = this.stateMapper.map(this.originalStack);
        this.state = this.stateMapper.map(this.originalStack);
        ItemPreviewService.PreviewBuildResult initialResult = this.previewService.buildPreview(this.originalStack, this.state, this.baselineState, this.registryAccess());
        this.previewStack = initialResult.previewStack();
        this.messages = List.copyOf(initialResult.messages());
        this.cleanPreviewStack = this.previewStack.copy();
        this.dirty = false;
    }

    public Minecraft minecraft() {
        return this.minecraft;
    }

    public ItemStack originalStack() {
        return this.originalStack.copy();
    }

    public ItemStack previewStack() {
        return this.previewStack.copy();
    }

    public ItemEditorState state() {
        return this.state;
    }

    public List<ValidationMessage> messages() {
        return this.messages;
    }

    public boolean hasErrors() {
        return this.messages.stream().anyMatch(message -> message.severity() == ValidationMessage.Severity.ERROR);
    }

    public boolean dirty() {
        return this.dirty;
    }

    public void rebuildPreview() {
        ItemPreviewService.PreviewBuildResult result = this.previewService.buildPreview(this.originalStack, this.state, this.baselineState, this.registryAccess());
        this.previewStack = result.previewStack();
        this.messages = List.copyOf(result.messages());
        this.dirty = !ItemStack.isSameItemSameComponents(this.cleanPreviewStack, this.previewStack)
                || this.cleanPreviewStack.getCount() != this.previewStack.getCount();
        this.notifyListeners();
    }

    public void reset() {
        this.state = this.stateMapper.map(this.originalStack);
        this.rebuildPreview();
    }

    public ItemApplyService.ApplyResult apply() {
        return this.applyService.apply(this.minecraft, this.previewStack);
    }

    public void addListener(Runnable listener) {
        this.listeners.add(listener);
    }

    public RegistryAccess registryAccess() {
        if (this.minecraft.level != null) {
            return this.minecraft.level.registryAccess();
        }
        return RegistryAccess.EMPTY;
    }

    private void notifyListeners() {
        this.listeners.forEach(Runnable::run);
    }
}
