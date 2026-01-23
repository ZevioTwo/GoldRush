package net.coding.template.handler;

import lombok.extern.slf4j.Slf4j;
import net.coding.template.entity.response.CommonResponse;
import net.coding.template.exception.BusinessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public CommonResponse<?> handleBusinessException(BusinessException e, HttpServletRequest request) {
        log.warn("业务异常: {} - {}", request.getRequestURI(), e.getMessage());
        return CommonResponse.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResponse<?> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();

        StringBuilder errorMsg = new StringBuilder();
        for (FieldError error : fieldErrors) {
            errorMsg.append(error.getField()).append(": ").append(error.getDefaultMessage()).append("; ");
        }

        log.warn("参数校验失败: {} - {}", request.getRequestURI(), errorMsg.toString());
        return CommonResponse.error(400, errorMsg.toString());
    }

    @ExceptionHandler(Exception.class)
    public CommonResponse<?> handleException(Exception e, HttpServletRequest request) {
        log.error("系统异常: {} - {}", request.getRequestURI(), e.getMessage(), e);
        return CommonResponse.error(500, "系统繁忙，请稍后重试");
    }

    // 增加契约相关异常处理

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResponse<?> handleConstraintViolationException(ConstraintViolationException e, HttpServletRequest request) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));

        log.warn("校验失败: {} - {}", request.getRequestURI(), errorMsg);
        return CommonResponse.error(400, errorMsg);
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public CommonResponse<?> handleDuplicateKeyException(DuplicateKeyException e, HttpServletRequest request) {
        log.warn("数据重复: {} - {}", request.getRequestURI(), e.getMessage());
        return CommonResponse.error(400, "数据已存在，请勿重复提交");
    }
}
