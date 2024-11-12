package com.BhillionDollarApps.extrack_a_track.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.net.http.HttpRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class SpleeterUtility {

    private static final String SPLEETER_COMMAND = "spleeter separate -p spleeter:4stems -o ";

    /**
     * Extracts stems from a given audio file using Spleeter.
     * @param inputFilePath The path to the original audio file.
     * @param outputFolderPath The folder path where the extracted stems should be stored.
     * @return A map containing the paths to the extracted stem files.
     * @throws IOException if an error occurs during processing.
     * @throws InterruptedException if the Spleeter command is interrupted.
     */
    public Map<String, String> extractStems(String inputFilePath, String outputFolderPath) throws IOException, InterruptedException {
        // Ensure the output folder exists
        File outputFolder = new File(outputFolderPath);
        if (!outputFolder.exists()) {
            outputFolder.mkdirs();
        }

        // Construct the command to run Spleeter
        String command = SPLEETER_COMMAND + outputFolderPath + " " + inputFilePath;

        // Execute the Spleeter command
        Process process = Runtime.getRuntime().exec(command);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Spleeter processing failed with exit code " + exitCode);
        }

        // Prepare the map of output stem paths
        Map<String, String> stemPaths = new HashMap<>();
        stemPaths.put("bass", outputFolderPath + "/bass.wav");
        stemPaths.put("piano", outputFolderPath + "/piano.wav");
        stemPaths.put("vocals", outputFolderPath + "/vocals.wav");
        stemPaths.put("drums", outputFolderPath + "/drums.wav");

        return stemPaths;
    }

    /**
     * Converts extracted stems to other formats if needed.
     * This is a placeholder for additional conversion logic.
     */
    public Map<String, String> convertToOtherFormats(String inputFilePath, String outputFolderPath) {
        // Placeholder for conversion logic, e.g., converting to MP3
        return new HashMap<>();
    }
    
    private static HttpRequest.BodyPublisher ofFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        String boundary = "JavaBoundary";
        var byteArrays = new ArrayList<byte[]>();
        
        // Prefix for the file
        byteArrays.add(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + path.getFileName() + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(("Content-Type: audio/wav\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        byteArrays.add(Files.readAllBytes(path));
        byteArrays.add(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        
        return HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
    }
    
    

}
