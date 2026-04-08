<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Sign in</title>
    <link rel="stylesheet" href="/auth-views.css" />
</head>
<body class="auth-page">
<c:set var="isPlatformFlow" value="${not empty platformRegisterDto}" />
<c:set var="loginAction" value="${isPlatformFlow ? '/platform/login' : '/login'}" />
<c:set var="signUpHref" value="${isPlatformFlow ? '/platform/signUp' : '/signUp'}" />
<c:set var="resolvedClientId" value="${not empty registerDto.clientId ? registerDto.clientId : param.client_id}" />
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

            <% if (request.getParameter("error") != null) { %>
            <div class="auth-error">Invalid email or password. Please try again.</div>
            <% } %>

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
