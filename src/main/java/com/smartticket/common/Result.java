package com.smartticket.common;

/**
 * 统一返回结果。
 * 所有接口都返回这个格式，前端处理起来方便：
 * {
 *   "code": 200,
 *   "message": "操作成功",
 *   "data": {...}
 * }
 */
public class Result<T> {

    private Integer code;    // 200 成功，500 失败
    private String message;  // 提示信息
    private T data;          // 真正的数据

    public Result() {
    }

    public Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /** 成功，带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    /** 成功，不带数据 */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    /** 失败 */
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    /** 失败，带自定义状态码 */
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
