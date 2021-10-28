package com.bojian.tool.utils;

import com.alibaba.fastjson.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;


@SpringBootTest
public class HttpClientUtilTest {

    @Test
    public void executeGet() {
        String url = "https://www.baidu.com/";
        String message = HttpClientUtil.executeGet(url, null);
        System.out.println("message = " + message);
    }

    @Test
    public void postForm() {
    }

    @Test
    public void postJson() {
        //本地
        String localUrl="";
        //远程
        String remoteUrl="";

        //参数
        JSONObject loginJson=new JSONObject();
        loginJson.put("key","");

        //市局header头所需要的参数
        Map<String,String> mapHeaders=new HashMap<>();
        mapHeaders.put("Cookie","");
        //返回结果信息
        String resultJson = HttpClientUtil.postJson(localUrl, loginJson.toString(), mapHeaders);
        System.out.println("resultJson = " + resultJson);
    }

    @Test
    public void postHeader() {
    }

    @Test
    public void testPostHeader() {
    }

    @Test
    public void testPostHeader1() {
    }

    @Test
    public void testPostHeader2() {
    }

    @Test
    public void postCookie() {
    }
}