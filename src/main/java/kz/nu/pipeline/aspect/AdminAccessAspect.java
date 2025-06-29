package kz.nu.pipeline.aspect;

import kz.nu.pipeline.annotation.AdminAccess;
import kz.nu.pipeline.model.User;
import kz.nu.pipeline.service.UserService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminAccessAspect {

    private final UserService userService;

    /**
     * Intercepts method calls annotated with RoleAccess and checks if the current user has the required role.
     *
     * @param joinPoint the join point
     * @return the result of the method call if access is allowed
     * @throws Throwable if an error occurs during the method call
     */
    @Around("@annotation(kz.nu.pipeline.annotation.AdminAccess) || @within(kz.nu.pipeline.annotation.AdminAccess)")
    public Object checkRoleAccess(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        AdminAccess roleAccess = method.getAnnotation(AdminAccess.class);
        if (roleAccess == null) {
            roleAccess = method.getDeclaringClass().getAnnotation(AdminAccess.class);
        }

        if (roleAccess == null) {
            return joinPoint.proceed();
        }

        User currentUser = userService.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
        if (!currentUser.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return joinPoint.proceed();
    }
}
