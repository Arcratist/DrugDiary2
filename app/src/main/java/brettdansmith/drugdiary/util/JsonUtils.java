package brettdansmith.drugdiary.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class JsonUtils {
    private JsonUtils() {
    }

    public static JSONObject object(JSONObject parent, String key) throws JSONException {
        JSONObject child = parent.optJSONObject(key);
        if (child == null) {
            child = new JSONObject();
            parent.put(key, child);
        }
        return child;
    }

    public static JSONArray array(JSONObject parent, String key) throws JSONException {
        JSONArray child = parent.optJSONArray(key);
        if (child == null) {
            child = new JSONArray();
            parent.put(key, child);
        }
        return child;
    }

    public static double optDoubleFromString(JSONObject object, String key, double fallback) {
        Object value = object.opt(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}

