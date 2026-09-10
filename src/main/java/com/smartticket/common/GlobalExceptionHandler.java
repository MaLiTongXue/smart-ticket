package com.smartticket.common;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 好处：Controller 里再也不用写一堆 try-catch，代码干净很多。
 * 面试常问："你们项目怎么做统一异常处理的？"——就答这个类。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：我们自己主动抛的，把消息原样返回 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        return Result.error(e.getMessage());
    }

    /** 参数校验失败：@NotBlank、@NotNull 这些注解没通过时触发 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidException(MethodArgumentNotValidException e) {
        // 取出第一个校验失败的提示信息
        String message = e.getBindingResult().getFieldError() != null
                ? e.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        return Result.error(400, message);
    }

    /** 兜底：其它没预料到的异常，打印堆栈方便排查，但不把细节暴露给前端 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.error("服务器开小差了：" + e.getMessage());
    }
}
