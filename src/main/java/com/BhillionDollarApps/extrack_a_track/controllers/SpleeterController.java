package com.BhillionDollarApps.extrack_a_track.controllers;

import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.BhillionDollarApps.extrack_a_track.repositories.TracksRepository;
import com.BhillionDollarApps.extrack_a_track.services.SpleeterService;
import com.BhillionDollarApps.extrack_a_track.config.S3FileDownloader;
import com.BhillionDollarApps.extrack_a_track.config.S3FileUploader;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

@CrossOrigin(origins = "https://www.bhilliondollar.com")
@Controller
@RequestMapping("/spleeter")
public class SpleeterController {

    private static final Logger logger = Logger.getLogger(SpleeterController.class.getName());
    private final String tempDirPath = System.getProperty("java.io.tmpdir") + "/BhillionAppTemp";
    private final String spleeterPath = "C:/Users/bhill/spleeter_env/Scripts/spleeter";  // Path to Spleeter executable

    @Autowired
    private S3FileDownloader s3FileDownloader;
    @Autowired
    private S3FileUploader s3FileUploader;
    @Autowired
    private TracksRepository tracksRepository;
    @Autowired
    private SpleeterService spleeterService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadTrackToSpleeter(
            @RequestParam("trackFile") MultipartFile trackFile,
            @RequestParam("stems") int stems) {
        try {
            File tempInputFile = File.createTempFile("uploadedTrack", ".wav");
            trackFile.transferTo(tempInputFile);

            File outputDir = new File("src/main/resources/static/stems");
            if (!outputDir.exists()) outputDir.mkdirs();

            ProcessBuilder processBuilder = new ProcessBuilder(
                spleeterPath, "separate", tempInputFile.getAbsolutePath(),
                "-o", outputDir.getAbsolutePath(),
                "-p", "spleeter:" + stems + "stems"
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            captureProcessOutput(process);

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Spleeter process failed with exit code " + exitCode);
            }

            tempInputFile.delete();

            return ResponseEntity.ok().body(Map.of("success", true, "message", "Spleeter processing completed."));
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error during Spleeter processing", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/{id}/export-spleeter")
    public String exportTrackToSpleeter(@PathVariable("id") Long trackId,
                                        @RequestParam("stems") int stems,
                                        RedirectAttributes redirectAttributes) {
        try {
            Tracks track = tracksRepository.findById(trackId)
                    .orElseThrow(() -> new RuntimeException("Track not found with ID: " + trackId));

            if (stems != 2 && stems != 5) {
                throw new IllegalArgumentException("Only 2 or 5 stems are supported.");
            }

            track.setStatus("PROCESSING");
            tracksRepository.save(track);

            String s3Key = track.getS3Key();
            if (s3Key == null || s3Key.isEmpty()) {
                throw new RuntimeException("No S3 key found for track ID: " + trackId);
            }

            // Check for "/original/" in the S3 key
            int originalIndex = s3Key.lastIndexOf("/original/");
            if (originalIndex == -1) {
                logger.warning("The S3 key format for track ID " + trackId + " is incorrect. Expected '/original/' in path.");
                redirectAttributes.addFlashAttribute("error", "Track S3 key format is incorrect. Please re-upload the track.");
                return "redirect:/tracks/" + trackId;
            }
            
            String originalFolder = s3Key.substring(0, originalIndex);
            String tempWavFilePath = tempDirPath + File.separator + track.getId() + "-" + sanitizeTitle(track.getTitle()) + ".wav";
            s3FileDownloader.downloadFile("extract-a-trackbucket", s3Key, tempWavFilePath);

            // Ensure consistent directory path for output
            String outputDirectoryPath = tempDirPath + File.separator + sanitizeTitle(track.getTitle());
            File outputDirectory = new File(outputDirectoryPath);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdirs();
            }

            logger.info("Executing Spleeter command with output directory: " + outputDirectoryPath);

            ProcessBuilder processBuilder = new ProcessBuilder(
                "cmd.exe", "/c",
                "C:/Users/bhill/spleeter_env/Scripts/activate && spleeter separate \"" + tempWavFilePath + "\" -p spleeter:" + stems + "stems -o \"" + outputDirectoryPath + "\""
            );
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logger.info("Spleeter process output: " + line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException("Spleeter process failed with exit code " + exitCode);
            }

            File[] filesInOutputDir = outputDirectory.listFiles();
            if (filesInOutputDir == null || filesInOutputDir.length == 0) {
                throw new IOException("No stem files found in the output directory: " + outputDirectoryPath);
            }

            handleSpleeterResponse(track, originalFolder);

            redirectAttributes.addFlashAttribute("message", "Track processing has started.");
            return "redirect:/tracks/" + trackId;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initiating Spleeter processing for track ID: " + trackId, e);
            redirectAttributes.addFlashAttribute("error", "Failed to initiate track processing.");
            return "redirect:/tracks/" + trackId;
        }
    }






