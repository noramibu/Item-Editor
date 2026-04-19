package me.noramibu.itemeditor.service;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.LinkedHashMultimap;
import me.noramibu.itemeditor.editor.ValidationMessage;
import me.noramibu.itemeditor.util.HeadTextureUtil;
import me.noramibu.itemeditor.util.ItemEditorText;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Util;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Objects;
import java.util.UUID;

final class ProfileSpecialDataApplier extends AbstractPreviewApplierSupport implements SpecialDataApplier {

    @Override
    public void apply(SpecialDataApplyContext context) {
        if (Objects.equals(context.special().profileName, context.baselineSpecial().profileName)
                && Objects.equals(context.special().profileUuid, context.baselineSpecial().profileUuid)
                && Objects.equals(context.special().profileTextureValue, context.baselineSpecial().profileTextureValue)
                && Objects.equals(context.special().profileTextureSignature, context.baselineSpecial().profileTextureSignature)) {
            this.restoreOriginalComponent(context.originalStack(), context.previewStack(), DataComponents.PROFILE);
            return;
        }

        String profileName = context.special().profileName.trim();
        String profileUuidRaw = context.special().profileUuid.trim();
        String textureInputRaw = context.special().profileTextureValue.trim();
        String textureSignature = context.special().profileTextureSignature.trim();

        if (profileName.isBlank() && profileUuidRaw.isBlank() && textureInputRaw.isBlank() && textureSignature.isBlank()) {
            this.clearToPrototype(context.previewStack(), DataComponents.PROFILE);
            return;
        }

        UUID profileUuid = null;
        if (!profileUuidRaw.isBlank()) {
            profileUuid = tryParseUuid(profileUuidRaw);
        }

        String textureValue = "";
        if (!textureInputRaw.isBlank()) {
            textureValue = HeadTextureUtil.normalizeTextureInput(textureInputRaw);
            if (!HeadTextureUtil.isBase64(textureValue)) {
                context.messages().add(ValidationMessage.error(ItemEditorText.str("preview.validation.profile_texture")));
                textureValue = "";
            }
        }

        if (!textureValue.isBlank()) {
            UUID resolvedId = profileUuid == null ? Util.NIL_UUID : profileUuid;
            Property texturesProperty = textureSignature.isBlank()
                    ? new Property("textures", textureValue)
                    : new Property("textures", textureValue, textureSignature);
            var mutableProperties = LinkedHashMultimap.<String, Property>create();
            mutableProperties.put("textures", texturesProperty);
            PropertyMap properties = new PropertyMap(mutableProperties);
            GameProfile profile = new GameProfile(resolvedId, profileName, properties);
            context.previewStack().set(DataComponents.PROFILE, ResolvableProfile.createResolved(profile));
            return;
        }

        if (!textureSignature.isBlank()) {
            context.messages().add(ValidationMessage.warning(ItemEditorText.str("preview.validation.profile_signature_without_texture")));
        }

        if (!profileName.isBlank()) {
            context.previewStack().set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(profileName));
            return;
        }

        if (profileUuid != null) {
            context.previewStack().set(DataComponents.PROFILE, ResolvableProfile.createUnresolved(profileUuid));
        }
    }

    private static UUID tryParseUuid(String raw) {
        try {
            return UUID.fromString(raw.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
