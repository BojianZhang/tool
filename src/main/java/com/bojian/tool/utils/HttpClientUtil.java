package com.bojian.tool.utils;

import org.apache.http.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.pool.PoolStats;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author zbj
 * @version 0.1
 * @application httpClientUtil工具类
 * @time 2021-10-28-9:36
 */
public class HttpClientUtil {

    //定制一个httpClient
    private static final HttpClientBuilder httpClientBuilder= HttpClients.custom();

    //在static中对httpClient初始化
    static{
        /**
         * 绕过不安全的https请求的证书验证
         */
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.INSTANCE)
                .register("https", trustHttpsCertificates())
                .build();

        /**
         * 创建连接池
         */
        PoolingHttpClientConnectionManager pool=new PoolingHttpClientConnectionManager(registry);
        pool.setMaxTotal(50);//连接池最大有50个连接

        /**
         * 路由:域名/ip+端口
         */
        pool.setDefaultMaxPerRoute(50);//每个路由默认有多少连接

        //连接池的最大连接数
        System.out.println("pool.getMaxTotal():"+pool.getMaxTotal());
        //每个路由的最大连接数
        System.out.println("pool.getDefaultMaxPerRoute():" + pool.getDefaultMaxPerRoute());
        PoolStats totalStats = pool.getTotalStats();
        //连接池的最大连接数
        System.out.println("totalStats.getMax():" + totalStats.getMax());
        //连接池里有多少连接是被占用了
        System.out.println("totalStats.getLeased():" + totalStats.getLeased());
        //连接池里有多少连接是可用的
        System.out.println("totalStats.getAvailable():" + totalStats.getAvailable());
        httpClientBuilder.setConnectionManager(pool);

        /**
         * 设置请求的默认配置
         */
        RequestConfig requestConfig= RequestConfig.custom()
                .setConnectTimeout(5000)//连接超时时间(tcp握手时间)
                .setSocketTimeout(3000)//读取时间
                .setConnectionRequestTimeout(5000)//从连接池里获取连接的超时时间
                .build();
        httpClientBuilder.setDefaultRequestConfig(requestConfig);

