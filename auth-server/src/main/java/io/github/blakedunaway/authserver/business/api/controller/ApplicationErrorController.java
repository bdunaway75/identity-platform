package io.github.blakedunaway.authserver.business.api.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
public class ApplicationErrorController implements ErrorController {

    @RequestMapping("/error")
    public String error(final HttpServletRequest request,
                        @RequestParam(required = false) final String error,
                        @RequestParam(required = false) final String message,
                        final Model model) {

        //nasty, but so we know exact causes
        final Object statusCodeAttribute = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        final Object requestUriAttribute = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        final Object servletErrorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        final Object servletException = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        final int statusCode = statusCodeAttribute instanceof Integer code ? code : 500;
        final HttpStatus status = HttpStatus.resolve(statusCode);
        final String resolvedMessage = StringUtils.isNotBlank(message)
                ? message
                : StringUtils.isNotBlank(error)
                ? error
                : resolveMessage(statusCode);

        model.addAttribute("statusCode", statusCode);
        model.addAttribute("errorTitle", status != null ? status.getReasonPhrase() : "Unexpected Error");
        model.addAttribute("errorMessage", resolvedMessage);

        final String requestUri = requestUriAttribute == null
                ? request.getRequestURI()
                : String.valueOf(requestUriAttribute);
        final String servletMessage = servletErrorMessage == null ? null : String.valueOf(servletErrorMessage);

        log.error(
                "Application error rendered for uri {} with status {}, resolved message {}, servlet message {}, query" +
                        " error {}, query message {}.",
                requestUri,
                statusCode,
                resolvedMessage,
                servletMessage,
                error,
                message,
                servletException instanceof Throwable throwable ? throwable : null);

        return "error";
    }

    private String resolveMessage(final int statusCode) {
        return switch (statusCode) {
            case 400 -> "The request could not be completed. Please go back and try again.";
            case 401 -> "You need to sign in again before continuing.";
            case 403 -> "You do not have permission to view this page.";
            case 404 -> "We could not find the page you were looking for.";
            default -> "Something went wrong on our side. Please try again in a moment.";
        };
    }
}
