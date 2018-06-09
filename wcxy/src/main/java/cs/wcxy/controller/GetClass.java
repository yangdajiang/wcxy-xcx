package cs.wcxy.controller;

import cs.wcxy.util.RedisUtil;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by 15114 on 2018/5/17.
 */
@Controller
public class GetClass {

    @RequestMapping("/getClass")
    @ResponseBody
    public String getClassInfo(@RequestParam("session_3rd") String session_3rd,
                               HttpServletResponse httpResponse) throws IOException {

        RedisUtil redisUtil = new RedisUtil();
        Jedis jedis = redisUtil.getJedis();
        List<String> stringList =jedis.lrange(session_3rd,0,-1);
        if(stringList==null){
            httpResponse.sendError(404);
            System.out.println(2);
            return null;
        }

        String id = null;
        String password = null;

        for(String s : stringList){
            if(s.substring(0,s.indexOf(":")).equals("id")) {
                id = s.substring(s.indexOf(":")+1,s.length());
                System.out.println(id);
            }else {
                password = s.substring(s.indexOf(":")+1,s.length());
                System.out.println(password);
            }
        }

        CookieStore cookieStore = new BasicCookieStore();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();

        List<NameValuePair> valuePairs = new LinkedList<>();
        valuePairs.add(new BasicNameValuePair("userName", id));
        valuePairs.add(new BasicNameValuePair("userPassword", password));
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(valuePairs, Consts.UTF_8);
        entity.setContentType("application/x-www-form-urlencoded");
        // 创建一个post请求
        HttpPost post = new HttpPost("http://zhjsapi.changxiaoyuan.com/user/login.action");
        // 注入post数据
        post.setEntity(entity);
        httpClient.execute(post);
        List<Cookie> cookies = cookieStore.getCookies();
        String JSESSIONID = cookies.get(0).getValue();

        BasicClientCookie cookie = new BasicClientCookie("JSESSIONID" ,JSESSIONID);
        cookie.setVersion(0);
        cookie.setDomain("zhjsapi.changxiaoyuan.com");   //设置范围
        cookie.setPath("/");
        cookieStore.addCookie(cookie);
        CloseableHttpClient httpClient2 = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        CloseableHttpResponse ress = null;
        HttpGet get = new HttpGet("http://zhjsapi.changxiaoyuan.com/user/getStuClass.action");
        ress = httpClient2.execute(get);
        String result = EntityUtils.toString(ress.getEntity());

        redisUtil.returnResource(jedis);
        return result;

    }

}
