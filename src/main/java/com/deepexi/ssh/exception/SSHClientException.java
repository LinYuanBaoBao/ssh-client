package com.deepexi.ssh.exception;

/**
 * ssh 客户端异常
 *
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
public class SSHClientException extends RuntimeException {
    public SSHClientException(String message) {
        super(message);
    }

    public SSHClientException(Throwable cause) {
        super(cause);
    }

    public SSHClientException(String message, Throwable e) {
        super(message, e);
    }
}
