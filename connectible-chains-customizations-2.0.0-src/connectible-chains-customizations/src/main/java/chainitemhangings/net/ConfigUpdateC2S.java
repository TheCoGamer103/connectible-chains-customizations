package chainitemhangings.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

/** Client -> server: an edited config from the in-game menu. The server checks permission. */
public record ConfigUpdateC2S(String json) implements CustomPacketPayload {

    public static final Type<ConfigUpdateC2S> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("chainitemhangings", "config_update"));

    public static final StreamCodec<ByteBuf, ConfigUpdateC2S> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(1 << 18), ConfigUpdateC2S::json,
            ConfigUpdateC2S::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
