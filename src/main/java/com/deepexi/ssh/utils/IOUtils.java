package com.deepexi.ssh.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * @author linyuan - linyuan@deepexi.com
 * @since 2020/8/2
 */
@Slf4j
public class IOUtils {
    /**
     * 外部对象：能够实时输出IO流的
     * 按行从实时IO流中读取数据
     *
     * @param inputStream    外部对象输出的IO流
     * @param lineHandler    处理带行号的行数据方法
     * @param endCondition   外部对象执行结束的方法
     * @param isNonEndMethod endCondition 不是外部对象执行结束的方法
     * @param timeoutCheck   timeout判断
     * @throws IOException IO异常
     */
    public static void readLineFromRealTimeStream(InputStream inputStream,
                                                  LineHandler lineHandler,
                                                  Condition endCondition,
                                                  boolean isNonEndMethod,
                                                  TimeoutCheck timeoutCheck) throws IOException {
        boolean end = false;
        int lineNum = 1;
        while (!end) {
            // end后还要再读取一次，否则会遗漏最后一部分内容
            if (endCondition.test() ^ isNonEndMethod) {
                end = true;
            }

            doTimeoutCheck(timeoutCheck);

            read(inputStream, lineHandler, lineNum);
        }

    }

    /**
     *
     * @param inputStream
     * @param lineHandler
     * @param lineNum
     * @throws IOException
     */
    public static void read(InputStream inputStream, LineHandler lineHandler, int lineNum) throws IOException {
        // todo:: list<byte> 浪费空间
        int available = inputStream.available();
        if (available > 0) {
            log.debug("{} bytes available.", available);

            List<Byte> bytes = new ArrayList<>();
            for (int i = 0; i < available; i++) {
                if (bytes == null) {
                    bytes = new ArrayList<>();
                }
                int c = inputStream.read();

                if (c != '\n') {
                    bytes.add((byte) c);
                } else {
                    byte[] bs = getBytes(bytes);
                    doLineHandler(lineHandler, lineNum, bs);
                    bytes = null;
                }

                if (inputStream.available() == 0 && bytes != null) {
                    byte[] bs = getBytes(bytes);
                    doLineHandler(lineHandler, lineNum, bs);
                    bytes = null;
                }
            }
        } else {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     *
     * @param bytes
     * @return
     */
    public static byte[] getBytes(List<Byte> bytes) {
        if (bytes.size() > 1) {
            int last = bytes.size() - 1;
            if (bytes.get(last) == '\r') {
                bytes.remove(last);
            }
        }
        byte[] bs = new byte[bytes.size()];
        for (int i1 = 0; i1 < bytes.size(); i1++) {
            bs[i1] = bytes.get(i1);
        }
        return bs;
    }

    /**
     *
     * @param lineHandler
     * @param lineNum
     * @param bs
     */
    public static void doLineHandler(LineHandler lineHandler, int lineNum, byte[] bs) {
        String line = new String(bs, StandardCharsets.UTF_8);
        while (line.length() > 1000) {
            lineHandler.handler(lineNum, line.substring(0, 1000));
            line = line.substring(1000);
            ++lineNum;
        }
        lineHandler.handler(lineNum, line);
        ++lineNum;
    }

    private static void doTimeoutCheck(TimeoutCheck timeoutCheck) {
        if (timeoutCheck != null) {
            timeoutCheck.check();
        }
    }

    @FunctionalInterface
    public interface Condition {
        boolean test();
    }

    @FunctionalInterface
    public interface LineHandler {
        void handler(int lineNum, String line);
    }

    @FunctionalInterface
    public interface TimeoutCheck {
        void check();
    }

}
