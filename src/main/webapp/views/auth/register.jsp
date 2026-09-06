<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Create account</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body class="auth-shell">
<main class="auth-card card">
    <c:url var="homeUrl" value="/home" />
    <a class="brand" href="${homeUrl}">JPA Web</a>
    <header class="auth-header">
        <p class="eyebrow">Get started</p>
        <h1>Create account</h1>
        <p class="muted">Email verification is required.</p>
    </header>

    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${not empty infoMessage}">
        <div class="alert info" role="status"><c:out value="${infoMessage}" /></div>
    </c:if>

    <c:url var="registerUrl" value="/register" />
    <form method="post" action="${registerUrl}">
        <div class="form-group">
            <label for="username">Username</label>
            <input id="username" name="username" type="text" minlength="3" maxlength="50"
                   pattern="[A-Za-z0-9._-]+" value="${fn:escapeXml(username)}"
                   autocomplete="username" required autofocus>
            <small>3–50 characters: letters, numbers, dot, dash or underscore.</small>
        </div>
        <div class="form-group">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" maxlength="254"
                   value="${fn:escapeXml(email)}" autocomplete="email" required>
        </div>
        <div class="field-row">
            <div class="form-group">
                <label for="password">Password</label>
                <input id="password" name="password" type="password" minlength="8" maxlength="72"
                       autocomplete="new-password" required>
            </div>
            <div class="form-group">
                <label for="confirmPassword">Confirm</label>
                <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="72"
                       autocomplete="new-password" required>
            </div>
        </div>
        <button class="button primary full" type="submit">Create account</button>
    </form>

    <div class="auth-links">
        <c:url var="loginUrl" value="/login" />
        <span>Already registered? <a href="${loginUrl}">Sign in</a></span>
    </div>
</main>
</body>
</html>
