package com.deepexi.ssh.exception;

/**
 * 构建 ssh 客户端错误异常
 *
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
public class BuildSSHClientException extends RuntimeException {
    public BuildSSHClientException(Throwable cause) {
        super(cause);
    }
}
