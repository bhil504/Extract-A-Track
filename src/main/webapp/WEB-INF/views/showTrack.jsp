<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Track Details</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light p-5">
    <div class="shadow-lg p-3 m-2 rounded">
        <div class="container">
            <h1>"<c:out value="${track.title}"/> " Details Page</h1>
            
            <!-- Feedback Messages -->
            <c:if test="${not empty message}">
                <div class="alert alert-success">${message}</div>
            </c:if>
            <c:if test="${not empty error}">
                <div class="alert alert-danger">${error}</div>
            </c:if>
        
            <!-- Download Original WAV File -->
            <form action="/tracks/${track.id}/download-wav" method="get">
                <button type="submit" class="btn btn-outline-primary shadow text-white mt-2 mb-2">Download Original WAV</button>
            </form>

            <!-- Convert to MP3 Button with Progress Bar -->
            <form id="convertForm" action="${pageContext.request.contextPath}/tracks/${track.id}/convert-to-mp3-async" method="post" onsubmit="startConvert(event)">
                <button type="submit" class="btn btn-outline-warning shadow text-white mt-2 mb-2">Convert to MP3</button>
            </form>
            <div class="progress mb-3">
                <div id="convertProgressBar" class="progress-bar progress-bar-striped bg-warning" role="progressbar" style="width: 0%">0%</div>
            </div>

            <!-- Download Converted MP3 File -->
            <form action="/tracks/${track.id}/download-mp3" method="get">
                <button type="submit" class="btn btn-primary shadow text-white mt-2 mb-2">Download MP3</button>
            </form>
        </div>
    </div>

    <!-- Export to Spleeter Form with Progress Bar -->
    <div class="shadow-lg p-3 m-2 rounded">
        <h2>Separate Track With Spleeter</h2>
        <form id="spleeterForm" method="post" action="${pageContext.request.contextPath}/spleeter/${track.id}/export-spleeter" onsubmit="startSpleeter(event)">
            <label for="stems">Number of Stems:</label>
            <select name="stems" id="stems">
                <option value="2">2 Stems (Vocals and Accompaniment)</option>
                <option value="5">5 Stems (Vocals, Bass, Drums, Piano, and Other)</option>
            </select>
            <button type="submit" class="btn btn-primary shadow text-white mt-2 mb-2">Process Track</button>
        </form>
        <div class="progress mb-3">
            <div id="spleeterProgressBar" class="progress-bar progress-bar-striped bg-primary" role="progressbar" style="width: 0%">0%</div>
        </div>
    </div>
	
	
	<div class="shadow-lg p-3 m-2 rounded">
	    <h2>Download Individual Stems</h2>

	    <c:if test="${not empty track.vocals}">
	        <form action="/tracks/${track.id}/download-stem" method="get">
	            <input type="hidden" name="stem" value="vocals">
	            <button type="submit" class="btn btn-outline-info shadow text-white mt-2 mb-2">Download Vocals</button>
	        </form>
	    </c:if>

	    <c:if test="${not empty track.accompaniment}">
	        <form action="/tracks/${track.id}/download-stem" method="get">
	            <input type="hidden" name="stem" value="accompaniment">
	            <button type="submit" class="btn btn-outline-info shadow text-white mt-2 mb-2">Download Accompaniment</button>
	        </form>
	    </c:if>

	    <c:if test="${not empty track.bass}">
	        <form action="/tracks/${track.id}/download-stem" method="get">
	            <input type="hidden" name="stem" value="bass">
	            <button type="submit" class="btn btn-outline-info shadow text-white mt-2 mb-2">Download Bass</button>
	        </form>
	    </c:if>

	    <c:if test="${not empty track.drums}">
	        <form action="/tracks/${track.id}/download-stem" method="get">
	            <input type="hidden" name="stem" value="drums">
	            <button type="submit" class="btn btn-outline-info shadow text-white mt-2 mb-2">Download Drums</button>
	        </form>
	    </c:if>

	    <c:if test="${not empty track.piano}">
	        <form action="/tracks/${track.id}/download-stem" method="get">
	            <input type="hidden" name="stem" value="piano">
	            <button type="submit" class="btn btn-outline-info shadow text-white mt-2 mb-2">Download Piano</button>
	        </form>
	    </c:if>

	    <c:if test="${not empty track.other}">
	        <form action="/tracks/${track.id}/download-stem" method="get">
	            <input type="hidden" name="stem" value="other">
	            <button type="submit" class="btn btn-outline-info shadow text-white mt-2 mb-2">Download Other</button>
	        </form>
	    </c:if>
	</div>



    <!-- Track Details Section -->
    <div class="shadow-lg p-3 m-2 rounded">
        <h2>Track Details:</h2>
        <p><strong>Title:</strong> ${track.title}</p>
        <p><strong>Genre:</strong> ${track.genre}</p>
        <p><strong>Status:</strong> ${track.status}</p>
        <p><strong>Lyrics:</strong> ${track.lyrics}</p>
    </div>

    <!-- Librosa Analysis Section with Progress Bar -->
    <div class="shadow-lg p-3 m-2 rounded">
        <h2>Analyze Track with Librosa</h2>
        <form id="librosaForm" method="post" action="/librosa/analyzeTrack/${track.id}" onsubmit="startLibrosa(event)">
            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
            <button type="submit" class="btn btn-info m-1">Analyze Track</button>
        </form>
        <div class="progress mb-3">
            <div id="librosaProgressBar" class="progress-bar progress-bar-striped bg-info" role="progressbar" style="width: 0%">0%</div>
        </div>
        
        <!-- Display Librosa Analysis Results -->
        <div class="mb-4 m-2">
            <h5>Track Analysis Results from Librosa:</h5>
            <c:if test="${not empty track.tempo}">
                <p>Tempo: ${track.tempo}</p>
                <p>${track.spectralFeatures}</p>
                <p>Key: ${track.songKey}</p>
                <p>${track.beats}</p>
                <p>${track.melody}</p>
                <p>${track.mfcc}</p>
            </c:if>
        </div>
    </div>

    <!-- Edit, Delete, and Back Buttons -->
    <div class="shadow-lg p-3 m-2 rounded">
        <a href="/tracks/${track.id}/edit" class="btn btn-warning mt-4 m-1">Edit</a>
        <c:if test="${track.user.id == userId}">
            <form action="/tracks/delete/${track.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this track?');">
                <input type="hidden" name="_method" value="DELETE">
                <input type="submit" value="Delete" class="btn btn-danger m-1"/>
            </form>
        </c:if>
        <a href="/welcome" class="btn btn-success mt-4 m-1">Back to Dashboard</a>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        function updateProgressBar(progressBar, percent) {
            progressBar.style.width = percent + '%';
            progressBar.textContent = percent + '%';
        }

        function startConvert(event) {
            event.preventDefault();
            const form = document.getElementById('convertForm');
            const progressBar = document.getElementById('convertProgressBar');
            uploadWithProgress(form, progressBar);
        }

        function startSpleeter(event) {
            event.preventDefault();
            const form = document.getElementById('spleeterForm');
            const progressBar = document.getElementById('spleeterProgressBar');
            uploadWithProgress(form, progressBar);
        }

        function startLibrosa(event) {
            event.preventDefault();
            const form = document.getElementById('librosaForm');
            const progressBar = document.getElementById('librosaProgressBar');
            uploadWithProgress(form, progressBar);
        }

        function uploadWithProgress(form, progressBar) {
            const xhr = new XMLHttpRequest();
            xhr.open(form.method, form.action, true);
            xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");

            xhr.upload.onprogress = function(event) {
                if (event.lengthComputable) {
                    const percentComplete = Math.round((event.loaded / event.total) * 100);
                    updateProgressBar(progressBar, percentComplete);
                }
            };

            xhr.onload = function() {
                if (xhr.status === 200) {
                    updateProgressBar(progressBar, 100);
                    progressBar.classList.replace("bg-info", "bg-success");
                    progressBar.textContent = 'Complete';
                    setTimeout(() => {
                        window.location.href = window.location.href;
                    }, 1000); // Refresh after 1 second
                } else {
                    progressBar.classList.replace("bg-info", "bg-danger");
                    progressBar.textContent = 'Failed';
                }
            };

            xhr.onerror = function() {
                progressBar.classList.replace("bg-info", "bg-danger");
                progressBar.textContent = 'Error';
            };

            const formData = new FormData(form);
            xhr.send(new URLSearchParams(formData).toString());
        }
    </script>

</body>
</html>
