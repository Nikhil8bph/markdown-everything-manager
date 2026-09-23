package io.github.nikhil8bph.markcraft.security;

import io.github.nikhil8bph.markcraft.dtos.response.ErrorInfo;
import io.github.nikhil8bph.markcraft.dtos.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

@Component
public class LoopbackHostFilter extends OncePerRequestFilter {

    private static final Pattern LOOPBACK_HOST = Pattern.compile("^(127\\.0\\.0\\.1|localhost|\\[::1\\])(?::([0-9]{1,5}))?$");

    private final ObjectMapper mapper;

    public LoopbackHostFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isLoopbackHost(request.getHeader("Host"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                    new ErrorInfo("INVALID_REQUEST", "Request host is not allowed", List.of())));
            return;
        }
        if (isMutation(request.getMethod()) && !isAllowedOrigin(request.getHeader("Origin"), request.getHeader("Host"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                    new ErrorInfo("INVALID_REQUEST", "Request origin is not allowed", List.of())));
            return;
        }
        chain.doFilter(request, response);
    }

    private static boolean isMutation(String method) {
        return !method.equals("GET") && !method.equals("HEAD") && !method.equals("OPTIONS");
    }

    static boolean isAllowedOrigin(String origin, String host) {
        if (origin == null) return true;
        try {
            URI uri = new URI(origin);
            URI destination = new URI("http://" + host);
            return uri.getScheme().equals("http")
                    && uri.getRawUserInfo() == null && uri.getRawPath().isEmpty()
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && uri.getHost() != null && uri.getHost().equalsIgnoreCase(destination.getHost())
                    && uri.getPort() == destination.getPort();
        } catch (URISyntaxException | NullPointerException exception) {
            return false;
        }
    }

    static boolean isLoopbackHost(String host) {
        if (host == null) return false;
        Matcher match = LOOPBACK_HOST.matcher(host.toLowerCase(Locale.ROOT));
        if (!match.matches()) return false;
        if (match.group(2) == null) return true;
        int port = Integer.parseInt(match.group(2));
        return port > 0 && port <= 65535;
    }
}
