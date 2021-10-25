
package com.yomahub.tlog.exception;

/**
 * 自定义标签表达式错误
 * @author Bryan.Zhang
 * @since 1.3.4
 */
public class TLogCustomLabelExpressionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** 异常信息 */
	private String message;

	public TLogCustomLabelExpressionException(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
}
