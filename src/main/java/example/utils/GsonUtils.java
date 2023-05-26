package example.utils;

import com.google.gson.Gson;
import example.Entity.ExtraInfo;
import example.Model.JsonPack;

public class GsonUtils {
    private static final Gson gson = new Gson();

    public static String msg2Json(int code, String msg, ExtraInfo extra) {
        return gson.toJson(new JsonPack(code, msg, extra));
    }

    public static String msg2Json(int code, String msg) {
        return gson.toJson(new JsonPack(code, msg));
    }

    public static String obj2Str(JsonPack msg) {
        return gson.toJson(msg);
    }
}
