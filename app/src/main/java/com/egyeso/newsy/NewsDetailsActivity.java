package com.egyeso.newsy;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.util.Objects;

public class NewsDetailsActivity extends AppCompatActivity implements AppBarLayout.OnOffsetChangedListener {

    private boolean isHideTolbarView = false;
    private FrameLayout data_behavior;
    private LinearLayout titleAppbar;
    private String mUrl;
    private String mTitle;
    private String mSource;

    @SuppressLint({"CheckResult", "SetTextI18n"})
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news_details);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final CollapsingToolbarLayout collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        collapsingToolbarLayout.setTitle("");

        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        appBarLayout.addOnOffsetChangedListener(this);
        data_behavior = findViewById(R.id.date_behavior);
        titleAppbar = findViewById(R.id.title_appbar);
        ImageView imageView = findViewById(R.id.backdrop);
        TextView appbar_title = findViewById(R.id.title_on_appbar);
        TextView appbar_subtitle = findViewById(R.id.subtitle_on_appbar);
        TextView data = findViewById(R.id.date);
        TextView time = findViewById(R.id.time);
        TextView title = findViewById(R.id.title);

        Intent intent = getIntent();
        mUrl = intent.getStringExtra("url");
        mTitle = intent.getStringExtra("title");
        String mImg = intent.getStringExtra("img");
        String mData = intent.getStringExtra("data");
        mSource = intent.getStringExtra("source");
        String mAuthor = intent.getStringExtra("author");

        RequestOptions requestOptions = new RequestOptions();
        requestOptions.error(Utils.getRandomDrawbleColor());
        Glide.with(this).load(mImg).apply(requestOptions).transition(DrawableTransitionOptions.withCrossFade()).into(imageView);

        appbar_title.setText(mSource);
        appbar_subtitle.setText(mUrl);
        data.setText(Utils.DateFormat(mData));
        title.setText(mTitle);

        mAuthor = " \u2022 " + mAuthor;

        time.setText(mSource + mAuthor + " \u2022 " + Utils.DateToTimeFormat(mData));
        initWebView(mUrl);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebView(String url){
        WebView webView = findViewById(R.id.webView);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportZoom(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        supportFinishAfterTransition();
    }

    @Override
    public void onOffsetChanged(AppBarLayout appBarLayout, int i) {
        int maxScroll = appBarLayout.getTotalScrollRange();
        float percentage = (float) Math.abs(i)/(float) maxScroll;
        //noinspection DuplicateCondition
        if (percentage == 1f && isHideTolbarView){
            data_behavior.setVisibility(View.GONE);
            titleAppbar.setVisibility(View.VISIBLE);
            isHideTolbarView = !isHideTolbarView;
        }

        else //noinspection DuplicateCondition
            if (percentage == 1f && isHideTolbarView){
            data_behavior.setVisibility(View.VISIBLE);
            titleAppbar.setVisibility(View.VISIBLE);
            isHideTolbarView = !isHideTolbarView;
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_news,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.view_web){
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(mUrl));
            startActivity(intent);
            return true;
        }else if (id == R.id.share){
            try {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plan");
                i.putExtra(Intent.EXTRA_SUBJECT, mSource);
                String body = mTitle + "\n" + mUrl + "\n" + "Share from the News App" + "\n";
                i.putExtra(Intent.EXTRA_TEXT,body);
                startActivity(Intent.createChooser(i,"Share with :"));
            }catch (Exception e){
                Toast.makeText(this, "Hmm.. Sorry. \nCannot be share", Toast.LENGTH_SHORT).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
