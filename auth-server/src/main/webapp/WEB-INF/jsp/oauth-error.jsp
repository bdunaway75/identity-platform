<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html>
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
    <title>Authorization Error</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/auth-views.css"/>
</head>
<body class="auth-page">
<main class="auth-shell">
    <section class="auth-card">
        <div class="auth-content auth-error-state">
            <div class="auth-badge">Authorization Error</div>

            <header class="auth-header">
                <h1 class="auth-title">Authorization failed</h1>
                <p class="auth-subtitle">Your OAuth request was rejected before login could start.</p>
                <div class="auth-meta">Error code: <strong>${error}</strong></div>
            </header>

            <c:if test="${not empty description}">
                <div class="auth-error">
                    ${description}
                </div>
            </c:if>

            <div class="auth-actions auth-actions-inline">
                <a class="auth-button" href="/login">Return to login</a>
                <a class="auth-button auth-button-secondary" href="/">Back to home</a>
            </div>
        </div>
    </section>
</main>
</body>
</html>
