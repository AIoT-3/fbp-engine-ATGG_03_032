package com.fbp.engine.plugin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class PluginClassLoaderTest {

    @TempDir
    Path tempDir;

    private File validJarFile;

    @BeforeEach
    void setUp() throws Exception {
        validJarFile = createRealDummyJar(tempDir.resolve("dummy-plugin.jar"), "com.fbp.test.DummyPlugin");
    }

    @Test
    @DisplayName("JAR 로드: 외부 JAR의 클래스를 정상적으로 로드")
    void testLoadClassFromJar() throws Exception {
        URL[] urls = {validJarFile.toURI().toURL()};

        try (PluginClassLoader classLoader = new PluginClassLoader(urls, this.getClass().getClassLoader())) {
            Class<?> loadedClass = classLoader.loadClass("com.fbp.test.DummyPlugin");

            assertNotNull(loadedClass);
            assertEquals("com.fbp.test.DummyPlugin", loadedClass.getName());

            Object instance = loadedClass.getDeclaredConstructor().newInstance();
            assertNotNull(instance);
        }
    }

    @Test
    @DisplayName("클래스 격리: 플러그인 클래스가 엔진의 내부 클래스에 영향을 주지 않음 (독립된 클래스로더)")
    void testClassIsolation() throws Exception {
        URL[] urls = {validJarFile.toURI().toURL()};

        PluginClassLoader classLoader1 = new PluginClassLoader(urls, this.getClass().getClassLoader());
        PluginClassLoader classLoader2 = new PluginClassLoader(urls, this.getClass().getClassLoader());
        
        Class<?> class1 = classLoader1.loadClass("com.fbp.test.DummyPlugin");
        Class<?> class2 = classLoader2.loadClass("com.fbp.test.DummyPlugin");
        
        assertEquals(classLoader1, class1.getClassLoader());
        assertEquals(classLoader2, class2.getClassLoader());
        assertNotSame(class1, class2, "Classes loaded by different ClassLoaders should not be identical");
        
        classLoader1.close();
        classLoader2.close();
    }

    @Test
    @DisplayName("리소스 해제: close() 호출 시 JAR 파일 핸들 해제 (추가 리소스 로드 불가)")
    void testResourceReleaseOnClose() throws Exception {
        URL[] urls = {validJarFile.toURI().toURL()};
        PluginClassLoader classLoader = new PluginClassLoader(urls, this.getClass().getClassLoader());

        assertNotNull(classLoader.loadClass("com.fbp.test.DummyPlugin"));

        classLoader.close();
        
       File anotherJar = createRealDummyJar(tempDir.resolve("another.jar"), "com.fbp.test.Another");
        PluginClassLoader anotherLoader = new PluginClassLoader(new URL[]{anotherJar.toURI().toURL()}, this.getClass().getClassLoader());
        anotherLoader.close();
        
        assertThrows(ClassNotFoundException.class, () -> {
            anotherLoader.loadClass("com.fbp.test.Another");
        }, "Should not be able to load class after classloader is closed");
    }

    @Test
    @DisplayName("존재하지 않는 JAR: 없는 경로의 JAR → 예외")
    void testNonExistentJar() throws Exception {
        File nonExistentFile = tempDir.resolve("not-found.jar").toFile();
        URL[] urls = {nonExistentFile.toURI().toURL()};

        try (PluginClassLoader classLoader = new PluginClassLoader(urls, this.getClass().getClassLoader())) {

            assertThrows(ClassNotFoundException.class, () -> {
                classLoader.loadClass("com.fbp.test.AnyClass");
            });
        }
    }

    private File createRealDummyJar(Path jarPath, String className) throws Exception {
        String simpleName = className.substring(className.lastIndexOf('.') + 1);
        String packageName = className.substring(0, className.lastIndexOf('.'));

        Path sourceDir = tempDir.resolve("src");
        Files.createDirectories(sourceDir);
        Path sourceFile = sourceDir.resolve(simpleName + ".java");
        
        String sourceCode = "package " + packageName + ";\n" +
                            "public class " + simpleName + " {\n" +
                            "    public " + simpleName + "() {}\n" +
                            "}\n";
        Files.writeString(sourceFile, sourceCode);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("Cannot find system Java compiler. Make sure you are running with a JDK, not a JRE.");
        }
        int result = compiler.run(null, null, null, sourceFile.toString());
        if (result != 0) {
            throw new RuntimeException("Compilation failed");
        }

        Path classFile = sourceDir.resolve(simpleName + ".class");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(jarPath.toFile()))) {
            String entryName = className.replace('.', '/') + ".class";
            jos.putNextEntry(new JarEntry(entryName));
            jos.write(Files.readAllBytes(classFile));
            jos.closeEntry();
        }
        
        return jarPath.toFile();
    }
}
