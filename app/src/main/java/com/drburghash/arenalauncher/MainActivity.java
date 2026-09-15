package com.drburghash.bmsblank;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int REQ_FILE_CHOOSER = 1001;
    private static final int REQ_SAVE_TEXT = 1002;
    private static final int REQ_PICK_APP = 1003;
    private static final int REQ_PICK_LOCAL = 1004;

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingText;
    private String pendingFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF0B1020);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        webView.addJavascriptInterface(new AndroidBridge(), "Android");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && url.startsWith("file:///android_asset/")) injectNativeTools();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    Toast.makeText(MainActivity.this, "لا يوجد تطبيق لاختيار الملفات", Toast.LENGTH_SHORT).show();
                    return false;
                }
                return true;
            }
        });

        if (android.os.Build.VERSION.SDK_INT >= 23) {
            getWindow().setStatusBarColor(0xFF0B1020);
            getWindow().setNavigationBarColor(0xFF0B1020);
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        webView.loadUrl("file:///android_asset/index.html");
    }

    private boolean handleUrl(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        String host = uri.getHost();
        if ("file".equalsIgnoreCase(scheme)) return false;
        if ("bms-native.local".equalsIgnoreCase(host)) {
            launchComponentInternal(uri.getQueryParameter("c"));
            return true;
        }
        if ("bms-local.local".equalsIgnoreCase(host)) {
            openLocalInternal(uri.getQueryParameter("u"));
            return true;
        }
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception e) {
                Toast.makeText(this, "تعذر فتح الرابط", Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return false;
    }

    private void injectNativeTools() {
        String js = "(function(){" +
                "if(document.getElementById('bmsNativeTools'))return;" +
                "var u=document.getElementById('appUrl');if(!u)return;" +
                "var f=u.closest('.field');" +
                "var d=document.createElement('div');d.id='bmsNativeTools';d.style.margin='12px 0';" +
                "d.innerHTML='<div style=\"font-size:13px;color:#b9c2d6;margin-bottom:8px\">التطبيق ليس له رابط؟</div>'+" +
                "'<div style=\"display:flex;gap:8px\"><button type=\"button\" id=\"bmsPickNative\" style=\"flex:1;min-height:48px;border-radius:14px;border:1px solid #4da3ff66;background:#152746;color:white\">📱 تطبيق مثبت</button><button type=\"button\" id=\"bmsPickLocal\" style=\"flex:1;min-height:48px;border-radius:14px;border:1px solid #4da3ff66;background:#152746;color:white\">📄 ملف HTML</button></div>'+" +
                "'<div id=\"bmsChoiceNote\" style=\"font-size:12px;color:#9fb1cf;margin-top:8px;line-height:1.6\">للروابط العادية اكتب http أو https. رابط blob:null مؤقت ولا يصلح للحفظ.</div>';" +
                "f.parentNode.insertBefore(d,f.nextSibling);" +
                "document.getElementById('bmsPickNative').onclick=function(){Android.pickInstalledApp()};" +
                "document.getElementById('bmsPickLocal').onclick=function(){Android.pickLocalHtml()};" +
                "window.BMSSetInstalled=function(n,c,i){var name=document.getElementById('appName');if(name&&!name.value.trim())name.value=n||'';u.value='https://bms-native.local/?c='+encodeURIComponent(c||'');u.dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('bmsChoiceNote').textContent='تم اختيار تطبيق مثبت: '+(n||'');};" +
                "window.BMSSetLocal=function(n,r){var name=document.getElementById('appName');if(name&&!name.value.trim())name.value=(n||'').replace(/\\.html?$/i,'');u.value='https://bms-local.local/?u='+encodeURIComponent(r||'');u.dispatchEvent(new Event('input',{bubbles:true}));document.getElementById('bmsChoiceNote').textContent='تم اختيار ملف محلي: '+(n||'');};" +
                "var form=document.getElementById('appForm');if(form)form.addEventListener('submit',function(e){if(/^blob:/i.test((u.value||'').trim())){e.preventDefault();e.stopImmediatePropagation();alert('رابط blob:null مؤقت ولا يعمل بعد إغلاق الصفحة. اختر تطبيق مثبت أو ملف HTML محلي.');}},true);" +
                "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_FILE_CHOOSER) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getData() != null) results = new Uri[]{data.getData()};
                else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) results[i] = data.getClipData().getItemAt(i).getUri();
                }
            }
            if (fileCallback != null) { fileCallback.onReceiveValue(results); fileCallback = null; }
            return;
        }

        if (requestCode == REQ_SAVE_TEXT) {
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingText != null) {
                try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    if (os != null) { os.write(pendingText.getBytes(StandardCharsets.UTF_8)); os.flush(); Toast.makeText(this, "تم حفظ النسخة الاحتياطية", Toast.LENGTH_SHORT).show(); }
                } catch (Exception e) { Toast.makeText(this, "تعذر حفظ النسخة الاحتياطية", Toast.LENGTH_SHORT).show(); }
            }
            pendingText = null; pendingFileName = null; return;
        }

        if (requestCode == REQ_PICK_APP) {
            if (resultCode == RESULT_OK && data != null) {
                ComponentName component = data.getComponent();
                if (component == null && data.getSelector() != null) component = data.getSelector().getComponent();
                if (component != null) sendPickedApp(component); else Toast.makeText(this, "تعذر تحديد التطبيق", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        if (requestCode == REQ_PICK_LOCAL && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                getContentResolver().takePersistableUriPermission(uri, flags);
            } catch (Exception ignored) {}
            String js = "window.BMSSetLocal&&window.BMSSetLocal(" + q(getDisplayName(uri)) + "," + q(uri.toString()) + ");";
            webView.evaluateJavascript(js, null);
        }
    }

    private String getDisplayName(Uri uri) {
        String result = "ملف HTML";
        try (android.database.Cursor c = getContentResolver().query(uri, null, null, null, null)) {
            if (c != null && c.moveToFirst()) { int i = c.getColumnIndex(OpenableColumns.DISPLAY_NAME); if (i >= 0) result = c.getString(i); }
        } catch (Exception ignored) {}
        return result;
    }

    private void sendPickedApp(ComponentName component) {
        try {
            ActivityInfo info = getPackageManager().getActivityInfo(component, 0);
            String label = String.valueOf(info.loadLabel(getPackageManager()));
            String iconData = drawableToDataUrl(info.loadIcon(getPackageManager()));
            String js = "window.BMSSetInstalled&&window.BMSSetInstalled(" + q(label) + "," + q(component.flattenToString()) + "," + q(iconData) + ");";
            webView.evaluateJavascript(js, null);
        } catch (Exception e) { Toast.makeText(this, "تعذر قراءة بيانات التطبيق", Toast.LENGTH_SHORT).show(); }
    }

    private String drawableToDataUrl(Drawable drawable) {
        try {
            Bitmap bitmap;
            if (drawable instanceof BitmapDrawable) bitmap = ((BitmapDrawable) drawable).getBitmap();
            else {
                int w = Math.max(1, drawable.getIntrinsicWidth()), h = Math.max(1, drawable.getIntrinsicHeight());
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap); drawable.setBounds(0,0,canvas.getWidth(),canvas.getHeight()); drawable.draw(canvas);
            }
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap,192,192,true);
            ByteArrayOutputStream out = new ByteArrayOutputStream(); scaled.compress(Bitmap.CompressFormat.PNG,90,out);
            return "data:image/png;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP);
        } catch (Exception e) { return ""; }
    }

    private String q(String v) { return JSONObject.quote(v == null ? "" : v); }

    private void launchComponentInternal(String flat) {
        try {
            ComponentName c = ComponentName.unflattenFromString(flat);
            if (c == null) throw new Exception();
            Intent i = new Intent(Intent.ACTION_MAIN); i.addCategory(Intent.CATEGORY_LAUNCHER); i.setComponent(c); i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED); startActivity(i);
        } catch (Exception e) { Toast.makeText(this, "التطبيق غير متاح أو تم حذفه", Toast.LENGTH_SHORT).show(); }
    }

    private void openLocalInternal(String uriString) {
        try {
            Uri uri = Uri.parse(uriString); Intent i = new Intent(Intent.ACTION_VIEW); i.setDataAndType(uri,"text/html"); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(i);
        } catch (Exception e) {
            try { webView.loadUrl(uriString); } catch (Exception ignored) { Toast.makeText(this, "تعذر فتح الملف المحلي", Toast.LENGTH_SHORT).show(); }
        }
    }

    public class AndroidBridge {
        @JavascriptInterface public void saveText(String fileName, String content) {
            runOnUiThread(() -> {
                pendingFileName = (fileName == null || fileName.trim().isEmpty()) ? "BMS_Backup.json" : fileName; pendingText = content == null ? "" : content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.addCategory(Intent.CATEGORY_OPENABLE); intent.setType("application/json"); intent.putExtra(Intent.EXTRA_TITLE,pendingFileName);
                try { startActivityForResult(intent,REQ_SAVE_TEXT); } catch (ActivityNotFoundException e) { pendingText=null; pendingFileName=null; Toast.makeText(MainActivity.this,"تعذر فتح نافذة الحفظ",Toast.LENGTH_SHORT).show(); }
            });
        }
        @JavascriptInterface public void pickInstalledApp() {
            runOnUiThread(() -> { Intent base=new Intent(Intent.ACTION_MAIN,null);base.addCategory(Intent.CATEGORY_LAUNCHER);Intent picker=new Intent(Intent.ACTION_PICK_ACTIVITY);picker.putExtra(Intent.EXTRA_INTENT,base);picker.putExtra(Intent.EXTRA_TITLE,"اختر تطبيقًا");try{startActivityForResult(picker,REQ_PICK_APP);}catch(Exception e){Toast.makeText(MainActivity.this,"تعذر فتح قائمة التطبيقات",Toast.LENGTH_SHORT).show();} });
        }
        @JavascriptInterface public void pickLocalHtml() {
            runOnUiThread(() -> { Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/html");i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(i,REQ_PICK_LOCAL);}catch(Exception e){Toast.makeText(MainActivity.this,"تعذر فتح اختيار الملفات",Toast.LENGTH_SHORT).show();} });
        }
        @JavascriptInterface public void launchComponent(String flat) { runOnUiThread(() -> launchComponentInternal(flat)); }
        @JavascriptInterface public void openWeb(String url) { runOnUiThread(() -> handleUrl(Uri.parse(url))); }
        @JavascriptInterface public void openLocal(String uri) { runOnUiThread(() -> openLocalInternal(uri)); }
    }
}
