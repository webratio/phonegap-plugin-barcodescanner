/**
 * PhoneGap is available under *either* the terms of the modified BSD license *or* the
 * MIT License (2008). See http://opensource.org/licenses/alphabetical for full text.
 *
 * Copyright (c) Matt Kane 2010
 * Copyright (c) 2011, IBM Corporation
 * Copyright (c) 2013, Maciej Nux Jaros
 */
package com.phonegap.plugins.barcodescanner;

import java.lang.Exception;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import java.util.Map;
import java.util.EnumMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;
import com.google.zxing.client.android.Intents;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.EncodeHintType;

import android.graphics.Bitmap;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import android.util.Log;

/**
 * This calls out to the ZXing barcode reader and returns the result.
 *
 * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
 */
public class BarcodeScanner extends CordovaPlugin {
    public static final int REQUEST_CODE = 0x0ba7c0de;
    public static final int ENCODE_REQUEST_CODE = 2000;

    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static final String SCAN = "scan";
    private static final String ENCODE = "encode";
    private static final String CANCELLED = "cancelled";
    private static final String FORMAT = "format";
    private static final String TEXT = "text";
    private static final String DATA = "data";
    private static final String TYPE = "type";
    private static final String SCAN_INTENT = "com.phonegap.plugins.barcodescanner.SCAN";
    private static final String ENCODE_DATA = "ENCODE_DATA";
    private static final String ENCODE_TYPE = "ENCODE_TYPE";
    private static final String ENCODE_INTENT = "com.phonegap.plugins.barcodescanner.ENCODE";
    private static final String TEXT_TYPE = "TEXT_TYPE";
    private static final String EMAIL_TYPE = "EMAIL_TYPE";
    private static final String PHONE_TYPE = "PHONE_TYPE";
    private static final String SMS_TYPE = "SMS_TYPE";

    private static final String LOG_TAG = "BarcodeScanner";

    private CallbackContext callbackContext;

    /**
     * Constructor.
     */
    public BarcodeScanner() {
    }

    /**
     * Executes the request.
     *
     * This method is called from the WebView thread. To do a non-trivial amount of work, use:
     *     cordova.getThreadPool().execute(runnable);
     *
     * To run on the UI thread, use:
     *     cordova.getActivity().runOnUiThread(runnable);
     *
     * @param action          The action to execute.
     * @param args            The exec() arguments.
     * @param callbackContext The callback context used when calling back into JavaScript.
     * @return                Whether the action was valid.
     *
     * @sa https://github.com/apache/cordova-android/blob/master/framework/src/org/apache/cordova/CordovaPlugin.java
     */
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;

