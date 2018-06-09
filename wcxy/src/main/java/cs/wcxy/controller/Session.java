package cs.wcxy.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cs.wcxy.util.RedisUtil;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by 15114 on 2018/5/16.
 */

@Controller
@RequestMapping("/Session")
public class Session {

    @RequestMapping("/getSession")
    @ResponseBody
    public String getSession(@RequestParam("code") String code , HttpServletResponse responses) throws URISyntaxException, IOException {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        URIBuilder uriBuilder = new URIBuilder("https://api.weixin.qq.com/sns/jscode2session");

        uriBuilder.addParameter("appid","wxdeb85021df04d56f");
        uriBuilder.addParameter("secret","babed8780edd95c7f804ff0a82467c92");
        uriBuilder.addParameter("js_code",code);
        uriBuilder.addParameter("grant_type","authorization_code");

        HttpGet get = new HttpGet(uriBuilder.build());
        CloseableHttpResponse response = httpClient.execute(get);
        HttpEntity httpEntity = response.getEntity();
        String context =  EntityUtils.toString(httpEntity);
        System.out.println(context);
        JSONObject jsonObject = JSON.parseObject(context);
        String session_3rd = jsonObject.getString("session_key");
        RedisUtil redisUtil = new RedisUtil();
        Jedis jedis = redisUtil.getJedis();
        if(session_3rd!=null){
            String uuid = java.util.UUID.randomUUID().toString().replace("-","");
            String json = "{\"session_3rd\":\"" + uuid + "\"}";
            jedis.set(uuid,session_3rd);
            //System.out.println(json);
            redisUtil.returnResource(jedis);
            return json;
        }else {
            responses.sendError(404);
            redisUtil.returnResource(jedis);
            //System.out.println(2);
            return null;
        }
    }
}
