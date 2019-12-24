package com.example.gifaffinity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
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
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import at.mukprojects.giphy4j.Giphy;
import at.mukprojects.giphy4j.entity.giphy.GiphyContainer;
import at.mukprojects.giphy4j.entity.giphy.GiphyData;
import at.mukprojects.giphy4j.entity.giphy.GiphyImage;
import at.mukprojects.giphy4j.entity.search.SearchFeed;
import at.mukprojects.giphy4j.exception.GiphyException;

class MyGiphyAdapter extends PagedListAdapter<MyGiphyAdapter.Gif, MyGiphyAdapter.ViewHolder> {
    private static final String TAG = "GifAffinity.Adapter";

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
            Drawable placeholderDrawable = null;
            byte[] thumbnailBytes;
            synchronized (gif) {
                thumbnailBytes = gif.thumbnailBytes;
            }
            if (gif.thumbnailBytes != null) {
                Bitmap thumbnailBitmap = BitmapFactory.decodeByteArray(gif.thumbnailBytes, 0, gif.thumbnailBytes.length);
                placeholderDrawable = new BitmapDrawable(context.getResources(), thumbnailBitmap);
            } else {
                if (gif.call != null) gif.call.cancel();
                int width = Integer.parseInt(gif.fixedHeight.getWidth());
                int height = Integer.parseInt(gif.fixedHeight.getHeight());
                Bitmap placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                placeholder.eraseColor(0xFFFFFFFF);
                placeholderDrawable = new BitmapDrawable(placeholder);
            }
            Glide.with(context)
                    .load(gif.fixedHeight.getUrl())
                    .placeholder(placeholderDrawable)
                    .fitCenter()
                    .into(holder.image);
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
            // This should never happen
            holder.text.setText("Error at position " + Integer.toString(position));
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
        Call call; // okhttp call
        byte[] thumbnailBytes;

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
        Context context;
        Giphy giphy;
        OkHttpClient client = new OkHttpClient();

        GifDataSource(Context context) {
            this.giphy = new Giphy(GIPHY_API_KEY);
            this.context = context;
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
                gif.thumbnail = getSmallestStill(giphyData.getImages());
                gif.fixedHeight = giphyData.getImages().getFixedHeight();
                gifList.add(gif);
                gif.name = giphyData.title;
                gif.thumbnailBytes = null;
                if (gif.thumbnail != null) loadThumbnail(gif);
            }
            return gifList;
        }

        private GiphyImage getSmallestStill(GiphyImage... gifs) {
            int smallestSize = Integer.MAX_VALUE;
            GiphyImage smallest = null;
            for (GiphyImage gi : gifs) {
                int size;
                try {
                    size = Integer.parseInt(gi.getSize());
                } catch (Exception e) {
                    size = Integer.MAX_VALUE;
                }
                if (size < smallestSize) {
                    smallestSize = size;
                    smallest = gi;
                }
            }
            return smallest;
        }

        private GiphyImage getSmallestStill(GiphyContainer images) {
            return getSmallestStill(
                    images.getDownsizedStill(),
                    images.getFixedHeightSmallStill(),
                    images.getFixedHeightStill(),
                    images.getFixedWidthSmallStill(),
                    images.getFixedWidthStill()
            );
        }

        public void loadThumbnail(final Gif gif) {
            Request request = new Request.Builder().url(gif.thumbnail.getUrl()).build();
            Call call = client.newCall(request);
            gif.call = call;
            call.enqueue(new Callback() {
                public void onFailure(Request request, IOException e) {
                    Log.e(TAG, "onFailure: " + e);
                }
                public void onResponse(Response response) {
                    try {
                        ResponseBody responseBody = response.body();
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                        synchronized (gif) {
                            gif.thumbnailBytes = responseBody.bytes();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "onResponse: " + e);
                    }
                }
            });
        }
    }

    public static class GifDataSourceFactory extends DataSource.Factory<Integer, Gif> {
        Context context;

        public GifDataSourceFactory(Context context) {
            this.context = context;
        }

        @Override
        public DataSource<Integer, Gif> create() {
            GifDataSource dataSource = new GifDataSource(context);
            return dataSource;
        }
    }
}
