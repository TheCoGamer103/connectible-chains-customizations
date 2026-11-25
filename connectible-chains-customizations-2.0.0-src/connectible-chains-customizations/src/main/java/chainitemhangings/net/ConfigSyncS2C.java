package chainitemhangings.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/** Server -> client: the server's chain customizations config, and whether this player may edit it. */
public record ConfigSyncS2C(String json, boolean canEdit) implements CustomPacketPayload {

    public static final Type<ConfigSyncS2C> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("chainitemhangings", "config_sync"));

    public static final StreamCodec<ByteBuf, ConfigSyncS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1 << 18), ConfigSyncS2C::json,
            ByteBufCodecs.BOOL, ConfigSyncS2C::canEdit,
            ConfigSyncS2C::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
