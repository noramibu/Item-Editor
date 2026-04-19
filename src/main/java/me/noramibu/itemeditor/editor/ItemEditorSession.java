package me.noramibu.itemeditor.editor;

import me.noramibu.itemeditor.service.ItemApplyService;
import me.noramibu.itemeditor.service.ItemEditorStateMapper;
import me.noramibu.itemeditor.service.ItemPreviewService;
import me.noramibu.itemeditor.storage.RawEditorOptionsService;
import me.noramibu.itemeditor.storage.model.RawEditorOptions;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class ItemEditorSession {
    private static final long RAW_TYPING_REBUILD_MIN_INTERVAL_NANOS = 130_000_000L;
    private static final Set<String> RAW_EDITOR_FIELDS = Set.of(
            "rawEditorEdited",
            "rawEditorText",
            "rawEditorShowDefaults",
            "rawEditorWordWrap",
            "rawEditorHorizontalScroll",
            "rawEditorFontSizePercent",
            "rawEditorOptionsLoaded"
    );

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
    private boolean rebuildQueued;
    private long queuedRebuildAtNanos;
    private long lastPreviewRebuildAtNanos;
    private String cachedRawPreviewInput;
    private ItemPreviewService.PreviewBuildResult cachedRawPreviewResult;
    private String cachedRawParsedInput;
    private RawItemDataUtil.ParseResult cachedRawParsedResult;

    public ItemEditorSession(Minecraft minecraft, ItemStack originalStack) {
        this.minecraft = minecraft;
        this.originalStack = originalStack.copy();
        this.stateMapper = new ItemEditorStateMapper();
        this.previewService = new ItemPreviewService();
        this.applyService = new ItemApplyService();
        this.baselineState = this.stateMapper.map(this.originalStack, this.registryAccess());
        this.state = this.stateMapper.map(this.originalStack, this.registryAccess());
        String initialRaw = RawItemDataUtil.serialize(this.originalStack, this.registryAccess());
        this.baselineState.rawEditorText = initialRaw;
        this.state.rawEditorText = initialRaw;
        this.applySavedRawEditorOptions(this.baselineState);
        this.applySavedRawEditorOptions(this.state);
        ItemPreviewService.PreviewBuildResult initialResult = this.previewService.buildPreview(this.originalStack, this.state, this.baselineState, this.registryAccess());
        this.previewStack = initialResult.previewStack();
        this.messages = List.copyOf(initialResult.messages());
        this.cleanPreviewStack = this.previewStack.copy();
        this.dirty = false;
        this.lastPreviewRebuildAtNanos = System.nanoTime();
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

    public boolean previewMatchesOriginal() {
        return ItemStack.isSameItemSameComponents(this.originalStack, this.previewStack)
                && this.originalStack.getCount() == this.previewStack.getCount();
    }

    public void rebuildPreview() {
        boolean rawEdited = this.state.rawEditorEdited;
        String rawText = this.state.rawEditorText;
        boolean rawShowDefaults = this.state.rawEditorShowDefaults;
        RawItemDataUtil.ParseResult rawParse = null;

        ItemPreviewService.PreviewBuildResult result;
        if (rawEdited) {
            rawParse = this.cachedRawParse(rawText);
            result = this.cachedRawResult(rawText, rawParse);
        } else {
            this.clearRawPreviewCache();
            result = this.previewService.buildPreview(this.originalStack, this.state, this.baselineState, this.registryAccess());
        }
        this.previewStack = result.previewStack();
        this.messages = List.copyOf(result.messages());

        if (rawEdited) {
            this.syncStructuredStateFromRaw(rawParse, rawText, rawShowDefaults);
        } else {
            this.state.rawEditorText = RawItemDataUtil.serialize(this.previewStack, this.registryAccess());
        }

        this.dirty = !ItemStack.isSameItemSameComponents(this.cleanPreviewStack, this.previewStack)
                || this.cleanPreviewStack.getCount() != this.previewStack.getCount();
        this.lastPreviewRebuildAtNanos = System.nanoTime();
        this.notifyListeners();
    }

    public void queueRebuildPreview(long debounceMillis) {
        if (debounceMillis <= 0L) {
            this.rebuildPreview();
            return;
        }
        long now = System.nanoTime();
        long targetAt = now + debounceMillis * 1_000_000L;
        if (this.state.rawEditorEdited) {
            targetAt = Math.max(targetAt, this.lastPreviewRebuildAtNanos + RAW_TYPING_REBUILD_MIN_INTERVAL_NANOS);
        }
        this.rebuildQueued = true;
        this.queuedRebuildAtNanos = targetAt;
    }

    public void flushQueuedRebuild() {
        if (!this.rebuildQueued) {
            return;
        }
        this.rebuildQueued = false;
        this.rebuildPreview();
    }

    public void cancelQueuedRebuild() {
        this.rebuildQueued = false;
    }

    public void setTransientValidationMessages(List<ValidationMessage> messages) {
        List<ValidationMessage> safeMessages = messages == null ? List.of() : List.copyOf(messages);
        if (this.messages.equals(safeMessages)) {
            return;
        }
        this.messages = safeMessages;
        this.notifyListeners();
    }

    public void tick() {
        if (!this.rebuildQueued) {
            return;
        }
        if (System.nanoTime() >= this.queuedRebuildAtNanos) {
            this.flushQueuedRebuild();
        }
    }

    public void reset() {
        ItemEditorState previous = this.state;
        this.state = this.stateMapper.map(this.originalStack, this.registryAccess());
        this.state.rawEditorEdited = false;
        this.state.rawEditorText = RawItemDataUtil.serialize(this.originalStack, this.registryAccess());
        this.state.rawEditorShowDefaults = previous.rawEditorShowDefaults;
        this.state.rawEditorWordWrap = previous.rawEditorWordWrap;
        this.state.rawEditorHorizontalScroll = !this.state.rawEditorWordWrap;
        this.state.rawEditorFontSizePercent = previous.rawEditorFontSizePercent;
        this.state.rawEditorOptionsLoaded = previous.rawEditorOptionsLoaded;
        this.state.uiRawEditorOptionsExpanded = previous.uiRawEditorOptionsExpanded;
        this.rebuildQueued = false;
        this.clearRawPreviewCache();
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

    private ItemPreviewService.PreviewBuildResult cachedRawResult(
            String rawText,
            RawItemDataUtil.ParseResult rawParse
    ) {
        if (rawText != null
                && this.cachedRawPreviewResult != null
                && rawText.equals(this.cachedRawPreviewInput)) {
            return this.copyPreviewBuildResult(this.cachedRawPreviewResult);
        }

        RawItemDataUtil.ParseResult parseResult = rawParse == null
                ? this.cachedRawParse(rawText)
                : rawParse;
        ItemPreviewService.PreviewBuildResult built = this.previewService.buildRawPreviewFromParsed(
                this.originalStack,
                parseResult,
                this.registryAccess()
        );
        this.cachedRawPreviewInput = rawText;
        this.cachedRawPreviewResult = this.copyPreviewBuildResult(built);
        return built;
    }

    private ItemPreviewService.PreviewBuildResult copyPreviewBuildResult(ItemPreviewService.PreviewBuildResult source) {
        return new ItemPreviewService.PreviewBuildResult(
                source.previewStack().copy(),
                List.copyOf(source.messages())
        );
    }

    private void clearRawPreviewCache() {
        this.cachedRawPreviewInput = null;
        this.cachedRawPreviewResult = null;
        this.cachedRawParsedInput = null;
        this.cachedRawParsedResult = null;
    }

    private void syncStructuredStateFromRaw(
            RawItemDataUtil.ParseResult parsed,
            String rawText,
            boolean rawShowDefaults
    ) {
        if (parsed == null) {
            parsed = this.cachedRawParse(rawText);
        }
        if (!parsed.success()) {
            return;
        }

        ItemEditorState mapped = this.stateMapper.map(parsed.stack(), this.registryAccess());
        this.copyStateIntoExisting(this.state, mapped, true);
        this.state.rawEditorEdited = true;
        this.state.rawEditorText = rawText;
        this.state.rawEditorShowDefaults = rawShowDefaults;
    }

    private RawItemDataUtil.ParseResult cachedRawParse(String rawText) {
        if (rawText != null
                && this.cachedRawParsedResult != null
                && rawText.equals(this.cachedRawParsedInput)) {
            return this.copyParseResult(this.cachedRawParsedResult);
        }

        RawItemDataUtil.ParseResult parsed = RawItemDataUtil.parse(rawText, this.registryAccess());
        this.cachedRawParsedInput = rawText;
        this.cachedRawParsedResult = this.copyParseResult(parsed);
        return parsed;
    }

    private RawItemDataUtil.ParseResult copyParseResult(RawItemDataUtil.ParseResult source) {
        if (source == null) {
            return new RawItemDataUtil.ParseResult(null, "unknown", -1, -1);
        }
        ItemStack stackCopy = source.stack() == null ? null : source.stack().copy();
        return new RawItemDataUtil.ParseResult(stackCopy, source.error(), source.line(), source.column());
    }

    private void copyStateIntoExisting(Object target, Object source, boolean root) {
        if (target == null || source == null || target.getClass() != source.getClass()) {
            return;
        }

        for (Field field : target.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            field.setAccessible(true);
            String name = field.getName();
            if ((root && RAW_EDITOR_FIELDS.contains(name)) || this.shouldPreserveUiField(name)) {
                continue;
            }

            try {
                Object sourceValue = field.get(source);
                Class<?> fieldType = field.getType();

                if (this.isSimpleValue(fieldType)) {
                    field.set(target, sourceValue);
                    continue;
                }

                if (Collection.class.isAssignableFrom(fieldType)) {
                    @SuppressWarnings("unchecked")
                    Collection<Object> targetCollection = (Collection<Object>) field.get(target);
                    @SuppressWarnings("unchecked")
                    Collection<Object> sourceCollection = (Collection<Object>) sourceValue;
                    if (targetCollection == null) {
                        field.set(target, sourceCollection);
                    } else {
                        targetCollection.clear();
                        if (sourceCollection != null) {
                            targetCollection.addAll(sourceCollection);
                        }
                    }
                    continue;
                }

                if (sourceValue == null) {
                    if (!Modifier.isFinal(field.getModifiers())) {
                        field.set(target, null);
                    }
                    continue;
                }

                Object targetValue = field.get(target);
                if (targetValue == null || !this.isStateModelType(fieldType)) {
                    if (!Modifier.isFinal(field.getModifiers())) {
                        field.set(target, sourceValue);
                    }
                    continue;
                }

                this.copyStateIntoExisting(targetValue, sourceValue, false);
            } catch (IllegalAccessException exception) {
                throw new RuntimeException("Failed to synchronize editor state field: " + field.getName(), exception);
            }
        }
    }

    private boolean shouldPreserveUiField(String fieldName) {
        return fieldName.startsWith("ui")
                || fieldName.endsWith("Collapsed")
                || fieldName.startsWith("selected")
                || fieldName.startsWith("dragging")
                || fieldName.equals("miniMapScrollOffset");
    }

    private boolean isSimpleValue(Class<?> type) {
        return type.isPrimitive()
                || type.isEnum()
                || type == String.class
                || Number.class.isAssignableFrom(type)
                || type == Boolean.class
                || type == Character.class;
    }

    private boolean isStateModelType(Class<?> type) {
        Package typePackage = type.getPackage();
        if (typePackage == null) {
            return false;
        }
        return "me.noramibu.itemeditor.editor".equals(typePackage.getName());
    }

    private void applySavedRawEditorOptions(ItemEditorState target) {
        RawEditorOptions options = RawEditorOptionsService.instance().load();
        target.rawEditorShowDefaults = options.showDefaultKeys;
        target.rawEditorWordWrap = options.wordWrap;
        target.rawEditorHorizontalScroll = !target.rawEditorWordWrap;
        target.rawEditorFontSizePercent = options.fontSizePercent;
        target.rawEditorOptionsLoaded = true;
    }
}
