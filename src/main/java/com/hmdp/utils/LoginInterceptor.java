package com.hmdp.utils;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import org.springframework.beans.BeanUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class LoginInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取session
        HttpSession session = request.getSession();

        // 2.判断session中的用户是否存在于数据库
        Object user = session.getAttribute("user");
        if (user == null){
            // 3.若不存在则拦截，即返回false，并且设置响应状态码为401
            response.setStatus(401);
            return false;
        }

        // 4.若存在则将用户信息存储于 ThreadLocal中
        UserHolder.saveUser((UserDTO) user);

        // 5.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
