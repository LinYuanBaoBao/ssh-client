package com.deepexi.ssh.exception;

import java.util.List;

/**
 * 错误退出码异常
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
public class UnexpectedExitStatusException extends SSHClientException {
    private Integer exitStatus;
    private List<String> out;

    public UnexpectedExitStatusException(int exitStatus, List<String> out) {
        super("Unexpected exit status: " + exitStatus);
        this.exitStatus = exitStatus;
        this.out = out;
    }

    public Integer getExitStatus() {
        return exitStatus;
    }

    public List<String> getOut() {
        return out;
    }
}
