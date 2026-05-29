package com.fbp.engine.node.internal;

import com.fbp.engine.core.connection.Connection;
import com.fbp.engine.message.ErrorMessage;
import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FileWriterNodeTest {

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

        Thread.sleep(100);

        Connection errorConnection = new Connection("err-conn");
        target.getOutputPort("_error").connect(errorConnection);

        Assertions.assertDoesNotThrow(() ->
                target.process("in", new Message(Map.of("test", "value")))
        );

        Message errorMsg = errorConnection.poll();
        Assertions.assertNotNull(errorMsg, "An error message should be sent to the error port.");
        ErrorMessage errorMessage = assertInstanceOf(ErrorMessage.class, errorMsg, "The message should be an ErrorMessage.");
        
        Exception cause = errorMessage.getException();
        RuntimeException runtimeException = assertInstanceOf(RuntimeException.class, cause, "The wrapper exception should be RuntimeException");
        assertInstanceOf(IOException.class, runtimeException.getCause(), "The root cause should be IOException");
        Assertions.assertEquals("Stream closed", runtimeException.getCause().getMessage());
    }
}
