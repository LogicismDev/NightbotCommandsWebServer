package me.Logicism.NightbotCommandsWebServer.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class TextUtils {

    public static Map<String, String> queryToMap(String query) {
        Map<String, String> map = new HashMap<>();

        for (String entry : query.split("&")) {
            String[] entryQ = entry.split("=");

            map.put(entryQ[0], entryQ[1]);
        }

        return map;
    }

}
