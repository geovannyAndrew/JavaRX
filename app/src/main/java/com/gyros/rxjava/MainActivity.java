package com.gyros.rxjava;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import com.gyros.rxjava.models.Comment;
import com.gyros.rxjava.models.Post;
import com.gyros.rxjava.requests.ServiceGenerator;
import com.jakewharton.rxbinding3.view.RxView;

import org.reactivestreams.Subscription;

import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;
import retrofit2.http.POST;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView textView;

    private CompositeDisposable disposables = new CompositeDisposable();
    private RecyclerView recyclerView;
    private RecyclerAdapter adapter;
    private SearchView searchView;
    private long timeSinceLastRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recycler_view);
        searchView = findViewById(R.id.search_view);
        initRecyclerView();
        //testFilter();
        //testCreateOperator();
        //testTake();
        //testTakeWhile();
        //testMapOperator();
        //testBufferOperatorSimple();
        //testBufferOperatorUI();
        testDebounceOperatorUI();
    }

    //Prevent click spamming
    private void testThrottleFirstOperatorUI(){
        Button button = findViewById(R.id.buttonClickable);
        RxView.clicks(button)
                .throttleFirst(2000, TimeUnit.MILLISECONDS) // Throttle the clicks so 500 ms must pass before registering a new click
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Unit>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposables.add(d);
                    }
                    @Override
                    public void onNext(Unit unit) {
                        Log.d(TAG, "onNext: time since last clicked: " + (System.currentTimeMillis() - timeSinceLastRequest));
                        someMethod(); // Execute some method when a click is registered
                    }
                    @Override
                    public void onError(Throwable e) {
                    }
                    @Override
                    public void onComplete() {
                    }
                });
    }

    private void someMethod(){

    }

    private void testDebounceOperatorUI(){
        timeSinceLastRequest = System.currentTimeMillis();

        // create the Observable
        Observable<String> observableQueryText = Observable
                .create(new ObservableOnSubscribe<String>() {
                    @Override
                    public void subscribe(final ObservableEmitter<String> emitter) throws Exception {

                        // Listen for text input into the SearchView
                        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                return false;
                            }

                            @Override
                            public boolean onQueryTextChange(final String newText) {
                                if(!emitter.isDisposed()){
                                    emitter.onNext(newText); // Pass the query to the emitter
                                }
                                return false;
                            }
                        });
                    }
                })
                .debounce(500, TimeUnit.MILLISECONDS) // Apply Debounce() operator to limit requests
                .subscribeOn(Schedulers.io());

        // Subscribe an Observer
        observableQueryText.subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposables.add(d);
            }
            @Override
            public void onNext(String s) {
                Log.d(TAG, "onNext: time  since last request: " + (System.currentTimeMillis() - timeSinceLastRequest));
                Log.d(TAG, "onNext: search query: " + s);
                timeSinceLastRequest = System.currentTimeMillis();

                // method for sending a request to the server
                sendRequestToServer(s);
            }
            @Override
            public void onError(Throwable e) {
            }
            @Override
            public void onComplete() {
            }
        });
    }

    // Fake method for sending a request to the server
    private void sendRequestToServer(String query){
        // do nothing
    }

    private void testBufferOperatorUI(){
        // global disposables object
        final CompositeDisposable disposables = new CompositeDisposable();

// detect clicks to a button
        RxView.clicks(findViewById(R.id.buttonClickable))
                .map(new Function<Unit, Integer>() { // convert the detected clicks to an integer
                    @Override
                    public Integer apply(Unit unit) throws Exception {
                        return 1;
                    }
                })
                .buffer(4, TimeUnit.SECONDS) // capture all the clicks during a 4 second interval
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Integer>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposables.add(d); // add to disposables to you can clear in onDestroy
                    }
                    @Override
                    public void onNext(List<Integer> integers) {
                        Log.d(TAG, "onNext: You clicked " + integers.size() + " times in 4 seconds!");
                    }
                    @Override
                    public void onError(Throwable e) {

                    }
                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void testBufferOperatorSimple(){
        Observable<List<Task>> tasksObservable = Observable.fromIterable(DataSource.Companion.createTaskList())
                .subscribeOn(Schedulers.io())
                .buffer(2)
                .observeOn(AndroidSchedulers.mainThread());
        tasksObservable.subscribe(new Observer<List<Task>>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(List<Task> tasks) {
                Log.d(TAG,"onNext: "+tasks.size());
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testMapOperator(){
        Observable<String> tasksObservable = Observable.fromIterable(DataSource.Companion.createTaskList())
                .subscribeOn(Schedulers.computation())
                .map(new Function<Task, String>() {
                    @Override
                    public String apply(Task task) throws Exception {
                        return task.getDescription();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
        tasksObservable.subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(String task) {
                Log.d(TAG,"onNext: "+task);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testTakeWhile(){
        Observable<Task> tasksObservable = Observable.fromIterable(DataSource.Companion.createTaskList())
                .subscribeOn(Schedulers.computation())
                .takeWhile(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
        tasksObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG,"onNext: "+task.toString());
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testTake(){
        Observable<Task> tasksObservable = Observable.fromIterable(DataSource.Companion.createTaskList())
                .subscribeOn(Schedulers.computation())
                .take(2)
                .observeOn(AndroidSchedulers.mainThread());
        tasksObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG,"onNext: "+task.toString());
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testFlowable(){
        Flowable.range(0, 1000000)
                .onBackpressureBuffer()
                .observeOn(Schedulers.computation())
                .subscribe(new FlowableSubscriber<Integer>() {
                    @Override
                    public void onSubscribe(Subscription s) {

                    }
                    @Override
                    public void onNext(Integer integer) {
                        Log.d(TAG, "onNext: " + integer);
                    }
                    @Override
                    public void onError(Throwable t) {
                        Log.e(TAG, "onError: ", t);
                    }
                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void testFilter(){
        Observable<Task> tasksObservable = Observable.fromIterable(DataSource.Companion.createTaskList())
                .subscribeOn(Schedulers.computation())
                .filter(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        Thread.sleep(1000);
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
        tasksObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG,"onNext: "+task.toString());
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testCreateOperator(){
        final Task task = DataSource.Companion.createTaskList().get(0);

        Observable<Task> taskObservable = Observable.create(new ObservableOnSubscribe<Task>() {
            @Override
            public void subscribe(ObservableEmitter<Task> emitter) throws Exception {
                if(!emitter.isDisposed()){
                    emitter.onNext(task);
                    emitter.onComplete();
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG,"onNext: "+task.toString());
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testCreateOperatorList(){
        final List<Task> tasks = DataSource.Companion.createTaskList();

        Observable<Task> taskObservable = Observable.create(new ObservableOnSubscribe<Task>() {
            @Override
            public void subscribe(ObservableEmitter<Task> emitter) throws Exception {
                for (Task task:tasks){
                    if(!emitter.isDisposed()){
                        emitter.onNext(task);
                    }
                }
                if(!emitter.isDisposed()){
                    emitter.onComplete();
                }

            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG,"onNext: "+task.toString());
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testRange(){
        //Print from 0 to 8 because it's not inclusivo
        Observable<Integer> observable = Observable.range(0,9)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
        observable.subscribe(new Observer<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG,"onSubscribe");
            }

            @Override
            public void onNext(Integer integer) {
                Log.d(TAG,"onNext: "+integer);
            }

            @Override
            public void onError(Throwable e) {
                Log.d(TAG,"onError:");
            }

            @Override
            public void onComplete() {
                Log.d(TAG,"onComplete:");
            }
        });
    }

    private void testFlatMap(){
        getPostsObservable()
                .subscribeOn(Schedulers.io())
                //Execute all post at the same time
                .flatMap(new Function<Post, ObservableSource<Post>>() {
                    @Override
                    public ObservableSource<Post> apply(Post post) throws Exception {
                        //Thread.sleep(200);
                        return getCommentsObservable(post);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Post>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposables.add(d);
                    }

                    @Override
                    public void onNext(Post post) {
                        //Log.d(TAG,"onNext"+post.getBody());
                        adapter.updatePost(post);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"onError:",e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    /**
     * Same as flat map operator the online diference is order list is ordered and syncronized, it's to say is processing one post at the time
     */
    private void testConcatMap(){
        getPostsObservable()
                .subscribeOn(Schedulers.io())
                //Execute one post at the time
                .concatMap(new Function<Post, ObservableSource<Post>>() {
                    @Override
                    public ObservableSource<Post> apply(Post post) throws Exception {
                        //Thread.sleep(200);
                        return getCommentsObservable(post);
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Post>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposables.add(d);
                    }

                    @Override
                    public void onNext(Post post) {
                        //Log.d(TAG,"onNext"+post.getBody());
                        adapter.updatePost(post);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"onError:",e);
                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    private void testSimpleSubcribe(){
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

    private void initRecyclerView(){
        adapter = new RecyclerAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private Observable<Post> getCommentsObservable(final Post post){
        return ServiceGenerator.getRequestApi()
                .getComments(post.getId())
                .map(new Function<List<Comment>, Post>() {
                    @Override
                    public Post apply(List<Comment> comments) throws Exception {
                        int delay = ((new Random()).nextInt(5) + 1) * 1000; // sleep thread for x ms
                        Thread.sleep(delay);
                        Log.d(TAG, "apply: sleeping thread " + Thread.currentThread().getName() + " for " + String.valueOf(delay)+ "ms");

                        post.setComments(comments);
                         return post;
                    }
                }).subscribeOn(Schedulers.io());
    }


    private Observable<Post> getPostsObservable(){
        return ServiceGenerator.getRequestApi()
                .getPosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<List<Post>, ObservableSource<Post>>() {
                    @Override
                    public ObservableSource<Post> apply(List<Post> posts) throws Exception {
                        adapter.setPosts(posts);
                        return Observable.fromIterable(posts)
                                .subscribeOn(Schedulers.io());

                    }
                });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }
}
