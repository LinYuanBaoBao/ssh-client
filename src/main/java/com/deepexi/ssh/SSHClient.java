package com.deepexi.ssh;

import com.deepexi.ssh.exception.*;
import com.deepexi.ssh.utils.IOUtils;
import com.jcraft.jsch.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * ssh 客户端
 *
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
@Slf4j
public class SSHClient {
    private JSch jSch = new JSch();
    private Session session;
    private Session jumpSession;
    private ChannelSftp sftpChannel;

    @Getter
    @Setter
    private Integer execTimeout = 10000;    // 执行指令超时时间

    private SSHClient() {
    }

    public void connect(int timeout) {
        try {
            log.debug("Open session.");
            this.session.connect(timeout);
        } catch (JSchException e) {
            if (StringUtils.equals("Auth fail", e.getMessage())) {
                throw new AuthFailException(e.getMessage(), e);
            }
            throw new SSHClientException("Open session fail: " + e.getMessage(), e);
        }
    }

    /**
     * 执行命令，返回标准输出行的信息
     */
    public List<String> exec(String cmd) throws UnexpectedExitStatusException {
        return exec(cmd, 0);
    }

    /**
     * 执行命令，返回标准输出行的信息
     *
     * @param expectExitStatus 期望的响应状态值
     */
    public List<String> exec(String cmd, int expectExitStatus) throws UnexpectedExitStatusException {
        List<String> lines = new ArrayList<>();
        int exitStatus = exec(cmd, lines::add);
        if (exitStatus != expectExitStatus) {
            throw new UnexpectedExitStatusException(exitStatus, lines);
        }
        return lines;
    }

    /**
     * 执行命令
     *
     * @param cmd      要执行的命令
     * @param consumer 处理命令输出
     * @return 命令执行响应状态值
     */
    public int exec(String cmd, LineConsumer consumer) {
        log.debug("Ready to exec command: {}.", cmd);
        ChannelExec exec = null;
        try {
            log.debug("Open exec channel.");
            exec = (ChannelExec) session.openChannel("exec");
            exec.setPty(true);   // 以pty模式执行可以在disconnect后kill掉正在执行的指令，防止某些死循环或高耗时指令无法被中断，浪费主机资源
            // TODO:: 在pty模式下没有err stream，考虑如何处理
//            StringBuilder err = new StringBuilder();
//            exec.setErrStream(new OutputStream() {
//                @Override
//                public void write(int b) {
//                    err.append((char) b);
//                }
//            });
            exec.setCommand(cmd);
            log.debug("Start to execute command.");
            exec.connect(10000);

            InputStream inputStream = exec.getInputStream();
            // 处理正常输出流
            this.parseInputStream(cmd, consumer, exec, inputStream);

            return exec.getExitStatus();
        } catch (JSchException | IOException e) {
            throw new SSHClientException(e);
        } finally {
            if (exec != null && !exec.isClosed()) {
                log.debug("Close exec channel.");
                exec.disconnect();
            }
        }
    }


    /**
     * scp 文件夹或文件
     *
     * @param path 目标路径
     * @param file 待移动文件/文件夹
     */
    public void scp(File file, String path) {
        openSftpChannel(10 * 1000);

        try {
            mkdirs(path);
            doScp(path, file);
        } catch (Exception e) {
            throw new SftpChannelException(String.format("SCP failed: %s", e.getMessage()));
        }
    }

    public ChannelSftp openSftpChannel(int timeout) {
        checkSessionOpen();

        if (!isSftpOpen()) {
            try {
                this.sftpChannel = (ChannelSftp) this.session.openChannel("sftp");
                this.sftpChannel.connect(timeout);
            } catch (Exception e) {
                throw new SSHClientException(String.format("Sftp open failed: %s", e.getMessage()));
            }
        }

        return sftpChannel;
    }

    public void closeSftpChannel() {
        if (isSftpOpen()) {
            this.sftpChannel.disconnect();
        }
    }

    /**
     * 创建文件夹
     *
     * @param path 文件夹路径
     */
    void mkdirs(String path) throws SftpException {
        checkSftpOpen();

        if (dirExist(path)) {
            return;
        }

        String[] paths = path.split("/");
        StringBuffer pathBuf = new StringBuffer("/");
        String currentPath;

        for (String folder : paths) {
            if (StringUtils.isNotBlank(folder)) {
                pathBuf.append(folder).append("/");
                currentPath = pathBuf.toString();
                log.debug("SSHClient mkdirs: {}", currentPath);
                if (!dirExist(currentPath)) {
                    this.sftpChannel.mkdir(currentPath);
                }
            }
        }
    }

