package kr.co.MadaAPI.vo;

import lombok.*;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewVo implements Comparable<ReviewVo>{

    String productOptionContent;
    int cnt;
    double reviewScore;

    @Override
    public int compareTo(@NotNull ReviewVo o) {
        if(this.reviewScore >o.reviewScore){
            return -1;
        }else if(this.reviewScore <o.reviewScore){
            return 1;
        }else{
            if(this.cnt >=o.cnt){
                return -1;
            }else{
                return 1;
            }
        }
    }
}
