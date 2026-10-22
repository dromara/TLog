package com.yomahub.tlog.exception;

/**
 * 自定义标签表达式错误
 *
 * @author Bryan.Zhang
 * @since 1.3.4
 */
public class TLogCustomLabelExpressionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private String message;

    public TLogCustomLabelExpressionException(String message) {
        super(message);
        this.message = message;
    }

    public TLogCustomLabelExpressionException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /** 保留 1.x 的可修改异常信息 API。 */
    public void setMessage(String message) {
        this.message = message;
    }
}
