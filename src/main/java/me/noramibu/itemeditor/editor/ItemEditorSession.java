package me.noramibu.itemeditor.editor;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import me.noramibu.itemeditor.editor.text.RawEditorPreparedText;
import me.noramibu.itemeditor.service.ItemApplyService;
import me.noramibu.itemeditor.service.ItemEditorStateMapper;
import me.noramibu.itemeditor.service.ItemPreviewService;
import me.noramibu.itemeditor.storage.RawEditorOptionsService;
import me.noramibu.itemeditor.storage.model.RawEditorOptions;
import me.noramibu.itemeditor.util.AsyncDispatchUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import me.noramibu.itemeditor.util.RawItemDataUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;

public final class ItemEditorSession {
    private static final ExecutorService RAW_PREPARATION_EXECUTOR =
            AsyncDispatchUtil.newSingleThreadExecutor("itemeditor-raw-prepare");
    private static final Set<String> RAW_EDITOR_FIELDS = Set.of(
            "rawEditorEdited",
            "rawEditorText",
            "rawEditorShowDefaults",
            "rawEditorAutocompleteDisabled",
            "rawEditorWordWrap",
            "rawEditorFontSizePercent");

    private final Minecraft minecraft;
    private final Supplier<RegistryAccess> registries;
    private final ItemStack originalStack;
    private final ItemEditorStateMapper stateMapper;
    private final ItemPreviewService previewService;
    private final ItemApplyService applyService;
    private final List<Runnable> listeners = new ArrayList<>();
    private final ItemEditorState baselineState;
    private final ItemEditorSessionOrigin origin;

    private ItemEditorState state;
    private final ItemStack cleanPreviewStack;
    private ItemStack previewStack;
    private List<ValidationMessage> messages = List.of();
    private boolean dirty;
    private String cachedRawPreviewInput;
    private ItemPreviewService.PreviewBuildResult cachedRawPreviewResult;
    private String cachedRawParsedInput;
    private RawItemDataUtil.ParseResult cachedRawParsedResult;
    private long rawPreparationVersion;
    private CompletableFuture<RawEditorPreparedText> rawPreparation;
    private RawEditorPreparedText preparedRawText;

    public ItemEditorSession(Minecraft minecraft, ItemStack originalStack) {
        this(minecraft, originalStack, ItemEditorSessionOrigin.TRANSIENT);
    }

    public ItemEditorSession(Minecraft minecraft, ItemStack originalStack, ItemEditorSessionOrigin origin) {
        this(
                minecraft,
                originalStack,
                origin,
                () -> minecraft.level == null ? RegistryAccess.EMPTY : minecraft.level.registryAccess(),
                RawEditorOptionsService.instance().load());
    }

