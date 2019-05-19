package dev.bugakov.tinkoffnews;

import android.content.ClipData;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.reactivestreams.Subscription;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private SwipeRefreshLayout mSwipeRefreshLayout;

    public static boolean isNetworkAvailable(final Context context) {

        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();

        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DBHelper dbHelper = new DBHelper(MainActivity.this);
        final SQLiteDatabase db = dbHelper.getWritableDatabase();

        final RecyclerView recyclerView = findViewById(R.id.list);

        //разделитель
        DividerItemDecoration itemDecorator = new DividerItemDecoration(MainActivity.this, DividerItemDecoration.VERTICAL);
        itemDecorator.setDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.divider));
        recyclerView.addItemDecoration(itemDecorator);

        try {
            getList(recyclerView, db);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //swipe-to-refresh
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {
                try {
                    getList(recyclerView, db);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mSwipeRefreshLayout.setRefreshing(false);
            }
        });
    }


    //проверки и получение
    public void getList(RecyclerView recyclerView, SQLiteDatabase db) throws IOException {

        //проверка на наличие сети
        if (isNetworkAvailable(getApplicationContext())) {

            ConnectivityManager manager = (ConnectivityManager)
                    getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

            NetworkInfo info = manager.getActiveNetworkInfo();

            //проверка на скорость соединения
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            } else if (info.getType() == ConnectivityManager.TYPE_MOBILE &&
                    info.getSubtype() == TelephonyManager.NETWORK_TYPE_GPRS) {

                Toast toast = Toast.makeText(getApplicationContext(),
                        "Слабое подключение", Toast.LENGTH_SHORT);
                toast.show();

            }

            Observable<String> observableLocal =
                    Observable.create(subscriber -> {
                        URL url = new URL("https://api.tinkoff.ru/v1/news");
                        new Thread(new Runnable() {
                            public void run() {
                                //код сетевого запроса
                                try {
                                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                                    try {
                                        InputStream in = new BufferedInputStream(urlConnection.getInputStream());
                                        Scanner s = new Scanner(in).useDelimiter("\\A");
                                        String result = s.hasNext() ? s.next() : "";
                                        //longInfo(result);

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

                            ArrayList<Item> mCatNames = new ArrayList<>();
                            ArrayList<Item> item = new ArrayList<>();

                            try {
                                JSONObject jsonObj = new JSONObject(items);
                                JSONArray jsonObject2 = jsonObj.getJSONArray("payload");
                                for (int i = 0; i < jsonObject2.length(); i++) {
                                    mCatNames.add(new Item(jsonObject2.getJSONObject(i).getInt("id"),
                                            jsonObject2.getJSONObject(i).getString("text"),
                                            jsonObject2.getJSONObject(i).getJSONObject("publicationDate").getLong("milliseconds")));
                                }
                                quicksort(mCatNames);
                                Collections.reverse(mCatNames);

                                db.delete("myTable", null, null);

                                DataAdapter adapterMain = new DataAdapter(MainActivity.this, item);

                                //кеширование
                                for (Item list_item: mCatNames) {
                                    ContentValues cv = new ContentValues();
                                    String mBuf = String.valueOf(Html.fromHtml(list_item.text));
                                    Integer mId = list_item.id;
                                    long time = list_item.milliseconds;
                                    item.add(new Item(mId, mBuf, time));
                                    cv.put("title", mBuf);
                                    cv.put("idm", mId);
                                    cv.put("milliseconds", time);
                                    db.insert("myTable", null, cv);
                                }

                                adapterMain.notifyDataSetChanged();

                                runOnUiThread(new Runnable() {

                                    @Override
                                    public void run() {
                                        recyclerView.setAdapter(adapterMain);
                                        // Stuff that updates the UI

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
        else
        {
            Toast toast = Toast.makeText(getApplicationContext(),
                    "Нет сети, отображены сохраненные данные", Toast.LENGTH_SHORT);
            toast.show();
            List<Item> item = new ArrayList<>();

            //чтение кешированных данных
            Cursor c = db.query("myTable", null, null, null, null, null, null);

            if (c.moveToFirst()) {

                int idColIndex = c.getColumnIndex("idm");
                int textColIndex = c.getColumnIndex("text");
                int timeColIndex = c.getColumnIndex("milliseconds");

                do {
                    item.add(new Item(c.getInt(idColIndex), c.getString(textColIndex), c.getLong(timeColIndex)));
                } while (c.moveToNext());
            }
            DataAdapter adapterMain = new DataAdapter(MainActivity.this, item);
            adapterMain.notifyDataSetChanged();
            recyclerView.setAdapter(adapterMain);
        }
    }


    // быстрая сортировка
    public static void quicksort(ArrayList<Item> main) {
        quicksort(main, 0, main.size() - 1);
    }

    public static void quicksort(ArrayList<Item> main, int left, int right) {
        if (right <= left) return;
        int i = partition(main, left, right);
        quicksort(main, left, i-1);
        quicksort(main, i+1, right);
    }

    private static int partition(ArrayList<Item>  a, int left, int right) {
        int i = left - 1;
        int j = right;
        while (true) {
            while (less(a.get(++i).milliseconds, a.get(right).milliseconds))
                ;
            while (less(a.get(right).milliseconds, a.get(--j).milliseconds))
                if (j == left) break;
            if (i >= j) break;
            exch(a, i, j);
        }
        exch(a, i, right);
        return i;
    }

    private static boolean less(long x, long y) {
        return (x < y);
    }

    private static void exch(ArrayList<Item>  a, int i, int j) {
        Item swap = a.get(i);
        a.set(i, a.get(j));
        a.set(j, swap);
    }
}