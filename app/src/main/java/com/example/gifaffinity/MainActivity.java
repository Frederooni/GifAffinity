package com.example.gifaffinity;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.SearchView;

import com.example.gifaffinity.MyGiphyAdapter.Gif;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "GifAffinity.Main";

    private RecyclerView recyclerView;
    private MyGiphyAdapter.GifDataSourceFactory factory;
    private MyGiphyAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Make mysterious crashes less mysterious
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                Log.e(TAG, "Uncaught exception in thread " + t, e);
                System.exit(1);
            }
        });
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_layout);
        recyclerView = findViewById(R.id.gif_grid);

        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        adapter = new MyGiphyAdapter(this);
        PagedList.Config config = new PagedList.Config.Builder().setPageSize(75).build();
        factory = new MyGiphyAdapter.GifDataSourceFactory(this);
        LiveData<PagedList<Gif>> gifs = new LivePagedListBuilder<Integer, Gif>(factory, config).build();
        gifs.observe(this, new Observer<PagedList<Gif>>() {
            @Override
            public void onChanged(@Nullable PagedList<Gif> gifs) {
                adapter.submitList(gifs);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_rating_g);
        item.setChecked(true);
        item = menu.findItem(R.id.action_search);
        SearchView v = (SearchView) item.getActionView();
        v.setQueryHint("Search...");
        v.setIconifiedByDefault(false);
        v.setIconified(false);
        v.setFocusable(true);
        v.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "SearchView has focus? " + hasFocus);
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    doSearch(null);
                }
                else {
                    SearchView searchView = (SearchView) v;
                    doSearch(searchView.getQuery().toString());
                }
            }
        });
        v.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.d(TAG, "SearchView query text submit: " + query);
                doSearch(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                Log.d(TAG, "SearchView query text change: " + newText);
                doSearch(newText);
                return true;
            }
        });
        v.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "SearchView search clicked");
            }
        });
        v.setOnCloseListener(new SearchView.OnCloseListener() {
            @Override
            public boolean onClose() {
                Log.d(TAG, "SearchView closed");
                doSearch(null); // Never called
                return false;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_rating_g) {
            item.setChecked(true);
            changeRating("g");
            return true;
        }
        if (id == R.id.action_rating_pg) {
            item.setChecked(true);
            changeRating("pg");
            return true;
        }
        if (id == R.id.action_rating_pg13) {
            item.setChecked(true);
            changeRating("pg-13");
            return true;
        }
        if (id == R.id.action_rating_r) {
            item.setChecked(true);
            changeRating("r");
            return true;
        }
        if (id == R.id.action_search) {
            View v = item.getActionView();
            v.requestFocus();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeRating(String rating) {
        factory.setRating(rating);
        factory.invalidate();
    }

    private void doSearch(String query) {
        factory.setSearchQuery(query);
        factory.invalidate();
    }
}
