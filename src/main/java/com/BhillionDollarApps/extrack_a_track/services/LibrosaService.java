package com.BhillionDollarApps.extrack_a_track.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.BhillionDollarApps.extrack_a_track.config.S3FileDownloader;
import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LibrosaService {

    @Autowired
    private S3FileDownloader s3FileDownloader;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Tracks analyzeTrackWithLibrosa(String filePath, Tracks track) {
        try {
            // Ensure the file exists
            File file = new File(filePath);
            if (!file.exists()) {
                throw new IllegalArgumentException("File does not exist at path: " + filePath);
            }

            // Define the path to the Spleeter virtual environment’s Python executable
            String pythonPath = "C:\\Users\\bhill\\spleeter_env\\Scripts\\python.exe";
            String scriptPath = "C:\\Users\\bhill\\Documents\\BhillionDollarApps\\librosa\\librosa_api.py";

            // Run the Python script for Librosa analysis
            ProcessBuilder processBuilder = new ProcessBuilder(pythonPath, scriptPath, filePath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // Capture the output of the script
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // Wait for the process to finish
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Python script execution failed with exit code: " + exitCode + "\nError: " + output.toString());
            }

            // Parse the JSON output
            Map<String, Object> analysisResults = objectMapper.readValue(output.toString(), Map.class);

            // Check for an error field in the JSON output
            if (analysisResults.containsKey("error")) {
                String error = (String) analysisResults.get("error");
                throw new RuntimeException("Error from Python script: " + error);
            }

            // Convert and save the Librosa analysis results to the track
            track.setTempo(((Double) analysisResults.get("tempo")).floatValue());
            track.setSpectralCentroid(((Double) analysisResults.get("spectral_centroid")).floatValue());
            track.setRms(((Double) analysisResults.get("rms")).floatValue());
            track.setSongKey(analysisResults.getOrDefault("key", "Unknown").toString());

            // Serialize additional features as JSON strings for storage in the `Tracks` object
            track.setBeats(objectMapper.writeValueAsString(analysisResults.getOrDefault("beats", new int[0])));
            track.setMelody(objectMapper.writeValueAsString(analysisResults.getOrDefault("melody", new float[0])));
            track.setMfcc(objectMapper.writeValueAsString(analysisResults.getOrDefault("mfcc", new float[0])));
            track.setSpectralFeatures(objectMapper.writeValueAsString(analysisResults.getOrDefault("spectral_features", new HashMap<>())));

            return track;

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Error analyzing track with Librosa", e);
        }
    }


    // Method to format beats
    public String formatBeats(List<Integer> beatsList) {
        int totalBeats = beatsList.size();
        String firstFiveBeats = beatsList.subList(0, Math.min(5, totalBeats)).toString();
        return "Beats: " + totalBeats + " (First 5: " + firstFiveBeats + ")";
    }

    // Method to format melody
    public String formatMelody(List<Float> melodyList) {
        double averageMelody = melodyList.stream()
                                         .mapToDouble(Float::doubleValue)
                                         .average()
                                         .orElse(0.0);
        return "Melody (average): " + String.format("%.3f", averageMelody);
    }

    // Method to format MFCCs
    public String formatMFCCs(List<Float> mfccList) {
        String firstFiveMFCCs = mfccList.subList(0, Math.min(5, mfccList.size()))
                                        .stream()
                                        .map(val -> String.format("%.2f", val))
                                        .collect(Collectors.joining(", "));
        return "MFCCs: [" + firstFiveMFCCs + "] (Total: " + mfccList.size() + ")";
    }

    // Method to format spectral features
    public String formatSpectralFeatures(float spectralCentroid, float rms) {
        return "Spectral Features:\n- Centroid: " + spectralCentroid + "\n- RMS: " + rms;
    }

    // Parse JSON string to a list of integers (beats)
    public List<Integer> parseBeats(String beatsJson) throws IOException {
        return objectMapper.readValue(beatsJson, new TypeReference<List<Integer>>() {});
    }

    // Parse JSON string to a list of floats (melody)
    public List<Float> parseMelody(String melodyJson) throws IOException {
        return objectMapper.readValue(melodyJson, new TypeReference<List<Float>>() {});
    }

    // Parse JSON string to a list of floats (MFCCs)
    public List<Float> parseMFCCs(String mfccJson) throws IOException {
        return objectMapper.readValue(mfccJson, new TypeReference<List<Float>>() {});
    }

    public String analyzeTrack(String bucketName, String keyName) throws IOException {
        // Define a local path for temporary storage
        String tempDir = System.getProperty("java.io.tmpdir");
        String tempFilePath = tempDir + File.separator + keyName.substring(keyName.lastIndexOf("/") + 1);

        // Download the file from S3 using the injected s3FileDownloader instance
        s3FileDownloader.downloadFile(bucketName, keyName, tempFilePath);

        return tempFilePath; // Or return the analysis result if processed further
    }
}
