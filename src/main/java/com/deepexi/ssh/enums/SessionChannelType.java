package com.deepexi.ssh.enums;

/**
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
public enum SessionChannelType {

    SHELL("shell"), EXEC("exec"), SFTP("sftp");


    private String type;

    SessionChannelType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
