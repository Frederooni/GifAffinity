package com.example.gifaffinity;

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

import com.example.gifaffinity.MyGiphyAdapter.Gif;

public class MainActivity extends AppCompatActivity {
    static final String TAG = "GifAffinity.Main";

    private RecyclerView recyclerView;

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
        final MyGiphyAdapter adapter = new MyGiphyAdapter(this);
        PagedList.Config config = new PagedList.Config.Builder().setPageSize(75).build();
        MyGiphyAdapter.GifDataSourceFactory factory = new MyGiphyAdapter.GifDataSourceFactory(this);
        LiveData gifs = new LivePagedListBuilder(factory, config).build();
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
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
