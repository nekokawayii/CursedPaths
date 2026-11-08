package cn.blockforge.generated.nnnnr1201forge2;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;

/** 1.21：同一个类里若既有游戏总线事件又有 Mod 总线事件，不能整类 register，
 *  否则会抛「IModBusEvent events are not allowed on the common NeoForge bus」。
 *  这里按方法的事件类型分别挂到正确的总线上（仅支持静态方法）。 */
public final class EventBuses {
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerSplit(Class<?> clazz) {
        for (Method method : clazz.getDeclaredMethods()) {
            SubscribeEvent annotation = method.getAnnotation(SubscribeEvent.class);
            if (annotation == null || method.getParameterCount() != 1) continue;
            Class<?> eventType = method.getParameterTypes()[0];
            if (!Event.class.isAssignableFrom(eventType)) continue;
            boolean modBus = net.neoforged.fml.event.IModBusEvent.class.isAssignableFrom(eventType);
            Consumer<Event> consumer = event -> {
                try {
                    method.setAccessible(true);
                    method.invoke(null, event);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException re) throw re;
                    if (cause instanceof Error err) throw err;
                    throw new RuntimeException(cause);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            };
            if (modBus) {
                GeneratedMod.MOD_BUS.addListener(annotation.priority(), (Class) eventType, (Consumer) consumer);
            } else {
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(annotation.priority(), (Class) eventType, (Consumer) consumer);
            }
        }
    }
    private EventBuses() { }
}
