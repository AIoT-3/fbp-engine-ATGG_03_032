package com.fbp.engine.node.internal;

import com.fbp.engine.message.Message;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.Map;

public class FileWriterNodeTest {
    FileWriterNode target;

    @BeforeEach
    void setUp(){
        target = new FileWriterNode("target", "submit/test.txt");
    }

    @Order(1)
    @Test
    @DisplayName("파일 생성")
    void checkFileCreated(){
        target.initialize();

        File file = new File("submit/test.txt");

        Assertions.assertTrue(file.exists());
    }

    @Order(2)
    @Test
    @DisplayName("내용 기록")
    void writeContent(){
        target.initialize();

        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader("submit/test.txt"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            while ((bufferedReader.readLine()) != null) {}
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for(int i=0; i<3; i++) {
            target.process("in", new Message(Map.of("test" + i, "value" + i)));
        }

        int lineCount = 0;
        String s2 = null;
        try {
            while ((s2 = bufferedReader.readLine()) != null) {
                lineCount++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertEquals(3, lineCount);
    }

    @Order(3)
    @Test
    @DisplayName("shutdown 후 파일 닫힘")
    void ifShutdownThenFileClose(){
        target.initialize();
        target.shutdown();

        Assertions.assertThrows(RuntimeException.class,
                () -> target.process("in", new Message(Map.of("test","value"))));
    }
}
