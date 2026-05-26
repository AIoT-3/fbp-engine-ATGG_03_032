package com.fbp.engine.plugin;

import java.util.List;

public interface NodeProvider {
    /** 이 플러그인이 제공하는 노드 타입 목록 */
    List<NodeDescriptor> getNodeDescriptors();
}