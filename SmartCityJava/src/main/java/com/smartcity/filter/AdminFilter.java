package com.smartcity.filter;

import com.smartcity.model.User;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;

/**
 * Admin Filter
 * Protects admin-only routes by checking session user role.
 */
public class AdminFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        HttpSession session = httpRequest.getSession(false);

        if (session == null || session.getAttribute("user") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/login.jsp?error=Please+login+first");
            return;
        }

        User user = (User) session.getAttribute("user");
        if (!"admin".equals(user.getRole())) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/index.jsp?error=Admin+access+required");
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
