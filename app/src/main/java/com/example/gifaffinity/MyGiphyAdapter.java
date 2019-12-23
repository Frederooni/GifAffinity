package com.example.gifaffinity;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.paging.DataSource;
import androidx.paging.PagedListAdapter;
import androidx.paging.PositionalDataSource;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import at.mukprojects.giphy4j.Giphy;
import at.mukprojects.giphy4j.entity.giphy.GiphyData;
import at.mukprojects.giphy4j.entity.giphy.GiphyImage;
import at.mukprojects.giphy4j.entity.search.SearchFeed;
import at.mukprojects.giphy4j.exception.GiphyException;

class MyGiphyAdapter extends PagedListAdapter<MyGiphyAdapter.Gif, MyGiphyAdapter.ViewHolder> {
    private static final String GIPHY_API_KEY = "p7ToW673BjPcLL4AhMMe3p5W1gsxQ6dq";
    Context context;

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView text;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.gif_text);
            image = itemView.findViewById(R.id.gif_image);
        }
    }

    MyGiphyAdapter(Context context) {
        super(DIFF_CALLBACK);
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.gif_layout, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final Gif gif = getItem(position);
        Log.d(MainActivity.TAG, "onBindViewHolder(position " + position + ", gif " + gif + ")");
        if (gif != null) {
            holder.text.setVisibility(View.GONE);
            holder.image.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(gif.fixedHeight.getUrl())
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .fitCenter()
                    .into(holder.image);
            // ZZZ TODO: Not sure if we should use this
            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent gifViewIntent = new Intent(context, GifViewActivity.class);
                    gifViewIntent.setData(Uri.parse(gif.fixedHeight.getUrl()));
                    gifViewIntent.putExtra("title", gif.name);
                    context.startActivity(gifViewIntent);
                }
            });
        } else {
            holder.text.setText(Integer.toString(position));
            holder.image.setVisibility(View.GONE);
            holder.text.setVisibility(View.VISIBLE);
        }
    }

    static final DiffUtil.ItemCallback<Gif> DIFF_CALLBACK = new DiffUtil.ItemCallback<Gif>() {
        @Override
        public boolean areItemsTheSame(Gif oldItem, Gif newItem) {
            return oldItem.position == newItem.position;
        }

        @Override
        public boolean areContentsTheSame(Gif oldItem, Gif newItem) {
            return oldItem.position == newItem.position;
        }
    };

    static class Gif {
        GiphyImage thumbnail;
        GiphyImage fixedHeight;
        String name;
        int position;
        Gif(int position) {
            this.position = position;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }
    }

    public static class GifDataSource extends PositionalDataSource<Gif> {
        Giphy giphy;

        GifDataSource() {
            this.giphy = new Giphy(GIPHY_API_KEY);
        }

        /**
         * Load initial list data.
         * <p>
         * This method is called to load the initial page(s) from the DataSource.
         * <p>
         * Result list must be a multiple of pageSize to enable efficient tiling.
         *
         * @param params   Parameters for initial load, including requested start position, load size, and
         *                 page size.
         * @param callback Callback that receives initial load data, including
         */
        @Override
        public void loadInitial(@NonNull LoadInitialParams params, @NonNull LoadInitialCallback<Gif> callback) {
            try {
                Log.d(MainActivity.TAG, "loadInitial: requestedLoadSize " + params.requestedLoadSize + " placeholdersEnabled " + params.placeholdersEnabled);
                SearchFeed feed = giphy.trend("offset=0", "limit=" + params.requestedLoadSize, "rating=g");
                int totalCount = feed.getPagination().getTotalCount();
                Log.d(MainActivity.TAG, "loadInitial: count " + feed.getPagination().getCount());
                Log.d(MainActivity.TAG, "loadInitial: totalCount " + totalCount);
                List<Gif> gifList = loadGifList(0, feed);
                Log.d(MainActivity.TAG, String.format("loadInitial: callback.onResult(%d gifs, pos 0, total count %d)", gifList.size(), totalCount));
                callback.onResult(gifList, 0, totalCount);
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "Error", e);
            }

        }

        /**
         * Called to load a range of data from the DataSource.
         * <p>
         * This method is called to load additional pages from the DataSource after the
         * LoadInitialCallback passed to dispatchLoadInitial has initialized a PagedList.
         * <p>
         * Unlike {@link #loadInitial(LoadInitialParams, LoadInitialCallback)}, this method must return
         * the number of items requested, at the position requested.
         *
         * @param params   Parameters for load, including start position and load size.
         * @param callback Callback that receives loaded data.
         */
        @Override
        public void loadRange(@NonNull LoadRangeParams params, @NonNull LoadRangeCallback<Gif> callback) {
            Log.d(MainActivity.TAG, "loadAfter: startPosition " + params.startPosition + " loadSize " + params.loadSize);
            try {
                SearchFeed feed = giphy.trend("offset=" + params.startPosition, "limit=" + params.loadSize, "rating=g");
                int totalCount = feed.getPagination().getTotalCount();
                int count = feed.getPagination().getCount();
                Log.d(MainActivity.TAG, "loadAfter: count " + count);
                Log.d(MainActivity.TAG, "loadAfter: totalCount " + totalCount);
                List<Gif> gifList = loadGifList(params.startPosition, feed);
                Log.d(MainActivity.TAG, String.format("loadAfter: callback.onResult(%d gifs)", gifList.size()));
                callback.onResult(gifList);
            } catch (GiphyException e) {
                e.printStackTrace();
            }
        }

        private List<Gif> loadGifList(int position, SearchFeed feed) {
            ArrayList<Gif> gifList = new ArrayList<>();
            int pos = position;
            for (GiphyData giphyData : feed.getDataList()) {
                Gif gif = new Gif(pos);
                gif.position = pos++; // TODO: Is this still needed?
                gif.thumbnail = giphyData.getImages().getFixedHeightStill();
                gif.fixedHeight = giphyData.getImages().getFixedHeight();
                gifList.add(gif);
                gif.name = giphyData.title;
            }
            return gifList;
        }
    }

    public static class GifDataSourceFactory extends DataSource.Factory<Integer, Gif> {
        @Override
        public DataSource<Integer, Gif> create() {
            GifDataSource dataSource = new GifDataSource();
            return dataSource;
        }
    }
}
