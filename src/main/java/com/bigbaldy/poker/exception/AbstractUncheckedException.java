package com.bigbaldy.poker.exception;

import com.bigbaldy.poker.resource.ResponseResource;
import org.springframework.http.HttpStatus;

public abstract class AbstractUncheckedException extends RuntimeException {
    private static final long serialVersionUID = 9185363771965400078L;

    private IErrorInfo errorInfo;

    protected abstract int getModuleCode();

    public abstract HttpStatus getHttpStatus();

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
     * @return eg: (module_code=101,error_code=2) => 101002
     */
    public final int getCode() {
        return this.getModuleCode() * 1000 + this.errorInfo.getCode();
    }

    @Override
    public final String getMessage() {
        return errorInfo.getMessage();
    }

    public ResponseResource toResult() {
        return ResponseResource.builder()
                .code(this.getCode())
                .message(this.getMessage())
                .build();
    }

    public ResponseResource<?> toResponseResource(){
        return ResponseResource.builder()
                .code(this.getCode())
                .message(this.getMessage())
                .build();
    }
}
