package cn.blockforge.generated.nnnnr1201forge2;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 1.21 网络兼容层：把旧的 SimpleChannel 写法（messageBuilder/encoder/decoder/consumerMainThread）
 * 保留下来，底层用 NeoForge 1.21 的 payload 注册实现。一个 Frame 包按 id 分派。
 */
public final class SimpleChannel {
    /** 旧式方向枚举。 */
    public enum NetworkDirection { PLAY_TO_CLIENT, PLAY_TO_SERVER, BOTH }

    /** 旧式上下文。 */
    public static final class Context {
        private final ServerPlayer sender;
        private final FriendlyByteBuf buf;
        Context(ServerPlayer sender, FriendlyByteBuf buf) { this.sender = sender; this.buf = buf; }
        public ServerPlayer getSender() { return sender; }
        public FriendlyByteBuf payload() { return buf; }
        public void setPacketHandled(boolean v) { }
    }

    /** 发送目标（替代 PacketDistributor.PLAYER/ALL）。 */
    public static final class Target {
        private final ServerPlayer player; private final boolean all; private final net.minecraft.world.entity.Entity tracked;
        private Target(ServerPlayer p, boolean all) { this(p, all, null); }
        private Target(ServerPlayer p, boolean all, net.minecraft.world.entity.Entity tracked) { this.player = p; this.all = all; this.tracked = tracked; this.nearDim = null; this.nx=this.ny=this.nz=this.nr=0; }
        public static Target player(ServerPlayer p) { return new Target(p, false); }
        public static Target all() { return new Target(null, true); }
        public static Target tracking(net.minecraft.world.entity.Entity e) { return new Target(null, false, e); }

        private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> nearDim;
        private final double nx, ny, nz, nr;
        public static Target near(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, double x, double y, double z, double radius) {
            return new Target(dim, x, y, z, radius);
        }
        private Target(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, double x, double y, double z, double radius) {
            this.player = null; this.all = false; this.tracked = null; this.nearDim = dim; this.nx = x; this.ny = y; this.nz = z; this.nr = radius;
        }
    }

    private static final Map<Integer, Entry<?>> ENTRIES = new HashMap<>();
    private static final java.util.List<SimpleChannel> CHANNELS = new java.util.ArrayList<>();
    private static boolean registered;

    public record Frame(int id, byte[] data) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Frame> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(GeneratedMod.MOD_ID, "frame"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Frame> CODEC = StreamCodec.of(
            (buf, f) -> { buf.writeVarInt(f.id()); buf.writeByteArray(f.data()); },
            buf -> new Frame(buf.readVarInt(), buf.readByteArray()));
        @Override public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private record Entry<T>(int id, Class<T> type, SimpleChannel.NetworkNetworkDirection dir,
                            BiConsumer<T, FriendlyByteBuf> encoder,
                            Function<FriendlyByteBuf, T> decoder,
                            BiConsumer<T, java.util.function.Supplier<Context>> handler) { }

    private final String name;

    private SimpleChannel(String name) { this.name = name; CHANNELS.add(this); }

    public static SimpleChannel create(String name) { return new SimpleChannel(name); }

    /** 兼容旧写法：NetworkRegistry.newSimpleChannel(id, version, clientPredicate, serverPredicate)。 */
    public static SimpleChannel create(ResourceLocation id, java.util.function.Supplier<String> version,
                                      java.util.function.Predicate<String> client, java.util.function.Predicate<String> server) {
        return new SimpleChannel(id.getPath());
    }

    public <T> MessageBuilder<T> messageBuilder(Class<T> type, int id, NetworkDirection dir) {
        return new MessageBuilder<>(this, type, id, dir);
    }

    public final class MessageBuilder<T> {
        private final Class<T> type; private final int id; private final NetworkDirection dir;
        private BiConsumer<T, FriendlyByteBuf> encoder;
        private Function<FriendlyByteBuf, T> decoder;
        private BiConsumer<T, java.util.function.Supplier<Context>> consumer;
        MessageBuilder(SimpleChannel ch, Class<T> type, int id, NetworkDirection dir) { this.type = type; this.id = id; this.dir = dir; }
        public MessageBuilder<T> encoder(BiConsumer<T, FriendlyByteBuf> e) { this.encoder = e; return this; }
        public MessageBuilder<T> decoder(Function<FriendlyByteBuf, T> d) { this.decoder = d; return this; }
        public MessageBuilder<T> consumerMainThread(BiConsumer<T, java.util.function.Supplier<Context>> c) { this.consumer = c; return this; }
        public void add() { ENTRIES.put(id, new Entry<>(id, type, dir, encoder, decoder, consumer)); }
    }

    public <T> void send(Target target, T msg) { Deliver.send(target, msg); }
    public <T> void sendToServer(T msg) { Deliver.toServer(msg); }
    public <T> void sendToPlayer(ServerPlayer p, T msg) { Deliver.toPlayer(p, msg); }
    public <T> void sendToAll(T msg) { Deliver.toAll(msg); }

    static int idOf(Object msg) {
        for (Entry<?> e : ENTRIES.values()) if (e.type().isInstance(msg)) return e.id();
        throw new IllegalArgumentException("未注册的包类型: " + msg.getClass());
    }

    static byte[] encode(int id, Object msg) {
        Entry<?> e = ENTRIES.get(id);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        ((Entry<Object>) e).encoder().accept(msg, buf);
        byte[] out = new byte[buf.readableBytes()];
        buf.readBytes(out);
        return out;
    }

    static void decode(int id, byte[] data, ServerPlayer sender) {
        Entry<?> e = ENTRIES.get(id);
        if (e == null) return;
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        Object msg = ((Entry<Object>) e).decoder().apply(buf);
        if (e.handler() != null) ((Entry<Object>) e).handler().accept(msg, () -> new Context(sender, buf));
    }

    /** 由 GeneratedMod 在 mod 事件总线注册。 */
    public static void register(RegisterPayloadHandlersEvent event) {
        if (registered) return;
        registered = true;
        event.registrar(GeneratedMod.MOD_ID).playBidirectional(Frame.TYPE, Frame.CODEC,
            (frame, ctx) -> {
                ServerPlayer sender = ctx.player() instanceof ServerPlayer sp ? sp : null;
                decode(frame.id(), frame.data(), sender);
            });
    }

    /** 发送辅助。 */
    static final class Deliver {
        static <T> void send(Target target, T msg) {
            int id = idOf(msg);
            Frame frame = new Frame(id, encode(id, msg));
            if (target.nearDim != null) {
                var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
                if (server != null) { var level = server.getLevel(target.nearDim); if (level != null)
                    PacketDistributor.sendToPlayersNear(level, null, target.nx, target.ny, target.nz, target.nr, frame); }
            } else if (target.all) PacketDistributor.sendToAllPlayers(frame);
            else if (target.tracked != null) PacketDistributor.sendToPlayersTrackingEntityAndSelf(target.tracked, frame);
            else if (target.player != null) PacketDistributor.sendToPlayer(target.player, frame);
        }
        static <T> void toServer(T msg) {
            int id = idOf(msg);
            PacketDistributor.sendToServer(new Frame(id, encode(id, msg)));
        }
        static <T> void toPlayer(ServerPlayer p, T msg) {
            int id = idOf(msg);
            PacketDistributor.sendToPlayer(p, new Frame(id, encode(id, msg)));
        }
        static <T> void toAll(T msg) {
            int id = idOf(msg);
            PacketDistributor.sendToAllPlayers(new Frame(id, encode(id, msg)));
        }
    }

    private SimpleChannel() { this.name = null; }
}
