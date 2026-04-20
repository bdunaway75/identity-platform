<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
    <title>Sign in</title>
    <link rel="stylesheet" href="/auth-views.css" />
</head>
<body class="auth-page">
<c:set var="isPlatformFlow" value="${not empty platformRegisterDto}" />
<c:set var="loginAction" value="${isPlatformFlow ? '/platform/login' : '/login'}" />
<c:set var="signUpHref" value="${isPlatformFlow ? '/platform/signUp' : '/signUp'}" />
<c:set var="resolvedClientId" value="${not empty registerDto.clientId ? registerDto.clientId : param.client_id}" />
<c:set var="hasAuthError" value="${not empty param.error}" />
<c:set var="hasAuthMessage" value="${not empty param.message}" />
<div class="auth-shell">
    <form class="auth-card" method="post" action="${loginAction}" autocomplete="on">
        <div class="auth-content">
            <div class="auth-header">
                <div class="auth-badge">Sign In</div>
                <h1 class="auth-title">Welcome back</h1>
                <div class="auth-subtitle">
                    Sign in to continue to your workspace and finish the authorization flow securely.
                </div>
                <c:if test="${not isPlatformFlow and not empty resolvedClientId}">
                <div class="auth-meta">
                    Client:
                    ${resolvedClientId}
                </div>
                </c:if>
            </div>

            <c:if test="${hasAuthMessage}">
            <div class="auth-notice" aria-live="polite">
                ${param.message}
            </div>
            </c:if>

            <c:if test="${hasAuthError}">
            <div class="auth-error" aria-live="polite">
                <c:choose>
                    <c:when test="${param.error eq 'true'}">
                        We could not find a user with those credentials, or the password was incorrect. Please try again.
                    </c:when>
                    <c:when test="${param.error eq 'locked'}">
                        Your account is locked. Contact your support team to regain access.
                    </c:when>
                    <c:when test="${param.error eq 'disabled'}">
                        Your account is disabled. Contact your support team if you believe this is a mistake.
                    </c:when>
                    <c:when test="${param.error eq 'account_expired'}">
                        Your account has expired. Contact your support team to continue.
                    </c:when>
                    <c:otherwise>
                        ${param.error}
                    </c:otherwise>
                </c:choose>
            </div>
            </c:if>

            <c:if test="${not isPlatformFlow}">
            <input type="hidden" name="clientId"
                   value="${resolvedClientId}" />
            </c:if>

            <div class="auth-field">
                <label class="auth-label" for="email">Email</label>
                <input
                        id="email"
                        name="email"
                        class="auth-input"
                        type="email"
                        inputmode="email"
                        autocomplete="username"
                        placeholder="you@example.com"
                        required
                        autofocus
                />
            </div>

            <div class="auth-field">
                <label class="auth-label" for="password">Password</label>
                <input
                        id="password"
                        name="password"
                        class="auth-input"
                        type="password"
                        autocomplete="current-password"
                        placeholder="At least 8 characters"
                        minlength="8"
                        maxlength="72"
                        required
                />
            </div>

            <div class="auth-actions">
                <button class="auth-button" type="submit">Continue</button>
            </div>

            <div class="auth-divider"></div>

            <div class="auth-footer">
                Need an account?<br/>
                <a href="${signUpHref}">Create one here</a>
            </div>
        </div>
    </form>
</div>
</body>
</html>