    /**
     * 销毁连接
     */
    public void dispose() {
        log.debug("Close session.");
        closeSftpChannel();
        this.session.disconnect();
        if (this.jumpSession != null) {
            log.debug("Close jump session.");
            this.jumpSession.disconnect();
        }
    }

    private void parseInputStream(String cmd, LineConsumer consumer, ChannelExec exec, InputStream inputStream) throws IOException {
        long start = System.currentTimeMillis();

        IOUtils.LineHandler lineHandler = (lineNum, line) -> {
            consumer.accept(line);
        };
        IOUtils.TimeoutCheck timeoutCheck = () -> {
            if (System.currentTimeMillis() - start > execTimeout) {
                throw new SSHClientException(String.format("Execute command \"%s\" timeout.", cmd));
            }
        };
        IOUtils.readLineFromRealTimeStream(inputStream, lineHandler, exec::isClosed, false, timeoutCheck);
    }

    private void doScp(String path, File file) throws Exception {
        checkSftpOpen();
        checkPathExist(path);

        if (file.isFile()) {
            try (FileInputStream input = new FileInputStream(file)) {
                this.sftpChannel.put(input, path + "/" + file.getName());
            }

            return;
        }

        for (File f : file.listFiles()) {
            String subPath = path + "/" + f.getName();

            if (f.isDirectory()) {
                this.sftpChannel.mkdir(subPath);
                doScp(subPath, f);
            } else {
                try (InputStream tmp = new FileInputStream(f)) {
                    this.sftpChannel.put(tmp, subPath);
                }
            }
        }
    }

    public void checkSftpOpen() {
        checkSessionOpen();

        if (!isSftpOpen()) {
            throw new SftpChannelException("sftp channel is not open.");
        }
    }

    private void checkSessionOpen() {
        if (!this.session.isConnected()) {
            throw new SSHClientException("ssh channel is not open.");
        }
    }

    private boolean isSftpOpen() {
        return Objects.nonNull(sftpChannel) && !sftpChannel.isClosed();
    }

    private boolean dirExist(String path) {
        checkSftpOpen();
        try {
            this.sftpChannel.stat(path);
            return true;
        } catch (SftpException e) {
            return false;
        }
    }

    private void checkPathExist(String path) {
        if (!dirExist(path)) {
            throw new SftpChannelException("No such path.");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * ssh 客户端 Builder，通过其创建 SSHClient 对象
     */
    @Accessors(chain = true)
    @Data
    public static class Builder {
        private String host = "localhost";
        private Integer port = 22;
        private String username = "root";
        private String password;
        private String privateKey;
        private JumpHost jumpHost;
        private boolean strictHostKeyChecking = false;
        private int execTimeout = 10000;

        public SSHClient build() {
            SSHClient ssh = new SSHClient();
            ssh.jSch = new JSch();
            try {
                Session session;
                if (jumpHost != null) {
                    Session jumpSession = ssh.jSch.getSession(jumpHost.getUsername(), jumpHost.getHost(), jumpHost.getPort());
                    if (jumpHost.getPrivateKey() != null) {
                        ssh.jSch.addIdentity(jumpHost.getPrivateKey());
                    } else {
                        jumpSession.setPassword(jumpHost.getPassword());
                    }
                    jumpSession.setConfig("StrictHostKeyChecking", "no");
                    jumpSession.connect();
                    int assigned = jumpSession.setPortForwardingL(0, host, port);
                    ssh.jumpSession = jumpSession;

                    session = ssh.jSch.getSession(username, "localhost", assigned);
                } else {
                    session = ssh.jSch.getSession(username, host, port);
                }

                if (this.privateKey != null) {
                    // TODO::
                    ssh.jSch.addIdentity(privateKey);
                } else {
                    if (this.password != null) {
                        session.setPassword(password);
                    } else {
                        // TODO::
                        throw new RuntimeException("Should specify password or private key.");
                    }
                }

                if (!strictHostKeyChecking) {
                    session.setConfig("StrictHostKeyChecking", "no");
                }

                ssh.setExecTimeout(this.execTimeout);
                ssh.session = session;
            } catch (JSchException e) {
                throw new BuildSSHClientException(e);
            }
            return ssh;
        }
    }

    @Accessors(chain = true)
    @Data
    public static class JumpHost {
        private String host;
        private Integer port;
        private String username;
        private String password;
        private String privateKey;
    }

    @FunctionalInterface
    public interface LineConsumer extends Consumer<String> {
    }
}
