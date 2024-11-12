<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Upload New Track</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light p-5">

<div class="container">
    <h2>Upload a New Track</h2>
    
    <!-- Display error and success messages -->
    <c:if test="${not empty successMessage}">
        <div class="alert alert-success">${successMessage}</div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert alert-danger">${errorMessage}</div>
    </c:if>

    <!-- Form to upload a new track -->
    <form id="uploadForm" enctype="multipart/form-data">
        <div class="mb-3">
            <label for="title" class="form-label">Title:</label>
            <input type="text" id="title" name="title" class="form-control" required>
        </div>
        
        <div class="mb-3">
            <label for="genre" class="form-label">Genre:</label>
            <input type="text" id="genre" name="genre" class="form-control" required>
        </div>

        <div class="mb-3">
            <label for="lyrics" class="form-label">Lyrics:</label>
            <textarea id="lyrics" name="lyrics" class="form-control"></textarea>
        </div>

        <div class="mb-3">
            <label for="trackFile" class="form-label">Upload WAV File:</label>
            <input type="file" id="file" name="file" accept=".wav" class="form-control" required>
        </div>

        <!-- Progress bar -->
        <div class="progress mb-3" style="height: 25px;">
            <div id="progressBar" class="progress-bar progress-bar-striped progress-bar-animated bg-info" 
                 role="progressbar" style="width: 0%;">0%
            </div>
        </div>

        <button type="button" class="btn btn-primary m-1" onclick="uploadTrack()">Upload Track</button>
    </form>
</div>

<!-- Link to return to dashboard -->
<a href="/welcome" class="btn btn-secondary m-1">Cancel</a>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
<script>
function uploadTrack() {
    const form = document.getElementById('uploadForm');
    const formData = new FormData(form);
    const xhr = new XMLHttpRequest();

    xhr.open("POST", "/tracks/upload-track", true);

    // Update progress bar
    xhr.upload.onprogress = function(event) {
        if (event.lengthComputable) {
            const percentComplete = (event.loaded / event.total) * 100;
            const progressBar = document.getElementById('progressBar');
            progressBar.style.width = percentComplete + '%';
            progressBar.textContent = Math.round(percentComplete) + '%';
        }
    };

    // Handle upload completion
    xhr.onload = function() {
        const progressBar = document.getElementById('progressBar');
        if (xhr.status === 200) {
            const response = JSON.parse(xhr.responseText);
            if (response.status === "success") {
                progressBar.textContent = 'Upload Complete';
                progressBar.classList.replace("bg-info", "bg-success");

                // Redirect to the URL provided in the response
                setTimeout(() => {
                    window.location.href = response.redirectUrl;
                }, 1000); // 1-second delay to show "Upload Complete" message
            } else {
                console.error(response.message);
                progressBar.textContent = 'Upload Failed';
                progressBar.classList.replace("bg-info", "bg-danger");
            }
        } else {
            progressBar.textContent = 'Upload Failed';
            progressBar.classList.replace("bg-info", "bg-danger");
        }
    };

    // Handle errors
    xhr.onerror = function() {
        console.error("An error occurred during the upload.");
        const progressBar = document.getElementById('progressBar');
        progressBar.textContent = 'Upload Error';
        progressBar.classList.replace("bg-info", "bg-danger");
    };

    xhr.send(formData);
}
</script>



</body>
</html>
