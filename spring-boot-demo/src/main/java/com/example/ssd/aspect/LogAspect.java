package com.example.ssd.aspect;

import com.example.ssd.annotation.Log;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class LogAspect {


    @Before("@annotation(log)")
    public void logBefore(JoinPoint joinPoint, Log log) {
        System.out.println("【前置通知】方法执行前: " + joinPoint.getSignature().getName() + "，描述：" + log.value());
    }

    @AfterReturning(value = "@annotation(log)", returning = "result")
    public void logAfterReturning(JoinPoint joinPoint, Log log, Object result) {
        System.out.println("【返回通知】方法执行后：" + joinPoint.getSignature().getName() + "，返回值：" + result);
    }

    @Around("@annotation(log)")
    public Object logAround(ProceedingJoinPoint pjp, Log log) throws Throwable {
        System.out.println("【环绕通知 - 前】：" + log.value());
        Object result = pjp.proceed();
        System.out.println("【环绕通知 - 后】：" + log.value());
        return result + "zms";
    }
}
