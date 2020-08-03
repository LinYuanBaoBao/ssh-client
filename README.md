# SSH-Client

通过 SSH-Client 可以直连/跳机方式连接到远程主机，执行命令、传输文件。

项目地址：https://github.com/LinYuanBaoBao/ssh-client.git

## 快速上手

引入依赖：
```java
<dependency>
    <groupId>com.github.LinYuanBaoBao</groupId>
    <artifactId>ssh-client</artifactId>
    <version>1.0.0-RELEASE</version>
</dependency>
```   

### 建立/断开 ssh 连接
```java
// 配置跳机
SSHClient.JumpHost jumpHost = new SSHClient.JumpHost();
jumpHost.setHost("127.122.188.117")
        .setPassword("abcd1234")
        .setPort(22)
        .setUsername("root");

SSHClient ssh = SSHClient.builder()
        .setHost("10.50.3.3")
        .setPassword("abcd1234")
        .setJumpHost(jumpHost)
        .setPort(22)
        .setUsername("root")
        .build();
ssh.setExecTimeout(3000);        // 设置执行命令超时时间
ssh.connect(10 * 1000);          // 连接远程主机，并设置连接超时时间
ssh.openSftpChannel(10 * 1000);  // 开启 sftp，并设置传输文件超时时间
```

### 执行命令

#### 基本使用
```java
List<String> output = ssh.exec("echo hello");
System.out.print(output);
// or
ssh.exec("echo hello", System.out::println);
```

#### 执行超时
```java
try {
    ssh.exec("while true;do ls;sleep 1s; done;", System.out::println);
} catch (SSHClientException e) {
    e.printStackTrace();
}
```

#### 错误退出码
```java
try {
    ssh.exec("ls aaaaaaaaaaaaaaaa");
} catch (UnexpectedExitStatusException e) {
    System.out.println("exitStatus："+e.getExitStatus());
    System.out.println("errMsg："+e.getOut());
}
```

### 创建文件夹

```java
ssh.mkdirs("/tmp/scp/sub1/sub2");
```

### 传输文件
```java
File sourceDir = new File(getClass().getResource("/assert/scp/file").getFile());
ssh.scp(sourceDir, "/tmp/scp/sub1/sub2");
```

