import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
import java.io.FileInputStream;
import java.io.BufferedInputStream;

public class MusicPlayer {
    private String filePath;
    private AdvancedPlayer player;
    private Thread playerThread;
    private boolean running;
    private boolean looping;

    public MusicPlayer(String filePath) {
        this.filePath = filePath;
        this.running = false;
        this.looping = true;
    }

    public void play() {
        if (running) return;
        running = true;
        looping = true;

        playerThread = new Thread(() -> {
            while (running && looping) {
                try {
                    FileInputStream fis = new FileInputStream(filePath);
                    BufferedInputStream bis = new BufferedInputStream(fis);
                    player = new AdvancedPlayer(bis);

                    player.setPlayBackListener(new PlaybackListener() {
                        @Override
                        public void playbackFinished(PlaybackEvent evt) {
                            // Will loop back in the while
                        }
                    });

                    player.play();
                    // Player finished, if still looping it will restart
                } catch (Exception e) {
                    System.err.println("Erro ao tocar musica: " + e.getMessage());
                    running = false;
                }
            }
        });
        playerThread.setDaemon(true);
        playerThread.start();
    }

    public void stop() {
        running = false;
        looping = false;
        if (player != null) {
            player.close();
        }
        if (playerThread != null) {
            playerThread.interrupt();
        }
    }

    public boolean isPlaying() {
        return running;
    }
}
