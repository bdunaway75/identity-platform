<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover"/>
    <meta name="theme-color" content="#050809" />
    <meta name="apple-mobile-web-app-status-bar-style" content="black-translucent" />
    <title>Check your email</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/auth-views.css"/>
</head>
<body class="auth-page">
<main class="auth-shell">
    <section class="auth-card auth-card-wide">
        <div class="auth-content">
            <header class="auth-header">
                <div class="auth-badge">Email Verification</div>
                <div class="auth-mail-hero" aria-hidden="true">
                    <span class="auth-mail-icon"></span>
                </div>
                <h1 class="auth-title" id="verification-title">Check your inbox</h1>
                <p class="auth-subtitle" id="verification-subtitle">
                    We sent a verification link to <strong><c:out value="${emailAddress}" /></strong>.
                    This window will continue automatically once your email is verified.
                </p>
            </header>

            <div class="auth-status-panel" aria-live="polite">
                <div class="auth-status-row auth-status-row-complete">
                    <span class="auth-status-dot"></span>
                    <span>Email sent</span>
                </div>
                <div class="auth-status-row" id="verification-status">
                    <span class="auth-status-dot auth-status-dot-live"></span>
                    <span>Waiting for verification</span>
                </div>
                <div class="auth-status-row" id="redirect-status">
                    <span class="auth-status-dot"></span>
                    <span>Redirecting after confirmation</span>
                </div>
            </div>

            <div class="auth-redirect-countdown" id="verification-countdown" hidden>
                <div class="auth-redirect-copy">Redirecting shortly</div>
                <div class="auth-countdown" aria-live="polite" aria-label="5 seconds remaining">
                    <div class="auth-countdown-window">
                        <div class="auth-countdown-strip" id="countdown-strip">
                            <div class="auth-countdown-digit">5</div>
                            <div class="auth-countdown-digit">4</div>
                            <div class="auth-countdown-digit">3</div>
                            <div class="auth-countdown-digit">2</div>
                            <div class="auth-countdown-digit">1</div>
                            <div class="auth-countdown-digit">0</div>
                        </div>
                    </div>
                    <div class="auth-countdown-meta">
                        <span class="auth-countdown-label">seconds</span>
                        <div class="auth-countdown-progress-track" aria-hidden="true">
                            <div class="auth-countdown-progress-bar" id="countdown-progress"></div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="auth-footer">
                You can safely leave this tab open while you verify your email.
            </div>
        </div>
    </section>
</main>

<script>
    (() => {
        const REDIRECT_SECONDS = 5;
        const sessionId = "<c:out value='${sessionId}' />";
        const fallbackHref = "${fallbackHref}";
        const title = document.getElementById("verification-title");
        const subtitle = document.getElementById("verification-subtitle");
        const status = document.getElementById("verification-status");
        const redirectStatus = document.getElementById("redirect-status");
        const countdown = document.getElementById("verification-countdown");
        const countdownMeter = countdown.querySelector(".auth-countdown");
        const countdownStrip = document.getElementById("countdown-strip");
        const countdownProgress = document.getElementById("countdown-progress");

        const events = new EventSource("/two-factor/events?session=" + encodeURIComponent(sessionId));

        events.addEventListener("verified", (event) => {
            events.close();
            const redirectUrl = event.data || fallbackHref;
            let secondsRemaining = REDIRECT_SECONDS;

            title.textContent = "Email verified";
            subtitle.textContent = "Your account is ready. We will send you back to sign in shortly.";
            status.classList.add("auth-status-row-complete");
            status.querySelector(".auth-status-dot").classList.remove("auth-status-dot-live");
            status.querySelector("span:last-child").textContent = "Email verified";
            redirectStatus.classList.add("auth-status-row-complete");
            countdown.hidden = false;

            const updateCountdown = () => {
                countdownMeter.setAttribute("aria-label", secondsRemaining + " seconds remaining");
                countdownStrip.style.transform = "translateY(-" + ((REDIRECT_SECONDS - secondsRemaining) * 3.2) + "rem)";
                countdownProgress.style.width = ((secondsRemaining / REDIRECT_SECONDS) * 100) + "%";
            };

            updateCountdown();
            const intervalId = window.setInterval(() => {
                secondsRemaining = Math.max(0, secondsRemaining - 1);
                updateCountdown();

                if (secondsRemaining <= 0) {
                    window.clearInterval(intervalId);
                    window.location.assign(redirectUrl);
                }
            }, 1000);
        });

        events.onerror = () => {
            status.querySelector("span:last-child").textContent = "Still waiting for verification";
        };
    })();
</script>
</body>
</html>