        /**
         * 设置默认的一些header
         */
        List<Header> defaultHeaders=new ArrayList<>();
        //设置用户代理(解决不是真人的问题)
        BasicHeader userAgentHeader=new BasicHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/94.0.4606.81 Safari/537.36 Edg/94.0.992.50");
        defaultHeaders.add(userAgentHeader);
        httpClientBuilder.setDefaultHeaders(defaultHeaders);
    }

    /**
     * 构造安全连接工厂
     * @return
     */
    private static ConnectionSocketFactory trustHttpsCertificates() {
        SSLContextBuilder sslContextBuilder=new SSLContextBuilder();

        try {
            sslContextBuilder.loadTrustMaterial(null,new TrustStrategy(){
                //判断是否信任url
                @Override
                public boolean isTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                    return true;
                }
            });
            SSLContext sslContext=sslContextBuilder.build();

            return new SSLConnectionSocketFactory(sslContext,
                    new String[]{"SSLv2Hello","SSLv3","TLSv1","TLSv1.2"},
                    null,
                    NoopHostnameVerifier.INSTANCE);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("构造安全连接工厂失败");
        }
    }

    /**
     * 发送get请求
     * @param url 请求url,参数需经过URLEndode编码处理
     * @param headers 自定义请求头
     * @return 返回结果
     */
    public static String executeGet(String url, Map<String,String> headers){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpGet请求对象
        HttpGet httpGet=new HttpGet(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpGet.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            response=closeableHttpClient.execute(httpGet);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                //获取响应结果:DecompressingEntity
                /**
                 * HttpEntity不仅可以作为结果，也可以作为请求的参数实体，有很多实现
                 */
                HttpEntity entity=response.getEntity();
                //工具类，对HttpEntity操作的工具类
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            //使用工具,确保流的关闭
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 发送表单类型的post请求
     * @param url 请求的url
     * @param list 参数列表
     * @param headers 自定义头
     * @return 返回结果
     */
    public static String postForm(String url, List<NameValuePair> list, Map<String,String> headers){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是form类型
        httpPost.addHeader("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
        //给post对象设置请求参数
        UrlEncodedFormEntity formEntity=new UrlEncodedFormEntity(list, Consts.UTF_8);
        httpPost.setEntity(formEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            System.out.println("prepare to execute url:"+httpPost.getRequestLine());
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                HttpEntity entity=response.getEntity();
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 发送Json类型的post请求
     * @param url 请求url
     * @param body json字符串
     * @param headers 自定义header
     * @return 返回结果
     */
    public static String postJson(String url,String body, Map<String,String> headers){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是Json类型
        httpPost.addHeader("Content-Type","application/json;charset=utf-8");
        //给post对象设置请求参数
        StringEntity jsonEntity=new StringEntity(body,Consts.UTF_8);
        jsonEntity.setContentType("application/json;charset=utf-8");
        jsonEntity.setContentEncoding(Consts.UTF_8.name());
        httpPost.setEntity(jsonEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                HttpEntity entity=response.getEntity();
                return EntityUtils.toString(entity, StandardCharsets.UTF_8);
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 发送获取header的post请求(application/x-www-form-urlencoded;charset=utf-8)
     * @param url 请求url
     * @param list 参数列表
     * @param headers 自定义头
     * @param header 需要返回的值
     * @return
     */
    public static String postHeader(String url,List<NameValuePair> list, Map<String,String> headers,String header){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是form类型
        httpPost.addHeader("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
        //给post对象设置请求参数
        UrlEncodedFormEntity formEntity=new UrlEncodedFormEntity(list, Consts.UTF_8);
        httpPost.setEntity(formEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            System.out.println("prepare to execute url:"+httpPost.getRequestLine());
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                return response.getHeaders(header)[0].getValue().toString();
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }


    /**
     * 发送获取headers的post请求(application/x-www-form-urlencoded;charset=utf-8)
     * @param url
     * @param list
     * @param headers
     * @return
     */
    public static Header[] postHeader(String url, List<NameValuePair> list, Map<String,String> headers){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是form类型
        httpPost.addHeader("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
        //给post对象设置请求参数
        UrlEncodedFormEntity formEntity=new UrlEncodedFormEntity(list, Consts.UTF_8);
        httpPost.setEntity(formEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            System.out.println("prepare to execute url:"+httpPost.getRequestLine());
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                return response.getAllHeaders();
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 发送获取headers的post请求(application/json;charset=utf-8)
     * @param url 接口地址
     * @param body 接口请求体
     * @param headers 接口请求头
     * @param header 需要返回的请求头
     * @return
     */
    public static String postHeader(String url,String body, Map<String,String> headers,String header){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是Json类型
        httpPost.addHeader("Content-Type","application/json;charset=utf-8");
        //给post对象设置请求参数
        StringEntity jsonEntity=new StringEntity(body,Consts.UTF_8);
        jsonEntity.setContentType("application/json;charset=utf-8");
        jsonEntity.setContentEncoding(Consts.UTF_8.name());
        httpPost.setEntity(jsonEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            System.out.println("prepare to execute url:"+httpPost.getRequestLine());
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                return response.getHeaders(header)[0].getValue().toString();
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }


    /**
     * 发送获取headers的post请求("application/json;charset=utf-8")
     * @param url 接口地址
     * @param body 接口请求体
     * @param headers 接口请求头
     * @return
     */
    public static Header[] postHeader(String url,String body, Map<String,String> headers){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是Json类型
        httpPost.addHeader("Content-Type","application/json;charset=utf-8");
        //给post对象设置请求参数
        StringEntity jsonEntity=new StringEntity(body,Consts.UTF_8);
        jsonEntity.setContentType("application/json;charset=utf-8");
        jsonEntity.setContentEncoding(Consts.UTF_8.name());
        httpPost.setEntity(jsonEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            System.out.println("prepare to execute url:"+httpPost.getRequestLine());
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                return response.getAllHeaders();
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }

    /**
     * 通过发送post请求获取cookie信息("application/json;charset=utf-8")
     * @param url
     * @param body
     * @param headers
     * @return
     */
    public static String postCookie(String url,String body, Map<String,String> headers){
        //可关闭的httpClient客户端,相当于浏览器
        CloseableHttpClient closeableHttpClient=httpClientBuilder.build();
        //构造httpPost请求对象
        HttpPost httpPost=new HttpPost(url);
        //自定义请求头设置
        if(headers!=null){
            Set<Map.Entry<String,String>> entries=headers.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                httpPost.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
            }
        }
        //确保请求头是Json类型
        httpPost.addHeader("Content-Type","application/json;charset=utf-8");
        //给post对象设置请求参数
        StringEntity jsonEntity=new StringEntity(body,Consts.UTF_8);
        jsonEntity.setContentType("application/json;charset=utf-8");
        jsonEntity.setContentEncoding(Consts.UTF_8.name());
        httpPost.setEntity(jsonEntity);
        //可关闭的响应
        CloseableHttpResponse response=null;
        try{
            System.out.println("prepare to execute url:"+httpPost.getRequestLine());
            response=closeableHttpClient.execute(httpPost);
            StatusLine statusLine=response.getStatusLine();
            if(HttpStatus.SC_OK== statusLine.getStatusCode()){
                return response.getHeaders("Set-Cookie")[0].getValue().toString().split(",")[0].split("=")[1].split(";")[0];
            }else{
                System.out.println("响应失败,响应码为:"+statusLine.getStatusCode());
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            HttpClientUtils.closeQuietly(response);
        }
        return null;
    }
}
