package com.pewpo.spotifyapp;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Error;
import com.spotify.sdk.android.player.Metadata;
import com.spotify.sdk.android.player.PlaybackState;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerEvent;
import com.spotify.sdk.android.player.Spotify;
import com.spotify.sdk.android.player.SpotifyPlayer;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.TracksPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

import static android.widget.Toast.LENGTH_SHORT;


public class MainActivity extends Activity implements SpotifyPlayer.NotificationCallback, ConnectionStateCallback {

    /*
     * Used libraries:
     * https://github.com/spotify/android-sdk
     * https://github.com/spotify/android-auth
     * https://github.com/kaaes/spotify-web-api-android
     * */
    // Tähän oma asiakas id jonka saa spotify for developers sivulta.
    private static final String CLIENT_ID = ""; // poistettu näkyvistä

    // Tähän redirect URI jonka saa myös spotify for developers sivulta
    private static final String REDIRECT_URI = ""; // poistettu näkyvistä

    // Request code that will be used to verify if the result comes from correct activity
    // Can be any integer
    private static final int REQUEST_CODE = 9456;

    private Metadata mMetadata;
    private ImageView bandLogo;
    private EditText addSongTxtField;
    private TextView songInfo;
    private ProgressBar logoProgressBar;
    private ProgressBar songProgressBar;
    private Player mPlayer;
    private Map<Integer,String[]> searchedSongs;
    private PlaybackState playBackState;
    SpotifyService spotify;
    Toast toast;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //setScope -> Kysytään käyttäjältä lupaa toimintoihin
        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming","user-library-read"});
        AuthenticationRequest request = builder.build();
        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
        Init();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);
        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE)
        {
            final AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if (response.getType() == AuthenticationResponse.Type.TOKEN)
            {
                Config playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);
                Spotify.getPlayer(playerConfig, this, new SpotifyPlayer.InitializationObserver()
                {
                    //API:iin
                    SpotifyApi api = new SpotifyApi();

                    @Override
                    public void onInitialized(SpotifyPlayer spotifyPlayer)
                    {
                        //Asetetaan accestocken että saadaan tehtyä validi pyyntö APIin
                        api.setAccessToken(response.getAccessToken());
                        spotify = api.getService();
                        mPlayer = spotifyPlayer;
                        mPlayer.addConnectionStateCallback(MainActivity.this);
                        mPlayer.addNotificationCallback(MainActivity.this);
                        getRandomMetalPlaylist();
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                    }
                });
            }
        }

    }
    private void Init()
    {
        bandLogo = findViewById(R.id.bandLogo);
        logoProgressBar = findViewById(R.id.logoProgressBar);
        songProgressBar = findViewById(R.id.songProgressBar);
        addSongTxtField = findViewById(R.id.addSongTxtField);
        songInfo = findViewById(R.id.songInfo);
    }

    @Override
    protected void onDestroy()
    {
        // VERY IMPORTANT! This must always be called or else you will leak resources
        Spotify.destroyPlayer(this);
        super.onDestroy();
    }

    @Override
    public void onPlaybackEvent(PlayerEvent playerEvent)
    {
        Log.d("MainActivity", "Playback event received: " + playerEvent.name());
        playBackState = mPlayer.getPlaybackState();
        mMetadata = mPlayer.getMetadata();
        try {
            updateView();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPlaybackError(Error error)
    {
        Log.d("MainActivity", "Playback error received: " + error.name());
        switch (error) {
            // Handle error type as necessary
            default:
                break;
        }
    }

    @Override
    public void onLoggedIn() { Log.d("MainActivity", "User logged in"); }

    private final Player.OperationCallback mOperationCallback = new Player.OperationCallback()
    {
        @Override
        public void onSuccess() {}
        @Override
        public void onError(Error error) {
            Toast toast = Toast.makeText(getApplicationContext(), error.toString(), LENGTH_SHORT);
            toast.show();
        }
    };
    @Override
    public void onLoggedOut() {
        Log.d("MainActivity", "User logged out");
    }
    @Override
    public void onLoginFailed(Error error) {
        Log.d("MainActivity", "Login failed");
    }
    @Override
    public void onTemporaryError() {
        Log.d("MainActivity", "Temporary error occurred");
    }
    @Override
    public void onConnectionMessage(String message) {
        Log.d("MainActivity", "Received connection message: " + message);
    }

    public void onSelectSongButtonClicked(View view)
    {
        int key=0;
        switch(view.getId())
        {
            case R.id.Track0:
                key=0;
                break;
            case R.id.Track1:
                key=1;
                break;
            case R.id.Track2:
                key=2;
                break;
            case R.id.Track3:
                key=3;
                break;
            case R.id.Track4:
                key=4;
                break;
        }
        mPlayer.queue(mOperationCallback, "spotify:track:"+searchedSongs.get(key)[1]);
        toast = Toast.makeText(getApplicationContext(), "'"+searchedSongs.get(key)[0]+"' "+ getString(R.string.addedToPlaylist) , Toast.LENGTH_LONG);
        toast.show();
    }

    public void onSearchTracksButtonClicked(View view)
    {
        try
        {
            searchTracks(addSongTxtField.getText().toString());
        } catch(Exception e) {
          Log.e("<<SearchBtnClicked>>",e.getMessage());
        }
    }

    public void onPlayButtonClicked(View view)
    {
        try
        {
            if(mMetadata == null && playBackState == null)
                getRandomMetalPlaylist();
            else if (mMetadata != null && playBackState != null)
            {
                if (!playBackState.isPlaying)
                    mPlayer.resume(mOperationCallback);
            }
        }
        catch(Exception e){
            Log.e("onPlayBtnClicked", e.getMessage());
        }
    }

    public void onPauseButtonClicked(View view)          { mPlayer.pause(mOperationCallback); }
    public void onSkipToNextButtonClicked(View view)     { mPlayer.skipToNext(mOperationCallback); }
    public void onSkipToPreviousButtonClicked(View view) { mPlayer.skipToPrevious(mOperationCallback); }
    private void updateView() throws IOException
    {
        if (mMetadata != null)
        {
            findViewById(R.id.nextSongBtn).setEnabled(mMetadata.nextTrack != null);
            findViewById(R.id.previousSongBtn).setEnabled(mMetadata.prevTrack != null);
            findViewById(R.id.pauseSongBtn).setEnabled(mMetadata.currentTrack != null);
            if (mMetadata.currentTrack != null)
            {
                final String durationStr = String.format("%d,%d",
                        TimeUnit.MILLISECONDS.toMinutes(mMetadata.currentTrack.durationMs),
                        TimeUnit.MILLISECONDS.toSeconds(mMetadata.currentTrack.durationMs) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(mMetadata.currentTrack.durationMs)));

                handleProgBar();
                showImage(mMetadata.currentTrack.albumCoverWebUrl);
                songInfo.setText(mMetadata.currentTrack.albumName + "\n" + mMetadata.currentTrack.name + " - " + mMetadata.currentTrack.artistName + "\n" + durationStr);
            }
        }
    }

    private void changeSelectTrackState(Boolean bool, int i)
    {
        int visibility;
        if(bool)
            visibility = View.VISIBLE;
        else
            visibility = View.INVISIBLE;

        if(i==0)
            findViewById(R.id.Track0).setVisibility(visibility);
        else if(i==1)
            findViewById(R.id.Track1).setVisibility(visibility);
        else if(i==2)
            findViewById(R.id.Track2).setVisibility(visibility);
        else if(i==3)
            findViewById(R.id.Track3).setVisibility(visibility);
        else if(i==4)
            findViewById(R.id.Track4).setVisibility(visibility);
        else {
            findViewById(R.id.Track0).setVisibility(visibility);
            findViewById(R.id.Track1).setVisibility(visibility);
            findViewById(R.id.Track2).setVisibility(visibility);
            findViewById(R.id.Track3).setVisibility(visibility);
            findViewById(R.id.Track4).setVisibility(visibility);
        }
    }


    //--------FUNCTIONS CALLS TO ASYNC CLASSES-----------------------------------------------------
    private void showImage(String url)
    {
        DownloadImageTask logoTask = new DownloadImageTask();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            logoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, url);
        else
            logoTask.execute(url);
    }
    private void handleProgBar()
    {
        ProgBarHandler progBarHandler = new ProgBarHandler();
        progBarHandler.execute();
    }
    private void getRandomMetalPlaylist()
    {
        SearchPlaylistFromSpotify SPFS= new SearchPlaylistFromSpotify();
        SPFS.execute("Metal");
    }
    private void searchTracks(String trackname)
    {
        SearchTarcksFromSpotify  STFS = new SearchTarcksFromSpotify();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            STFS.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, trackname);
        else
            STFS.execute(trackname);
    }

    //-------------------------------------ASYNC CLASSES---------------------------------------
    //For showing pictures
    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap>
    {
        protected void onPreExecute() {
            logoProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Bitmap doInBackground(String... url)
        {
            Bitmap bitmap = null;
            try
            {
                URL imageUrl = new URL(url[0]);
                InputStream in = imageUrl.openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("<<LOADIMAGE>>", e.getMessage());
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap)
        {
            bandLogo.setImageBitmap(bitmap);
            logoProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    //For handling progression bar
    private class ProgBarHandler extends AsyncTask<Void,Integer,Integer>
    {
        protected void onPreExecute() {
            songProgressBar.setProgress(0);
        }

        @Override
        protected Integer doInBackground(Void... url)
        {
            int songProgress = 0;
            try
            {
                while (mPlayer.getPlaybackState().isPlaying)
                {
                    songProgress = (int) ((1 - (((double)  mMetadata.currentTrack.durationMs - (double) mPlayer.getPlaybackState().positionMs) / (double) mMetadata.currentTrack.durationMs)) * (100));
                    publishProgress(songProgress);
                    Thread.sleep(500);
                }
            }
            catch (Exception e) {
                Log.e("<<PROGRESBARHANDLER>>", e.getMessage());
            }
            return songProgress;
        }

        protected void onProgressUpdate(Integer... songProgress) {
            songProgressBar.setProgress(songProgress[0]);
        }
        @Override
        protected void onPostExecute(Integer songProgress) {
            songProgressBar.setProgress(songProgress);
        }
    }

    //Search playlists from Spotify Web API
    private class SearchPlaylistFromSpotify extends AsyncTask<String, String ,Void>
    {
        @Override
        protected Void doInBackground(String... playlist)
        {
            spotify.searchPlaylists(playlist[0], new Callback<PlaylistsPager>()
            {
                @Override
                public void success(PlaylistsPager playlistPager, Response response)
                {
                    int i=0;
                    Random r = new Random();
                    if(playlistPager.playlists.items.size()>0) {
                        if(playlistPager.playlists.items.size()>1)
                            i = r.nextInt(playlistPager.playlists.items.size() - 1);
                        String url = playlistPager.playlists.items.get(i).id;
                        publishProgress(url);
                    }
                }
                @Override
                public void failure(RetrofitError error) {
                    Log.e("<SearchPlaylistSpotify>", error.getMessage());
                }
            });
            return null;
        }

        @Override
        protected void onProgressUpdate(String... url)
        {
            try{
                mPlayer.playUri(mOperationCallback, "spotify:user:spotify:playlist:"+url[0], 0, 0);
            } catch(Exception e){
                Log.e("SearchPlaylistSpotify", e.getMessage());
            }
        }
    }

    //Search tracks from Spotify Web API
    private class SearchTarcksFromSpotify extends AsyncTask<String, Map ,Void>
    {
        @Override
        protected Void doInBackground(String... trackname)
        {
            spotify.searchTracks(trackname[0], new Callback<TracksPager>()
            {
                @Override
                public void success(TracksPager tracksPager, Response response)
                {
                    try
                    {
                        int val;
                        Random r = new Random();
                        int len = tracksPager.tracks.items.size();

                        Map<Integer, String[]> map = new HashMap<>();
                        if(len>5)
                            len = 5;
                        for (int i = 0; i < len; i++)
                        {
                            val = len > 1 ? r.nextInt(tracksPager.tracks.items.size() - 1) : 0;
                            map.put(i , new String[]{tracksPager.tracks.items.get(val).artists.get(0).name+" - "+tracksPager.tracks.items.get(val).name,tracksPager.tracks.items.get(val).id});
                        }
                        searchedSongs = map;
                        publishProgress(map);
                    } catch(Exception e) {
                        Log.e("<<searchTracksSpotify>>", e.getMessage());
                    }
                }

                @Override
                public void failure(RetrofitError error) {
                    Log.e("<<searchTracksSpotify>>", error.getMessage());
                }
            });
            return null;
        }

        @Override
       protected void onProgressUpdate(Map... map)
        {
            try
            {
                if(map[0].size() == 0) {
                    toast = Toast.makeText(getApplicationContext(),getString(R.string.noSearchResults), Toast.LENGTH_LONG);
                    toast.show();
                }
                else
                {
                    Map m = map[0];
                    String txt = getString(R.string.selectTrackMsg)+"\n";
                    for(int i=0; i< m.size(); i++)
                    {
                        String[] avc = (String[]) m.get(i);
                        txt = txt + "Btn " + i + ": " + avc[0] +"\n";
                        changeSelectTrackState(true,i);
                    }
                    for(int i=4; i>=m.size(); i--){
                        changeSelectTrackState(false,i);
                    }
                    toast = Toast.makeText(getApplicationContext(),txt, Toast.LENGTH_LONG);
                    toast.show();
                }

            } catch(Exception e){
                Log.e("SearchPlaylistSpotify", e.getMessage());
            }
        }
    }
}






