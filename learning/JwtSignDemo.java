import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * JWT 签名原理演示（不依赖任何第三方库，纯 JDK）
 *
 * 运行：java JwtSignDemo.java
 *
 * 演示内容：
 *   ① 手工生成一个 JWT
 *   ② 把三段拆开看
 *   ③ 验签（正确的情况）
 *   ④ 篡改 payload 后验签 → 失败
 *   ⑤ 篡改签名后验签   → 失败
 */
public class JwtSignDemo {

    /** 从项目 application.yml 里抄来的密钥 */
    static final String SECRET = "smart-ticket-jwt-secret-key-2026-please-change-me";

    public static void main(String[] args) throws Exception {

        // ============================================================
        // ① 生成 JWT：就三步
        // ============================================================
        String headerJson  = "{\"alg\":\"HS256\"}";
        String payloadJson = "{\"sub\":\"1\",\"username\":\"admin\",\"role\":\"USER\"}";

        String h = b64url(headerJson.getBytes(StandardCharsets.UTF_8));   // 第 1 段
        String p = b64url(payloadJson.getBytes(StandardCharsets.UTF_8));  // 第 2 段

        String signingInput = h + "." + p;                                 // 要签名的内容
        String s = b64url(hmacSha256(signingInput, SECRET));               // 第 3 段 ★

        String token = signingInput + "." + s;

        System.out.println("========== ① 生成 token ==========");
        System.out.println("  header  JSON : " + headerJson);
        System.out.println("  payload JSON : " + payloadJson);
        System.out.println();
        System.out.println("  token =");
        System.out.println("    " + token);
        System.out.println();
        System.out.println("  它由三段用 . 拼成：");
        System.out.println("    第1段 header    : " + h);
        System.out.println("    第2段 payload   : " + p);
        System.out.println("    第3段 signature : " + s);

        // ============================================================
        // ② 把三段拆开、解码
        // ============================================================
        System.out.println();
        System.out.println("========== ② 解码看内容（不需要密钥！）==========");
        String[] parts = token.split("\\.");
        System.out.println("  切出来几段: " + parts.length);
        System.out.println("  header  解开 = " + decode(parts[0]));
        System.out.println("  payload 解开 = " + decode(parts[1]));
        System.out.println("  ↑ 看到了吗？没有密钥也能解开 —— 这就是 Base64 不是加密");

        // ============================================================
        // ③ 验签：正常情况
        // ============================================================
        System.out.println();
        System.out.println("========== ③ 验签（token 没被改过）==========");
        System.out.println("  验签结果: " + verify(token, SECRET));

        // ============================================================
        // ④ 篡改 payload：把 USER 改成 ADMIN
        // ============================================================
        System.out.println();
        System.out.println("========== ④ 篡改 payload：把 role 改成 ADMIN ==========");
        String evilPayloadJson = "{\"sub\":\"1\",\"username\":\"admin\",\"role\":\"ADMIN\"}";
        String evilP = b64url(evilPayloadJson.getBytes(StandardCharsets.UTF_8));
        // 攻击者只能用原来的签名（因为他不知道密钥，算不出新的）
        String evilToken = h + "." + evilP + "." + s;
        System.out.println("  伪造的 token 是: " + evilToken);
        System.out.println("  但签名还是旧的，验签结果: " + verify(evilToken, SECRET));
        System.out.println("  ↑ 攻击者想改 payload，但算不出新签名 → 被识破");

        // ============================================================
        // ⑤ 篡改签名：改一个字符
        // ============================================================
        System.out.println();
        System.out.println("========== ⑤ 篡改签名：最后一位改成 X ==========");
        String badSig = s.substring(0, s.length() - 1) + "X";
        String badToken = h + "." + p + "." + badSig;
        System.out.println("  payload 完全没动，只改了签名");
        System.out.println("  验签结果: " + verify(badToken, SECRET));
        System.out.println("  ↑ 这就是你实验 1 遇到的情况");

        // ============================================================
        // ⑥ 换个密钥验签
        // ============================================================
        System.out.println();
        System.out.println("========== ⑥ 用错误的密钥验签 ==========");
        System.out.println("  用 'wrong-secret-wrong-secret-wrong!' 验签: "
                + verify(token, "wrong-secret-wrong-secret-wrong!"));
        System.out.println("  ↑ 密钥不对，签名就算不出来 → 验签失败");

        System.out.println();
        System.out.println("==================================================");
        System.out.println("  核心结论：");
        System.out.println("  · 签名 = HMAC-SHA256(header + \".\" + payload, 密钥)");
        System.out.println("  · 没有密钥 → 算不出签名 → 改不了 token");
        System.out.println("  · 密钥泄露 → 谁都能伪造 token（这就是你项目的缺陷 5）");
        System.out.println("==================================================");
    }

    // ================================================================
    //  核心就这两个方法
    // ================================================================

    /** ★ 签名：HMAC-SHA256。JDK 自带，5 行搞定 */
    static byte[] hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }

    /** ★ 验签：用同样的密钥重新算一遍，比对 */
    static boolean verify(String token, String secret) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length != 3) return false;

        String expected = b64url(hmacSha256(parts[0] + "." + parts[1], secret));
        return expected.equals(parts[2]);
    }

    /** Base64Url 编码 —— 注意是 URL 版，不是标准 Base64 */
    static String b64url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String decode(String b64) {
        return new String(Base64.getUrlDecoder().decode(b64), StandardCharsets.UTF_8);
    }
}
