package com.BhillionDollarApps.extrack_a_track.services;

import com.BhillionDollarApps.extrack_a_track.config.S3FileDownloader;
import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.BhillionDollarApps.extrack_a_track.repositories.TracksRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class TracksService {

    private static final Logger logger = Logger.getLogger(TracksService.class.getName());

    @Autowired
    private TracksRepository tracksRepository;

    @Autowired
    private FileService fileService;

    @Autowired
    private S3Client s3Client; // Use S3Client for AWS SDK v2

    @Autowired
    private S3FileDownloader s3FileDownloader;

    private final String bucketName = "extract-a-trackbucket"; // Set your bucket name

    // Method to upload a file to S3
    public void uploadTrackToS3(String s3Key, MultipartFile file) throws IOException {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            logger.info("File uploaded to S3 with key: " + s3Key);
        } catch (Exception e) {
            logger.severe("Failed to upload file to S3: " + e.getMessage());
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    // Save a new track with metadata
    public Tracks saveTrack(Tracks track) {
        return tracksRepository.save(track);
    }

    // Find a track by its ID
    public Optional<Tracks> findTrackById(Long id) {
        return tracksRepository.findById(id);
    }

    // Find all tracks for a specific user
    public List<Tracks> findTracksByUserId(Long userId) {
        return tracksRepository.findByUserId(userId);
    }
    
    // Method to upload a file to S3
    public void uploadTrackToS3(String bucketName, String s3Key, String filePath) {
        try {
            File file = new File(filePath);
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build(),
                    RequestBody.fromFile(file));

            System.out.println("File uploaded to S3 with key: " + s3Key);
        } catch (Exception e) {
            System.err.println("Failed to upload file to S3: " + e.getMessage());
            throw new RuntimeException("S3 upload failed", e);
        }
    }

    // Method to download WAV from S3
    public byte[] downloadWavFromS3(Long trackId, String downloadDirectory) {
        try {
            Tracks track = tracksRepository.findById(trackId)
                    .orElseThrow(() -> new RuntimeException("Track not found with ID: " + trackId));
            String s3Key = track.getS3Key();

            String localFilePath = downloadDirectory + "/track_" + trackId + ".wav";

            s3FileDownloader.downloadFile(bucketName, s3Key, localFilePath);

            Path filePath = Paths.get(localFilePath);
            return Files.readAllBytes(filePath);

        } catch (Exception e) {
            logger.severe("Failed to download WAV file from S3: " + e.getMessage());
            throw new RuntimeException("Failed to download WAV file from S3.");
        }
    }

    // Method to delete a track from S3
    public void deleteTrackFromS3(String s3Key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build());
            logger.info("File deleted from S3 with key: " + s3Key);
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file from S3 with key: " + s3Key, e);
        }
    }

    // Method to store a file temporarily
    public String storeFileTemporarily(MultipartFile file) {
        try {
            File tempFile = File.createTempFile("temp-", file.getOriginalFilename());
            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                fos.write(file.getBytes());
            }
            return tempFile.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Error saving temporary file", e);
        }
    }

    // Method to delete a temporary file
    public void deleteTempFile(String tempFilePath) {
        File tempFile = new File(tempFilePath);
        if (tempFile.exists() && !tempFile.delete()) {
            logger.warning("Failed to delete temporary file: " + tempFilePath);
        }
    }

    // Method to upload a track to S3 from a file path
    public void uploadTrackToS3(String s3Key, String filePath) {
        try {
            s3Client.putObject(PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build(), RequestBody.fromFile(Paths.get(filePath)));
            logger.info("File uploaded to S3 with key: " + s3Key);
        } catch (Exception e) {
            throw new RuntimeException("Error uploading file to S3 with key: " + s3Key, e);
        }
    }
    
    
    
    public void deleteS3Folder(String bucketName, String folderPrefix) {
        try {
            ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderPrefix)
                    .build();
            
            ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);
            
            for (S3Object s3Object : response.contents()) {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(bucketName)
                        .key(s3Object.key())
                        .build());
                logger.info("Deleted file from S3: " + s3Object.key());
            }
            
            logger.info("Successfully deleted folder and contents from S3: " + folderPrefix);
        } catch (Exception e) {
            logger.severe("Error deleting folder from S3 with prefix: " + folderPrefix);
            throw new RuntimeException("Error deleting folder from S3 with prefix: " + folderPrefix, e);
        }}
    
    
    
    
    public void deleteTrackById(Long id) {
        Optional<Tracks> trackOpt = tracksRepository.findById(id);
        if (trackOpt.isPresent()) {
            Tracks track = trackOpt.get();
            String folderPrefix = "user-uploads/" + track.getUser().getId() + "/" + track.getTitle().replaceAll("[^a-zA-Z0-9-_\\.]", "_") + "/";
            deleteS3Folder(bucketName, folderPrefix);
        }
        tracksRepository.deleteById(id);
    }


    // Additional methods for retrieving and managing tracks can go here
}
