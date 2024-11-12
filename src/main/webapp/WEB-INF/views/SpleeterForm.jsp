<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>  
<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Spleeter Form</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light p-5">
	<h1 class="m-4">Spleeter Track Separation</h1>
	<c:if test="${not empty errorMessage}">
    	<div class="alert alert-danger">
        	<c:out value="${errorMessage}" />
        </div>
    </c:if>
    <div>
    	<h2>What is Track Separation:</h2>
        <h6 class="fst-italic"><small>Track Separation Form located below this description</small></h6>
    </div>
	<div>
        <p class="shadow-lg p-3 mb-5  rounded"><small>What is Track Separation?
Our track separation feature is powered by Spleeter, a state-of-the-art audio separation tool developed by Deezer. Spleeter allows you to break down your audio tracks into individual components or "stems." This is incredibly useful for musicians, producers, and DJs who want more control over the various elements of a track, such as vocals, drums, bass, and other instruments.

How It Works:
Separate Audio into Stems: With Spleeter, you can split a track into two or five different stems.
2-Stem Mode: Separates the track into vocals and accompaniment (instruments).
5-Stem Mode: Breaks down the track into vocals, drums, bass, piano, and other instruments.
Why Use Track Separation?
Remix or Mashup: Easily isolate vocals or instruments to create remixes or mashups.
Practice with Instrumentals: Use the accompaniment (minus vocals) to practice singing or playing along.
Creative Sound Design: Manipulate individual stems for advanced sound design or production.
Simply upload your track, select how many stems you want, and let Spleeter do the rest. Once processed, you'll be able to download the isolated components and use them in your projects!</small></p>
     </div>
        
    <div class="bg-dark text-light p-1 m-2">
    	<h2>Spleeter Track Separation Form</h2>
    	<form id="spleeterForm" method="POST" action="/spleeter/upload" enctype="multipart/form-data">
		<!-- Upload the WAV file directly for Spleeter processing -->
		    <div class="mb-3">
		        <label for="trackFile" class="form-label">Upload Your Track Here For Separation. (WAV only)</label>
		        <input class="form-control" type="file" id="trackFile" name="trackFile" accept=".wav" required>
		    </div>
		
		<!-- Stem Selection Dropdown -->
		    <label for="stems">Choose Separation Type:</label>
				<select id="stems" name="stems" class="form-control" required>
					<option value="2">Vocals/Instrumental (2 stems)</option>
				    <option value="5">Vocals/Drums/Bass/Piano/Other (5 stems)</option>
				</select>
		    <button type="submit" class="btn btn-primary m-3">Upload and Process with Spleeter</button>
		    
		<p>Your file will be saved in your app's root directory in src folder, in the main folder, in the resources folder, within the static folder, within the stems folder. This was unintentional and a change will be made ASAP to a more suitable download area!</p>
		</form>
    </div>
<!-- Back to Dashboard button -->
     <a href="/welcome" class="btn btn-success mt-4 m-1">Back to Dashboard</a>
     <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>