<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
    <title>Create account</title>
    <link rel="stylesheet" href="/auth-views.css" />
</head>
<body class="auth-page">
<c:set var="isPlatformFlow" value="${not empty platformRegisterDto}" />
<c:set var="modelName" value="${isPlatformFlow ? 'platformRegisterDto' : 'registerDto'}" />
<c:set var="signUpAction" value="${isPlatformFlow ? '/platform/signUp' : '/signUp'}" />
<c:set var="loginHref" value="${isPlatformFlow ? '/platform/login' : '/login'}" />
<c:set var="resolvedClientId" value="${not empty registerDto.clientId ? registerDto.clientId : param.client_id}" />
<div class="auth-shell">
    <form:form cssClass="auth-card" method="post" action="${signUpAction}" modelAttribute="${modelName}" autocomplete="on">
        <div class="auth-content">
            <div class="auth-header">
                <div class="auth-badge">Create Account</div>
                <h1 class="auth-title">Set up your sign-in</h1>
                <div class="auth-subtitle">
                    Create your account so you can continue into the app after authorization.
                </div>
                <c:if test="${not isPlatformFlow and not empty resolvedClientId}">
                    <div class="auth-meta">
                        Client:
                        ${resolvedClientId}
                    </div>
                </c:if>
            </div>

            <c:if test="${not isPlatformFlow}">
                <form:hidden path="clientId" value="${resolvedClientId}" />
            </c:if>

            <form:errors path="*" element="div" cssClass="auth-error-list" />

            <div class="auth-field">
                <label class="auth-label" for="email">Email</label>
                <form:input
                        path="email"
                        id="email"
                        cssClass="auth-input"
                        type="email"
                        placeholder="you@example.com"
                        autocomplete="username"
                        required="true"
                />
            </div>

            <div class="auth-field">
                <label class="auth-label" for="password">Password</label>
                <form:password
                        path="password"
                        id="password"
                        cssClass="auth-input"
                        placeholder="At least 8 characters"
                        autocomplete="new-password"
                        minlength="8"
                        maxlength="72"
                        required="true"
                />
            </div>

            <div class="auth-actions">
                <button class="auth-button" type="submit">Create account</button>
            </div>

            <div class="auth-divider"></div>

            <div class="auth-footer">
                Already have an account?<br/>
                <a href="${loginHref}">Back to sign in</a>
            </div>
        </div>
    </form:form>
</div>
</body>
</html>
