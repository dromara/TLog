package com.yomahub.tlog.web.wrapper;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 包装HttpServletRequest，目的是让其输入流可重复读
 * <p>
 * 2.0 起按原始字节缓存 body（1.x 按行读取会丢失换行，且使用平台默认编码），并按请求声明的编码解码
 *
 * @author jing.li
 * @since 1.2.5
 */
public class RequestWrapper extends HttpServletRequestWrapper {

    /**
     * 存储body数据的容器
     */
    private final byte[] body;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request The request to wrap
     * @throws IllegalArgumentException if the request is null
     */
    public RequestWrapper(HttpServletRequest request) {
        super(request);
        try {
            body = readAll(request.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static byte[] readAll(InputStream inputStream) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while ((n = inputStream.read(buffer)) != -1) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    /**
     * 获取请求Body
     *
     * @return String
     */
    public String getBodyString() {
        return new String(body, charset());
    }

    private Charset charset() {
        String encoding = getCharacterEncoding();
        try {
            return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream(), charset()));
    }

    @Override
    public ServletInputStream getInputStream() {
        final ByteArrayInputStream inputStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {
            @Override
            public int read() {
                return inputStream.read();
            }

            @Override
            public int read(byte[] b, int off, int len) {
                return inputStream.read(b, off, len);
            }

            @Override
            public boolean isFinished() {
                return inputStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("RequestWrapper 不支持异步读取");
            }
        };
    }
}
