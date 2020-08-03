package com.deepexi.ssh;

import com.deepexi.ssh.exception.SSHClientException;
import com.deepexi.ssh.exception.UnexpectedExitStatusException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

@Ignore
public class SSHClientTest {
    private SSHClient ssh;

    @Before
    public void setUp(){
        // 配置跳机
        SSHClient.JumpHost jumpHost = new SSHClient.JumpHost();
        jumpHost.setHost("127.122.188.117")
                .setPassword("abcd1234")
                .setPort(22)
                .setUsername("root");

        ssh = SSHClient.builder()
                .setHost("10.50.3.3")
                .setPassword("abcd1234")
                .setJumpHost(jumpHost)
                .setPort(22)
                .setUsername("root")
                .build();
        ssh.setExecTimeout(3000);                // 设置执行命令超时时间
        ssh.connect(10 * 1000);          // 连接远程主机，并设置连接超时时间
        ssh.openSftpChannel(10 * 1000);  // 开启 sftp，并设置传输文件超时时间
    }

    @Test
    public void exec() throws Exception {
        List<String> output = ssh.exec("echo hello");
        System.out.print(output);
        // or
        ssh.exec("echo hello", System.out::println);
    }

    @Test(expected = SSHClientException.class)
    public void execTimeout() throws Exception {
        ssh.exec("while true;do ls;sleep 1s; done;", System.out::println);
    }

    @Test(expected = UnexpectedExitStatusException.class)
    public void execErr() throws Exception {
        try {
            ssh.exec("ls aaaaaaaaaaaaaaaa");
        } catch (UnexpectedExitStatusException e) {
            System.out.println("exitStatus：" + e.getExitStatus());
            System.out.println("errMsg：" + e.getOut());
            throw e;
        }
    }

    @Test
    public void mkdirs() throws Exception {
        ssh.mkdirs("/tmp/scp/sub1/sub2");
    }

    @Test
    public void dispose() {
        ssh.dispose();
    }

    @Test
    public void scp() {
        ssh.scp(reource("/assert/scp/file"), "/tmp/scp/sub1/sub2");
        ssh.scp(reource("/assert/scp/zip"), "/tmp/scpZip");
    }

    private File reource(String path) {
        return new File(getClass().getResource(path).getFile());
    }

}