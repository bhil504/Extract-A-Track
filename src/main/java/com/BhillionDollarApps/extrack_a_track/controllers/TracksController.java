package com.BhillionDollarApps.extrack_a_track.controllers;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.core.io.Resource;
import com.BhillionDollarApps.extrack_a_track.config.S3FileDownloader;
import com.BhillionDollarApps.extrack_a_track.config.S3FileUploader;
import com.BhillionDollarApps.extrack_a_track.models.Tracks;
import com.BhillionDollarApps.extrack_a_track.models.User;
import com.BhillionDollarApps.extrack_a_track.repositories.TracksRepository;
import com.BhillionDollarApps.extrack_a_track.services.FileService;
import com.BhillionDollarApps.extrack_a_track.services.TracksService;
import com.BhillionDollarApps.extrack_a_track.services.UserService;
import com.BhillionDollarApps.extrack_a_track.services.LibrosaService;
import java.util.Map;
import java.util.HashMap;
import org.springframework.http.MediaType;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.S3Object;

@Controller
@RequestMapping("/tracks")
public class TracksController {

    private static final Logger logger = Logger.getLogger(TracksController.class.getName());
    private static final String BUCKET_NAME = "extract-a-trackbucket";

    @Autowired
    private TracksService tracksService;
    @Autowired
    private TracksRepository tracksRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private FileService fileService;
    @Autowired
    private LibrosaService librosaService;
    @Autowired
    private HttpSession session;
    @Autowired
    private S3FileDownloader S3FileDownloader;
    @Autowired
    private S3FileUploader S3FileUploader;

    @Autowired
    private S3Client s3Client; // Injected S3Client
    

    
    
    
//Display the form for creating a new track
    @GetMapping("/new")
    public String newTrackForm(Model model) {
        model.addAttribute("track", new Tracks());
        return "newTrack";
    }

    
    
    

