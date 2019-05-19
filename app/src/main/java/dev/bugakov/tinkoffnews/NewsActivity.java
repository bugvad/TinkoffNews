package dev.bugakov.tinkoffnews;

import android.content.ContentValues;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Scanner;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class NewsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_news);

        TextView textView = findViewById(R.id.textView);


        int value = getIntent().getExtras().getInt("id");

        Observable<String> observableLocal =
                Observable.create(subscriber -> {
                    URL url = new URL("https://api.tinkoff.ru/v1/news_content?id=" + value);
                    new Thread(new Runnable() {
                        public void run() {
                            //код сетевого запроса
                            try {
                                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                try {
                                    InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                    Scanner s = new Scanner(in).useDelimiter("\\A");
                                    String result = s.hasNext() ? s.next() : "";

                                    subscriber.onNext(result);
                                    subscriber.onComplete();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();


                });

        Observer<String> observer =
                new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(String items) {

                        //String.valueOf(Html.fromHtml(item.title))
                        try {
                            JSONObject jsonObj = new JSONObject(items);
                            JSONObject jsonObject2 = jsonObj.getJSONObject("payload");
                            String text = jsonObject2.getString("content");
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    textView.setText(String.valueOf(Html.fromHtml(text)));

                                }
                            });
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                };
        
        observableLocal.subscribe(observer);
    }
}
