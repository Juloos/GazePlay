package net.gazeplay.commons.utils.games;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.gazeplay.commons.threads.CustomThreadFactory;
import net.gazeplay.commons.threads.GroupingThreadFactory;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.media.MediaException;
import net.gazeplay.commons.configuration.Configuration;
import net.gazeplay.commons.configuration.ConfigurationBuilder;

@Slf4j
public class BackgroundMusicManager {

    public static final List<String> SUPPORTED_FILE_EXTENSIONS = Arrays.asList(".mp3", ".m4a");

    @Getter
    private static final BackgroundMusicManager instance = new BackgroundMusicManager();

    @Getter
    private final List<MediaPlayer> playlist = new ArrayList<MediaPlayer>();
    private MediaPlayer currentMusic;
    @Getter
    private int currentMusicIndex = 0;

    private final ExecutorService executorService = new ThreadPoolExecutor(1, 1, 3, TimeUnit.MINUTES,
            new LinkedBlockingQueue<>(), new CustomThreadFactory(this.getClass().getSimpleName(),
                    new GroupingThreadFactory(this.getClass().getSimpleName())));

    private final DoubleProperty volume = new SimpleDoubleProperty(0.25);

    private final ConfigurationBuilder configBuilder;
    
    private final BooleanProperty isPlayingPoperty = new SimpleBooleanProperty(this, "isPlaying", false);

    public BackgroundMusicManager() {
        configBuilder = ConfigurationBuilder.createFromPropertiesResource();
        final Configuration configuration = configBuilder.build();
        volume.set(configuration.getSoundLevel());

        // Maybe it is better to save the sound only when game exit and not each time the sound is changed
        volume.addListener((observable) -> {
            configBuilder.withSoundLevel(volume.getValue()).saveConfigIgnoringExceptions();
        });
        
        isPlayingPoperty.addListener((observable) -> {
            
            if(currentMusic != null) {
                if(isPlaying()) {
                    this.currentMusic.play();
                } else {
                    this.currentMusic.pause();
                }
            }

        });

        // TODO : remove this line so it is not the constructor's responsibility to
        // add music
        getAudioFromFolder(configuration.getMusicFolder());
    }

    public void getAudioFromFolder(String folderPath) {

        final File folder = new File(folderPath);
        if (!folder.exists()) {
            // throw new RuntimeException("invalid path for audio folder : " + folderPath);
            log.warn("invalid path for audio folder : " + folderPath);
            return;
        } else if (!folder.isDirectory()) {
            // throw new RuntimeException("path for audio folder is not a directory : " + folderPath);
            log.warn("path for audio folder is not a directory : " + folderPath);
            return;
        }
        
        if(!playlist.isEmpty()) {
            emptyPlaylist();
        }

        for (String file : folder.list((File dir, String name) -> {
            for (String ext : SUPPORTED_FILE_EXTENSIONS) {
                if (name.endsWith(ext)) {
                    return true;
                }
            }

            return false;
        })) {

            // file = file.replaceAll(" ", "%20");
            String filePath = folder.getPath() + File.separator + file;
            File currentFile = new File(filePath);
            playlist.add(createMediaPlayer(currentFile.toURI().toString()));
        }
    }

    /**
     * Stop current music playing (if any) then start the current selected music.
     */
    public void playPlayList() {

        if (playlist.isEmpty()) {
            return;
        }

        if (currentMusic != null) {
            stop();
        }

        final MediaPlayer nextMusic = playlist.get(currentMusicIndex);

        this.currentMusic = nextMusic;
        
        play();
    }

    public boolean isPlaying() {

        return this.isPlayingPoperty.getValue();
    }
    
    public BooleanProperty getIsPlayingProperty() {
        return isPlayingPoperty;
    }

    /**
     * Change the current selected music. If invalid index then nothing will be done.
     * If everything is correct, then it will play the newly selected music.
     * @param newMusicIndex The new index to use.
     */
    public void changeMusic(int newMusicIndex) {

        if (newMusicIndex < 0 || newMusicIndex >= playlist.size()) {
            return;
        }
        currentMusicIndex = newMusicIndex;
        //log.info("current index : {}", currentMusicIndex);
        playPlayList();
    }

    public void emptyPlaylist() {
        if (currentMusic != null) {
            currentMusic.stop();
        }
        playlist.clear();
    }

    public void pause() {
        this.isPlayingPoperty.setValue(false);
    }

