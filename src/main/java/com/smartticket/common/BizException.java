package com.smartticket.common;

/**
 * 业务异常。
 * 业务上出问题时（比如"工单不存在"），直接 throw new BizException("工单不存在");
 * 会被 GlobalExceptionHandler 捕获，统一转成 Result 返回给前端。
 */
public class BizException extends RuntimeException {

    public BizException(String message) {
        super(message);
    }
}
