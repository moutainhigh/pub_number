package com.ehome.dts.wx.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.ehome.dts.wx.common.bean.JsApiTicket;

@Component
public class JsApiTicketUtils {
	@Autowired
	private AccessTokenUtils accessTokenUtils;
    private static final long MAX_TIME = 7200 * 1000;// 微信允许最长Access_token有效时间（ms）
//    public static void main(String[] args) {
//		String accessString = AccessTokenUtils.getSavedAccess_token();
//		System.out.println(accessString);
//	}

    /*
     * 该方法实现获取Access_token、保存并且只保存2小时Access_token。如果超过两个小时重新获取；如果没有超过两个小时，直接获取。该方法依赖
     * ：public static String getAccessToken()；
     * 
     * 思路:将获取到的Access_token和当前时间存储到file里，
     * 取出时判断当前时间和存储里面的记录的时间的时间差，如果大于MAX_TIME,重新获取，并且将获取到的存储到file替换原来的内容
     * ，如果小于MAX_TIME，直接获取。
     */
    // 为了调用不抛异常，这里全部捕捉异常，代码有点长
    public String getSavedJsApiTicket() {
        String mAccess_token = null;// 需要获取的Access_token；
        FileOutputStream fos = null;// 输出流
        FileInputStream fis = null;// 输入流
        File file = new File("js_api_ticket.temp");// Access_token保存的位置
        try {
            // 如果文件不存在，创建
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        // 如果文件大小等于0，说明第一次使用，存入Access_token
        if (file.length() == 0) {
            try {
                mAccess_token = getJsApiTicket();// 获取AccessToken
                JsApiTicket at = new JsApiTicket();
                at.setTicket(mAccess_token);
                at.setExpires_in(System.currentTimeMillis());// 设置存入时间
                String json = JSON.toJSONString(at);
                fos = new FileOutputStream(file, false);// 不允许追加
                fos.write((json).getBytes());// 将AccessToken和当前时间存入文件
                fos.close();
                return mAccess_token;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            // 读取文件内容
            byte[] b = new byte[2048];
            int len = 0;
            try {
                fis = new FileInputStream(file);
                len = fis.read(b);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            String mJsonAccess_token = new String(b, 0, len);// 读取到的文件内容
            JsApiTicket access_token = JSON.parseObject(mJsonAccess_token, JsApiTicket.class);
            if (access_token.getExpires_in() > 0) {
                long saveTime = access_token.getExpires_in();
                long nowTime = System.currentTimeMillis();
                long remianTime = nowTime - saveTime;
                // System.out.println(TAG + "时间差：" + remianTime + "ms");
                if (remianTime < MAX_TIME) {
                    mAccess_token = access_token.getTicket();
                    return mAccess_token;
                } else {
                    mAccess_token = getJsApiTicket();
                    JsApiTicket at = new JsApiTicket();
                    at.setTicket(mAccess_token);
                    at.setExpires_in(System.currentTimeMillis());
                    String json = JSON.toJSONString(at);
                    try {
                        fos = new FileOutputStream(file, false);// 不允许追加
                        fos.write((json).getBytes());
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return mAccess_token;
                }

            } else {
                return null;
            }
        }

        return mAccess_token;
    }

    private String getJsApiTicket() {
        String urlString = "https://api.weixin.qq.com/cgi-bin/ticket/getticket?access_token="+accessTokenUtils.getSavedAccess_token()+"&type=jsapi";
        String reslut = null;
        try {
            URL reqURL = new URL(urlString);
            HttpsURLConnection httpsConn = (HttpsURLConnection) reqURL
                    .openConnection();
            InputStreamReader isr = new InputStreamReader(
                    httpsConn.getInputStream());
            char[] chars = new char[1024];
            reslut = "";
            int len;
            while ((len = isr.read(chars)) != -1) {
                reslut += new String(chars, 0, len);
            }
            isr.close();
        } catch (IOException e) {

            e.printStackTrace();
        }
        JsApiTicket access_token = JSON.parseObject(reslut, JsApiTicket.class);
        if (access_token.getTicket() != null) {
            return access_token.getTicket();
        } else {
            return null;
        }
    }
}