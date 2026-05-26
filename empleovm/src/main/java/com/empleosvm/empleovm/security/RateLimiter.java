package com.empleosvm.empleovm.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private final Map<String, int[]> intentos = new ConcurrentHashMap<>();
    private static final int MAX_INTENTOS = 5;
    private static final long VENTANA_MS = 15 * 60 * 1000;

    public boolean permite(String clave) {
        int[] datos = intentos.computeIfAbsent(clave, k -> new int[]{0, (int)(System.currentTimeMillis())});
        long ahora = System.currentTimeMillis();
        if (ahora - datos[1] > VENTANA_MS) {
            datos[0] = 0;
            datos[1] = (int) ahora;
        }
        return ++datos[0] <= MAX_INTENTOS;
    }

    public void reset(String clave) {
        intentos.remove(clave);
    }

    public String obtenerIpCliente() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";
        HttpServletRequest request = attrs.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
