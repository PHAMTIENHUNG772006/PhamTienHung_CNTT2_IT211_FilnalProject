package com.re.aspect;

import com.re.model.entity.Application;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {


    @Pointcut("execution(* com.re.controller..*.*(..)) || execution(* com.re.service.impl..*.*(..))")
    public void logAll() {

    }

    @Around(("logAll"))
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {

        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        long startTime = System.currentTimeMillis();

        log.info(">>>> Bắt đầu thực hiện: {}.{}()", className, methodName);

        Object result;
        try {

            result = joinPoint.proceed();
        } catch (Throwable throwable) {
            long timeTaken = System.currentTimeMillis() - startTime;
            log.error("Đã sảy ra lỗi tại: {}.{}() sau {} ms. Chi tiết lỗi: {}",
                    className, methodName, timeTaken, throwable.getMessage());
            throw throwable;
        }


        long timeTaken = System.currentTimeMillis() - startTime;

        log.info(" Hoàn thành: {}.{}() - Thời gian thực hiện: {} ms", className, methodName, timeTaken);

        return result;
    }

    @AfterReturning(
            pointcut =
                    "execution(* com.re.service.impl.ApplicationServiceImpl.submitApplicationWithFile(..))",
            returning = "application"
    )
    public void logApplySuccess(Application application) {

        log.info(
                "[APPLY JOB] Candidate {} đã ứng tuyển Job {}",
                application.getCandidate().getId(),
                application.getJob().getId()
        );
    }

}
