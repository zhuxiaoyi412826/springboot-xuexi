package org.example.redis.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Knife4j / OpenAPI3 文档信息。访问 http://localhost:8085/doc.html */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI musicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("在线听歌网站 API")
                        .version("v1.0")
                        .description("MyBatis-Plus + Sa-Token + Redis 多级缓存实战；"
                                + "登录接口 /api/auth/login 返回 satoken，页面把 token 放入请求头 satoken 即可。")
                        .license(new License().name("Learning Project")));
    }
}