    @PostMapping("/upload-track")
    public ResponseEntity<Map<String, String>> uploadTrack(@Valid @ModelAttribute("track") Tracks track,
                                                          @RequestParam("file") MultipartFile file,
                                                          BindingResult result, Model model,
                                                          HttpSession session) {
        Map<String, String> response = new HashMap<>();

        if (result.hasErrors() || file.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Please fill all fields and upload a valid file.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // Get the logged-in user ID from the session
            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                response.put("status", "error");
                response.put("message", "User not logged in. Please log in and try again.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            // Retrieve the user from the database
            User user = userService.getUserByID(userId);
            track.setUser(user); // Set the associated user
            track.setFileName(file.getOriginalFilename());
            track.setStatus("PROCESSING");

            // Save the track metadata to the database (initial save without S3 key)
            tracksRepository.save(track);

            // Define the S3 folder path for the track
            String trackFolderName = track.getFileName().substring(0, track.getFileName().lastIndexOf('.')); // remove extension for folder name
            String s3FolderPath = "user-uploads/" + userId + "/" + trackFolderName + "/original/";

            // Full S3 key for the WAV file within the track folder
            String s3Key = s3FolderPath + track.getFileName();

            // Upload the file to S3
            tracksService.uploadTrackToS3(s3Key, file);

            // Update track metadata with S3 details and mark as completed
            track.setS3Key(s3Key);
            track.setStatus("COMPLETED");

            // Save updated track metadata with the S3 key
            tracksRepository.save(track);

            logger.info("Track uploaded successfully for user: " + userId);

            // Return a JSON response indicating success and a redirect URL
            response.put("status", "success");
            response.put("redirectUrl", "/welcome");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error uploading track: " + e.getMessage(), e);
            response.put("status", "error");
            response.put("message", "An error occurred while uploading the track.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    
    
    
//Method to Download the user-uploaded track as a WAV File from S3 using the S3 key stored in MySQL
    @GetMapping("/{id}/download-wav")
    public ResponseEntity<byte[]> downloadOriginalWav(@PathVariable("id") Long trackId) {
        Tracks track = null; // Declare track outside the try block

        try {
            // Fetch the track from the database using the trackId
            track = tracksService.findTrackById(trackId)
                    .orElseThrow(() -> new RuntimeException("Track not found with ID: " + trackId));

            // Attempt to save the downloaded file in the Downloads directory
            String userDownloadPath = "C:\\Users\\bhill\\Downloads\\" + track.getFileName();

            // Try downloading the file from S3 to the user-specified path
            byte[] wavData = tracksService.downloadWavFromS3(trackId, userDownloadPath);

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + track.getFileName());
            headers.add(HttpHeaders.CONTENT_TYPE, "audio/wav");

            return new ResponseEntity<>(wavData, headers, HttpStatus.OK);

        } catch (Exception e) {
            // If there's an error, handle the fallback
            logger.severe("Failed to download WAV file from S3 or save to the specified location. Retrying in Documents folder.");

            try {
                if (track != null) {
                    // Fallback to saving in the Documents directory
                    String fallbackPath = System.getProperty("user.home") + "\\Documents\\" + track.getFileName();
                    byte[] wavData = tracksService.downloadWavFromS3(trackId, fallbackPath);

                    // Set headers for file download
                    HttpHeaders headers = new HttpHeaders();
                    headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + track.getFileName());
                    headers.add(HttpHeaders.CONTENT_TYPE, "audio/wav");

                    return new ResponseEntity<>(wavData, headers, HttpStatus.OK);
                } else {
                    throw new RuntimeException("Track information is unavailable.");
                }

            } catch (Exception fallbackException) {
                logger.severe("Fallback download attempt failed: " + fallbackException.getMessage());
                throw new RuntimeException("Failed to download WAV file from S3.", fallbackException);
            }
        }
    }
    
    
    
    
    
    @PostMapping("/{id}/convert-to-mp3-async")
    public String convertToMp3Async(@PathVariable("id") Long trackId, RedirectAttributes redirectAttributes) {
        String bucketName = "extract-a-trackbucket"; // Replace with your actual bucket name

        try {
            // Fetch the track details from the database
            Tracks track = tracksService.findTrackById(trackId).orElse(null);
            if (track == null) {
                logger.warning("Track with ID " + trackId + " not found.");
                redirectAttributes.addFlashAttribute("error", "Track not found.");
                return "redirect:/tracks/" + trackId;
            }

            // Step 1: Download the WAV file from S3 using S3Key
            String destinationPath = System.getProperty("java.io.tmpdir") + "/" + track.getUser().getId() + "-" + track.getTitle().replaceAll("[^a-zA-Z0-9-_\\.]", "_") + ".wav";
            S3FileDownloader.downloadFile(bucketName, track.getS3Key(), destinationPath);
            File wavFile = new File(destinationPath);

            // Check if WAV file was successfully downloaded
            if (!wavFile.exists() || wavFile.length() == 0) {
                logger.severe("Failed to download WAV file from S3. File path: " + destinationPath);
                redirectAttributes.addFlashAttribute("error", "Failed to download WAV file from S3.");
                return "redirect:/tracks/" + trackId;
            }

            // Convert the WAV file to a byte array
            byte[] wavData = Files.readAllBytes(wavFile.toPath());

            // Trigger asynchronous conversion
            CompletableFuture<byte[]> mp3DataFuture = fileService.convertWavToMp3Async(wavData);

            // Handle the conversion result asynchronously
            mp3DataFuture.thenAccept(mp3Data -> {
                try {
                    // Define the base folder path for the track, handling cases without "original" subfolder
                    String baseFolder;
                    int originalIndex = track.getS3Key().lastIndexOf("/original/");
                    if (originalIndex != -1) {
                        baseFolder = track.getS3Key().substring(0, originalIndex);
                    } else {
                        baseFolder = "user-uploads/" + track.getUser().getId() + "/" + track.getTitle().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
                    }
                    String mp3FolderPath = baseFolder + "/mp3/";

                    // Generate the MP3 file's full S3 key
                    String mp3S3Key = mp3FolderPath + track.getFileName().replace(".wav", ".mp3");

                    // Create a temporary MP3 file for upload
                    File tempMp3File = File.createTempFile("temp_audio_converted", ".mp3");
                    Files.write(tempMp3File.toPath(), mp3Data);

                    // Upload the MP3 file to the "mp3" subfolder in S3
                    S3FileUploader.uploadFile(bucketName, mp3S3Key, tempMp3File.getAbsolutePath());

                    // Update track details to reference the MP3 file in S3
                    track.setMp3S3Key(mp3S3Key);
                    tracksService.saveTrack(track);

                    // Log successful upload and conversion completion
                    logger.info("Successfully completed conversion and uploaded MP3 file to S3: " + mp3S3Key);

                    // Clean up temporary files
                    if (wavFile.exists() && !wavFile.delete()) {
                        logger.warning("Failed to delete temporary WAV file: " + wavFile.getAbsolutePath());
                    }
                    if (tempMp3File.exists() && !tempMp3File.delete()) {
                        logger.warning("Failed to delete temporary MP3 file: " + tempMp3File.getAbsolutePath());
                    }
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Error during MP3 upload or track update for track ID " + track.getId() + ": " + e.getMessage(), e);
                }
            }).exceptionally(ex -> {
                logger.log(Level.SEVERE, "MP3 conversion failed for track ID " + track.getId() + ": " + ex.getMessage(), ex);
                return null;
            });

            // Inform the user that the conversion has started
            redirectAttributes.addFlashAttribute("message", "Track conversion started. You will be notified once it's complete.");
            return "redirect:/tracks/" + trackId;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to start track conversion for track ID " + trackId + ": " + e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to start track conversion.");
            return "redirect:/tracks/" + trackId;
        }
    }



    
    
    
    
    
//Route to download to converted Mp3 file
    @GetMapping("/{id}/download-mp3")
    public ResponseEntity<Resource> downloadMp3File(@PathVariable("id") Long trackId) {
        try {
            // Fetch the track details from the database
            Optional<Tracks> optionalTrack = tracksService.findTrackById(trackId);
            if (optionalTrack.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            Tracks track = optionalTrack.get();
            
            String bucketName = "extract-a-trackbucket"; // Replace with your actual bucket name

            // Get the S3 key for the MP3 file
            String mp3S3Key = track.getMp3S3Key();

            // Download the MP3 file from S3 to a temporary location
            String tempFilePath = System.getProperty("java.io.tmpdir") + "/" + track.getUser().getId() + "-" + track.getTitle() + ".mp3";
            S3FileDownloader.downloadFile(bucketName, mp3S3Key, tempFilePath);
            File mp3File = new File(tempFilePath);

            // Prepare the resource for download
            InputStreamResource resource = new InputStreamResource(new FileInputStream(mp3File));

            // Create the response entity with the appropriate headers
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + track.getTitle() + ".mp3\"");
            headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(mp3File.length())
                    .body(resource);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    
    
    
    
//Edit an existing track
    @GetMapping("/{id}/edit")
    public String editTrackForm(@PathVariable("id") Long id, Model model) {
        Optional<Tracks> trackOpt = tracksService.findTrackById(id);
        if (trackOpt.isPresent()) {
            model.addAttribute("track", trackOpt.get());
            return "editTrack";
        }
        return "redirect:/welcome";
    }

    
    
    
    

    @PutMapping("/update/{id}")
    public String updateTrack(@PathVariable("id") Long id,
                              @Valid @ModelAttribute("track") Tracks track,
                              @RequestParam("file") MultipartFile file,
                              BindingResult result, Model model, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Validation errors occurred.");
            return "redirect:/tracks/" + id + "/edit";
        }

        try {
            Tracks existingTrack = tracksRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));

            Long userId = (Long) session.getAttribute("userId");
            if (userId == null) {
                redirectAttributes.addFlashAttribute("errorMessage", "User not logged in. Please log in and try again.");
                return "redirect:/tracks/" + id + "/edit";
            }

            // Define the folder path for the track, ensuring it includes the "original" subfolder
            String sanitizedTitle = existingTrack.getTitle().replaceAll("[^a-zA-Z0-9-_\\.]", "_");
            String trackFolder = "user-uploads/" + userId + "/" + sanitizedTitle + "/";
            String originalFolder = trackFolder + "original/";

            // Delete the entire track folder if a new file is uploaded
            if (file != null && !file.isEmpty()) {
                boolean folderDeleted = fileService.deleteFolderFromS3(BUCKET_NAME, trackFolder);
                if (!folderDeleted) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete existing folder from S3.");
                    return "redirect:/tracks/" + id + "/edit";
                }
            }

            // Update track details
            existingTrack.setTitle(track.getTitle());
            existingTrack.setGenre(track.getGenre());
            existingTrack.setLyrics(track.getLyrics());

            // If a new WAV file is uploaded, upload it to the "original" subfolder
            if (file != null && !file.isEmpty()) {
                String originalFileName = file.getOriginalFilename();
                if (!originalFileName.endsWith(".wav")) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Only WAV files are accepted.");
                    return "redirect:/tracks/" + id + "/edit";
                }

                // Define the S3 key with the "original" subfolder
                String newS3Key = originalFolder + originalFileName;

                String tempFilePath = tracksService.storeFileTemporarily(file);
                try {
                    // Upload the new file to the S3 "original" subfolder
                    tracksService.uploadTrackToS3(BUCKET_NAME, newS3Key, tempFilePath);
                } finally {
                    tracksService.deleteTempFile(tempFilePath);
                }

                existingTrack.setS3Key(newS3Key);
                existingTrack.setFileName(originalFileName);
            }

