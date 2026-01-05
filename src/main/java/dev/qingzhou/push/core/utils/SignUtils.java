package dev.qingzhou.push.core.utils;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SignUtils {

    /**
     * HmacSHA256 签名 (适用于钉钉、飞书)
     * 算法：URLEncode(Base64(HmacSHA256(Secret, StringToSign)))
     */
    public static String sign(String secret, String stringToSign) {
        try {
            byte[] signData = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, secret).hmac(stringToSign);
            String sign = Base64.encodeBase64String(signData);
            return URLEncoder.encode(sign, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Sign Error", e);
        }
    }
}