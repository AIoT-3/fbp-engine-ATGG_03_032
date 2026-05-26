package com.fbp.engine.plugin;

import java.util.ServiceLoader;

public class PluginManager {
    public void loadPlugins(){
        ServiceLoader<NodeProvider> serviceLoader = ServiceLoader.load(NodeProvider.class);
    }
}