    ItemEditorSession(
            Minecraft minecraft,
            ItemStack originalStack,
            ItemEditorSessionOrigin origin,
            Supplier<RegistryAccess> registries,
            RawEditorOptions options) {
        this.minecraft = minecraft;
        this.registries = registries;
        this.originalStack = originalStack.copy();
        this.origin = origin == null ? ItemEditorSessionOrigin.TRANSIENT : origin;
        this.stateMapper = new ItemEditorStateMapper();
        this.previewService = new ItemPreviewService();
        this.applyService = new ItemApplyService();
        this.baselineState = this.stateMapper.map(this.originalStack, this.registryAccess());
        this.state = this.stateMapper.map(this.originalStack, this.registryAccess());
        this.applySavedRawEditorOptions(this.baselineState, options);
        this.applySavedRawEditorOptions(this.state, options);
        ItemPreviewService.PreviewBuildResult initialResult = this.previewService.buildPreview(
                this.originalStack, this.state, this.baselineState, this.registryAccess());
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

    public ItemEditorSessionOrigin origin() {
        return this.origin;
    }

    public boolean hasStorageOrigin() {
        return this.origin instanceof ItemEditorSessionOrigin.Storage;
    }

    public ItemStack previewStack() {
        return this.previewStack.copy();
    }

    public ItemEditorState state() {
        return this.state;
    }

    public RawEditorPreparedText preparedRawEditorText() {
        String rawText = this.state.rawEditorText;
        return this.preparedRawText != null && this.preparedRawText.text().equals(rawText)
                ? this.preparedRawText
                : null;
    }

    public CompletableFuture<RawEditorPreparedText> prepareRawEditorTextAsync() {
        RawEditorPreparedText prepared = this.preparedRawEditorText();
        if (prepared != null) {
            return CompletableFuture.completedFuture(prepared);
        }
        if (this.rawPreparation != null) {
            return this.rawPreparation;
        }

        long version = this.rawPreparationVersion;
        String existingRaw = this.state.rawEditorText == null ? "" : this.state.rawEditorText;
        boolean serializePreview = existingRaw.isBlank() && !this.state.rawEditorEdited;
        ItemStack stack = serializePreview ? this.previewStack.copy() : null;
        RegistryAccess registryAccess = this.registryAccess();
        boolean showDefaults = this.state.rawEditorShowDefaults;
        CompletableFuture<RawEditorPreparedText> result = new CompletableFuture<>();
        this.rawPreparation = result;

        CompletableFuture.supplyAsync(
                        () -> {
                            String rawText = serializePreview
                                    ? RawItemDataUtil.serialize(stack, registryAccess, showDefaults)
                                    : existingRaw;
                            return RawEditorPreparedText.prepare(rawText);
                        },
                        RAW_PREPARATION_EXECUTOR)
                .whenComplete((rawText, error) -> this.minecraft.execute(
                        () -> this.completeRawPreparation(result, version, serializePreview, rawText, error)));
        return result;
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

    public ItemEditorChangeSet changes() {
        return ItemEditorChangeSet.detect(this.originalStack, this.previewStack, this.registryAccess());
    }

    public ItemEditorChangeSet changesWithValues() {
        return ItemEditorChangeSet.between(this.originalStack, this.previewStack, this.registryAccess());
    }

    public void rebuildPreview() {
        this.rebuildPreview(null);
    }

    public void rebuildRawPreview(String rawText, ItemStack parsedStack) {
        if (!this.state.rawEditorEdited
                || rawText == null
                || parsedStack == null
                || !rawText.equals(this.state.rawEditorText)) {
            return;
        }
        this.rebuildPreview(new RawItemDataUtil.ParseResult(parsedStack.copy(), null, -1, -1));
    }

    public boolean prepareStructuredTransition() {
        if (!this.state.rawEditorEdited) {
            return true;
        }
        String rawText = this.state.rawEditorText;
        RawItemDataUtil.ParseResult parsed = this.cachedRawParse(rawText);
        ItemPreviewService.PreviewBuildResult result = this.cachedRawResult(rawText, parsed);
        if (!parsed.success()
                || result.messages().stream()
                        .anyMatch(message -> message.severity() == ValidationMessage.Severity.ERROR)) {
            this.setTransientValidationMessages(result.messages());
            return false;
        }
        this.syncStructuredStateFromRaw(parsed, rawText, this.state.rawEditorShowDefaults);
        this.state.rawEditorEdited = false;
        this.invalidateRawPreparation(true);
        this.previewStack = result.previewStack();
        this.messages = List.copyOf(result.messages());
        this.dirty = !ItemStack.isSameItemSameComponents(this.cleanPreviewStack, this.previewStack)
                || this.cleanPreviewStack.getCount() != this.previewStack.getCount();
        this.notifyListeners();
        return true;
    }

    private void rebuildPreview(RawItemDataUtil.ParseResult suppliedRawParse) {
        boolean rawEdited = this.state.rawEditorEdited;
        String rawText = this.state.rawEditorText;
        boolean rawShowDefaults = this.state.rawEditorShowDefaults;
        RawItemDataUtil.ParseResult rawParse = null;

        ItemPreviewService.PreviewBuildResult result;
        if (rawEdited) {
            if (suppliedRawParse == null) {
                rawParse = this.cachedRawParse(rawText);
            } else {
                rawParse = suppliedRawParse;
                this.cachedRawParsedInput = rawText;
                this.cachedRawParsedResult = suppliedRawParse;
            }
            result = this.cachedRawResult(rawText, rawParse);
        } else {
            this.clearRawPreviewCache();
            result = this.previewService.buildPreview(
                    this.originalStack, this.state, this.baselineState, this.registryAccess());
        }
        this.previewStack = result.previewStack();
        this.messages = List.copyOf(result.messages());

        if (rawEdited) {
            this.syncStructuredStateFromRaw(rawParse, rawText, rawShowDefaults);
        } else {
            this.invalidateRawPreparation(true);
        }

        this.dirty = !ItemStack.isSameItemSameComponents(this.cleanPreviewStack, this.previewStack)
                || this.cleanPreviewStack.getCount() != this.previewStack.getCount();
        this.notifyListeners();
    }

    public void setTransientValidationMessages(List<ValidationMessage> messages) {
        List<ValidationMessage> safeMessages = messages == null ? List.of() : List.copyOf(messages);
        if (this.messages.equals(safeMessages)) {
            return;
        }
        this.messages = safeMessages;
        this.notifyListeners();
    }

    public void reset() {
        ItemEditorState previous = this.state;
        this.state = this.stateMapper.map(this.originalStack, this.registryAccess());
        this.state.rawEditorEdited = false;
        this.invalidateRawPreparation(true);
        this.state.rawEditorShowDefaults = previous.rawEditorShowDefaults;
        this.state.rawEditorAutocompleteDisabled = previous.rawEditorAutocompleteDisabled;
        this.state.rawEditorWordWrap = previous.rawEditorWordWrap;
        this.state.rawEditorFontSizePercent = previous.rawEditorFontSizePercent;
        this.state.uiRawEditorOptionsExpanded = previous.uiRawEditorOptionsExpanded;
        this.clearRawPreviewCache();
        this.rebuildPreview();
    }

    public ItemApplyService.ApplyResult apply() {
        if (this.origin instanceof ItemEditorSessionOrigin.External external) {
            try {
                ItemApplyService.ApplyResult result = external.saveHandler().apply(this.previewStack.copy());
                return result == null
                        ? ItemApplyService.ApplyResult.failure(ItemEditorText.str("apply.verify.error"))
                        : result;
            } catch (RuntimeException exception) {
                String message = exception.getMessage();
                return ItemApplyService.ApplyResult.failure(
                        message == null || message.isBlank() ? ItemEditorText.str("apply.verify.error") : message);
            }
        }
        return this.applyService.apply(this.minecraft, this.previewStack);
    }

    public void addListener(Runnable listener) {
        this.listeners.add(listener);
    }

    public RegistryAccess registryAccess() {
        return this.registries.get();
    }

    private void notifyListeners() {
        this.listeners.forEach(Runnable::run);
    }

    private ItemPreviewService.PreviewBuildResult cachedRawResult(
            String rawText, RawItemDataUtil.ParseResult rawParse) {
        if (rawText != null && this.cachedRawPreviewResult != null && rawText.equals(this.cachedRawPreviewInput)) {
            return this.copyPreviewBuildResult(this.cachedRawPreviewResult);
        }

        RawItemDataUtil.ParseResult parseResult = rawParse == null ? this.cachedRawParse(rawText) : rawParse;
        ItemPreviewService.PreviewBuildResult built =
                this.previewService.buildRawPreviewFromParsed(this.originalStack, parseResult);
        this.cachedRawPreviewInput = rawText;
        this.cachedRawPreviewResult = this.copyPreviewBuildResult(built);
        return built;
    }

    private ItemPreviewService.PreviewBuildResult copyPreviewBuildResult(ItemPreviewService.PreviewBuildResult source) {
        return new ItemPreviewService.PreviewBuildResult(source.previewStack().copy(), List.copyOf(source.messages()));
    }

    private void clearRawPreviewCache() {
        this.cachedRawPreviewInput = null;
        this.cachedRawPreviewResult = null;
        this.cachedRawParsedInput = null;
        this.cachedRawParsedResult = null;
    }

    private void completeRawPreparation(
            CompletableFuture<RawEditorPreparedText> result,
            long version,
            boolean serializedPreview,
            RawEditorPreparedText prepared,
            Throwable error) {
        if (this.rawPreparation == result) {
            this.rawPreparation = null;
        }
        if (version != this.rawPreparationVersion) {
            result.completeExceptionally(new CancellationException("Raw editor preparation was superseded"));
            return;
        }
        if (error != null || prepared == null) {
            result.completeExceptionally(
                    error == null ? new IllegalStateException("Raw editor preparation failed") : error);
            return;
        }
        String currentRaw = this.state.rawEditorText == null ? "" : this.state.rawEditorText;
        if (serializedPreview && !this.state.rawEditorEdited && currentRaw.isBlank()) {
            this.state.rawEditorText = prepared.text();
            currentRaw = prepared.text();
        }
        if (!prepared.text().equals(currentRaw)) {
            result.completeExceptionally(new CancellationException("Raw editor text changed during preparation"));
            return;
        }
        this.preparedRawText = prepared;
        result.complete(prepared);
    }

    private void invalidateRawPreparation(boolean clearText) {
        this.rawPreparationVersion++;
        this.rawPreparation = null;
        this.preparedRawText = null;
        if (clearText) {
            this.state.rawEditorText = "";
        }
    }

    private void syncStructuredStateFromRaw(
            RawItemDataUtil.ParseResult parsed, String rawText, boolean rawShowDefaults) {
        if (parsed == null) {
            parsed = this.cachedRawParse(rawText);
        }
        if (!parsed.success()) {
            return;
        }

        ItemEditorState mapped = this.stateMapper.map(parsed.stack(), this.registryAccess());
        this.copyStateIntoExisting(this.state, mapped, true);
        this.invalidateRawPreparation(false);
        this.state.rawEditorEdited = true;
        this.state.rawEditorText = rawText;
        this.state.rawEditorShowDefaults = rawShowDefaults;
    }

    private RawItemDataUtil.ParseResult cachedRawParse(String rawText) {
        if (rawText != null && this.cachedRawParsedResult != null && rawText.equals(this.cachedRawParsedInput)) {
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

    private void applySavedRawEditorOptions(ItemEditorState target, RawEditorOptions options) {
        target.rawEditorShowDefaults = options.showDefaultKeys;
        target.rawEditorAutocompleteDisabled = options.autocompleteDisabled;
        target.rawEditorWordWrap = options.wordWrap;
        target.rawEditorFontSizePercent = options.fontSizePercent;
    }
}
