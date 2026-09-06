<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Verify code</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body class="auth-shell">
<main class="auth-card card">
    <c:url var="homeUrl" value="/home" />
    <a class="brand" href="${homeUrl}">JPA Web</a>
    <header class="auth-header">
        <p class="eyebrow">Email verification</p>
        <h1>Enter code</h1>
        <p class="muted">Sent to <strong><c:out value="${email}" /></strong>. Valid for 5 minutes.</p>
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

    <c:url var="verifyUrl" value="/verify-otp" />
    <form method="post" action="${verifyUrl}">
        <div class="form-group">
            <label class="sr-only" for="otp">6-digit code</label>
            <input class="otp-input" id="otp" name="otp" type="text" inputmode="numeric"
                   autocomplete="one-time-code" pattern="[0-9]{6}" minlength="6" maxlength="6"
                   placeholder="000000" required autofocus>
        </div>
        <button class="button primary full" type="submit">Verify</button>
    </form>

    <c:url var="resendUrl" value="/verify-otp/resend" />
    <form class="auth-links" method="post" action="${resendUrl}">
        <span>Did not receive it?</span>
        <button class="link-button" type="submit">Resend code</button>
    </form>
</main>
</body>
</html>
