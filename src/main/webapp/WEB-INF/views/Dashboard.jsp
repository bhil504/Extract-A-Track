
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="ISO-8859-1">
    <title>Dashboard</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light p-5">

    <div class="container">
        <h1>Welcome to Extract-A-Track</h1>
        <h6 class="fst-italic">Logged in User: <small><c:out value="${user.username}"/></small></h6>
        <p class="lead">This is your dashboard where all of your uploads will be visible and accessible. You can manage and download your tracks from here.</p>

        <!-- Action Buttons -->
        <div class="d-flex justify-content-between mt-4">
            <a href="/tracks/new" class="btn btn-success m-3">Upload New Track</a>
            <a href="/logout" class="btn btn-danger m-3">Logout</a>
        </div>

<!-- Display User's Uploaded Tracks -->
        <div class="mt-5">
            <h3>Your Uploaded Tracks</h3>
            <c:if test="${empty userTracks}">
                <p class="text-muted">You have not uploaded any tracks yet.</p>
            </c:if>
            <c:if test="${not empty userTracks}">
                <table class="table table-striped table-hover table-dark mt-4">
            <thead> 
                <tr>
                    <th>Track Name</th>
                    <th>Status</th>
                    <th>Genre</th>
                </tr>
            </thead>          
            <tbody>
                <c:forEach var="track" items="${userTracks}">
                    <tr>
                        <td><a href="/tracks/${track.id}" class="text-light"><c:out value="${track.title}"/></a></td>
                        <td><c:out value="${track.status}"/></td>
                        <td><c:out value="${track.genre}"/></td>
                    </tr>
                </c:forEach>               
            </tbody>
        </table>
            </c:if>
        </div>
    </div>
    
<!-- Spleeter Section -->
    <div>
        <h6 class="fst-italic"><small>Press the Spleeter Track Separation button for Spleeter Separator</small></h6>
        <a class="btn btn-primary m-3" href="/spleeter/form">Spleeter Track Separator</a>
        <h2>What is Spleeter Track Separation?</h2>
    </div>

    <div>
       	<p class="shadow-lg p-3 m-2  rounded">What is Spleeter Track Separation?<small>
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
        
    <div>
        <p class="shadow-lg p-3 rounded">Click this button for our Track Separator <small>
	    <a class="btn btn-primary m-3" href="/spleeter/form">Spleeter Track Separator</a>
     </div>
    <a href="/logout" class="btn btn-danger">Log Out</a>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
