
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix = "c" uri = "http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>

<!DOCTYPE html>
<html>
<head>
<meta charset="ISO-8859-1">
<title>Login and Registration</title>
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
</head>
<body class="bg-dark text-light p-5">

<div class="container">
    <h1>Welcome to Extract-A-Track</h1>
    <p><small class="text-muted">by Bhillion Dollar Productions</small></p>

    <p class="fst-italic">Extract-A-Track provides storage and song separation services. Register or login to start managing your tracks.</p>

<!-- Registration Form -->
    <div class="row">
        <div class="col-md-6">
            <h2 class="fst-italic">Register</h2>
            <form:form action="/register" method="post" modelAttribute="newUser" class="m-4">
                <div class="form-group mb-3">
                    <label for="username">Username</label>
                    <form:input path="username" class="form-control" required="required"/>
                    <form:errors path="username" class="text-danger"/>
                </div>

                <div class="form-group mb-3">
                    <label for="email">Email</label>
                    <form:input path="email" class="form-control" type="email" required="required"/>
                    <form:errors path="email" class="text-danger"/>
                </div>   

                <div class="form-group mb-3">
                    <label for="password">Password</label>
                    <form:password path="password" class="form-control" required="required"/>
                    <form:errors path="password" class="text-danger"/>
                </div>

                <div class="form-group mb-3">
                    <label for="confirm">Confirm Password</label>
                    <form:password path="confirm" class="form-control" required="required"/>
                    <form:errors path="confirm" class="text-danger"/>
                </div>

                <button type="submit" class="btn btn-primary mt-3">Register</button>
            </form:form>
        </div>

<!-- Login Form -->
        <div class="col-md-6">
            <h2 class="fst-italic">Login</h2>
            <form:form action="/login" method="post" modelAttribute="newLogin" class="m-4">
                <div class="form-group mb-3">
                    <label for="email">Email</label>
                    <form:input path="email" class="form-control" type="email" required="required"/>
                    <form:errors path="email" class="text-danger"/>
                </div>

                <div class="form-group mb-3">
                    <label for="password">Password</label>
                    <form:password path="password" class="form-control" required="required"/>
                    <form:errors path="password" class="text-danger"/>
                </div>

                <!-- Error Message -->
                <c:if test="${not empty errorMessage}">
                    <div class="alert alert-danger mt-3">${errorMessage}</div>
                </c:if>

                <button type="submit" class="btn btn-primary mt-3">Login</button>
            </form:form>
        </div>
    </div>
</div>

<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
</body>
</html>
