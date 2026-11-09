package com.toystorage.backend.config.security;
import com.toystorage.backend.repository.users.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

/** Rejects stale pre-deployment sessions, disabled users and changed passwords. */
public class MfaSessionGuard extends OncePerRequestFilter {
    private final UserRepository users;
    public MfaSessionGuard(UserRepository users) { this.users = users; }
    @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        if (req.getServletPath().startsWith("/api/auth/")) res.setHeader("Cache-Control", "no-store");
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails p) {
            var user = users.findById(p.getId()).orElse(null);
            if (user == null || user.getStatus() == null || !"ACTIVE".equals(user.getStatus().name())
                    || Boolean.TRUE.equals(user.getMustChangePassword()) || !user.getPassword().equals(p.getPassword())
                    || !user.isTwoFactorEnabled() || user.getTotpSecret() == null) {
                if (req.getSession(false) != null) req.getSession(false).invalidate();
                SecurityContextHolder.clearContext();
                res.setStatus(401); return;
            }
        }
        chain.doFilter(req, res);
    }
}
