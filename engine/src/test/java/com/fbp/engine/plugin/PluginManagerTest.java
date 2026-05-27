package com.fbp.engine.plugin;

import com.fbp.engine.core.registry.NodeFactory;
import com.fbp.engine.core.registry.NodeRegistry;
import com.fbp.engine.core.registry.NodeRegistryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PluginManagerTest {

    @Mock
    private NodeRegistry nodeRegistry;

    @Mock
    private PluginProviderLoader pluginProviderLoader;

    private PluginManager pluginManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        pluginManager = new PluginManager(nodeRegistry, tempDir.toString(), pluginProviderLoader);
    }

    @Test
    @DisplayName("ClassPath 플러그인 로드: ServiceLoader로 ClassPath 내 NodeProvider 자동 발견 및 등록")
    void testLoadClasspathPlugins() {
        pluginManager.loadPlugins();
        verifyNoInteractions(pluginProviderLoader);
    }

    @Test
    @DisplayName("외부 JAR 로드: plugins/ 디렉토리의 JAR에서 NodeProvider 발견 및 등록")
    void testLoadExternalJarPlugin() throws Exception {
        Path pluginJar = tempDir.resolve("test-plugin.jar");
        createEmptyJarWithServiceFile(pluginJar, "com.fbp.test.TestNodeProvider");

        NodeProvider mockProvider = createMockNodeProvider("test-node");
        when(pluginProviderLoader.loadProviders(any(), any())).thenReturn(Collections.singletonList(mockProvider));

        pluginManager.loadPlugins();
        verify(pluginProviderLoader, times(1)).loadProviders(eq(pluginJar.toUri().toURL()), any());
        verify(nodeRegistry, times(1)).register(eq("test-node"), any(NodeFactory.class));
    }

    @Test
    @DisplayName("NodeRegistry 자동 등록: 로드된 플러그인의 노드 타입이 NodeRegistry에 등록됨")
    void testNodeRegistryAutoRegistration() throws Exception {
        testLoadExternalJarPlugin();
    }

    @Test
    @DisplayName("타입 충돌 처리: 내장 노드와 동일한 typeName의 플러그인 노드 → 정책에 맞게 처리")
    void testNodeTypeConflict() throws Exception {
        Path pluginJar = tempDir.resolve("conflict-plugin.jar");
        createEmptyJarWithServiceFile(pluginJar, "com.fbp.test.ConflictNodeProvider");

        NodeProvider mockProvider = createMockNodeProvider("conflict-node");
        when(pluginProviderLoader.loadProviders(any(), any())).thenReturn(Collections.singletonList(mockProvider));

        doThrow(new NodeRegistryException("Already registered"))
                .when(nodeRegistry).register(eq("conflict-node"), any(NodeFactory.class));

        pluginManager.loadPlugins();

        verify(nodeRegistry, times(1)).register(eq("conflict-node"), any(NodeFactory.class));
    }

    @Test
    @DisplayName("잘못된 JAR: 유효하지 않은 JAR 파일 → 예외 후 나머지 플러그인은 정상 로드")
    void testInvalidJarAndValidJar() throws Exception {
        Files.createFile(tempDir.resolve("invalid.jar"));
        Path validJar = tempDir.resolve("valid-plugin.jar");
        createEmptyJarWithServiceFile(validJar, "com.fbp.test.ValidNodeProvider");

        NodeProvider mockProvider = createMockNodeProvider("valid-node");
        when(pluginProviderLoader.loadProviders(eq(validJar.toUri().toURL()), any()))
                .thenReturn(Collections.singletonList(mockProvider));

        pluginManager.loadPlugins();

        verify(pluginProviderLoader, times(1)).loadProviders(any(), any());
        verify(pluginProviderLoader, times(1)).loadProviders(eq(validJar.toUri().toURL()), any());
        verify(nodeRegistry, times(1)).register(eq("valid-node"), any(NodeFactory.class));
    }

    @Test
    @DisplayName("plugins 디렉토리 없음: 디렉토리가 없으면 스캔 건너뜀 (예외 아님)")
    void testNoPluginsDirectory() {
        PluginManager manager = new PluginManager(nodeRegistry, "/non/existent/dir", pluginProviderLoader);

        manager.loadPlugins();

        verifyNoInteractions(pluginProviderLoader);
        verifyNoInteractions(nodeRegistry);
    }

    @Test
    @DisplayName("빈 plugins 디렉토리: 디렉토리는 있지만 JAR가 없으면 정상 (플러그인 0개)")
    void testEmptyPluginsDirectory() {
        pluginManager.loadPlugins();

        verifyNoInteractions(pluginProviderLoader);
        verifyNoInteractions(nodeRegistry);
    }

    @Test
    @DisplayName("플러그인 수 확인: 복수 JAR 로드 시 전체 등록된 노드 타입 수가 예상과 일치")
    void testLoadMultipleJars() throws Exception {
        Path jar1 = tempDir.resolve("plugin1.jar");
        createEmptyJarWithServiceFile(jar1, "com.fbp.test.Provider1");
        Path jar2 = tempDir.resolve("plugin2.jar");
        createEmptyJarWithServiceFile(jar2, "com.fbp.test.Provider2");

        NodeProvider provider1 = createMockNodeProvider("node1");
        NodeProvider provider2 = createMockNodeProvider("node2");
        when(pluginProviderLoader.loadProviders(eq(jar1.toUri().toURL()), any())).thenReturn(Collections.singletonList(provider1));
        when(pluginProviderLoader.loadProviders(eq(jar2.toUri().toURL()), any())).thenReturn(Collections.singletonList(provider2));

        pluginManager.loadPlugins();

        verify(nodeRegistry, times(1)).register(eq("node1"), any(NodeFactory.class));
        verify(nodeRegistry, times(1)).register(eq("node2"), any(NodeFactory.class));
    }

    private NodeProvider createMockNodeProvider(String nodeName) {
        NodeProvider mockProvider = mock(NodeProvider.class);
        NodeDescriptor mockDescriptor = mock(NodeDescriptor.class);
        when(mockDescriptor.typeName()).thenReturn(nodeName);
        when(mockDescriptor.factory()).thenReturn(mock(NodeFactory.class));
        when(mockProvider.getNodeDescriptors()).thenReturn(Collections.singletonList(mockDescriptor));
        return mockProvider;
    }

    private void createEmptyJarWithServiceFile(Path jarPath, String providerClassName) throws IOException {
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(Files.newOutputStream(jarPath))) {
            jos.putNextEntry(new java.util.jar.JarEntry("META-INF/services/com.fbp.engine.plugin.NodeProvider"));
            jos.write(providerClassName.getBytes());
            jos.closeEntry();
        }
    }
}
