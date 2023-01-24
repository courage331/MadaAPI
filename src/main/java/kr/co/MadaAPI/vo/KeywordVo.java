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
    int monthlyPcQcCnt;
    int monthlyMobileQcCnt;
    double monthlyAvePcClkCnt;
    double monthlyAveMobileClkCnt;
    double monthlyAvePcCtr;
    double monthlyAveMobileCtr;
    int plAvgDepth;
    double compIdx;
    long total;
    long clkCntSum;

}