    /**
     * If the music was paused, then play when it where in its timeline.
     */
    public void play() {
        
        if (currentMusic != null) {
            this.isPlayingPoperty.setValue(true);
        }
    }
    
    public void stop() {
        if (currentMusic != null) {
            if(isPlaying()) {
                pause();
            }
            currentMusic.stop();
        }
    }
    
    public void next() {
        currentMusicIndex = (currentMusicIndex + 1) % playlist.size();
        changeMusic(currentMusicIndex);
    }
    
    public void previous() {
        currentMusicIndex = (currentMusicIndex + playlist.size() - 1)
                    % playlist.size();
        changeMusic(currentMusicIndex);
    }

    public DoubleProperty volumeProperty() {
        return volume;
    }

    public void setVolume(double value) {
        if (value < 0) {
            throw new IllegalArgumentException("volume must be between 0 and 1");
        }
        if (value > 1) {
            throw new IllegalArgumentException("volume must be between 0 and 1");
        }
        this.volume.setValue(value);
    }

    public void playRemoteSound(String resourceUrlAsString) {
        
        Runnable asyncTask = () -> {
            
            MediaPlayer localMediaPlayer = getMediaPlayerFromSource(resourceUrlAsString);
            // If there is already the music in playlist, just play it
            if(localMediaPlayer == null) {
            
                // parse the URL early
                // in order to fail early if the URL is invalid
                URL resourceURL;
                try {
                    resourceURL = new URL(resourceUrlAsString);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("Invalid URL provided as sound resource : " + resourceUrlAsString, e);
                }

                final String resourceUrlExternalForm = resourceURL.toExternalForm();
                final File mediaFile = downloadAndGetFromCache(resourceURL, resourceUrlExternalForm);

                final String localResourceName = mediaFile.toURI().toString();
                log.info("Playing sound {}", localResourceName);

                try {
                   localMediaPlayer = createMediaPlayer(resourceUrlAsString);
                } catch (RuntimeException e) {
                    log.error("Exception while playing media file {} ", localResourceName, e);
                }
                    
            }
            
            if(localMediaPlayer != null) {
                playlist.add(localMediaPlayer);
                changeMusic(playlist.size() - 1);
            }
        };

        executorService.execute(asyncTask);
    }

    private File downloadAndGetFromCache(URL resourceURL, String resourceUrlExternalForm) {
        // the local cache filename is a Base64 encoding of the URL
        // so that we avoid name clash,
        // and so that we have the same local file for the same resource URL
        final Charset utf8 = Charset.forName("UTF-8");
        byte[] encodedUrl = Base64.getEncoder().encode(resourceUrlExternalForm.getBytes(utf8));
        final String localCacheFileName = new String(encodedUrl, utf8);

        File musicCacheFolder = new File("cache/music");
        File outputFile = new File(musicCacheFolder, localCacheFileName);

        if (!outputFile.exists()) {
            // use a temporary file while downloading
            // to avoid using corrupted file is the download is interrupted
            File tempOutputFile = new File(outputFile.getAbsolutePath() + ".downloading");
            if (tempOutputFile.exists()) {
                boolean tempOutputFileDeleted = tempOutputFile.delete();
                log.trace("tempOutputFileDeleted = {}", tempOutputFileDeleted);
            }
            try {
                log.info("Downloading music file {}", resourceURL);

                FileUtils.copyURLToFile(resourceURL, tempOutputFile, 10000, 10000);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            tempOutputFile.renameTo(outputFile);
            log.info("Finished downloading music file {}", resourceURL);
        } else {
            log.info("Found music file in cache for {}", resourceURL);
        }
        return outputFile;
    }

    private MediaPlayer createMediaPlayer(String source) {
        
        try {
            final Media media = new Media(source);
            final MediaPlayer player = new MediaPlayer(media);
            player.setOnError(() -> {
                log.error("error on audio media loading : " + player.getError());
            });
            player.volumeProperty().bind(volume);
            player.setOnEndOfMedia(() -> {
                next();
            });

            return player;
        } catch (MediaException e) {
            log.error("error while loading media {}, type : {}", source, e.getType(), e);
        }
        return null;
    }
    
    /**
     * Look through playlist and search for a corresponding mediaplayer
     * @param source The source to look for.
     * @return The media player found or null.
     */
    private MediaPlayer getMediaPlayerFromSource(final String source) {
        
        for(MediaPlayer mediaPlayer : playlist) {
            final Media media = mediaPlayer.getMedia();
            if(media.getSource().equals(source)) {
                return mediaPlayer;
            }
        }
        return null;
    }
}
