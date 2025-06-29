package kz.nu.pipeline.security;

import jakarta.annotation.Nonnull;
import kz.nu.pipeline.model.User;
import kz.nu.pipeline.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;
import java.util.NoSuchElementException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver implements HandlerMethodArgumentResolver {
    private final UserService userService;

    @Value("${jwt.enabled}")
    private Boolean jwtEnabled;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(User.class);
    }

    @Override
    public Object resolveArgument(
            @Nonnull MethodParameter parameter, ModelAndViewContainer mavContainer,
            @Nonnull NativeWebRequest webRequest, WebDataBinderFactory binderFactory
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Boolean.TRUE.equals(jwtEnabled)) {
            return Optional.ofNullable(authentication)
                    .filter(auth -> auth.isAuthenticated() && auth.getPrincipal() != null)
                    .map(Principal::getName)
                    .flatMap(userService::getUserByUsername)
                    .orElseThrow(() -> new NoSuchElementException("User not found"));
        }
        return userService.getUserByUsername("default")
                .orElseThrow(() -> new NoSuchElementException("User not found"));
    }
}
