package io.github.nikhil8bph.markcraft.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoopbackHostFilterTests {

    @Test
    void acceptsOnlyLiteralLoopbackHostValues() {
        assertThat(LoopbackHostFilter.isLoopbackHost("127.0.0.1")).isTrue();
        assertThat(LoopbackHostFilter.isLoopbackHost("127.0.0.1:8080")).isTrue();
        assertThat(LoopbackHostFilter.isLoopbackHost("localhost:4200")).isTrue();
        assertThat(LoopbackHostFilter.isLoopbackHost("LOCALHOST:4200")).isTrue();
        assertThat(LoopbackHostFilter.isLoopbackHost("[::1]:8080")).isTrue();
        for (String host : new String[] {"evil.example", "127.0.0.1.evil.example", "0.0.0.0",
                "192.168.1.5", "localhost:0", "localhost:65536", "localhost:8080@evil.example", ""}) {
            assertThat(LoopbackHostFilter.isLoopbackHost(host)).as(host).isFalse();
        }
    }

    @Test
    void mutationOriginMustMatchTheRequestOriginWhenPresent() {
        assertThat(LoopbackHostFilter.isAllowedOrigin(null, "127.0.0.1:8080")).isTrue();
        assertThat(LoopbackHostFilter.isAllowedOrigin("http://127.0.0.1:8080", "127.0.0.1:8080")).isTrue();
        assertThat(LoopbackHostFilter.isAllowedOrigin("http://localhost:4200", "localhost:4200")).isTrue();
        assertThat(LoopbackHostFilter.isAllowedOrigin("http://evil.example", "127.0.0.1:8080")).isFalse();
        assertThat(LoopbackHostFilter.isAllowedOrigin("http://127.0.0.1:4200", "127.0.0.1:8080")).isFalse();
        assertThat(LoopbackHostFilter.isAllowedOrigin("null", "127.0.0.1:8080")).isFalse();
    }
}
