package kr.co.MadaAPI.vo;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class ResponseInfo {
    private int returnCode;
    private String returnMsg;
    private Object data = null;

    public ResponseInfo(int returnCode,String returnMsg) {
        this.returnCode = returnCode;
        this.returnMsg = returnMsg;
    }

    public ResponseInfo(int returnCode, String returnMsg, Object data) {
        this.returnCode = returnCode;
        this.returnMsg = returnMsg;
        this.data = data;
    }
}

