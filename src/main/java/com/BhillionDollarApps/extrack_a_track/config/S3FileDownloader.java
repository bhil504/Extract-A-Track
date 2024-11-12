package com.BhillionDollarApps.extrack_a_track.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

@Component
public class S3FileDownloader {

    private static final Logger logger = Logger.getLogger(S3FileDownloader.class.getName());

    @Autowired
    private S3Client s3Client;

    public void downloadFile(String bucketName, String s3Key, String localFilePath) throws IOException {
        try {
            // Log start of download
            logger.info("Starting download of file from S3. Bucket: " + bucketName + ", Key: " + s3Key);

            // Create the GetObjectRequest for the S3 object
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            // Define the path and ensure parent directories exist
            Path path = Paths.get(localFilePath);
            Files.createDirectories(path.getParent());

            // Download the file to the specified local path
            try (var s3Object = s3Client.getObject(getObjectRequest)) {
                Files.copy(s3Object, path, StandardCopyOption.REPLACE_EXISTING);
            }

            // Log successful download
            logger.info("File downloaded successfully from S3. Saved to: " + localFilePath);

        } catch (SdkClientException e) {
            // Log and throw client exception
            logger.severe("Error downloading file from S3: " + e.getMessage());
            throw new IOException("Error downloading file from S3", e);
        } catch (IOException e) {
            // Log and rethrow IO exception
            logger.severe("I/O Error during file download: " + e.getMessage());
            throw e;
        }
    }
    
    
    
    
    public Resource downloadFileAsResource(String bucketName, String s3Key) throws IOException {
        String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + s3Key.substring(s3Key.lastIndexOf("/") + 1);

        downloadFile(bucketName, s3Key, tempFilePath);

        Path path = Paths.get(tempFilePath);
        Resource resource = new UrlResource(path.toUri());
        if (resource.exists() && resource.isReadable()) {
            return resource;
        } else {
            throw new IOException("Could not read file: " + tempFilePath);
        }
    }
}
