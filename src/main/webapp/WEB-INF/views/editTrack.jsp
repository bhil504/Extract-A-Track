<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html>
<head>
    <meta charset="ISO-8859-1">
    <title>Edit Track</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light p-5">

    <div class="container">
        <h1>Edit Track: "<c:out value="${track.title}"/> "</h1>    

        <!-- Form for updating track -->
        <form action="/tracks/update/${track.id}" method="post" enctype="multipart/form-data" class="mt-4">
            <!-- Hidden input to simulate PUT request method -->
            <input type="hidden" name="_method" value="PUT">
            
            <!-- Track Title -->
            <div class="mb-3">
                <label for="title" class="form-label">Track Title</label>
                <input type="text" class="form-control" id="title" name="title" required value="${track.title}">
            </div>

            <!-- Genre -->
            <div class="mb-3">
                <label for="genre" class="form-label">Genre</label>
                <input type="text" class="form-control" id="genre" name="genre" required value="${track.genre}">
            </div>

            <!-- Lyrics -->
            <div class="mb-3">
                <label for="lyrics" class="form-label">Lyrics</label>
                <textarea id="lyrics" name="lyrics" class="form-control"><c:out value="${track.lyrics}"/></textarea>
            </div>

            <!-- Optional Upload (Replace Existing File) -->
            <div class="mb-3">
                <label for="file" class="form-label">Replace WAV File (Optional):</label>
                <input type="file" id="file" name="file" accept=".wav" class="form-control">
            </div>

            <button type="submit" class="btn btn-primary mt-3">Update Track</button>
        </form>
        
        <!-- Delete Button with Confirmation -->
        <c:if test="${track.user.id == userId}">
            <form action="/tracks/delete/${track.id}" method="post" onsubmit="return confirm('Are you sure you want to delete this track?');">
                <!-- Hidden input to simulate DELETE request method -->
                <input type="hidden" name="_method" value="DELETE">
                <input type="submit" value="Delete" class="btn btn-danger m-1"/>
            </form>
        </c:if>

        <a href="/welcome" class="btn btn-secondary m-1">Cancel</a>
    </div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
