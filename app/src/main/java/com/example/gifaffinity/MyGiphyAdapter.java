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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
                int width = gif.fixedHeight.width;
                int height = gif.fixedHeight.height;
                Bitmap placeholder = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                placeholder.eraseColor(0xFFFFFFFF);
                placeholderDrawable = new BitmapDrawable(placeholder);
            }
            Glide.with(context)
                    .load(gif.fixedHeight.url)
                    .placeholder(placeholderDrawable)
                    .fitCenter()
                    .into(holder.image);
            holder.image.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent gifViewIntent = new Intent(context, GifViewActivity.class);
                    gifViewIntent.setData(Uri.parse(gif.fixedHeight.url));
                    gifViewIntent.putExtra("title", gif.title);
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
            try {
                return oldItem.id.equals(newItem.id);
            } catch (Exception e) {
                Log.e(TAG, "Unexpected error:", e);
                return false;
            }
        }

        @Override
        public boolean areContentsTheSame(Gif oldItem, Gif newItem) {
            return true;
        }
    };

    static class ImageInfo {
        String url;
        int width;
        int height;

        ImageInfo(String url, int width, int height) {
            this.url = url;
            this.width = width;
            this.height = height;
        }
    }

    static class Gif {
        ImageInfo thumbnail; // placeholder
        byte[] thumbnailBytes; // bytes needed to create the placeholder still image
        ImageInfo fixedHeight;
        String title; // title of the image
        String id; // unique id for the image
        Call call; // okhttp call

        Gif(String id) {
            this.id = id;
        }

        @NonNull
        @Override
        public String toString() {
            return title;
        }
    }

    public static class GifDataSource extends PositionalDataSource<Gif> {
        private static final String SEARCH_ENDPOINT = "https://api.giphy.com/v1/gifs/search";
        private static final String TRENDING_ENDPOINT = "https://api.giphy.com/v1/gifs/trending";
        private final String rating;
        private final String searchQuery;

        Context context;
        String endpoint;
        OkHttpClient client = new OkHttpClient();

        GifDataSource(Context context, String rating) {
            this.context = context;
            this.endpoint = TRENDING_ENDPOINT;
            this.rating = rating;
            this.searchQuery = null;
        }

        GifDataSource(Context context, String rating, String searchQuery) {
            this.context = context;
            this.endpoint = SEARCH_ENDPOINT;
            this.rating = rating;
            this.searchQuery = searchQuery;
        }

        private JSONObject genericSearch(String... parameters) throws Exception {
            String url = endpoint + "?api_key=" + GIPHY_API_KEY + "&rating=" + rating;
            if (searchQuery != null && endpoint.equals(SEARCH_ENDPOINT)) {
                url += "&q=" + searchQuery; // TODO: Need to encode?
            }
            for (String param : parameters) {
                if (param == null) continue;
                String[] split = param.split("=", 2);
                if (split == null || split.length != 2) continue;
                if (split[0] == null || split[0].length() == 0) continue;
                if (split[1] == null || split[1].length() == 0) continue;
                url += "&" + split[0] + "=" + split[1];
            }
            Request request = new Request.Builder().url(url).build();
            ResponseBody responseBody = client.newCall(request).execute().body();
            JSONObject json = new JSONObject(responseBody.string());
            return json;
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
                JSONObject json = genericSearch("offset=0", "limit=" + params.requestedLoadSize);
                int totalCount = getTotalCount(json);
                int count = getCount(json);
                Log.d(MainActivity.TAG, "loadInitial: count " + count);
                Log.d(MainActivity.TAG, "loadInitial: totalCount " + totalCount);
                List<Gif> gifList = loadGifList(0, json);
                Log.d(TAG, String.format("loadInitial: callback.onResult(%d gifs, pos 0, total count %d)", gifList.size(), totalCount));
                callback.onResult(gifList, 0, totalCount);
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        }

        private int getCount(JSONObject json) throws JSONException {
            return json.getJSONObject("pagination").getInt("count");
        }

        private int getTotalCount(JSONObject json) throws JSONException {
            return json.getJSONObject("pagination").getInt("total_count");
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
                JSONObject json = genericSearch("offset=" + params.startPosition, "limit=" + params.loadSize);
                int totalCount = getTotalCount(json);
                int count = getCount(json);
                Log.d(MainActivity.TAG, "loadAfter: count " + count);
                Log.d(MainActivity.TAG, "loadAfter: totalCount " + totalCount);
                List<Gif> gifList = loadGifList(params.startPosition, json);
                Log.d(MainActivity.TAG, String.format("loadAfter: callback.onResult(%d gifs)", gifList.size()));
                callback.onResult(gifList);
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        }

        private List<Gif> loadGifList(int position, JSONObject json) throws JSONException {
            ArrayList<Gif> gifList = new ArrayList<>();
            int pos = position;
            JSONArray dataArray = json.getJSONArray("data");
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject datum = (JSONObject) dataArray.get(i);
                JSONObject images = datum.getJSONObject("images");
                Gif gif = new Gif(datum.getString("id"));
                gif.thumbnail = getSmallestStill(images);
                gif.fixedHeight = getImageInfo(images.getJSONObject("fixed_height"));
                gifList.add(gif);
                gif.title = datum.getString("title");
                gif.thumbnailBytes = null;
                if (gif.thumbnail != null) loadThumbnail(gif);
            }
            return gifList;
        }

        private ImageInfo getImageInfo(JSONObject image) throws JSONException {
            if (image == null) return null;
            return new ImageInfo(image.getString("url"), image.getInt("width"), image.getInt("height"));
        }

        private ImageInfo getSmallestStill(JSONObject images) throws JSONException {
            int smallestSize = Integer.MAX_VALUE;
            JSONObject smallest = null;
            Iterator<String> it = images.keys();
            while (it.hasNext()) {
                String key = it.next();
                if (key.endsWith("_still")) {
                    JSONObject image = images.optJSONObject(key);
                    if (image == null) continue;
                    int size = image.optInt("size", Integer.MAX_VALUE);
                    if (size < smallestSize) {
                        smallestSize = size;
                        smallest = image;
                    }
                }
            }
            return getImageInfo(smallest);
        }

        void loadThumbnail(final Gif gif) {
            Request request = new Request.Builder().url(gif.thumbnail.url).build();
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
        private Context context;
        private String rating = "g";
        private String searchQuery;
        private GifDataSource latestSource;

        public GifDataSourceFactory(Context context) {
            this.context = context;
        }

        @Override
        public DataSource<Integer, Gif> create() {
            if (searchQuery == null)
                latestSource = new GifDataSource(context, rating);
            else
                latestSource = new GifDataSource(context, rating, searchQuery);
            return latestSource;
        }

        public void setRating(String rating) {
            this.rating = rating;
        }

        public void setSearchQuery(String query) {
            this.searchQuery = query;
        }

        public void invalidate() {
            if (latestSource != null) latestSource.invalidate();
        }
    }
}
