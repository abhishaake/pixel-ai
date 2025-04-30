package com.av.pixel.auth;

import com.av.pixel.dto.UserDTO;
import com.av.pixel.enums.PermissionEnum;
import com.av.pixel.exception.AuthenticationException;
import com.av.pixel.helper.TransformUtil;
import com.av.pixel.service.UserTokenService;
import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Objects;

@Component
@Aspect
@AllArgsConstructor
public class Interceptor {

    UserTokenService userTokenService;

    private static final String USER = "user";

    @Pointcut("@annotation(com.av.pixel.auth.Authenticated)")
    public void authentication() {
    }

    @Around("authentication()")
    public Object process(ProceedingJoinPoint jointPoint) throws Throwable {
        HttpServletRequest httpRequest = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        MethodSignature methodSignature = (MethodSignature) jointPoint.getSignature();
        Method method = methodSignature.getMethod();
        Authenticated authAnnotation = jointPoint.getTarget().getClass().getMethod(method.getName(), method.getParameterTypes()).getAnnotation(Authenticated.class);

        boolean allowedAny = allowedAny(authAnnotation.permissions());

        final String user = httpRequest.getHeader(USER);
        UserDTO userDTO = TransformUtil.fromJson(user, UserDTO.class);
        String token = fetchAuthToken(httpRequest);
        if (Objects.nonNull(userDTO)) {
            setAuthUserDTOParameterInMethodSignatureIfPresent(userDTO, jointPoint, token);
            return jointPoint.proceed();
        }

        if (StringUtils.isNotEmpty(token)) {
            userDTO = userTokenService.getUserFromToken(token);
            if (Objects.isNull(userDTO)) {
                if (allowedAny) {
                    return jointPoint.proceed();
                }
                throw new AuthenticationException();
            } else {
                setAuthUserDTOParameterInMethodSignatureIfPresent(userDTO, jointPoint, token);
            }
            return jointPoint.proceed();
        }
        if (allowedAny) {
            return jointPoint.proceed();
        }
        throw new AuthenticationException();
    }

    private boolean allowedAny (PermissionEnum[] permissionEnums) {
        if (permissionEnums == null) {
            return false;
        }
        for (PermissionEnum permissionEnum : permissionEnums) {
            if (PermissionEnum.ANY.equals(permissionEnum)) {
                return true;
            }
        }
        return false;
    }

    private void setAuthUserDTOParameterInMethodSignatureIfPresent(UserDTO userDTO, ProceedingJoinPoint jointPoint, String token) {
        for (Object parameter : jointPoint.getArgs()) {
            if (parameter instanceof UserDTO authUserDTO) {
                authUserDTO.setCode(userDTO.getCode());
                authUserDTO.setFirstName(userDTO.getFirstName());
                authUserDTO.setEmail(userDTO.getEmail());
                authUserDTO.setImageUrl(userDTO.getImageUrl());
                authUserDTO.setOnboardingDate(userDTO.getOnboardingDate());
                authUserDTO.setAccessToken(token);
                break;
            }
        }
    }

    private String fetchAuthToken(HttpServletRequest httpRequest) {
        String token = null;
        Cookie tokenCookie = getCookie(httpRequest,"token");
        if(tokenCookie!=null){
            token = tokenCookie.getValue();
        }
        if(StringUtils.isBlank(token)){
            token = httpRequest.getHeader("token");
        }
        if (StringUtils.isBlank(token)) {
            token = httpRequest.getParameter("token");
        }
        return token;
    }

    public static Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }
}
