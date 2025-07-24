package com.example.demo3.config.redis;

import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

public class AppInitializer extends AbstractHttpSessionApplicationInitializer {
    public AppInitializer() {
        super(RedisSessionConfig.class); // 注册 Session 配置类
    }
}
