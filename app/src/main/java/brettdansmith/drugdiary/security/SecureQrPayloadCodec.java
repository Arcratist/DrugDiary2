package brettdansmith.drugdiary.security;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.Result;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import org.json.JSONObject;

import java.util.EnumMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;

/**
 * App-wide PIN-protected QR codec.
 *
 * The QR payload contains only an encrypted JSON envelope. The sharing PIN is
 * deliberately separate from the profile PIN so exported data can be shared
 * with a temporary code without revealing the user's vault unlock credential.
 */
public final class SecureQrPayloadCodec {
    public static final int VERSION = 1;
    private static final String TYPE = "drugdiary.secure-qr";

    private SecureQrPayloadCodec() {
    }

    public static String encode(String clearJson, String pin) throws Exception {
        SecretKeySpec key = EncryptionManager.deriveKey("secure-qr:" + pin);
        return new JSONObject()
                .put("type", TYPE)
                .put("version", VERSION)
                .put("ciphertext", EncryptionManager.encrypt(clearJson, key))
                .toString();
    }

    public static String decode(String encryptedPayload, String pin) throws Exception {
        JSONObject object = new JSONObject(encryptedPayload);
        if (!TYPE.equals(object.optString("type")) || object.optInt("version") != VERSION) {
            throw new IllegalArgumentException("Unsupported DrugDiary secure QR payload.");
        }
        SecretKeySpec key = EncryptionManager.deriveKey("secure-qr:" + pin);
        return EncryptionManager.decrypt(object.getString("ciphertext"), key);
    }

    public static Bitmap toQrBitmap(String payload, int sizePx) throws Exception {
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);
        Bitmap bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < sizePx; y++) {
            for (int x = 0; x < sizePx; x++) {
                bitmap.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    public static String readQrBitmap(Bitmap bitmap) throws Exception {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        Result result = new MultiFormatReader().decode(new com.google.zxing.BinaryBitmap(
                new HybridBinarizer(new RGBLuminanceSource(width, height, pixels))));
        return result.getText();
    }
}
