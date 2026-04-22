<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
    <meta name="theme-color" content="#050809" />
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
    <title>Update password</title>
    <link rel="stylesheet" href="/auth-views.css"/>
</head>
<body class="auth-page">
<c:set var="formAction" value="${isPlatformFlow ? '/platform/credentials-expired' : '/credentials-expired'}"/>
<c:set var="loginHref" value="${isPlatformFlow ? '/platform/login' : '/login'}"/>
<main class="auth-shell">
    <form class="auth-card" method="post" action="${formAction}" autocomplete="on">
        <div class="auth-content">
            <div class="auth-header">
                <div class="auth-badge">Password Update</div>
                <h1 class="auth-title">Your password has expired</h1>
                <div class="auth-subtitle">
                    Update your password to continue signing in. After saving your new password, you will be sent back to the login page.
                </div>
            </div>

            <c:if test="${not empty errorMessage}">
                <div class="auth-error" aria-live="polite">
                        ${errorMessage}
                </div>
            </c:if>

            <input type="hidden" name="clientId" value="${passwordChangeRequest.clientId}"/>

            <div class="auth-field">
                <label class="auth-label" for="email">Email</label>
                <input
                        id="email"
                        name="email"
                        class="auth-input"
                        type="email"
                        inputmode="email"
                        autocomplete="username"
                        value="${passwordChangeRequest.email}"
                        readonly
                        required
                />
            </div>

            <div class="auth-field">
                <label class="auth-label" for="currentPassword">Current password</label>
                <input
                        id="currentPassword"
                        name="currentPassword"
                        class="auth-input"
                        type="password"
                        autocomplete="current-password"
                        placeholder="Enter your current password"
                        minlength="8"
                        maxlength="72"
                        required
                        autofocus
                />
            </div>

            <div class="auth-field">
                <label class="auth-label" for="newPassword">New password</label>
                <input
                        id="newPassword"
                        name="newPassword"
                        class="auth-input"
                        type="password"
                        autocomplete="new-password"
                        placeholder="Choose a new password"
                        minlength="8"
                        maxlength="72"
                        required
                />
            </div>

            <div class="auth-field">
                <label class="auth-label" for="confirmPassword">Confirm new password</label>
                <input
                        id="confirmPassword"
                        name="confirmPassword"
                        class="auth-input"
                        type="password"
                        autocomplete="new-password"
                        placeholder="Re-enter your new password"
                        minlength="8"
                        maxlength="72"
                        required
                />
            </div>

            <div class="auth-actions">
                <button class="auth-button" type="submit">Update password</button>
            </div>

            <div class="auth-divider"></div>

            <div class="auth-footer">
                Want to go back?<br/>
                <a href="${loginHref}">Return to login</a>
            </div>
        </div>
    </form>
</main>
</body>
</html>
