package kr.co.MadaAPI.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kr.co.MadaAPI.vo.KeywordVo;
import kr.co.MadaAPI.vo.ResponseInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class KeywordService {
    @Value("${naver.keyword.accessKey}")
    String accessKey;
    @Value("${naver.keyword.secretKey}")
    String secretKey;
    @Value("${naver.keyword.customerId}")
    String customerId;

    // 검색 API
    @Value("${naver.search.clientId}")
    String clientId;
    @Value("${naver.search.clientSecret}")
    String clientSecret;

    RestTemplate restTemplate = new RestTemplate();

    @Autowired
    Gson gson;

    private WebDriver driver;
    @Value("${selenium.driver.id}")
    public String WEB_DRIVER_ID;

    @Value("${selenium.driver.path}")
    public String WEB_DRIVER_PATH;

    public ResponseInfo searchKeywordSelenium(String url){

        List<KeywordVo> keywordVoList = new ArrayList();
        ResponseInfo responseInfo = new ResponseInfo();
        System.setProperty(WEB_DRIVER_ID, WEB_DRIVER_PATH);

        // WebDriver 옵션 설정
        ChromeOptions options = new ChromeOptions();
        //options.addArguments("--start-maximized");
        //options.addArguments("--disable-popup-blocking");
        options.addArguments("--headless");

        driver = new ChromeDriver(options);

        try {
            driver.get(url);
            driver.manage().timeouts().implicitlyWait(5, TimeUnit.SECONDS);
            String reviewCount = driver.findElement(By.xpath("/html/body/div/div/div[3]/div[2]/div[2]/div/div[3]/div[3]/ul/li[3]/a/span")).getText();

            JavascriptExecutor js = (JavascriptExecutor) driver;
            //String product = (String) js.executeScript("return __PRELOADED_STATE__.product.A.contentName");
            //System.out.println(product);
            String keyword = (String) js.executeScript("return document.querySelector(\"head > meta:nth-child(6)\").content");
            //String keywordArray = (String) js.executeScript("return document.querySelector(\"head > meta:nth-child(6)\")");
            String [] keywordArray = keyword.split(",");

            driver.close();

            Set<String> set = new HashSet<String>(Arrays.asList(keywordArray));
            if(set.contains("")) set.remove("");

            keywordArray = set.toArray(new String[0]);
            keywordVoList =  searchKeyword(keywordArray);

            responseInfo.setReturnCode(0);
            responseInfo.setReturnMsg("키워드 조회 성공");
            responseInfo.setData(keywordVoList);
        }catch(Exception e){
            log.info(e.toString());
            responseInfo.setReturnCode(-1);
            responseInfo.setReturnMsg("키워드 조회 실패");
        }
        driver.close();
        return responseInfo;
    }


    public List<KeywordVo> searchKeyword(String [] keywordArray) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {

        List<KeywordVo> keywordVoList = new ArrayList();

        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            String data = timestamp.getTime() + ".GET./keywordstool";
            String signature = HmacAndBase64(secretKey, data, "HmacSHA256");
            restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(Charset.forName("UTF-8")));

            HttpHeaders headers = new HttpHeaders();
            headers.add("Accept", "application/json");
            headers.add("X-Timestamp", timestamp.getTime() + "");
            headers.add("X-API-KEY", accessKey);
            headers.add("X-API-SECRET", secretKey);
            headers.add("X-CUSTOMER", customerId);
            headers.add("X-Signature", signature);
            HttpEntity<String> entity = new HttpEntity(headers);

            HttpHeaders headers2 = new HttpHeaders();
            headers2.add("Accept", "application/json");
            headers2.add("X-Naver-Client-Id", clientId);
            headers2.add("X-Naver-Client-Secret", clientSecret);
            HttpEntity<String> entity2 = new HttpEntity(headers2);


            for(String keyword : keywordArray){

                String keywordUrl = "https://api.naver.com/keywordstool?hintKeywords=" + keyword + "&showDetail=1";
                String searchUrl = "https://openapi.naver.com/v1/search/shop.json?query=" + keyword + "&display=1";

                ResponseEntity<String> response = restTemplate.exchange(keywordUrl, HttpMethod.GET, entity, String.class);
                ResponseEntity<String> response2 = restTemplate.exchange(searchUrl, HttpMethod.GET, entity2, String.class);

                JsonObject jsonObject = gson.fromJson(response.getBody(), JsonObject.class);
                JsonObject jsonObject2 = gson.fromJson(response2.getBody(), JsonObject.class);
                JsonArray keywordJsonArray = gson.fromJson(jsonObject.getAsJsonObject().get("keywordList"), JsonArray.class);

                for(JsonElement jsonElement : keywordJsonArray){
                    if(jsonElement.getAsJsonObject().get("relKeyword").toString().replace("\"","").equals(keyword)){
                        KeywordVo keywordVo = new KeywordVo();
                        int total = jsonObject2.getAsJsonObject().get("total").getAsInt();
                        int clkCntSum = calcSum(jsonElement.getAsJsonObject().get("monthlyPcQcCnt").getAsString(), jsonElement.getAsJsonObject().get("monthlyMobileQcCnt").getAsString());
                        double compIdx = (double)total/clkCntSum;
                        compIdx = (Math.round(compIdx*100)/10000.0);

                        keywordVo.setRelKeyword(keyword);
                        keywordVo.setMonthlyPcQcCnt(jsonElement.getAsJsonObject().get("monthlyPcQcCnt").getAsString().contains("<") ? "10" : jsonElement.getAsJsonObject().get("monthlyPcQcCnt").getAsString());
                        keywordVo.setMonthlyMobileQcCnt(jsonElement.getAsJsonObject().get("monthlyMobileQcCnt").getAsString().contains("<") ? "10" : jsonElement.getAsJsonObject().get("monthlyMobileQcCnt").getAsString());
                        keywordVo.setMonthlyAvePcClkCnt(jsonElement.getAsJsonObject().get("monthlyAvePcClkCnt").getAsString());
                        keywordVo.setMonthlyAveMobileClkCnt(jsonElement.getAsJsonObject().get("monthlyAveMobileClkCnt").getAsString());
                        keywordVo.setMonthlyAvePcCtr(jsonElement.getAsJsonObject().get("monthlyAvePcCtr").getAsString());
                        keywordVo.setMonthlyAveMobileCtr(jsonElement.getAsJsonObject().get("monthlyAveMobileCtr").getAsString());
                        keywordVo.setPlAvgDepth(jsonElement.getAsJsonObject().get("plAvgDepth").getAsString());
                        keywordVo.setTotal(String.valueOf(total));
                        keywordVo.setClkCntSum(String.valueOf(clkCntSum));
                        keywordVo.setCompIdx(String.valueOf(compIdx));
                        keywordVoList.add(keywordVo);
                        break;
                    }
                }
                Thread.sleep(300);
            }
        }catch(Exception e){
            log.info("searchKeyword 조회 에러 "+e.toString());
            log.info(e.toString());
        }
        return keywordVoList;
    }

    public int calcSum(String monthlyPcQcCnt, String monthlyMobileQcCnt){

        try{
            if(monthlyPcQcCnt.contains("<")){
                monthlyPcQcCnt = "10";
            }

            if(monthlyMobileQcCnt.contains("<")){
                monthlyMobileQcCnt = "10";
            }
            return Integer.parseInt(monthlyPcQcCnt)+Integer.parseInt(monthlyMobileQcCnt);
        }catch(Exception e){
            log.info("calcSum 오류 : " + e.toString());
            return 1;
        }



    }

    public String HmacAndBase64(String secret, String data, String Algorithms) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        try{
            //1. SecretKeySpec 클래스를 사용한 키 생성
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes("utf-8"), Algorithms);

            //2. 지정된  MAC 알고리즘을 구현하는 Mac 객체를 작성합니다.
            Mac hasher = Mac.getInstance(Algorithms);

            //3. 키를 사용해 이 Mac 객체를 초기화
            hasher.init(secretKey);

            //3. 암호화 하려는 데이터의 바이트의 배열을 처리해 MAC 조작을 종료
            byte[] hash = hasher.doFinal(data.getBytes());

            //4. Base 64 Encode to String
            return Base64.encodeBase64String(hash);
        }catch(Exception e){
            log.info(e.toString());
            return "false";
        }
    }
}