        if (action.equals(ENCODE)) {
            JSONObject obj = args.optJSONObject(0);
            if (obj != null) {
                String type = obj.optString(TYPE);
                String data = obj.optString(DATA);
                String format = obj.optString(FORMAT);

                // If the type is null then force the type to text
                if (type == null) {
                    type = TEXT_TYPE;
                }

                if (data == null) {
                    callbackContext.error("User did not specify data to encode");
                    return true;
                }

                encode(type, data, format);
            } else {
                callbackContext.error("User did not specify data to encode");
                return true;
            }
        } else if (action.equals(SCAN)) {
            scan(args);
        } else {
            return false;
        }
        return true;
    }

    /**
     * Starts an intent to scan and decode a barcode.
     */
    public void scan(JSONArray args) {
        Intent intentScan = new Intent(SCAN_INTENT);
        intentScan.addCategory(Intent.CATEGORY_DEFAULT);
        intentScan.putExtra(Intents.Scan.PREFER_FRONTCAMERA, false);
        intentScan.putExtra(Intents.Scan.SHOW_FLIP_CAMERA_BUTTON, false);

        // add config as intent extras
        if(args.length() > 0) {

            JSONObject obj;
            JSONArray names;
            String key;
            Object value;

            for(int i=0; i<args.length(); i++) {

                try {
                    obj = args.getJSONObject(i);
                } catch(JSONException e) {
                    Log.i("CordovaLog", e.getLocalizedMessage());
                    continue;
                }

                names = obj.names();
                for(int j=0; j<names.length(); j++) {
                    try {
                        key = names.getString(j);
                        value = obj.get(key);

                        if(value instanceof Integer) {
                            intentScan.putExtra(key, (Integer)value);
                        } else if(value instanceof String) {
                            intentScan.putExtra(key, (String)value);
                        }

                    } catch(JSONException e) {
                        Log.i("CordovaLog", e.getLocalizedMessage());
                        continue;
                    }
                }
            }

        }

        // avoid calling other phonegap apps
        intentScan.setPackage(this.cordova.getActivity().getApplicationContext().getPackageName());

        this.cordova.startActivityForResult((CordovaPlugin) this, intentScan, REQUEST_CODE);
    }

    /**
     * Initiates a barcode encode.
     *
     * @param type
     *            Encoding type.
     * @param data
     *            The data to encode in the bar code.
     */
    public void encode(String type, String data, String format) {
        try {
            String contentsToEncode = data;
            Map<EncodeHintType, Object> hints = null;
            String encoding = guessAppropriateEncoding(contentsToEncode);
            if (encoding != null) {
                hints = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
                hints.put(EncodeHintType.CHARACTER_SET, encoding);
            }
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix result;
            try {
                BarcodeFormat barcodeFormat = BarcodeFormat.valueOf(format);
                int width = 177; // default QR maximum width
                int height = 177; // default QR maximum height
                if (BarcodeFormat.CODE_128 == barcodeFormat) {
                    height /= 2;
                }
                result = writer.encode(contentsToEncode, BarcodeFormat.valueOf(format), width, height, hints);
            } catch (IllegalArgumentException iae) {
                throw iae;
            }
            int width = result.getWidth();
            int height = result.getHeight();
            int[] pixels = new int[width * height];
            for (int y = 0; y < height; y++) {
                int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            File cache = null;

            // SD Card Mounted
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                cache = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/"
                        + cordova.getActivity().getPackageName() + "/cache/");
            } else {

                // Use internal storage
                cache = this.cordova.getActivity().getCacheDir();
            }

            // Create the cache directory if it doesn't exist
            cache.mkdirs();
            File outputFile = File.createTempFile("barcode", ".png", cache);
            outputFile.deleteOnExit();
            FileOutputStream out = null;
            try {
                out = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            JSONObject obj = new JSONObject();
            obj.put("file", outputFile.getAbsolutePath());
            this.callbackContext.success(obj);
        } catch (Exception e) {
            this.callbackContext.error(e.getLocalizedMessage());
        }
    }

    /**
     * Called when the barcode scanner intent completes.
     *
     * @param requestCode The request code originally supplied to startActivityForResult(),
     *                       allowing you to identify who this result came from.
     * @param resultCode  The integer result code returned by the child activity through its setResult().
     * @param intent      An Intent, which can return result data to the caller (various data can be attached to Intent "extras").
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, intent.getStringExtra("SCAN_RESULT"));
                    obj.put(FORMAT, intent.getStringExtra("SCAN_RESULT_FORMAT"));
                    obj.put(CANCELLED, false);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put(TEXT, "");
                    obj.put(FORMAT, "");
                    obj.put(CANCELLED, true);
                } catch (JSONException e) {
                    Log.d(LOG_TAG, "This should never happen");
                }
                //this.success(new PluginResult(PluginResult.Status.OK, obj), this.callback);
                this.callbackContext.success(obj);
            } else {
                //this.error(new PluginResult(PluginResult.Status.ERROR), this.callback);
                this.callbackContext.error("Unexpected error");
            }
        } else if (requestCode == ENCODE_REQUEST_CODE) {
            this.callbackContext.success();
        }
    }

    /**
     * Initiates a barcode encode.
     *
     * @param type Endoiding type.
     * @param data The data to encode in the bar code.
     */
    public void encode(String type, String data) {
        Intent intentEncode = new Intent(ENCODE_INTENT);
        intentEncode.putExtra(ENCODE_TYPE, type);
        intentEncode.putExtra(ENCODE_DATA, data);
        // avoid calling other phonegap apps
        intentEncode.setPackage(this.cordova.getActivity().getApplicationContext().getPackageName());

        this.cordova.getActivity().startActivity(intentEncode);
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }
}