    private void handleSpleeterResponse(Tracks track, String originalFolder) {
        try {
            // Set the base path for stems alongside the original and mp3 folders
            String s3StemsBasePath = originalFolder + "/stems/";

            // Upload stems to S3 with descriptive names and save metadata in MySQL
            uploadSeparatedStemsToS3(tempDirPath + "/" + sanitizeTitle(track.getTitle()), track, s3StemsBasePath);

            // Update track status and save to the database
            track.setStatus("COMPLETED");
            tracksRepository.save(track);
            logger.info("Spleeter processing completed successfully for track: " + track.getTitle());
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error handling the Spleeter response for track: " + track.getTitle(), e);
            track.setStatus("FAILED");
            tracksRepository.save(track);
        }
    }

    private void uploadSeparatedStemsToS3(String outputDirPath, Tracks track, String s3StemsBasePath) throws IOException {
        File outputDir = new File(outputDirPath);

        // Recursively list all WAV files in the output directory
        List<File> stemFiles = new ArrayList<>();
        findStemFiles(outputDir, stemFiles);

        // Check if any stem files are found
        if (stemFiles.isEmpty()) {
            throw new IOException("No stem files found in the output directory: " + outputDirPath);
        }

        // Loop through each file found and map it to the correct descriptive name
        for (File stemFile : stemFiles) {
            String stemName = stemFile.getName().replace(".wav", "").toLowerCase();

            // Define the S3 key using descriptive stem names (e.g., vocals, accompaniment)
            String s3StemKey = s3StemsBasePath + stemName + ".wav";

            // Upload each stem file to S3 with its specific S3 key
            s3FileUploader.uploadFile("extract-a-trackbucket", s3StemKey, stemFile.getAbsolutePath());

            // Update the correct field in the Tracks model with the S3 key
            updateTrackStemFields(track, stemName, s3StemKey);
        }

        // Save the track with updated S3 stem keys in MySQL
        tracksRepository.save(track);
    }
    
    
    
    
 // Helper method to recursively find stem files in the directory
    private void findStemFiles(File dir, List<File> stemFiles) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                // Recursively search in subdirectories
                findStemFiles(file, stemFiles);
            } else if (file.isFile() && file.getName().endsWith(".wav")) {
                // Add WAV files to the list
                stemFiles.add(file);
            }
        }
    }
    
    

    private void updateTrackStemFields(Tracks track, String stemName, String s3StemKey) {
        switch (stemName) {
            case "vocals":
                track.setVocals(s3StemKey);
                break;
            case "accompaniment":
                track.setAccompaniment(s3StemKey);
                break;
            case "piano":
                track.setPiano(s3StemKey);
                break;
            case "bass":
                track.setBass(s3StemKey);
                break;
            case "drums":
                track.setDrums(s3StemKey);
                break;
            case "other":
                track.setOther(s3StemKey);
                break;
            default:
                logger.warning("Unrecognized stem name: " + stemName + ". No field updated.");
                break;
        }
    }

    private void captureProcessOutput(Process process) throws IOException {
        try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                logger.warning("Spleeter output: " + line);
            }
        }
    }

    private String sanitizeTitle(String title) {
        return title.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    private Map<String, Object> parseJson(String jsonResponse) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonResponse, new TypeReference<>() {});
    }

    @GetMapping("/form")
    public String displaySpleeterForm() {
        return "SpleeterForm";
    }
    
    
    
    
    
    
}
