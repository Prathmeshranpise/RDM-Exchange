package com.rdm.rdmexchange;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    ValueCallback<Uri[]> f_string;
    String cam_path;

    ActivityResultLauncher<Intent> myARL = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            Uri[] results = null;
            if(result.getResultCode() == Activity.RESULT_CANCELED){
                f_string.onReceiveValue(null);
                return;
            }

            if(result.getResultCode() == Activity.RESULT_OK) {
                if(null == f_string){
                    return;
                }
                ClipData clipData;
                String stringData;

                try {
                    clipData = result.getData().getClipData();
                    stringData = result.getData().getDataString();
                }catch (Exception e){
                    clipData = null;
                    stringData = null;
                }

                if(clipData == null && stringData == null && cam_path != null){
                    results = new Uri[]{Uri.parse(cam_path)};
                }else {
                    if(null != clipData){
                        final int numSelectedFiles = clipData.getItemCount();
                        results = new Uri[numSelectedFiles];
                        for(int i = 0; i < clipData.getItemCount(); i++){
                            results[i] = clipData.getItemAt(i).getUri();
                        }
                    }else{
                        try {
                            assert result.getData() != null;
                            Bitmap cam_photo = (Bitmap) result.getData().getExtras().get("data");
                            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                            cam_photo.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                            stringData = MediaStore.Images.Media.insertImage(getContentResolver(), cam_photo, "a", "a");
                        }catch (Exception ignored){}
                        results = new Uri[]{Uri.parse(stringData)};
                    }
                }
            }
            f_string.onReceiveValue(results);
            f_string = null;
        }
    });

    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        webView= (WebView) findViewById(R.id.webview);
        WebSettings mWebSettings = webView.getSettings();
        mWebSettings.setJavaScriptEnabled(true);
        mWebSettings.setAllowFileAccess(true);
        mWebSettings.setAllowContentAccess(true);
        mWebSettings.setAllowFileAccessFromFileURLs(true);
        mWebSettings.setAllowUniversalAccessFromFileURLs(true);
        mWebSettings.setUseWideViewPort(false);
        mWebSettings.setDomStorageEnabled(true);

        webView.setWebViewClient(new WebViewClient(){
            private WebView view;

            public boolean shuldOverrideUrlLoading(WebView view, String url){
                view.loadUrl(url);
                return true;
            }
        });

        webView.loadUrl("https://Ico.rdmexchange.com");
        webView.setWebChromeClient(new WebChromeClient(){
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filepathCallback, FileChooserParams fileChooserParams ){
                f_string = filepathCallback;

                Intent takePictureIntent = null;
                takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if(takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null){
                    File photoFile = null;

                    try {
                        photoFile = create_image();
                        takePictureIntent.putExtra("Photo path", cam_path);
                    }catch (IOException ex){
                        Log.e(TAG, "Image file creation fail", ex);
                    }

                    if(photoFile != null){
                        cam_path = "file:" + photoFile.getAbsolutePath();
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                    }else{
                        takePictureIntent = null;
                    }
                }
                Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
                contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                contentSelectionIntent.setType("*/*");
                Intent[] intentArray;
                intentArray = new Intent[]{takePictureIntent};

                Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
                chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
                myARL.launch(chooserIntent);
                return true;
            }
        });
    }

    private File create_image() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name = "file_" + file_name + "_";
        File sd_directory = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".jpg", sd_directory);
    }
}
