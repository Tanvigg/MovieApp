package com.example.tanvigupta.movieapp.network;

import android.util.Log;

import com.example.tanvigupta.movieapp.MovieApplication;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit retrofit;
    private static MovieService movieService;
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_PRAGMA = "Pragma";
    private static final long cacheSize = 5 * 1024 * 1024; // 5 MB



    private static Retrofit getInstance() {
        return new Retrofit.Builder().baseUrl("http://api.themoviedb.org/3/movie/").client(okHttpClient()).addConverterFactory(GsonConverterFactory.create()).build();

    }

    private static OkHttpClient okHttpClient(){
        return new OkHttpClient.Builder().cache(cache()).addInterceptor(httpLoggingInterceptor()).addNetworkInterceptor(networkInterceptor()).addNetworkInterceptor(offlineInterceptor()).build();

    }

    private static Cache cache(){
        return new Cache(new File(MovieApplication.getInstance().getCacheDir(),"preview_cache"),cacheSize);
    }

    /**
     * This interceptor will be called both if the network is available and if the network is not available
     * @return
     */

    private static Interceptor offlineInterceptor() {
        return new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Log.d("Network", "offline interceptor: called.");
                Request request = chain.request();

                if (MovieApplication.hasNetwork()) {
                    CacheControl cacheControl = new CacheControl.Builder()
                            .maxStale(7, TimeUnit.DAYS)
                            .build();

                    request = request.newBuilder()
                            .removeHeader(HEADER_PRAGMA)
                            .removeHeader(HEADER_CACHE_CONTROL)
                            .cacheControl(cacheControl)
                            .build();
                }
                return chain.proceed(request);
            }
        };
    }


    /**
     * This interceptor will be called ONLY if the network is available
     * @return
     */

        private static Interceptor networkInterceptor() {
            return new Interceptor() {
                @Override
                public Response intercept(Chain chain) throws IOException {
                    Log.d("Network", "network interceptor: called.");

                    Response response = chain.proceed(chain.request());

                    CacheControl cacheControl = new CacheControl.Builder()
                            .maxAge(5, TimeUnit.SECONDS)
                            .build();

                    return response.newBuilder()
                            .removeHeader(HEADER_PRAGMA)
                            .removeHeader(HEADER_CACHE_CONTROL)
                            .header(HEADER_CACHE_CONTROL, cacheControl.toString())
                            .build();
                }
            };
        }


    private static HttpLoggingInterceptor httpLoggingInterceptor ()
    {
        HttpLoggingInterceptor httpLoggingInterceptor =
                new HttpLoggingInterceptor( new HttpLoggingInterceptor.Logger()
                {
                    @Override
                    public void log (String message)
                    {
                        Log.d("Network", "log: http log: " + message);
                    }
                } );
        httpLoggingInterceptor.setLevel( HttpLoggingInterceptor.Level.BODY);
        return httpLoggingInterceptor;
    }




    public static MovieService getMovieService() {
        if (movieService == null) {
            movieService = getInstance().create(MovieService.class);
        }
        return movieService;
    }
}
