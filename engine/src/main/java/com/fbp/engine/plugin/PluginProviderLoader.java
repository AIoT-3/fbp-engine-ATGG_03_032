package com.fbp.engine.plugin;

import java.net.URL;
import java.util.List;

public interface PluginProviderLoader {
    List<NodeProvider> loadProviders(URL jarUrl, ClassLoader parentClassLoader);
}
