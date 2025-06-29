package kz.nu.pipeline.config;

import kz.nu.pipeline.security.AuthenticatedUserResolver;
import lombok.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer(AuthenticatedUserResolver authenticatedUserResolver) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                        .allowedHeaders("*");
            }

            @Override
            public void addArgumentResolvers(@NonNull List<HandlerMethodArgumentResolver> resolvers) {
                resolvers.add(authenticatedUserResolver);
            }
        };
    }
}
