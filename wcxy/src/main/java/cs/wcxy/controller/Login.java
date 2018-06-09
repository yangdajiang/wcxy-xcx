package cs.wcxy.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import cs.wcxy.util.RedisUtil;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by 15114 on 2018/5/16.
 */
@Controller
@RequestMapping("/User")
public class Login {

    @RequestMapping("/Login")
    @ResponseBody
    public String login(@RequestParam("id") String id ,
                        @RequestParam("password") String password,
                        @RequestParam("session_3rd") String session_3rd) throws URISyntaxException {

        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(
                "http://zhjsapi.changxiaoyuan.com/user/login.action");
        List<NameValuePair> formparams = new ArrayList<>();
        formparams.add(new BasicNameValuePair("userName", id));
        formparams.add(new BasicNameValuePair("userPassword", password));

        UrlEncodedFormEntity entity = null;
        try {
            entity = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(entity);
            CloseableHttpResponse response = httpClient.execute(post);

            JSONObject jsonObject1 = new JSONObject();
            jsonObject1.put("mid_res","账号或密码错误");

            try {
                HttpEntity resEntity = response.getEntity();
                if (resEntity != null) {
                    String res = EntityUtils.toString(resEntity, "UTF-8");
                    JSONObject jsonObject = JSON.parseObject(res);
                    String isSucceed = jsonObject.get("IsSucceed").toString();
                    //System.out.print(isSucceed);
                    if(!isSucceed.equals("true")){
                        return jsonObject1.toJSONString();
                    }
                }
            } finally {
                response.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭连接,释放资源
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        RedisUtil redisUtil = new RedisUtil();
        Jedis jedis = redisUtil.getJedis();
        String json = null;
        if(jedis.exists(session_3rd)){
            String u_id = "id:" + id;
            String u_password = "password:" + password;
            jedis.del(session_3rd);
            if(jedis.rpush(session_3rd,u_id,u_password) == 2)
            {
                jedis.expire(session_3rd, 1800);
                json = "{\"res\":\"绑定成功\"}";
                redisUtil.returnResource(jedis);
                return json;
            }else{
                json = "{\"res\":\"绑定失败\"}";
                redisUtil.returnResource(jedis);
                return json;
            }
        }else {
            json = "{\"res\":\"绑定失败\"}";
            redisUtil.returnResource(jedis);
            return json;
        }
    }

    @RequestMapping("/Logout")
    @ResponseBody
    public String Logout(@RequestParam("session_3rd") String session_3rd){

        RedisUtil redisUtil = new RedisUtil();
        Jedis jedis = redisUtil.getJedis();
        if(jedis.expire(session_3rd, 180)==1){
            redisUtil.returnResource(jedis);
            return "{\"res\" : \"操作成功\"}";
        }else {
            redisUtil.returnResource(jedis);
            return "{\"res\" : \"操作失败\"}";
        }
    }
}
