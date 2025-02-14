package com.example.admin.sinlineapplication.util;

import android.os.Build;
import androidx.annotation.RequiresApi;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class Token {

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String assembleToken(String version, String resourceName, String expirationTime,
            String signatureMethod, String accessKey)
            throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        StringBuilder sb = new StringBuilder();
        String res = URLEncoder.encode(resourceName, "UTF-8");
        String sig = URLEncoder
                .encode(generatorSignature(version, resourceName, expirationTime, accessKey, signatureMethod), "UTF-8");
        sb.append("version=")
                .append(version)
                .append("&res=")
                .append(res)
                .append("&et=")
                .append(expirationTime)
                .append("&method=")
                .append(signatureMethod)
                .append("&sign=")
                .append(sig);
        return sb.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String generatorSignature(String version, String resourceName, String expirationTime,
            String accessKey, String signatureMethod)
            throws NoSuchAlgorithmException, InvalidKeyException {
        String encryptText = expirationTime + "\n" + signatureMethod + "\n" + resourceName + "\n" + version;
        String signature;
        byte[] bytes = HmacEncrypt(encryptText, accessKey, signatureMethod);
        signature = Base64.getEncoder().encodeToString(bytes);
        return signature;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] HmacEncrypt(String data, String key, String signatureMethod)
            throws NoSuchAlgorithmException, InvalidKeyException {
        // 根据给定的字节数组构造一个密钥,第二参数指定一个密钥算法的名称
        SecretKeySpec signinKey = null;
        signinKey = new SecretKeySpec(Base64.getDecoder().decode(key),
                "Hmac" + signatureMethod.toUpperCase());

        // 生成一个指定 Mac 算法 的 Mac 对象
        Mac mac = null;
        mac = Mac.getInstance("Hmac" + signatureMethod.toUpperCase());

        // 用给定密钥初始化 Mac 对象
        mac.init(signinKey);

        // 完成 Mac 操作
        return mac.doFinal(data.getBytes());
    }

    public enum SignatureMethod {
        SHA1, MD5, SHA256;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String token() throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String version = "2020-05-29";
        String resourceName = "userid/411659";// 这里写你的用户id
        String expirationTime = System.currentTimeMillis() / 1000 + 100 * 24 * 60 * 60 + "";
        String signatureMethod = SignatureMethod.SHA1.name().toLowerCase();
        // 这个也是换成自己的Accesskey
        String accessKey = "LBbS64ycVUNIbBwt7IWRSyVpKhjK9A/CAeQX8kTeVqqMmSv9+QQ/K/mQIrEj58Ih";// 这里放你的AccessKey（在Onenet官网的账户信息里面）
        String token = assembleToken(version, resourceName, expirationTime, signatureMethod, accessKey);
        // // System.out.println("Authorization:" + token);
        return token;
    }
}
