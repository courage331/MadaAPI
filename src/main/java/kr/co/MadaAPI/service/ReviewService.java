package kr.co.MadaAPI.service;

import com.google.gson.*;
import kr.co.MadaAPI.vo.ResponseInfo;
import kr.co.MadaAPI.vo.ReviewVo;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ReviewService {

    private WebDriver driver;

    @Value("${selenium.driver.id}")
    public String WEB_DRIVER_ID;

    @Value("${selenium.driver.path}")
    public String WEB_DRIVER_PATH;

    @Autowired
    Gson gson;

    public ResponseInfo reviewSelenium(String url){

        ResponseInfo responseInfo = new ResponseInfo();

        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);

        // WebDriver 옵션 설정
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--single-process");

        driver = new ChromeDriver(options);
        List<ReviewVo> resultList =new ArrayList();

        try {
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
            String productNo = (String) js.executeScript("return __PRELOADED_STATE__.product.A.productNo");
            String merchantNo = (String) js.executeScript("return __PRELOADED_STATE__.product.A.channel.naverPaySellerNo");
            String reviewCount = String.valueOf(js.executeScript("return __PRELOADED_STATE__.product.A.reviewAmount.totalReviewCount"));
            //System.out.println(reviewCount);
            //driver.close();
            resultList =  getReview(reviewCount, productNo, merchantNo);

            responseInfo.setReturnCode(0);
            responseInfo.setReturnMsg("리뷰 조회 성공");
            responseInfo.setData(resultList);
        }catch(Exception e){
            log.info(e.toString());
            responseInfo.setReturnCode(-1);
            responseInfo.setReturnMsg("리뷰 조회 실패");
            responseInfo.setData(e.toString());
        }
        driver.quit();
        return responseInfo;
    }

    public List<ReviewVo> getReview(String reviewCount, String productNo, String merchantNo) {
        List<ReviewVo> responseList = new ArrayList();
        Map<String, Integer> countMap = new HashMap();
        Map<String, Integer> scoreMap = new HashMap();

        try{
            int reviewCnt = Integer.parseInt(reviewCount.replace(",", ""));
            int pageSize = 30;
            int pageNum = reviewCnt % pageSize == 0 ? reviewCnt / pageSize : reviewCnt / pageSize + 1;

            String url = "";

            for (int i = 1; i <= pageNum; i++) {
                url = "https://smartstore.naver.com/i/v1/reviews/paged-reviews?page=" + i +
                        "&pageSize="+pageSize+"&merchantNo=" + merchantNo + "originProductNo=" + productNo + "&sortType=REVIEW_RANKING";

                MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
                parameters.add("page", i + "");
                parameters.add("pageSize", pageSize+"");
                parameters.add("merchantNo", merchantNo);
                parameters.add("originProductNo", productNo);
                parameters.add("sortType", "REVIEW_CREATE_DATE_DESC");

                ResponseEntity<String> response = new RestTemplate().postForEntity(url, parameters, String.class);
                JsonObject jsonObject = gson.fromJson(response.getBody(), JsonObject.class);
                JsonArray contentsArray = gson.fromJson(jsonObject.getAsJsonObject().get("contents"), JsonArray.class);
                /**
                 *             page           : i,
                 *             pageSize       : 30,
                 *             merchantNo     : merchantNo,
                 *             originProductNo: originProductNo,
                 *             sortType       : 'REVIEW_RANKING'
                 */
                //HttpEntity<String> entity = new HttpEntity<String>(reviewJsonObject.toString(), headers);
                String key = "";
                int score = 0;
                //System.out.println(contentsArray);
                for (JsonElement jsonElement : contentsArray) {
                    try {
                        key = jsonElement.getAsJsonObject().get("productOptionContent").getAsString();
                        score = jsonElement.getAsJsonObject().get("reviewScore").getAsInt();
                        if (countMap.containsKey(key)) {
                            countMap.put(key, countMap.get(key) + 1);
                            scoreMap.put(key, scoreMap.get(key)+score);
                        } else {
                            countMap.put(key, 1);
                            scoreMap.put(key, score);
                        }
                    } catch (Exception e) {
                        log.info("getReview() foreach문 error : "+ e.toString());
                    }
                }
            }
            //log.info("에러 나는것들 : "+cnt);
            //log.info(scoreMap.size()+"::"+countMap.size());
            for( String key : countMap.keySet() ){
                double score = Math.round(((double)scoreMap.get(key)/countMap.get(key)) * 10)/10.0;
                ReviewVo reviewVo = ReviewVo.builder().productOptionContent(key).cnt(countMap.get(key)).reviewScore(score).build();
                responseList.add(reviewVo);
            }
            Collections.sort(responseList);
        }catch(Exception e){
            log.info("getReview 에러 : " + e.toString());
        }
        return responseList;
    }

    //Map 정렬
    public static LinkedHashMap<String, Integer> sortMapByValue(Map<String, Integer> map) {
        LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
        try{
            List<Map.Entry<String, Integer>> entries = new LinkedList<>(map.entrySet());
            // 오름차순
            //Collections.sort(entries, (o1, o2) -> o1.getValue().compareTo(o2.getValue()));
            // 내림차순
            Collections.sort(entries, (o1, o2) -> o2.getValue().compareTo(o1.getValue()));

            for (Map.Entry<String, Integer> entry : entries) {
                result.put(entry.getKey(), entry.getValue());
            }
        }catch(Exception e){
            log.info("sortMapByValue 오류 : "+e.toString());
        }
        return result;
    }
}
