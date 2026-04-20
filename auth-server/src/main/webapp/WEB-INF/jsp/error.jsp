<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
    <title>Something went wrong</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/auth-views.css"/>
</head>
<body class="auth-page">
<main class="auth-shell">
    <section class="auth-card">
        <div class="auth-content auth-error-state">
            <div class="auth-badge">System Error</div>

            <header class="auth-header">
                <h1 class="auth-title">${errorTitle}</h1>
                <p class="auth-subtitle">${errorMessage}</p>
                <div class="auth-meta">Status code: <strong>${statusCode}</strong></div>
            </header>

            <div class="auth-actions auth-actions-inline">
                <a class="auth-button" href="/login">Return to login</a>
                <a class="auth-button auth-button-secondary" href="/">Back to home</a>
            </div>
        </div>
    </section>
</main>
</body>
</html>
