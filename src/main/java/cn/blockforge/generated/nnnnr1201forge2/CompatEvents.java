package cn.blockforge.generated.nnnnr1201forge2;

/** 1.21 兼容：事件基类不再统一暴露 setCanceled，取消时先判 ICancellableEvent。 */
public final class CompatEvents {
    public static void cancel(Object event) {
        if (event instanceof net.neoforged.bus.api.ICancellableEvent c) c.setCanceled(true);
    }
    private CompatEvents() { }
}
