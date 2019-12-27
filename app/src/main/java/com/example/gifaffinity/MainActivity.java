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

import android.os.Parcel;
import android.os.PersistableBundle;
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
        PagedList.Config config = new PagedList.Config.Builder().setPageSize(50).build();
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
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.d(TAG, "onSaveInstanceState");
        super.onSaveInstanceState(outState);
        // If android:viewHierarchyState gets too large then we will get a
        // TransactionTooLargeException, so to avoid that we clear the state, which doesn't
        // seem to cause any harm.  To see how large outState gets, uncomment the following line:
        // print_bundle(outState, 1);
        outState.clear();
    }

    int bundle_size(Bundle b) {
        Parcel parcel = Parcel.obtain();
        parcel.writeValue(b);
        byte[] bytes = parcel.marshall();
        parcel.recycle();
        return bytes.length;
    }

    void print_bundle(Bundle b, int level) {
        String indent = "";
        for (int i = 0; i < level; i++) indent += "    ";
        Log.d(TAG, indent + "size " + bundle_size(b));
        for (String key : b.keySet()) {
            Object o = b.get(key);
            Log.d(TAG, indent + key + " is " + o.getClass().getSimpleName());
            if (o instanceof Bundle) {
                print_bundle((Bundle) o, level + 1);
            }
        }
    }

    @Override
    public void onStateNotSaved() {
        Log.d(TAG, "onStateNotSaved");
        super.onStateNotSaved();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        Log.d(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(@Nullable Bundle savedInstanceState, @Nullable PersistableBundle persistentState) {
        Log.d(TAG, "onRestoreInstanceState");
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem item = menu.findItem(R.id.action_rating_g);
        item.setChecked(true);
        item = menu.findItem(R.id.action_search);
        setupSearch(item);
        return true;
    }

    private boolean isSearchMode = false;

    private void setupSearch(MenuItem item) {
        final SearchView v = (SearchView) item.getActionView();
        v.setQueryHint("Search...");
        v.setIconifiedByDefault(false);
        v.setIconified(false);
        v.setFocusable(true);
        item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                Log.d(TAG, "SearchView menu item expanded");
                isSearchMode = true;
                SearchView searchView = (SearchView) v;
                doSearch(searchView.getQuery().toString());
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                Log.d(TAG, "SearchView menu item collapsed");
                isSearchMode = false;
                doSearch(null);
                return true;
            }
        });
        v.setOnQueryTextFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                Log.d(TAG, "SearchView has focus? " + hasFocus);
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
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
