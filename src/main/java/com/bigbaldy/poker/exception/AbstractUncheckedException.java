package com.bigbaldy.poker.exception;

import com.bigbaldy.poker.web.response.ResultVO;

public abstract class AbstractUncheckedException extends RuntimeException {
    private static final long serialVersionUID = 9185363771965400078L;

    private IErrorInfo errorInfo;

    protected abstract int getModuleCode();

    public AbstractUncheckedException(IErrorInfo errorInfo) {
        this(errorInfo, (Throwable) null);
    }

    public AbstractUncheckedException(IErrorInfo errorInfo, String message) {
        this(errorInfo, message, null);
    }

    public AbstractUncheckedException(IErrorInfo errorInfo, Throwable cause) {
        super(errorInfo.getMessage(), cause);
        this.errorInfo = errorInfo;
    }

    public AbstractUncheckedException(IErrorInfo errorInfo, String message, Throwable cause) {
        super(message, cause);
        this.errorInfo = errorInfo;
    }

    /**
     * @return eg: (module_code=1,error_code=2) => 101002
     */
    public final int getCode() {
        return (100 + this.getModuleCode()) * 1000 + this.errorInfo.getCode();
    }

    @Override
    public final String getMessage() {
        return errorInfo.getMessage();
    }

    public ResultVO toResult() {
        return ResultVO.builder()
                .code(this.getCode())
                .message(this.getMessage())
                .build();
    }
}
