package com.tools11.today;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnTouch;
import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;
import uk.co.senab.photoview.PhotoView;

import static java.net.URI.*;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener{

    private static final String TAG = "Today";
    private static final int LOAD_IMAGE = 0x03;
    private static final int LOAD_WAITING = 0x04 ;
    @BindView(R.id.photo_view)
    PhotoView mPhotoView;
    @BindView(R.id.days)
    TextView mDays;
    @BindView(R.id.loading)
    GifImageView mGifImageView;
    Context mContext;
    Bitmap todayBitmap;
    GifDrawable gifFromAssets;
    List<Uri> mLsp = new ArrayList<Uri>();


    Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            if(msg.what == LOAD_IMAGE){
                mGifImageView.setVisibility(View.INVISIBLE);
                mPhotoView.setZoomable(true);
                mPhotoView.setImageBitmap(todayBitmap);
            }else if(msg.what == LOAD_WAITING){
                mGifImageView.setVisibility(View.VISIBLE);
                mGifImageView.setImageDrawable(gifFromAssets);
            }
        }
    };


    Runnable bitmaploader = new Runnable() {
        @Override
        public void run() {
            mHandler.sendEmptyMessage(LOAD_WAITING);
            synchronized(mLsp) {
                int allimage = mLsp.size();
                if(allimage!=0) {
                    int random = (int) (Math.random() * allimage);
                    Log.d(TAG, "random = " + random);
                    String randomurl = mLsp.get(random).toString();
                    Log.d(TAG, "random url = " + randomurl);
                    try {
                        if (randomurl != null) {
                            todayBitmap = Glide.with(mContext)
                                    .load(Uri.parse(randomurl))
                                    .asBitmap()
                                    .centerCrop()
                                    .into(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                                    .get();
                        }
                        mHandler.sendEmptyMessage(LOAD_IMAGE);
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    };

    Runnable lsplistloader = new Runnable() {
        @Override
        public void run() {
            getLSPImageList();
        }
    };

    private void getLSPImageList() {
        synchronized(mLsp) {
            try {
                URL url = new URL("https://raw.githubusercontent.com/11tools/lspimage/main/lsp.txt");
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String str;
                while ((str = in.readLine()) != null) {
                    Log.d(TAG, "get line " + str);
                    mLsp.add(Uri.parse(str));
                }
                in.close();
            } catch (IOException e) {
            }
        }
    }

    private void loadResource(){
        try {
            gifFromAssets = new GifDrawable( getAssets(), "loading.gif" );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initUi(){
        mDays.setText(R.string.lsp_show);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mContext = this;
        loadResource();
        initUi();
        Thread lspThread = new Thread(lsplistloader);
        lspThread.start();
        Thread bitmapThread = new Thread(bitmaploader);
        bitmapThread.start();
        mPhotoView.setOnTouchListener((View.OnTouchListener)this);

    }



    private String getRedirectUrl(String path) {
        String url = null;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(path).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(5000);
            url = conn.getHeaderField("Location");
            conn.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(view == mPhotoView){
            Log.d(TAG,"start load next picture");
            Thread bitmapThread = new Thread(bitmaploader);
            bitmapThread.start();
        }
        return false;
    }
}