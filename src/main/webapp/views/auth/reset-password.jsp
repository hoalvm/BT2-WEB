<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>New password</title>
    <c:url var="styleUrl" value="/assets/css/app.css" />
    <link rel="stylesheet" href="${styleUrl}">
</head>
<body class="auth-shell">
<main class="auth-card card">
    <header class="auth-header">
        <p class="eyebrow">Final step</p>
        <h1>New password</h1>
        <p class="muted">Use at least 8 characters.</p>
    </header>

    <c:if test="${not empty errorMessage}">
        <div class="alert error" role="alert"><c:out value="${errorMessage}" /></div>
    </c:if>

    <c:url var="resetUrl" value="/reset-password" />
    <form method="post" action="${resetUrl}">
        <div class="form-group">
            <label for="password">New password</label>
            <input id="password" name="password" type="password" minlength="8" maxlength="72"
                   autocomplete="new-password" required autofocus>
        </div>
        <div class="form-group">
            <label for="confirmPassword">Confirm password</label>
            <input id="confirmPassword" name="confirmPassword" type="password" minlength="8" maxlength="72"
                   autocomplete="new-password" required>
        </div>
        <button class="button primary full" type="submit">Update password</button>
    </form>
</main>
</body>
</html>