            // Save the updated track details
            tracksRepository.save(existingTrack);
            redirectAttributes.addFlashAttribute("successMessage", "Track updated successfully!");
            return "redirect:/tracks/" + id;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating track: " + e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update track. Please try again.");
            return "redirect:/tracks/" + id + "/edit";
        }
    }





    
    
    
    
// Display an individual track's details
    @GetMapping("/{id}")
    public String showTrack(@PathVariable("id") Long id, Model model) {
        Optional<Tracks> trackOpt = tracksService.findTrackById(id);
        if (trackOpt.isPresent()) {
            model.addAttribute("track", trackOpt.get());
            return "showTrack";
        }
        return "redirect:/welcome";
    }

    
    
    
    

    @DeleteMapping("/delete/{id}")
    public String deleteTrack(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        try {
            Optional<Tracks> trackOpt = tracksRepository.findById(id);
            if (trackOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "Track not found.");
                return "redirect:/welcome";
            }

            Tracks track = trackOpt.get();
            String folderPrefix = track.getS3Key();

            // Find the parent folder of "original/"
            if (folderPrefix != null && folderPrefix.contains("/original/")) {
                folderPrefix = folderPrefix.substring(0, folderPrefix.lastIndexOf("/original/"));
            }

            // Add a trailing slash to delete the entire folder path in S3
            folderPrefix += "/";

            if (folderPrefix != null && !folderPrefix.isEmpty()) {
                boolean deleted = fileService.deleteFolderFromS3(BUCKET_NAME, folderPrefix);
                if (!deleted) {
                    redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete folder from S3.");
                    return "redirect:/welcome";
                }
            }

            tracksRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Track deleted successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/welcome";
    }
    
    
    
    
    
    @GetMapping("/{id}/download-stem")
    public ResponseEntity<InputStreamResource> downloadStem(@PathVariable Long id, @RequestParam String stem) {
        try {
            Tracks track = tracksRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Track not found with ID: " + id));

            // Determine S3 key based on stem type
            String stemS3Key;
            switch (stem.toLowerCase()) {
                case "vocals": stemS3Key = track.getVocals(); break;
                case "accompaniment": stemS3Key = track.getAccompaniment(); break;
                case "bass": stemS3Key = track.getBass(); break;
                case "drums": stemS3Key = track.getDrums(); break;
                case "piano": stemS3Key = track.getPiano(); break;
                case "other": stemS3Key = track.getOther(); break;
                default: throw new IllegalArgumentException("Invalid stem type: " + stem);
            }

            // Check if the stem exists in the database
            if (stemS3Key == null) {
                throw new RuntimeException("Stem file not found for type: " + stem);
            }

            // Prepare S3 download request
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket("extract-a-trackbucket")  // replace with your actual bucket name
                    .key(stemS3Key)
                    .build();

            // Use the injected s3Client instance to get the object
            ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);

            // Create filename in the format "trackName-stem.wav"
            String fileName = track.getTitle().replaceAll("[^a-zA-Z0-9-_\\.]", "_") + "-" + stem + ".wav";

            // Return the stem file as a downloadable resource
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(s3ObjectStream));

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error downloading stem: " + e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }







    
    
    
    
    
//Method to download the user uploaded WAV from S3, send the file through Librosa API, retrieve the corresponding analysis and save the information in MySQL to be displayed on the showTrack.jsp   
    @Autowired
    private S3FileDownloader s3FileDownloader;

    @GetMapping("/downloadAndAnalyze/{trackId}")
    public String downloadAndAnalyze(@PathVariable Long trackId) {
        Optional<Tracks> optionalTrack = tracksService.findTrackById(trackId);
        if (!optionalTrack.isPresent()) {
            return "redirect:/errorPage";
        }

        Tracks track = optionalTrack.get();
        String s3Key = track.getS3Key();
        String downloadDirectory = System.getProperty("java.io.tmpdir");
        String localFilePath = downloadDirectory + "/" + s3Key;

        try {
            s3FileDownloader.downloadFile("extract-a-trackbucket", s3Key, localFilePath);
            File downloadedFile = new File(localFilePath);

            // Process with Librosa and update the track
            librosaService.analyzeTrackWithLibrosa(downloadedFile.getPath(), track);
            tracksService.saveTrack(track);
        } catch (IOException e) {
            e.printStackTrace();
            return "redirect:/errorPage";
        }

        return "redirect:/dashboard";
    }


    
    
   
}
