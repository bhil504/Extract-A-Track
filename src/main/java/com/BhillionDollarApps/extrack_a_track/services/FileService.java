package com.BhillionDollarApps.extrack_a_track.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
@EnableAsync
public class FileService {

    private static final Logger logger = Logger.getLogger(FileService.class.getName());

    private final S3Client s3Client;

    @Autowired
    public FileService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Async
    public CompletableFuture<byte[]> convertWavToMp3Async(byte[] wavData) {
        byte[] mp3Data = null;
        try {
            mp3Data = convertWavToMp3UsingFFmpeg(wavData);
        } catch (Exception e) {
            throw new RuntimeException("MP3 conversion failed: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(mp3Data);
    }

    private byte[] convertWavToMp3UsingFFmpeg(byte[] wavData) throws IOException, InterruptedException {
        File tempWavFile = File.createTempFile("temp_audio", ".wav");
        File tempMp3File = File.createTempFile("temp_audio_converted", ".mp3");

        Files.write(tempWavFile.toPath(), wavData);

        String[] command = {
                "ffmpeg", "-y", "-i", tempWavFile.getAbsolutePath(),
                "-codec:a", "libmp3lame", "-b:a", "192k", tempMp3File.getAbsolutePath()
        };
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();
        process.waitFor();

        byte[] mp3Data = Files.readAllBytes(tempMp3File.toPath());

        tempWavFile.delete();
        tempMp3File.delete();

        return mp3Data;
    }

    public String uploadFileToS3(Path filePath, String bucketName, String s3Key) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build(),
                    filePath);

            logger.info("File uploaded to S3 with key: " + s3Key);
            return s3Key;
        } catch (Exception e) {
            logger.severe("Error uploading file to S3: " + e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public boolean deleteFolderFromS3(String bucketName, String folderPrefix) {
        try {
            ListObjectsV2Request listObjectsRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(folderPrefix)
                    .build();

            ListObjectsV2Response listObjectsResponse;
            do {
                listObjectsResponse = s3Client.listObjectsV2(listObjectsRequest);

                List<ObjectIdentifier> keysToDelete = listObjectsResponse.contents().stream()
                        .map(S3Object::key)
                        .map(key -> ObjectIdentifier.builder().key(key).build())
                        .collect(Collectors.toList());

                if (!keysToDelete.isEmpty()) {
                    DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                            .bucket(bucketName)
                            .delete(Delete.builder().objects(keysToDelete).build())
                            .build();
                    s3Client.deleteObjects(deleteRequest);
                }

                listObjectsRequest = listObjectsRequest.toBuilder()
                        .continuationToken(listObjectsResponse.nextContinuationToken())
                        .build();
            } while (listObjectsResponse.isTruncated());

            return true;
        } catch (Exception e) {
            logger.severe("Error deleting folder from S3: " + e.getMessage());
            return false;
        }
    }
}
