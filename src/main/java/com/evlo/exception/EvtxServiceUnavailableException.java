package com.evlo.exception;

/**
 * 외부 EVTX 파서 서비스(evtx-service)에 연결할 수 없을 때 사용.
 * 사용자에게는 "관리자에게 문의해 주세요" 등으로 안내.
 */
public class EvtxServiceUnavailableException extends RuntimeException {

    public EvtxServiceUnavailableException(String message) {
        super(message);
    }

    public EvtxServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
