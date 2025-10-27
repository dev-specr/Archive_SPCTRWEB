package com.spectre.common;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String requestId = UUID.randomUUID().toString();
        MDC.put("requestId", requestId);
        try {
            if (response instanceof HttpServletResponse resp) {
                resp.setHeader("X-Request-Id", requestId);
            }
            chain.doFilter(request, response);
        } finally {
            MDC.remove("requestId");
        }
    }
}