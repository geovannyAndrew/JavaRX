package com.gyros.rxjava;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView textView;

    private CompositeDisposable disposables = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);

        Observable<Task> taskObservable = Observable
                .fromIterable(DataSource.Companion.createTaskList())
                .subscribeOn(Schedulers.io())
                .filter(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        Log.d(TAG,"test: "+Thread.currentThread().getName());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposables.add(d);
                Log.d(TAG,"on Subcribe: called.");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG,"on Next: "+Thread.currentThread().getName());
                Log.d(TAG,"on Next: "+task.getDescription());

            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG,"onError: ",e);
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete: called");
            }
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
