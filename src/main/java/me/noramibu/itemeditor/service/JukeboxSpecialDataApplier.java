package me.noramibu.itemeditor.service;

import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.IdFieldNormalizer;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.EitherHolder;
import net.minecraft.world.item.JukeboxSong;
import net.minecraft.world.item.JukeboxPlayable;

import java.util.Objects;

final class JukeboxSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().jukeboxSongId, context.baselineSpecial().jukeboxSongId)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.JUKEBOX_PLAYABLE);
            return;
        }

        if (context.special().jukeboxSongId.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.JUKEBOX_PLAYABLE);
            return;
        }

        Identifier songId = IdFieldNormalizer.parse(context.special().jukeboxSongId);
        if (songId == null) {
            context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.jukebox_id")));
            return;
        }

        ResourceKey<JukeboxSong> songKey = ResourceKey.create(Registries.JUKEBOX_SONG, songId);
        context.registryAccess().lookupOrThrow(Registries.JUKEBOX_SONG).get(songKey)
                .ifPresentOrElse(
                        songHolder -> context.previewStack().set(DataComponents.JUKEBOX_PLAYABLE, new JukeboxPlayable(new EitherHolder<>(songHolder))),
                        () -> context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.jukebox_id")))
                );
    }
}
