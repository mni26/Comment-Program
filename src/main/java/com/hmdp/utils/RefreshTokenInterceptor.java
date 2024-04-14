package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;


// TODO 对所有路径进行拦截，判断当前redis中是否有用户信息，若有则进行30min刷新
public class RefreshTokenInterceptor implements HandlerInterceptor {
    // TODO 因为当前类并没有存放在spring的ioc容器中，故无法通过依赖注入得到对象，只能通过构造方法，使得调用他的方法自己来传入StringRedisTemplate对象
    private StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public RefreshTokenInterceptor() {
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1.获取请求头中的token
        String token = request.getHeader("authorization");

        // 2.基于token获取redis中的user对象
        // 判断token是否为空
        if (StrUtil.isBlank(token)){
            // token为空，即redis中无用户数据，无法进行刷新操作
            return true;
        }

        // 拼接得到tokenKey
        String key = RedisConstants.LOGIN_USER_KEY + token;

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(key);

        // 判断userMap是否为空map集合
        if (userMap.isEmpty()){
            // userMap为空，即redis中无用户数据，无法进行刷新操作
            return true;
        }

        // 4.若不为空则将hashMap的user数据再转成userdto
        UserDTO userDTO = BeanUtil.fillBeanWithMap(userMap, new UserDTO(), false);

        // 5.将用户信息userdto存储于 ThreadLocal中
        UserHolder.saveUser(userDTO);

        // 6.刷新tokenKey的有效期
        stringRedisTemplate.expire(key, RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        // 7.放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
