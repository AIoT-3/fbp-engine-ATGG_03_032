package com.fbp.engine.plugin;

import com.fbp.engine.core.node.Node;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

class NodeProviderTest {

    @Test
    @DisplayName("올바른 NodeDescriptor 목록을 반환한다")
    void shouldReturnValidNodeDescriptors() {
        NodeProvider provider = () -> List.of(
                new NodeDescriptor("MqttNode", "MQTT", Node.class, (id, config) -> null),
                new NodeDescriptor("HttpNode", "HTTP", Node.class, (id, config) -> null)
        );

        List<NodeDescriptor> descriptors = provider.getNodeDescriptors();

        assertNotNull(descriptors);
        assertEquals(2, descriptors.size());
    }

    @Test
    @DisplayName("노드를 제공하지 않는 Provider는 빈 리스트를 반환한다")
    void shouldReturnEmptyListWhenNoNodesProvided() {
        NodeProvider emptyProvider = Collections::emptyList;

        List<NodeDescriptor> descriptors = emptyProvider.getNodeDescriptors();

        assertNotNull(descriptors);
        assertTrue(descriptors.isEmpty());
    }

    @Test
    @DisplayName("반환된 descriptor의 typeName과 factory는 null이 아니어야 한다 (정합성)")
    void descriptorFieldsShouldNotBeNull() {
        NodeProvider provider = () -> List.of(
                new NodeDescriptor("FilterNode", "Filter", Node.class, (id, config) -> null)
        );

        List<NodeDescriptor> descriptors = provider.getNodeDescriptors();

        for (NodeDescriptor descriptor : descriptors) {
            assertNotNull(descriptor.typeName());
            assertFalse(descriptor.typeName().isBlank());
            assertNotNull(descriptor.factory());
        }
    }
}