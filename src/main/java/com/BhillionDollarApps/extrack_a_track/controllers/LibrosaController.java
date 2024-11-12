package com.BhillionDollarApps.extrack_a_track.controllers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.BhillionDollarApps.extrack_a_track.config.S3FileDownloader;
import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.BhillionDollarApps.extrack_a_track.repositories.TracksRepository;
import com.BhillionDollarApps.extrack_a_track.services.LibrosaService;

@Controller
@RequestMapping("/librosa")
public class LibrosaController {


    @Autowired
    private TracksRepository tracksRepository;
    @Autowired
    private LibrosaService librosaService;
    @Autowired
    private S3FileDownloader s3FileDownloader;

//Method from Analyze with Librosa Button to download the corresponding track and send it to Librosa for analysis
    @PostMapping("/analyzeTrack/{trackId}")
    public String analyzeTrack(@PathVariable("trackId") Long trackId, Model model) {
        // Fetch the track from the database
        Tracks track = tracksRepository.findById(trackId)
                .orElseThrow(() -> new IllegalArgumentException("Track not found for id: " + trackId));

        // Ensure the track has an S3 key for the original WAV data
        if (track.getS3Key() == null || track.getS3Key().isEmpty()) {
            throw new IllegalArgumentException("No S3 key found for track with id: " + trackId);
        }

        try {
            // Create a temporary file using the track title as the file name
            String fileName = track.getTitle().replaceAll("\\s+", "_") + ".wav";
            Path tempFilePath = Files.createTempFile(fileName, ".wav");

            // Download the WAV file from S3 to the temporary file using the injected instance
            String bucketName = "extract-a-trackbucket"; // Replace with your actual bucket name
            s3FileDownloader.downloadFile(bucketName, track.getS3Key(), tempFilePath.toString());

            // Perform the Librosa analysis by passing the temporary file
            track = librosaService.analyzeTrackWithLibrosa(tempFilePath.toString(), track);

            // Format the analysis results for better readability
            String formattedBeats = librosaService.formatBeats(librosaService.parseBeats(track.getBeats()));
            String formattedMelody = librosaService.formatMelody(librosaService.parseMelody(track.getMelody()));
            String formattedMFCCs = librosaService.formatMFCCs(librosaService.parseMFCCs(track.getMfcc()));

            // Update the track with formatted results
            track.setBeats(formattedBeats);
            track.setMelody(formattedMelody);
            track.setMfcc(formattedMFCCs);

            // Save the updated track with analysis results
            tracksRepository.save(track);

            // Clean up the temporary file after processing
            Files.deleteIfExists(tempFilePath);

            // Add updated track to the model
            model.addAttribute("track", track);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error handling WAV file for analysis", e);
        }

        // Redirect to the showTrack page to display the results
        return "redirect:/tracks/" + trackId;
    }
}
