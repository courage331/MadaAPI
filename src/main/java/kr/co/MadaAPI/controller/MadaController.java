package kr.co.MadaAPI.controller;

import kr.co.MadaAPI.service.KeywordService;
import kr.co.MadaAPI.service.ProductDateService;
import kr.co.MadaAPI.service.ReviewService;
import kr.co.MadaAPI.vo.KeywordVo;
import kr.co.MadaAPI.vo.ResponseInfo;
import kr.co.MadaAPI.vo.ReviewVo;
import kr.co.MadaAPI.vo.UrlVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
@CrossOrigin(origins ="*")
@RequestMapping("/mada")
public class MadaController {

    @Autowired
    ReviewService reviewService;

    @Autowired
    KeywordService keywordService;

    @Autowired
    ProductDateService productDateService;


    @RequestMapping(value = "/getReview", method= RequestMethod.POST) //post방식
    public ResponseInfo reviewController(@RequestBody UrlVo urlVo){

        ResponseInfo responseInfo = new ResponseInfo();

        long start = System.currentTimeMillis();
        log.info("/getReview 요청 url : " +urlVo.getUrl());

        responseInfo = reviewService.reviewSelenium(urlVo.getUrl());

        long end = System.currentTimeMillis();
        log.info((end-start)/1000+"초");

        return responseInfo;
    }

    @RequestMapping(value = "/getProductDate", method= RequestMethod.POST) //post방식
    public ResponseInfo getProductDateController(@RequestBody UrlVo urlVo){

        ResponseInfo responseInfo = new ResponseInfo();

        long start = System.currentTimeMillis();
        log.info("/getProductDate 요청 url : " +urlVo.getUrl());

        responseInfo = productDateService.getProductDate(urlVo.getUrl());

        long end = System.currentTimeMillis();
        log.info((end-start)/1000+"초");

        return responseInfo;
    }

    @RequestMapping(value = "/getKeyword", method= RequestMethod.POST) //post방식
    public ResponseInfo keywordController(@RequestBody UrlVo urlVo) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {

        ResponseInfo responseInfo = new ResponseInfo();

        long start = System.currentTimeMillis();
        log.info("/getKeyword 요청 url : " +urlVo.getUrl());

        responseInfo = keywordService.searchKeywordSelenium(urlVo.getUrl());

        long end = System.currentTimeMillis();
        log.info((end-start)/1000+"초");

        return responseInfo;
    }

}
