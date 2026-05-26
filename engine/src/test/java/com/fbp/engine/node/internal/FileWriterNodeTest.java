package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FileWriterNodeTest {

    FileWriterNode target;
    File testFile;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        testFile = tempDir.resolve("test.txt").toFile();

        target = new FileWriterNode("target", testFile.getAbsolutePath());
    }

    @Order(1)
    @Test
    @DisplayName("파일 생성")
    void checkFileCreated() {
        target.initialize();

        Assertions.assertTrue(testFile.exists());
    }

    @Order(2)
    @Test
    @DisplayName("내용 기록")
    void writeContent() throws IOException {
        target.initialize();

        for(int i = 0; i < 3; i++) {
            target.process("in", new Message(Map.of("test" + i, "value" + i)));
        }

        int lineCount = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(testFile))) {
            while (bufferedReader.readLine() != null) {
                lineCount++;
            }
        }

        Assertions.assertEquals(3, lineCount);
    }

    @Order(3)
    @Test
    @DisplayName("shutdown 후 파일 닫힘")
    void ifShutdownThenFileClose() throws InterruptedException {
        target.initialize();
        target.shutdown();

        Thread.sleep(500);

        Assertions.assertThrows(Exception.class,
                () -> target.process("in", new Message(Map.of("test", "value"))));
    }
}