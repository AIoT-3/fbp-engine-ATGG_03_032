package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import com.fbp.engine.core.node.AbstractNode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FileWriterNode extends AbstractNode {
    private String filePath;
    private BufferedWriter writer;

    public FileWriterNode(String id, String filePath) {
        super(id);
        if(filePath == null || filePath.isBlank()){
            throw new IllegalArgumentException("filePath must be notBlank");
        }
        this.filePath = filePath;

        addInputPort("in");
    }

    @Override
    public void initialize() {
        File file = new File(filePath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            this.writer = new BufferedWriter(new FileWriter(filePath, true));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onProcess(String portName, Message message) {
        try {
            writer.write(message.toString());
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void shutdown() {
    if(writer != null) {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    }
}
