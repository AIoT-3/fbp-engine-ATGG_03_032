package com.fbp.engine.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Slf4j
public class PluginScanner {

    public static void jarFileScan(String dirPath){
        log.debug("--- Scanning for JARs in directory path:{}", dirPath);

        File dirFile = new File(dirPath);

        File[] jarFiles = dirFile.listFiles((file, name) -> name.endsWith(".jar"));

        if (jarFiles == null){
            log.debug("Failed to scan directory: {}", dirPath);
            return;
        }
        if(jarFiles.length == 0) {
            log.debug("Scanning completed... No JAR files detected in directory path:{} ---", dirPath);
            return;
        }

        //log.debug("Found {} JAR files: {}", jarFiles.length, foundFileNames);


        for (File file : jarFiles) {
            System.out.println("--- Scanning JAR: " + file.getName() + " ---");

            try (JarFile jar = new JarFile(file)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    System.out.println("  > " + entry.getName());
                }
            } catch (IOException e) {
                System.err.println("Failed to read " + file.getName());
            }
        }
    }
}
