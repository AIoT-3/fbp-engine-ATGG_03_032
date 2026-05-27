package com.fbp.engine.plugin;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class DefaultPluginProviderLoader implements PluginProviderLoader {
    @Override
    public List<NodeProvider> loadProviders(URL jarUrl, ClassLoader parentClassLoader) {
        List<NodeProvider> providers = new ArrayList<>();
        PluginClassLoader pluginLoader = new PluginClassLoader(new URL[]{jarUrl}, parentClassLoader);
        ServiceLoader.load(NodeProvider.class, pluginLoader).forEach(providers::add);
        return providers;
    }
}
