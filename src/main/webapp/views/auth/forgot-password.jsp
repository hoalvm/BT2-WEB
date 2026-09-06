<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Forgot password</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body class="auth-shell">
<main class="auth-card card">
    <c:url var="loginUrl" value="/login" />
    <a class="back-link" href="${loginUrl}">← Sign in</a>
    <header class="auth-header">
        <p class="eyebrow">Account recovery</p>
        <h1>Reset password</h1>
        <p class="muted">We will email you a verification code.</p>
    </header>

    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>
    <c:if test="${not empty infoMessage}">
        <div class="alert info" role="status"><c:out value="${infoMessage}" /></div>
    </c:if>

    <c:url var="forgotUrl" value="/forgot-password" />
    <form method="post" action="${forgotUrl}">
        <div class="form-group">
            <label for="email">Email</label>
            <input id="email" name="email" type="email" maxlength="254"
                   value="${fn:escapeXml(email)}" autocomplete="email" required autofocus>
        </div>
        <button class="button primary full" type="submit">Send code</button>
    </form>
</main>
</body>
</html>
