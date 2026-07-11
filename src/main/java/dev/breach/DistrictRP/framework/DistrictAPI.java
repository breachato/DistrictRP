package dev.breach.DistrictRP.framework;

import dev.breach.DistrictRP.DistrictRP;

import java.util.concurrent.ConcurrentHashMap;

public final class DistrictAPI {

    private DistrictAPI() {}

    private static final ConcurrentHashMap<Class<?>, Object> services = new ConcurrentHashMap<>();

    public static <T> void bind(Class<T> serviceClass, T impl) {
        if (serviceClass == null) return;
        if (impl == null) {
            services.remove(serviceClass);
        } else {
            services.put(serviceClass, impl);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> serviceClass) {
        if (serviceClass == null) return null;
        return (T) services.get(serviceClass);
    }

    public static boolean isBound(Class<?> serviceClass) {
        return services.containsKey(serviceClass);
    }

    public static DistrictRP core() {
        return DistrictRP.get();
    }

    public static ModuleManager modules() {
        return DistrictRP.get().getModuleManager();
    }
}