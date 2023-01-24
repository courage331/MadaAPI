package kr.co.MadaAPI.service;


import com.google.gson.Gson;
import kr.co.MadaAPI.vo.ProductDateVo;
import kr.co.MadaAPI.vo.ResponseInfo;
import kr.co.MadaAPI.vo.ReviewVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.json.JSONParser;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.json.Json;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ProductDateService {


    private WebDriver driver;
    private WebElement element;

    @Value("${selenium.driver.id}")
    public String WEB_DRIVER_ID;

    @Value("${selenium.driver.path}")
    public String WEB_DRIVER_PATH;

    @Autowired
    Gson gson;

    public ResponseInfo getProductDate(String url) {

        ResponseInfo responseInfo = new ResponseInfo();

        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);

        // WebDriver 옵션 설정
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--start-maximized");
        //options.addArguments("--disable-popup-blocking");
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--single-process");

        driver = new ChromeDriver(options);
        Map<String,Integer> sortedMap = new HashMap<>();
        List<ReviewVo> resultList =new ArrayList();

        try{
            // 상품에 대한 정보 담겨있음
            driver.get(url);
            driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);

            HttpURLConnection c= (HttpURLConnection)new URL(url).openConnection();
            // set the HEAD request with setRequestMethod
            c.setRequestMethod("HEAD");
            // connection started and get response code
            c.connect();
            int r = c.getResponseCode();
            if(r == 404){
                return new ResponseInfo(-2,"상품이 존재하지 않습니다.");
            }

            JavascriptExecutor js = (JavascriptExecutor) driver;
            Map<String,Object> map = (Map<String, Object>) js.executeScript("return __PRELOADED_STATE__.product.A");
            //System.out.println(map);
            ProductDateVo productDateVo = new ProductDateVo();
            productDateVo.setProductId(map.get("id").toString());

            String reviewAmountString = map.get("reviewAmount").toString().replace("{","").replace("}","");
            String [] reviewAmountArray = reviewAmountString.split("[=|,]");
            Map<String,Double> reviewAmountMap = new HashMap();
            //System.out.println(Arrays.toString(reviewAmountArray));
            for(int i=0; i<reviewAmountArray.length; i+=2){
                reviewAmountMap.put(reviewAmountArray[i].trim(), Double.parseDouble(reviewAmountArray[i+1].trim()));
            }
            //averageReviewScore=4.86, generalReviewDisSatisfactionCount=0, generalReviewNormalCount=0, generalReviewSatisfactionCount=0, premiumReviewCount=0, premiumReviewHighlyRecommendationCount=0, premiumReviewNormalCount=0, premiumReviewNotRecommendationCount=0, premiumReviewRecommendationCount=0, productSatisfactionPercent=0, score1ReviewCount=3, score2ReviewCount=7, score3ReviewCount=35, score4ReviewCount=97, score5ReviewCount=1249, totalReviewCount=1391
            productDateVo.setReviewAmount(reviewAmountMap);

            String saleAmountString = map.get("saleAmount").toString().replace("{","").replace("}","");
            String [] saleAmountArray = saleAmountString.split("[=|,]");
            Map<String,Long> saleAmountMap = new HashMap();
            for(int i=0; i<saleAmountArray.length; i+=2){
                saleAmountMap.put(saleAmountArray[i].trim(),Long.parseLong(saleAmountArray[i+1].trim()));
            }
            //"saleAmount": "{cumulationSaleCount=4109, recentSaleCount=16}",
            productDateVo.setSaleAmount(saleAmountMap);

            Map<String,Object> rerepresentImageMap = (Map<String, Object>)map.get("representImage");
            productDateVo.setThumbnail(rerepresentImageMap.get("url").toString());

            //2021-05-07T00:45:31.515+0000
            String data = map.get("regDate").toString();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSZ");
            Date result;
            result = df.parse(data);
            //System.out.println("date:"+result); //prints date in current locale
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일 E요일");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            //System.out.println(sdf.format(result)); //prints date in the format sdf

            //String parsedLocalDateTimeNow = localDateTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일").withLocale(Locale.forLanguageTag("ja")));
            productDateVo.setRegDate(sdf.format(result));
            //System.out.println(sdf.format(result));
            productDateVo.setDelivery(map.get("productDeliveryLeadTimes"));
            productDateVo.setImages(map.get("productImages"));

            responseInfo.setReturnCode(0);
            responseInfo.setReturnMsg("등록일자 조회 성공");
            responseInfo.setData(productDateVo);
        }catch(Exception e){
            log.info(e.toString());
            responseInfo.setReturnCode(-1);
            responseInfo.setReturnMsg("등록일자 조회 실패");
            responseInfo.setData(e.toString());
        }
        driver.quit();
        return responseInfo;
    }
}
