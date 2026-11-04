package dev.glap.api;

import java.util.ServiceLoader;

/** Entry point for APIs provided by the running GLAP server. */
public interface GlapApi {
    FakeEntityFactory fakeEntities();

    GlapServerThread serverThread();

    static GlapApi get() {
        return ProviderHolder.INSTANCE;
    }

    interface Provider {
        GlapApi create();
    }

    final class ProviderHolder {
        private static final GlapApi INSTANCE = ServiceLoader.load(Provider.class, GlapApi.class.getClassLoader())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("GLAP API provider is missing"))
            .create();

        private ProviderHolder() {}
    }
}
