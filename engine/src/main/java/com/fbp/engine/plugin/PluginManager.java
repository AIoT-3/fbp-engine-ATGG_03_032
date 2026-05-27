package com.fbp.engine.plugin;

import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.core.registry.NodeRegistryException;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

@Slf4j
public class PluginManager {
    private final NodeRegistry nodeRegistry;
    private final String pluginDirectory;
    private final PluginProviderLoader pluginProviderLoader;

    public PluginManager(NodeRegistry nodeRegistry, String pluginDirectory) {
        this(nodeRegistry, pluginDirectory, new DefaultPluginProviderLoader());
    }

    public PluginManager(NodeRegistry nodeRegistry, String pluginDirectory, PluginProviderLoader pluginProviderLoader) {
        this.nodeRegistry = nodeRegistry;
        this.pluginDirectory = pluginDirectory;
        this.pluginProviderLoader = pluginProviderLoader;
    }

    public void loadPlugins() {
        log.info("Loading plugins...");

        List<NodeProvider> allProviders = new ArrayList<>();
        allProviders.addAll(loadClasspathPlugins());
        allProviders.addAll(loadExternalPlugins());

        registerNodeProviders(allProviders);

        log.info("...plugin loading complete.");
    }

    private List<NodeProvider> loadClasspathPlugins() {
        log.info("Scanning classpath for plugins...");
        List<NodeProvider> providers = new ArrayList<>();
        ServiceLoader.load(NodeProvider.class).forEach(providers::add);
        log.info("Found {} classpath plugins.", providers.size());
        return providers;
    }

    private List<NodeProvider> loadExternalPlugins() {
        log.info("Scanning external directory for plugins: {}", pluginDirectory);
        List<NodeProvider> providers = new ArrayList<>();
        List<File> pluginJars = PluginScanner.scanPlugin(pluginDirectory);

        for (File jar : pluginJars) {
            try {
                URL url = jar.toURI().toURL();
                providers.addAll(pluginProviderLoader.loadProviders(url, this.getClass().getClassLoader()));
            } catch (Exception e) {
                log.error("Failed to load plugin from JAR: {}", jar.getName(), e);
            }
        }
        log.info("Found {} external plugins.", providers.size());
        return providers;
    }

    private void registerNodeProviders(List<NodeProvider> providers) {
        int registeredNodeCount = 0;
        for (NodeProvider provider : providers) {
            try {
                List<NodeDescriptor> descriptors = provider.getNodeDescriptors();
                for (NodeDescriptor descriptor : descriptors) {
                    try {
                        nodeRegistry.register(descriptor.typeName(), descriptor.factory());
                        registeredNodeCount++;
                    } catch (NodeRegistryException e) {
                        log.warn("Node type conflict: '{}' is already registered. Skipping.", descriptor.typeName());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to get node descriptors from provider: {}", provider.getClass().getName(), e);
            }
        }
        log.info("Registered {} new nodes from plugins.", registeredNodeCount);
    }
}
