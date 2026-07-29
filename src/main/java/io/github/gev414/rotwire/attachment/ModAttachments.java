package io.github.gev414.rotwire.attachment;

import com.mojang.serialization.Codec;
import io.github.gev414.rotwire.Rotwire;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>>
            ATTACHMENT_TYPES = DeferredRegister.create(
                    NeoForgeRegistries.ATTACHMENT_TYPES,
                    Rotwire.MOD_ID
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<Boolean>
            > URBAN_TREES_COMPLETE = ATTACHMENT_TYPES.register(
                    "urban_trees_complete",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL, Boolean::booleanValue)
                            .build()
            );

    public static final DeferredHolder<
            AttachmentType<?>,
            AttachmentType<Boolean>
            > URBAN_TREES_DENSE_COMPLETE = ATTACHMENT_TYPES.register(
                    "urban_trees_dense_complete",
                    () -> AttachmentType.builder(() -> false)
                            .serialize(Codec.BOOL, Boolean::booleanValue)
                            .build()
            );

    private ModAttachments() {
    }
}
