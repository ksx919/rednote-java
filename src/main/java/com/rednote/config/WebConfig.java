package com.rednote.config;

import com.rednote.interceptor.LoginInterceptor;
import com.rednote.interceptor.LoggingInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoggingInterceptor loggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 日志拦截器 - 记录所有请求
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .order(1);

        // 登录拦截器 - 验证Token
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/**") // 拦截所有接口
                .excludePathPatterns(
                        "/users/login", // 放行登录
                        "/users/register" // 放行注册
                        )
                .order(2);
    }
}