package com.re.security.jwt;

import com.re.repository.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class JWTAuthFilter extends OncePerRequestFilter {
    private final JWTProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        // Kiểm tra danh sách đen logout
        if (tokenBlacklistRepository.existsByTokenString(jwt)) {
            log.warn("Token này đã bị hủy kích hoạt do người dùng đăng xuất!");

            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\": \"UNAUTHORIZED\", \"message\": \"Phiên làm việc đã kết thúc. Vui lòng đăng nhập lại.\"}");
            return;
        }

        try {
            username = jwtProvider.extractUsername(jwt);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 🛠️ SỬA TẠI ĐÂY: Lấy UserDetails đầy đủ quyền từ CustomUserDetailService bạn đã viết
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Kiểm tra tính hợp lệ của Token dựa trên UserDetails chuẩn
                if (jwtProvider.isTokenValid(jwt, userDetails.getUsername())) {

                    // 🛠️ SỬA TẠI ĐÂY: Nạp userDetails và danh sách quyền THẬT (userDetails.getAuthorities())
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities() // <--- Truyền quyền chuẩn vào đây, XÓA rỗng đi
                    );

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
        } catch (Exception e) {
            log.error("Invalid JWT token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private String getTokenFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if(authorization != null && authorization.startsWith("Bearer ")){
            return authorization.substring(7);
        }
        return null;
    }
}