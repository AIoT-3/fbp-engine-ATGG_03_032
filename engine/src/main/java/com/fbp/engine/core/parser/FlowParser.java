package com.fbp.engine.core.parser;

import java.io.InputStream;

public interface FlowParser {
    FlowDefinition parse(InputStream inputStream);
}
