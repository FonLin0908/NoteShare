package com.example.demo.interceptor;

import com.example.demo.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();

        // Session 可能包含舊版或無效資料，因此先保留原始型別。
        Object loginUserObj = session.getAttribute("loginUser");

        // 清除無效的登入資料並要求使用者重新登入。
        if (loginUserObj == null || !(loginUserObj instanceof User)) {
            // 安全清空 Session，避免髒資料殘留
            session.removeAttribute("loginUser");
            String currentUri = request.getRequestURI();
            String queryString = request.getQueryString();
            if (queryString != null) {
                currentUri += "?" + queryString;
            }
            request.setAttribute("msg", "請先登入系統。");
            response.sendRedirect("/login?target=" + currentUri);
            return false;
        }

        User user = (User) loginUserObj;

        // 管理員頁面需要額外的角色檢查。
        String uri = request.getRequestURI();
        if (uri.startsWith("/admin") && !"ADMIN".equals(user.getRole())) {
            request.getSession().setAttribute("alertMsg", "您的權限不足，無法訪問管理員後台！");
            response.sendRedirect("/member");
            return false;
        }

        return true;
    }
}
