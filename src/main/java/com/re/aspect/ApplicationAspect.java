package com.re.aspect;

import com.re.model.entity.Application;
import org.aspectj.lang.annotation.AfterReturning;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class ApplicationAspect {


    @AfterReturning(
            pointcut =
                    "execution(* com.re.service.impl.ApplicationServiceImpl.submitApplicationWithFile(..))"
    )
    public void logApplication() {

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
