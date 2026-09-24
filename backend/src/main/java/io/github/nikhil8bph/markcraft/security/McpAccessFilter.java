package io.github.nikhil8bph.markcraft.security;

import io.github.nikhil8bph.markcraft.config.McpProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class McpAccessFilter extends OncePerRequestFilter {

    private final McpProperties properties;

    public McpAccessFilter(McpProperties properties) {
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!path.startsWith("/mcp")) {
            chain.doFilter(request, response);
            return;
        }
        if (!properties.enabled() || !path.equals("/mcp")) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        List<String> authorization = Collections.list(request.getHeaders("Authorization"));
        byte[] expected = ("Bearer " + properties.token()).getBytes(StandardCharsets.UTF_8);
        if (authorization.size() != 1 || !MessageDigest.isEqual(
                authorization.getFirst().getBytes(StandardCharsets.UTF_8), expected)) {
            response.setHeader("WWW-Authenticate", "Bearer realm=\"MarkCraft MCP\"");
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        chain.doFilter(request, response);
    }
}
