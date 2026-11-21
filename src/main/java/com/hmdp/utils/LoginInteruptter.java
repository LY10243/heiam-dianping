package com.hmdp.utils;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import org.springframework.web.servlet.HandlerInterceptor;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
public class LoginInteruptter  implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //判断是否拦截
        UserDTO userDTO = UserHolder.getUser();
        if(userDTO==null)
        {
            //拦截
            response.setStatus(401);
            Result.fail("用户不存在");
            return true;
        }
        //由用户放行
        return true;
    }
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
