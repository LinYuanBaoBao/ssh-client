package com.deepexi.ssh.exception;

/**
 * 文件传输异常
 *
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
public class SftpChannelException extends SSHClientException {
    public SftpChannelException(String message) {
        super(message);
    }
}
