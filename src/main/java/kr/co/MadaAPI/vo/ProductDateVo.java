package kr.co.MadaAPI.vo;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProductDateVo {

        String productId;
        String reviewAmount;
        String saleAmount;
        String thumbnail;
        String regDate;
        Object delivery;
        Object images;
}
