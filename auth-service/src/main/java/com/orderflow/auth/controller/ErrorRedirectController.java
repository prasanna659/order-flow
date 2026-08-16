package com.orderflow.auth.controller;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Replaces Spring Boot's Whitelabel Error Page.
 *
 * When auth-service throws any unhandled exception — including OAuth2 errors
 * that escape the failure handler — Spring routes the request to /error.
 * Without this controller that results in the "Whitelabel Error Page" HTML.
 *
 * For OAuth2-initiated requests (browser is on auth-service during the redirect
 * dance), we redirect back to the frontend with an error message.
 * For API requests (Accept: application/json), we let the GlobalExceptionHandler
 * handle it — but as a safety net we return a JSON body here too.
 */
@Controller
public class ErrorRedirectController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(ErrorRedirectController.class);

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @RequestMapping("/error")
    public void handleError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object errorMessage = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        String message = buildMessage(statusCode, errorMessage, exception);

        log.error("UNHANDLED_ERROR status={} message={} uri={}",
                statusCode,
                errorMessage,
                request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI),
                (exception instanceof Throwable t) ? t : null);

        String accept = request.getHeader("Accept");

        // API client (Axios, Swagger) — return JSON
        if (accept != null && accept.contains("application/json")) {
            int status = statusCode instanceof Integer i ? i : 500;
            response.setStatus(status);
            response.setContentType("application/json;charset=UTF-8");
            String body = String.format(
                "{\"status\":%d,\"error\":\"Error\",\"message\":\"%s\",\"service\":\"auth-service\"}",
                status, escape(message));
            response.getWriter().write(body);
            return;
        }

        // Browser request (OAuth2 flow, direct navigation) — redirect to frontend
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8);
        response.sendRedirect(frontendUrl + "/auth?error=" + encoded);
    }

    private String buildMessage(Object status, Object msg, Object ex) {
        if (ex instanceof Throwable t && t.getMessage() != null && !t.getMessage().isBlank()) {
            return t.getMessage();
        }
        if (msg instanceof String s && !s.isBlank()) {
            return s;
        }
        int code = status instanceof Integer i ? i : 500;
        return switch (code) {
            case 400 -> "Bad request";
            case 401 -> "Authentication required";
            case 403 -> "Access denied";
            case 404 -> "Resource not found";
            case 500 -> "Internal server error — please try again";
            default  -> "An unexpected error occurred (status " + code + ")";
        };
    }

    private String escape(String s) {
        return s == null ? "" : s.replace("\"", "'").replace("\n", " ");
    }
}
