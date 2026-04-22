<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
    <meta name="theme-color" content="#050809" />
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
    <title>Something went wrong</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/auth-views.css"/>
</head>
<body class="auth-page">
<main class="auth-shell">
    <section class="auth-card">
        <div class="auth-content auth-error-state">
            <header class="auth-header">
                <h1 class="auth-title">${errorTitle}</h1>
                <p class="auth-subtitle">${errorMessage}</p>
            </header>

            <div class="auth-actions">
                <a class="auth-button" href="/app/login">Return to login</a>
            </div>
        </div>
    </section>
</main>
</body>
</html>
