package com.BhillionDollarApps.extrack_a_track.services;

import com.BhillionDollarApps.extrack_a_track.config.S3FileDownloader;
import com.BhillionDollarApps.extrack_a_track.config.S3FileUploader;
import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.BhillionDollarApps.extrack_a_track.repositories.TracksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SpleeterService {

    @Autowired
    private TracksRepository tracksRepository;

    @Autowired
    private S3FileUploader s3FileUploader;

    @Autowired
    private S3FileDownloader s3FileDownloader;

    private static final Logger logger = Logger.getLogger(SpleeterService.class.getName());
    private final String tempDirPath = "C:\\Users\\Bhillion Dollar Prod\\Documents\\App\\BhillionApp Version 3\\temp files";

    /**
     * Process the given track entity with Spleeter to separate it into stems.
     *
     * @param track     The `Tracks` entity object to process.
     * @param stemCount The number of stems to separate (e.g., 2 or 5).
     * @return A map of stem names to byte arrays representing the separated stems.
     * @throws Exception If an error occurs during processing.
     */
    public Map<String, Object> processWithSpleeter(Tracks track, int stemCount) throws Exception {
        File tempDir = createTempDir();
        String tempWavFilePath = downloadTrackFromS3(track, tempDir);
        String outputDirPath = tempDir.getAbsolutePath() + "/stems";

        // Process with Spleeter
        Map<String, Object> stemsData = processWithSpleeter(tempWavFilePath, outputDirPath, stemCount);

        // Clean up temporary files
        cleanUpTempFiles(tempWavFilePath, new File(outputDirPath));
        
        return stemsData;
    }

    /**
     * Process a track file path with Spleeter and save separated stems to the specified output directory.
     *
     * @param inputFilePath  Path to the input WAV file.
     * @param outputDirPath  Path to the output directory where stems will be saved.
     * @param stemCount      Number of stems to separate (e.g., 2 or 5).
     * @return A map of stem names to byte arrays representing the separated stems.
     * @throws Exception If an error occurs during processing.
     */
    public Map<String, Object> processWithSpleeter(String inputFilePath, String outputDirPath, int stemCount) throws Exception {
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new IOException("Failed to create output directory: " + outputDir.getAbsolutePath());
        }

        ProcessBuilder processBuilder = prepareProcessCommand(inputFilePath, outputDirPath, stemCount);
        Process process = processBuilder.start();

        logProcessOutput(process);

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Spleeter process failed with exit code " + exitCode);
        }

        return readStemsData(outputDir);
    }

    private File createTempDir() throws IOException {
        File tempDir = new File(tempDirPath);
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            throw new IOException("Failed to create temporary directory at: " + tempDir.getAbsolutePath());
        }
        return tempDir;
    }

    private String downloadTrackFromS3(Tracks track, File tempDir) throws IOException {
        String sanitizedTitle = track.getTitle().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        String tempWavFilePath = tempDir.getAbsolutePath() + "/" + track.getId() + "-" + sanitizedTitle + ".wav";
        s3FileDownloader.downloadFile("extract-a-trackbucket", track.getS3Key(), tempWavFilePath);
        logger.info("Downloaded WAV file at: " + tempWavFilePath);
        return tempWavFilePath;
    }

    private ProcessBuilder prepareProcessCommand(String inputFilePath, String outputDirPath, int stemCount) {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "spleeter", "separate", "-i", inputFilePath,
            "-o", outputDirPath,
            "-p", "spleeter:" + stemCount + "stems"
        );
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }

    private void logProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.warning("Spleeter output: " + line);
            }
        }
    }

    private Map<String, Object> readStemsData(File outputDir) throws IOException {
        File[] stemFiles = outputDir.listFiles((dir, name) -> name.endsWith(".wav"));
        if (stemFiles == null || stemFiles.length == 0) {
            throw new IOException("No stem files found. Output directory: " + outputDir.getAbsolutePath());
        }

        Map<String, Object> stemsData = new HashMap<>();
        for (File stemFile : stemFiles) {
            stemsData.put(stemFile.getName(), Files.readAllBytes(stemFile.toPath()));
        }
        return stemsData;
    }

    private void cleanUpTempFiles(String tempWavFilePath, File outputDir) throws IOException {
        Files.deleteIfExists(Paths.get(tempWavFilePath));
        for (File file : Objects.requireNonNull(outputDir.listFiles())) {
            file.delete();
        }
        outputDir.delete();
        logger.info("Temporary files cleaned up.");
    }

    public String saveAndProcessTrack(MultipartFile trackFile, int stemCount) throws IOException, InterruptedException {
        String sanitizedFilename = trackFile.getOriginalFilename().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
        File tempDir = createTempDir();
        File savedTrackFile = new File(tempDir, sanitizedFilename);
        trackFile.transferTo(savedTrackFile);

        String outputDirPath = tempDir.getAbsolutePath() + "/stems";
        try {
            processWithSpleeter(savedTrackFile.getAbsolutePath(), outputDirPath, stemCount);
        } catch (Exception e) {
            // Log the exception and handle it accordingly
            logger.severe("Error processing with Spleeter: " + e.getMessage());
            throw new IOException("Failed to process track with Spleeter", e); // Or handle differently based on needs
        }

        String zipFilePath = tempDirPath + "/" + sanitizedFilename + "_stems.zip";
        zipDirectory(Paths.get(outputDirPath), Paths.get(zipFilePath));

        String s3Key = "user-uploads/" + sanitizedFilename + "/stems.zip";
        s3FileUploader.uploadFile("extract-a-trackbucket", s3Key, zipFilePath);

        deleteDirectory(outputDirPath);
        Files.deleteIfExists(Paths.get(zipFilePath));

        return s3Key;
    }


    private void zipDirectory(Path sourceDirPath, Path zipFilePath) throws IOException {
        try (ZipOutputStream zs = new ZipOutputStream(Files.newOutputStream(zipFilePath))) {
            Files.walk(sourceDirPath).filter(path -> !Files.isDirectory(path)).forEach(path -> {
                ZipEntry zipEntry = new ZipEntry(sourceDirPath.relativize(path).toString());
                try {
                    zs.putNextEntry(zipEntry);
                    Files.copy(path, zs);
                    zs.closeEntry();
                } catch (IOException e) {
                    logger.severe("Error zipping file: " + e.getMessage());
                }
            });
        }
    }

    private void deleteDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                file.delete();
            }
        }
        directory.delete();
    }
}
