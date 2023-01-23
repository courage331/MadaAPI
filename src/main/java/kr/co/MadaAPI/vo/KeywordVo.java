package kr.co.MadaAPI.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class KeywordVo {

    String relKeyword;
    String monthlyPcQcCnt;
    String monthlyMobileQcCnt;
    String monthlyAvePcClkCnt;
    String monthlyAveMobileClkCnt;
    String monthlyAvePcCtr;
    String monthlyAveMobileCtr;
    String plAvgDepth;
    String compIdx;
    String total;
    String clkCntSum;

}
