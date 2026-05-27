package com.fbp.engine.plugin;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

@Slf4j
public class PluginScanner {
    private static final String SPI_PATH = "META-INF/services/com.fbp.engine.plugin.NodeProvider";

    public static List<File> scanPlugin(String dirPath){
        log.info(String.format("scanning plugins in dirPath:%s ...", dirPath));

        //jarFile 읽기
        List<File> jarFiles = scanJarFilesInDirectoryPath(dirPath);
        //조기 종료 체크
        if(checkEarlyStop(jarFiles)){return Collections.emptyList();}
        //디렉토리에서 찾은 모든 jar 파일명 추출
        String allJarFileNameInDirectory = extractFileNames(jarFiles);
        //플러그인 규격 준수 필터링
        List<File> validPlugins = filterValidPlugins(jarFiles);
        //플러그인 규격을 준수한 파일명 추출
        String validJarFileNameInDirectory = extractFileNames(validPlugins);

        log.info("... plugin scan complete. discovered JARs: [{}], loaded plugins: [{}]",
                allJarFileNameInDirectory,
                validJarFileNameInDirectory);
        return validPlugins;
    }

    private static List<File> scanJarFilesInDirectoryPath(String dirPath){
        File dir = new File(dirPath);
        File[] files = dir.listFiles();

        if (files == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(files)
                .filter(file -> file.getName().endsWith(".jar"))
                .toList();
    }

    private static boolean checkEarlyStop(List<File> jarFiles){
        if(jarFiles == null || jarFiles.isEmpty()){
            log.info("No JAR files detected");
            return true;
        }
        return false;
    }

    private static String extractFileNames(List<File> files){
        StringBuilder allJarFileNameInDirectoryBuilder = new StringBuilder();
        for(File file : files){
            if(file != null && file.exists()) {
                if (!allJarFileNameInDirectoryBuilder.isEmpty()) {
                    allJarFileNameInDirectoryBuilder.append(", ");
                }
                allJarFileNameInDirectoryBuilder.append(file.getName());
            }
        }
        return allJarFileNameInDirectoryBuilder.toString();
    }

    private static List<File> filterValidPlugins(List<File> jarFiles){
        List<File> validFiles = new ArrayList<>();
        for(File jarFile : jarFiles){
            try(JarFile jar = new JarFile(jarFile)){
                JarEntry jarEntry = jar.getJarEntry(SPI_PATH);
                if(jarEntry!=null){
                    validFiles.add(jarFile);
                }
            } catch (Exception e) {
                log.error("Failed to read JAR... file: {}", jarFile.getName(), e);
            }
        }
        return validFiles;
    }

}
