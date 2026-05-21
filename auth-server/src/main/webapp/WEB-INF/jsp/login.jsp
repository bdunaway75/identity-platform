<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
    <meta name="theme-color" content="#050809" />
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
    <title>Sign in</title>
    <link rel="stylesheet" href="/auth-views.css" />
</head>
<body class="auth-page">
<c:set var="isPlatformFlow" value="${not empty platformRegisterDto}" />
<c:set var="modelName" value="${isPlatformFlow ? 'platformRegisterDto' : 'registerDto'}" />
<c:set var="loginAction" value="${isPlatformFlow ? '/platform/login' : '/login'}" />
<c:set var="signUpHref" value="${isPlatformFlow ? '/platform/signUp' : '/signUp'}" />
<c:set var="resolvedClientId" value="${not empty registerDto.clientId ? registerDto.clientId : param.client_id}" />
<c:if test="${not isPlatformFlow and not empty resolvedClientId}">
    <c:url var="signUpHref" value="/signUp">
        <c:param name="client_id" value="${resolvedClientId}" />
    </c:url>
</c:if>
<c:set var="hasAuthMessage" value="${not empty param.message}" />
<div class="auth-shell">
    <form:form cssClass="auth-card" method="post" action="${loginAction}" modelAttribute="${modelName}" autocomplete="on">
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

            <form:errors path="*" element="div" cssClass="auth-error-list" />

            <c:if test="${not isPlatformFlow}">
            <form:hidden path="clientId" value="${resolvedClientId}" />
            </c:if>

            <div class="auth-field">
                <label class="auth-label" for="email">Email</label>
                <form:input
                        path="email"
                        id="email"
                        cssClass="auth-input"
                        type="email"
                        autocomplete="username"
                        placeholder="you@example.com"
                        required="true"
                        autofocus="true"
                />
            </div>

            <div class="auth-field">
                <label class="auth-label" for="password">Password</label>
                <form:password
                        path="password"
                        id="password"
                        cssClass="auth-input"
                        autocomplete="current-password"
                        placeholder="At least 8 characters"
                        minlength="8"
                        maxlength="72"
                        required="true"
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
    </form:form>
</div>
</body>
</html>
