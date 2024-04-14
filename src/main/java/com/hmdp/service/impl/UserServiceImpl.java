package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result sendCode(String phone, HttpSession session) {

        // 1.校验手机号是否合法
        if (RegexUtils.isPhoneInvalid(phone)){
            // 2.不符合，直接返回错误信息
            return Result.fail("您的手机号格式错误！");
        }

        // 3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        // 4.a将验证码保存到session中
//        session.setAttribute("code",code);
        // 4.b将验证码保存到resis中
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        // 5.发送验证码（需要使用第三方技术，这里不写了）
        log.debug("验证码已生成{}",code);

        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
       // 1.校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            // 2.不符合，直接返回错误信息
            return Result.fail("您的手机号格式错误！");
        }
       // 2.校验验证码与redis中存储的是否一致
        String code = loginForm.getCode();
        String  cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(code)){
            // 3.不一致 返回错误信息
            return Result.fail("验证码错误");
        }

       // 4.一致，则判断当前手机号对应用户是否存在
        User user = query().eq("phone", phone).one();
        if (user == null){
            // 5.不存在，则创建对应新用户并保存到数据库中
            user = creatWithPhone(phone);
        }

       // 6.将用户转化成UserDTO保存到redis中
        // 6.1.使用uuid随机生成token作为key
        String token = UUID.randomUUID().toString();

        // 6.2.使用hash数据结构存储userDTO对象
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
        // 存储
        stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token,userMap);

        // 6.3.设置tokenKey的有效期，防止长期呆在数据库占用内存
        stringRedisTemplate.expire(LOGIN_USER_KEY + token, CACHE_SHOP_TTL, TimeUnit.MINUTES);

        // 7.将token返回给前端
        return Result.ok(token);
    }

    private User creatWithPhone(String phone) {
        // 1.创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        // 2.保存用户
        save(user);
        return user;
    }
}
