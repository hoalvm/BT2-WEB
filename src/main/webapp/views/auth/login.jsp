<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Sign in</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body class="auth-shell">
<main class="auth-card card">
    <c:url var="homeUrl" value="/home" />
    <a class="brand" href="${homeUrl}">JPA Web</a>
    <header class="auth-header">
        <p class="eyebrow">Welcome back</p>
        <h1>Sign in</h1>
        <p class="muted">Use your username or email.</p>
    </header>

    <c:if test="${not empty successMessage}">
        <div class="alert success" role="status"><c:out value="${successMessage}" /></div>
    </c:if>
    <c:if test="${not empty infoMessage}">
        <div class="alert info" role="status"><c:out value="${infoMessage}" /></div>
    </c:if>
    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>

    <c:url var="loginUrl" value="/login" />
    <form method="post" action="${loginUrl}">
        <div class="form-group">
            <label for="identifier">Username or email</label>
            <input id="identifier" name="identifier" type="text" maxlength="254"
                   value="${fn:escapeXml(identifier)}" autocomplete="username" required autofocus>
        </div>
        <div class="form-group">
            <label for="password">Password</label>
            <input id="password" name="password" type="password" maxlength="72"
                   autocomplete="current-password" required>
        </div>
        <button class="button primary full" type="submit">Sign in</button>
    </form>

    <div class="auth-links">
        <c:url var="forgotUrl" value="/forgot-password" />
        <a href="${forgotUrl}">Forgot password?</a>
        <c:url var="registerUrl" value="/register" />
        <span>New here? <a href="${registerUrl}">Create account</a></span>
    </div>
</main>
</body>
</html>
