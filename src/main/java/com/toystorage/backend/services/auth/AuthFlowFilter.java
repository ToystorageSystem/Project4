package com.toystorage.backend.services.auth;

import com.toystorage.backend.repository.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
public class AuthFlowFilter extends OncePerRequestFilter {
    private final UserRepository users;
    private final SecondFactorService secondFactor;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String path = request.getRequestURI();
        if (!path.startsWith("/api/auth/") && auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {
            var user = users.findByEmail(auth.getName());
            if (user.isPresent() && (Boolean.TRUE.equals(user.get().getMustChangePassword())
                    || !secondFactor.enabled(user.get().getId()))) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Complete account security setup");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
