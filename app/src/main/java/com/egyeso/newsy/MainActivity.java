package com.egyeso.newsy;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.egyeso.newsy.api.ApiClient;
import com.egyeso.newsy.api.ApiInterface;
import com.egyeso.newsy.models.Articles;
import com.egyeso.newsy.models.News;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private static final String Api_KEY = "9d9c4b4a6f2f447fb143be601ea8d50c";
    private RecyclerView recyclerView;
    private List<Articles> articles = new ArrayList<>();
    private Adapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private RelativeLayout errorLayout;
    private ImageView errorImage;
    private TextView errorTitle, errorMessage;
    private Button btnRetry;
    private InterstitialAd mInterstitialAd;
    private ScheduledExecutorService service;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        swipeRefreshLayout = findViewById(R.id.swipe_Refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorAccent);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setNestedScrollingEnabled(false);
        onLoadingSwipeRefresh();

        errorLayout = findViewById(R.id.errorLayout);
        errorImage = findViewById(R.id.errorImage);
        errorTitle = findViewById(R.id.errorTitle);
        errorMessage = findViewById(R.id.errorMessage);
        btnRetry = findViewById(R.id.btnRetry);
        preparAd();
        service = Executors.newSingleThreadScheduledExecutor();
        service.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mInterstitialAd.isLoaded()){
                            mInterstitialAd.show();
                        }else{
                            Toast.makeText(MainActivity.this, "Error", Toast.LENGTH_SHORT).show();
                        }preparAd();
                    }
                });
            }
        },5,5, TimeUnit.SECONDS);
    }

    @Override
    protected void onDestroy() {
        service.shutdown();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        service.shutdown();
        super.onBackPressed();
    }

    @Override
    protected void onPause() {
        service.shutdown();
        super.onPause();
    }

    private void preparAd(){
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-8419407261151146/2733593521");
        mInterstitialAd.loadAd(new AdRequest.Builder().build());
    }

    private void LoadJson(final String keyword) {
        errorLayout.setVisibility(View.GONE);
        swipeRefreshLayout.setRefreshing(true);
        ApiInterface apiInterface = ApiClient.getApiClient().create(ApiInterface.class);
        String language = Utils.getLanguage();

        Call<News> newsCall;

        if (keyword.length() > 0) {
            newsCall = apiInterface.getNewsSearch(keyword, language, "publishedAt", Api_KEY);
        } else {
            newsCall = apiInterface.getNews("eg", Api_KEY);

        }
        newsCall.enqueue(new Callback<News>() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onResponse(@Nullable Call<News> call,@Nullable Response<News> response) {
                if (Objects.requireNonNull(response).isSuccessful() && Objects.requireNonNull(response.body()).getArticles() != null) {

                    if (!articles.isEmpty()) {
                        articles.clear();
                    }
                    articles = response.body().getArticles();
                    adapter = new Adapter(MainActivity.this, articles);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    initListener();
                    swipeRefreshLayout.setRefreshing(false);
                } else {
                    swipeRefreshLayout.setRefreshing(false);

                    String errorCode;
                    switch (response.code()){
                        case 404:
                            errorCode = "404 غير موجود";
                            break;

                        case 500:
                            errorCode = "500 خادم معطل";
                            break;
                        default:
                            errorCode = "خطأ غير معروف";
                            break;
                    }

                    showErrorMessage("لا يوجد نتيجة","حاول مرة اخرى\n" + errorCode);
                }
            }

            @Override
            public void onFailure(@Nullable Call<News> call,@Nullable Throwable t) {
                swipeRefreshLayout.setRefreshing(false);
                showErrorMessage("عفوآ...","\n" +"فشل الشبكة ، يرجى المحاولة مرة أخرى" + Objects.requireNonNull(t).toString());

            }
        });
    }

    private void initListener() {
        adapter.setOnItemClickListener(new Adapter.OnItemClickListener() {
            @SuppressLint("ObsoleteSdkInt")
            @Override
            public void onItemClick(View view, int position) {
                ImageView imageView = view.findViewById(R.id.img);
                Intent intent = new Intent(MainActivity.this, NewsDetailsActivity.class);
                Articles article = articles.get(position);
                intent.putExtra("url", article.getUrl());
                intent.putExtra("title", article.getTitle());
                intent.putExtra("img", article.getUrlToImage());
                intent.putExtra("data", article.getPublishedAt());
                intent.putExtra("source", article.getSource().getName());
                intent.putExtra("author", article.getAuthor());

                Pair<View, String> pair = Pair.create((View) imageView, ViewCompat.getTransitionName(imageView));
                ActivityOptionsCompat optionsCompat = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        MainActivity.this, pair
                );

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    startActivity(intent, optionsCompat.toBundle());
                } else {
                    startActivity(intent);
                }

            }
        });
    }

    @Override
    public void onRefresh() {
        LoadJson("");
    }

    private void onLoadingSwipeRefresh() {
        swipeRefreshLayout.post(
                new Runnable() {
                    @Override
                    public void run() {
                        LoadJson("");
                    }
                }
        );
    }

    private void showErrorMessage(String title, String message) {
        if (errorLayout.getVisibility() == View.GONE) {
            errorLayout.setVisibility(View.VISIBLE);
        }

        errorImage.setImageResource(R.drawable.no_result);
        errorTitle.setText(title);
        errorMessage.setText(message);

        btnRetry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoadingSwipeRefresh();
            }
        });
    }
}
