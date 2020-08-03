package com.deepexi.ssh.exception;

/**
 * 认证（用户名/密码错误）失败异常
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
public class AuthFailException extends SSHClientException {
    public AuthFailException(String message, Throwable e) {
        super(message, e);
    }
}
