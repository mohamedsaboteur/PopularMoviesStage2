package com.andalus.abomed7at55.popularmoviesstage1;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DetailsActivity extends AppCompatActivity {

    @BindView(R.id.tv_original_title)
    TextView tvOriginalTitle;
    @BindView(R.id.iv_poster)
    ImageView ivPoster;
    @BindView(R.id.tv_plot_synopsis)
    TextView tvPlot;
    @BindView(R.id.tv_user_rating)
    TextView tvRating;
    @BindView(R.id.tv_release_date)
    TextView tvReleaseDate;
    @BindView(R.id.review_recycler_view)
    RecyclerView reviewsRecyclerView;
    @BindView(R.id.rv_videos)
    RecyclerView videoRecyclerView;
    @BindView(R.id.iv_star_button)
    ImageView starButton;

    @BindView(R.id.view)
    View view1;
    @BindView(R.id.view2)
    View view2;
    @BindView(R.id.tv_label_review)
    TextView labelReview;
    @BindView(R.id.tv_lbl_trailer)
    TextView labelTrailer;

    public static final int RESULT_FAVORITE_CHANGED = 50;

    //Rating Colors
    private static final int RATING_HIGHEST = 8;
    private static final int RATING_ABOVE_NORMAL = 6;
    private static final int RATING_NORMAL = 4;
    private static final int RATING_UNDER_NORMAL = 2;

    private boolean isThereReviews = true;
    private boolean isThereVideos = true;

    private static final String REVIEWS_KEY = "reviews_key";
    private static final String VIDEOS_KEY = "videos_key";

    private static final String REVIEW_STATUS = "review_status";
    private static final String TRAILER_STATUS = "trailer_status";

    private String id;

    private NetworkingManager networkingReview;
    private NetworkingManager networkingVideos;

    private ContentResolver contentResolver;

    private Movie selectedMovie;

    private ArrayList<MovieReview> movieReviews;
    private ArrayList<MovieVideo> movieVideos;

    private ReviewsAdapter reviewsAdapter;
    private VideosAdapter videosAdapter;

    private AdapterClickListener reviewsAdapterClickListener;
    private AdapterClickListener videosAdapterClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);

        ButterKnife.bind(this);

        ActionBar mActionBar = getSupportActionBar();
        if (mActionBar != null) {
            mActionBar.setDisplayHomeAsUpEnabled(true);
        }

        receiveAndShowData();

        //This is the reviews click listener
        reviewsAdapterClickListener = new AdapterClickListener() {
            @Override
            public void onItemClicked(int itemPosition) {
                openLink(movieReviews.get(itemPosition).getUrl());
            }
        };
        //This is the videos click listener
        videosAdapterClickListener = new AdapterClickListener() {
            @Override
            public void onItemClicked(int itemPosition) {
                openLink(MovieVideo.YOUTUBE_BASE + movieVideos.get(itemPosition).getKey());
            }
        };

        if(savedInstanceState != null){
            movieReviews = savedInstanceState.getParcelableArrayList(REVIEWS_KEY);
            movieVideos = savedInstanceState.getParcelableArrayList(VIDEOS_KEY);
            if(movieReviews != null){
                reviewsAdapter = new ReviewsAdapter(movieReviews,reviewsAdapterClickListener);
                reviewsRecyclerView.setAdapter(reviewsAdapter);
            }
            if(movieVideos != null){
                videosAdapter = new VideosAdapter(movieVideos,videosAdapterClickListener);
                videoRecyclerView.setAdapter(videosAdapter);
            }
            isThereVideos = savedInstanceState.getBoolean(TRAILER_STATUS);
            isThereReviews = savedInstanceState.getBoolean(REVIEW_STATUS);
            Log.d("HERE","We are here!" + isThereVideos);
            if(!isThereReviews){
                //tvNoReviews.setVisibility(View.VISIBLE);
                labelReview.setVisibility(View.INVISIBLE);
                view2.setVisibility(View.INVISIBLE);
            }
            if(!isThereVideos){
                //tvNoVideos.setVisibility(View.VISIBLE);
                labelTrailer.setVisibility(View.INVISIBLE);
                view1.setVisibility(View.INVISIBLE);
            }
        }else{
            setUpReviewsAndVideos();
        }

        //RecyclerView setting up
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        reviewsRecyclerView.setLayoutManager(layoutManager);
        RecyclerView.LayoutManager videoLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        videoRecyclerView.setLayoutManager(videoLayoutManager);

        //Setting up preferences
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        contentResolver = getContentResolver();

        //Setting up initial image for the star button
        boolean isFavourite = preferences.getBoolean(selectedMovie.getId(),false);
        if(isFavourite){
            starButton.setImageResource(android.R.drawable.btn_star_big_on);
        }else {
            starButton.setImageResource(android.R.drawable.btn_star_big_off);
        }

        //starButton clickListener
        starButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isFav = preferences.getBoolean(selectedMovie.getId(),false);
                SharedPreferences.Editor editor = preferences.edit();
                if(isFav){
                    editor.putBoolean(selectedMovie.getId(),false);
                    starButton.setImageResource(android.R.drawable.btn_star_big_off);
                    //remove from favourite table database
                    contentResolver.delete(MoviesContentProvider.buildAppUri(id),null,null);
                }else {
                    editor.putBoolean(selectedMovie.getId(),true);
                    starButton.setImageResource(android.R.drawable.btn_star_big_on);
                    //add to favourite table database
                    contentResolver.insert(MoviesContentProvider.buildAppUri(id),
                            MoviesContentProvider
                                    .createContentValues(Long.parseLong(id),selectedMovie.getTitle(),
                                            selectedMovie.getPlot(),
                                            selectedMovie.getRating(),
                                            selectedMovie.getDate(),
                                            selectedMovie.getPoster()));
                }
                editor.apply();

                if(preferences.getInt(getString(R.string.pref_sort),ApiBuilder.SORT_FAVORITE) == ApiBuilder.SORT_FAVORITE){
                    setResult(RESULT_FAVORITE_CHANGED);
                }
            }
        });
        //Hiding the review and the trailer texts if there is no internet connection and no data to show
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if(networkInfo == null){
            if(movieVideos == null){
                labelTrailer.setVisibility(View.INVISIBLE);
                view1.setVisibility(View.INVISIBLE);
                isThereVideos = false;
            }else{
                if(movieVideos.size() == 0){
                    labelTrailer.setVisibility(View.INVISIBLE);
                    view1.setVisibility(View.INVISIBLE);
                    isThereVideos = false;
                }
            }
            if(movieReviews == null){
                labelReview.setVisibility(View.INVISIBLE);
                view2.setVisibility(View.INVISIBLE);
                isThereReviews = false;
            }else{
                if(movieReviews.size() == 0){
                    labelReview.setVisibility(View.INVISIBLE);
                    view2.setVisibility(View.INVISIBLE);
                    isThereReviews = false;
                }
            }
        }
    }

    /**
     * This method brings the data from the selected movie in the main activity class and displays
     * them in the details activity
     */
    private void receiveAndShowData() {
        //Receiving
        Intent i = getIntent();
        int position = i.getExtras().getInt(getString(R.string.movie_position));
        selectedMovie = MainActivity.getMovies()[position];
        String title = selectedMovie.getTitle();
        String posterPath = selectedMovie.getFullPoster();
        String plot = selectedMovie.getPlot();
        String rating = selectedMovie.getRating();
        String releaseDate = selectedMovie.getDate();
        id = selectedMovie.getId();
        float ratingFloat = Float.parseFloat(rating);
        int ratingColor;
        if (ratingFloat >= RATING_HIGHEST) {
            ratingColor = getResources().getColor(R.color.rating_best);
        } else if (ratingFloat >= RATING_ABOVE_NORMAL) {
            ratingColor = getResources().getColor(R.color.rating_above_normal);
        } else if (ratingFloat >= RATING_NORMAL) {
            ratingColor = getResources().getColor(R.color.rating_normal);
        } else if (ratingFloat >= RATING_UNDER_NORMAL) {
            ratingColor = getResources().getColor(R.color.rating_under_normal);
        } else {
            ratingColor = getResources().getColor(R.color.rating_worst);
        }
        //Displaying
        tvOriginalTitle.setText(title);
        Picasso.with(this)
                .load(posterPath)
                .error(android.R.drawable.stat_notify_error)
                .into(ivPoster);
        tvPlot.setText(plot);
        tvRating.setText(rating);
        tvRating.setTextColor(ratingColor);
        tvReleaseDate.setText(releaseDate);
    }

    /**
     * This method is used to set up the adapter of both reviews and videos recycler views.
     * It connects to the internet to bring the required data
     */
    private void setUpReviewsAndVideos(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        if(networkInfo != null){
            reviews();
            videos();
        }else {
            Toast.makeText(getBaseContext(), R.string.network_issue_message,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * This method handles the whole process for displaying reviews
     */
    private void reviews() {
        //Reviews
        networkingReview = new NetworkingManager();
        String apiReview = ApiBuilder.buildApi(id, ApiBuilder.REVIEW);
        MyBackgroundTask backgroundReviews = new MyBackgroundTask(apiReview, new MyBackgroundTaskCallBacks<String, String>() {

            @Override
            public String onBackground(String data) {
                try {
                    networkingReview.startConnection(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return networkingReview.retrieveData();
            }

            @Override
            public void onTaskFinished(String result) {
                try {
                    movieReviews = DataPicker.pickReviews(result);
                    if(movieReviews.size() == 0){
                        isThereReviews = false;
                        //tvNoReviews.setVisibility(View.VISIBLE);
                        labelReview.setVisibility(View.INVISIBLE);
                        view2.setVisibility(View.INVISIBLE);
                    }
                    reviewsAdapter = new ReviewsAdapter(movieReviews,reviewsAdapterClickListener);
                    reviewsRecyclerView.setAdapter(reviewsAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        backgroundReviews.execute();
    }

    /**
     * This method handles the whole process for displaying trailers
     */
    private void videos() {
        //videos
        networkingVideos = new NetworkingManager();
        String apiVideos = ApiBuilder.buildApi(id, ApiBuilder.VIDEOS);
        MyBackgroundTask backgroundVideos = new MyBackgroundTask(apiVideos, new MyBackgroundTaskCallBacks<String, String>() {

            @Override
            public String onBackground(String data) {
                try {
                    networkingVideos.startConnection(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return networkingVideos.retrieveData();
            }

            @Override
            public void onTaskFinished(String result) {
                try {
                    movieVideos = DataPicker.pickVideos(result);
                    if(movieVideos.size() == 0){
                        isThereVideos = false;
                        //tvNoVideos.setVisibility(View.VISIBLE);
                        labelTrailer.setVisibility(View.INVISIBLE);
                        view1.setVisibility(View.INVISIBLE);
                    }
                    videosAdapter = new VideosAdapter(movieVideos, videosAdapterClickListener);
                    videoRecyclerView.setAdapter(videosAdapter);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        backgroundVideos.execute();
    }

    /**
     * This method opens the given link in the browser
     *
     * @param url the link to be opened
     */
    private void openLink(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        try {
            startActivity(intent);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(REVIEWS_KEY,movieReviews);
        outState.putParcelableArrayList(VIDEOS_KEY,movieVideos);
        outState.putBoolean(REVIEW_STATUS,isThereReviews);
        outState.putBoolean(TRAILER_STATUS,isThereVideos);

    }

}
