package com.example.springsecurity.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.WebUtils;

public final class CookieUtil {

    private CookieUtil() {}

    public static final String ACCESS_TOKEN  = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";

    public static void addCookie(HttpServletResponse response,
                                 String name, String value, long maxAgeMs) {
        response.addHeader("Set-Cookie",
                name + "=" + value
                        + "; HttpOnly"
                        // + "; Secure"      ← REMOVE in dev (http://localhost)
                        + "; Path=/"
                        + "; SameSite=None"
                        + "; Max-Age=" + (maxAgeMs / 1000));
    }

    public static void clearCookie(HttpServletResponse response, String name) {
        response.addHeader("Set-Cookie",
                name + "=; HttpOnly; Secure; Path=/; SameSite=None; Max-Age=0");
    }

    public static String getCookieValue(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        return (cookie != null) ? cookie.getValue() : null;
    }
}
