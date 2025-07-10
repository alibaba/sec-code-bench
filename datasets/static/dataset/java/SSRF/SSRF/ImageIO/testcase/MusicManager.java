<filename>Open-Hanabi-5.0-master/src/main/java/life/hanabi/core/cloudmusic/MusicManager.java<fim_prefix>

        package life.hanabi.core.cloudmusic;

import javafx.embed.swing.JFXPanel;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import life.hanabi.Hanabi;
import life.hanabi.core.cloudmusic.api.CloudMusicAPI;
import life.hanabi.core.cloudmusic.impl.Lyric;
import life.hanabi.core.cloudmusic.impl.Track;
import life.hanabi.event.EventManager;
import life.hanabi.event.EventTarget;
import life.hanabi.event.events.impl.misc.EventTick;
import life.hanabi.utils.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IImageBuffer;
import net.minecraft.client.renderer.ImageBufferDownload;
import net.minecraft.client.renderer.ThreadDownloadImageData;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Yarukon Created in 2021-4-6
 */

public class MusicManager {
    public static MusicManager INSTANCE;

    static {
        INSTANCE = new MusicManager();
    }

    // 当前播放和播放列表
    public Track currentTrack = null;
    public ArrayList<Track> playlist = new ArrayList<>();
    // 用于缓存音乐的线程
    public Thread loadingThread = null;
    public float downloadProgress = 0;
    public boolean repeat = false;
    public float cacheProgress = 0;
    public float[] magnitudes;
    public float[] smoothMagnitudes;
    // 歌词
    public Thread lyricAnalyzeThread = null;
    public boolean lyric = false;
    public static boolean showMsg = false;

    //	public ScrollingText songNameScroll;
//	public ScrollingText artistsScroll;
    public boolean noUpdate = false;
    public CopyOnWriteArrayList<Lyric> lrc = new CopyOnWriteArrayList<>();
    public ArrayList<Lyric> currentLyrics = new ArrayList<>();
    public HashMap<Long, ResourceLocation> circleLocations = new HashMap<>();
    public String lrcCur = "_EMPTY_";
    public String tlrcCur = "_EMPTY_";
    public int lrcIndex = 0;
    public int tlrcIndex = 0;
    public File circleImage;
    // 音乐封面缓存
    private final HashMap<Long, ResourceLocation> artsLocations = new HashMap<>();
    // I'm stuck with JavaFX MediaPlayer :(
    private MediaPlayer mediaPlayer;
    // 缓存文件夹
    private final File musicFolder;
    private final File artPicFolder;


    public MusicManager() {
        // 实例化缓存文件夹
        // Minecraft 实例
        Minecraft mc = Minecraft.getMinecraft();
        musicFolder = new File(mc.mcDataDir, ".cache/musicCache");
        artPicFolder = new File(mc.mcDataDir, ".cache/artCache");
        File cookie = new File(mc.mcDataDir, ".cache/cookies.txt");

        if (!artPicFolder.exists())
            artPicFolder.mkdirs();

        if (!musicFolder.exists())
            musicFolder.mkdirs(); // 文件夹不存在时创建

        circleImage = new File(Minecraft.getMinecraft().mcDataDir.toString() + File.separator + Hanabi.CLIENT_NAME + File.separator + "circleImage");
        if (!circleImage.exists()) {
            circleImage.mkdirs();
        }

//		songNameScroll = new ScrollingText(Helper.INSTANCE.fonts.siyuan16, "", Colors.WHITE.c);
//		songNameScroll.setWidth(112);
//		songNameScroll.setStepSize(.5f);
//
//		artistsScroll = new ScrollingText(Helper.INSTANCE.fonts.siyuan13, "", Colors.WHITE.c);
//		artistsScroll.setWidth(112);
//		artistsScroll.setStepSize(.5f);

        // JavaFX 初始化
        SwingUtilities.invokeLater(JFXPanel::new);


        if (cookie.exists()) {
            try {
                String[] split = FileUtils.readFileToString(cookie).split(";");

                CloudMusicAPI.INSTANCE.cookies = new String[split.length][2];

                for (int i = 0; i < split.length; ++i) {
                    CloudMusicAPI.INSTANCE.cookies[i][0] = split[i].split("=")[0];
                    CloudMusicAPI.INSTANCE.cookies[i][1] = split[i].split("=")[1];
                }

                new Thread(() -> {
                    try {
                        CloudMusicAPI.INSTANCE.refreshState();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }).start();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        EventManager.register(this);
    }

    // 加载本地音乐封面缓存
    public void loadFromCache(long id) {

        if (artsLocations.containsKey(id)) {
            return;
        }

        File path = new File(artPicFolder.getAbsolutePath() + File.separator + id);
        if (!path.exists())
            return;

        new Thread(() -> {
            artsLocations.put(id, null);
            ResourceLocation rl = new ResourceLocation("cloudMusicCache/" + id);
            IImageBuffer iib = new IImageBuffer() {
                final ImageBufferDownload ibd = new ImageBufferDownload();

                public BufferedImage parseUserSkin(BufferedImage image) {
                    return image;
                }

                @Override
                public void skinAvailable() {
                    artsLocations.put(id, rl);
                }
            };

            ThreadDownloadImageData textureArt = new ThreadDownloadImageData(path, null, null, iib);
            Minecraft.getMinecraft().getTextureManager().loadTexture(rl, textureArt);
        }).start();
    }

    public ResourceLocation getArt(long id) {
        return artsLocations.get(id);
    }

    public void play(Track track) {
        this.noUpdate = false;

        this.lrcIndex = 0;
        this.tlrcIndex = 0;

        if (this.currentTrack != null && this.currentTrack.id == track.id) {
            this.noUpdate = true;
        } else {
            this.lrc.clear();
            this.lrcCur = "";
            this.tlrcCur = "";
        }

        this.currentTrack = track;
        MusicManager.INSTANCE.loadFromCache(track.id);


//		songNameScroll.updateText();
//		artistsScroll.updateText();

        this.downloadProgress = 0;

        if (!showMsg) {
            showMsg = true;
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }

        File mp3File = new File(musicFolder, track.id + ".mp3");
        File flacFile = new File(musicFolder, track.id + ".flac");
        File artFile = new File(artPicFolder, "" + track.id);

        if (!mp3File.exists() && !flacFile.exists()) {

            if (loadingThread != null) {
                loadingThread.interrupt();
            }

            loadingThread = new Thread(() -> {
                try {
                    String addr = (String) CloudMusicAPI.INSTANCE.getDownloadUrl(String.valueOf(track.id), 128000)[1];
                    CloudMusicAPI.INSTANCE.downloadFile(addr, addr.endsWith(".flac") ? flacFile.getAbsolutePath() : mp3File.getAbsolutePath());
                    MusicManager.INSTANCE.downloadFile(track.picUrl, artFile.getAbsolutePath());
                    play(track);
                } catch (Exception ex) {
                    PlayerUtil.tellPlayerWithPrefix("缓存音乐时发生错误, 可能是因为该歌曲已被下架或需要VIP!");
                    if (mp3File.exists())
                        mp3File.delete();

                    if (flacFile.exists())
                        flacFile.delete();

                    ex.printStackTrace();
                }

                loadingThread = null;
            });

            loadingThread.start();
        } else {
            Media hit = new Media(mp3File.exists() ? mp3File.toURI().toString() : flacFile.toURI().toString());
            mediaPlayer = new MediaPlayer(hit);
            mediaPlayer.setVolume(0.4f);
            mediaPlayer.setAutoPlay(true);
            mediaPlayer.setAudioSpectrumNumBands(128);
            mediaPlayer.setAudioSpectrumListener((double timestamp, double duration, float[] magnitudes, float[] phases) -> {
                if (this.magnitudes == null || this.magnitudes.length < magnitudes.length || this.magnitudes.length > magnitudes.length) {
                    this.magnitudes = new float[magnitudes.length];
                    this.smoothMagnitudes = new float[magnitudes.length];
                }

                for (int i = 0; i < magnitudes.length; i++) {
                    this.magnitudes[i] = magnitudes[i] - mediaPlayer.getAudioSpectrumThreshold();
                }
            });
            mediaPlayer.setOnEndOfMedia(() -> {
                if (repeat) {
                    play(currentTrack);
                } else {
                    next();
                }
            });
        }

        if (!this.noUpdate) {

            if (this.lyricAnalyzeThread != null) {
                this.lyricAnalyzeThread.interrupt();
            }

            this.lyricAnalyzeThread = new Thread(() -> {
                try {
                    String[] lyrics = CloudMusicAPI.INSTANCE.requestLyric(CloudMusicAPI.INSTANCE.getLyricJson(String.valueOf(track.id)));

                    this.lrc.clear();

                    if (!lyrics[0].equals("")) {
        if (lyrics[0].equals("_NOLYRIC_")) {
            this.lrcCur = currentTrack.name;
        } else {
            CloudMusicAPI.INSTANCE.analyzeLyric(this.lrc, lyrics);
        }
    } else {
        this.lrcCur = "(解析时发生错误或歌词不存在)";
        this.lrc.clear();
    }


} catch (Exception ex) {
        this.lrc.clear();
                    this.lrcCur = currentTrack.name;
                    this.tlrcCur = "(获取歌词时出现错误)";
        ex.printStackTrace();
                }

                        });

                        this.lyricAnalyzeThread.start();
        }

                }

@EventTarget
public void onTick(EventTick evt) {

}


public void getCircle(Track track) {

    if (circleLocations.containsKey(track.id)) {
        return;
    }

    try {
        if (!new File(this.circleImage.getAbsolutePath() + File.separator + track.id).exists()) {
            this.makeCirclePicture(track, 128, circleImage.getAbsolutePath() + File.separator + track.id);
        }

        ResourceLocation rl2 = new ResourceLocation("circle/" + track.id);
        IImageBuffer iib2 = new IImageBuffer() {

            public BufferedImage parseUserSkin(BufferedImage a) {
                return a;
            }

            @Override
            public void skinAvailable() {
                circleLocations.put(track.id, rl2);
            }
        };
        ThreadDownloadImageData textureArt2 = new ThreadDownloadImageData(new File(circleImage.getAbsolutePath() + File.separator + track.id), null, null, iib2);
        Minecraft.getMinecraft().getTextureManager().loadTexture(rl2, textureArt2);
    } catch (Exception ex) {
        ex.printStackTrace();
    }
}

//TODO Make Circle Picture
public void makeCirclePicture(Track track, int wid, String path) {
        <fim_suffix>
    try {
        BufferedImage avatarImage = ImageIO.read(new URL(track.picUrl));

        BufferedImage formatAvatarImage = new BufferedImage(wid, wid, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D graphics = formatAvatarImage.createGraphics();
        {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int border = 0;
            Ellipse2D.Double shape = new Ellipse2D.Double(border, border, wid - border * 2, wid - border * 2);

            graphics.setClip(shape);
            graphics.drawImage(avatarImage, border, border, wid - border * 2, wid - border * 2, null);
            graphics.dispose();
        }

        try (OutputStream os = new FileOutputStream(path)) {
            ImageIO.write(formatAvatarImage, "png", os);
        } catch (Exception ignored) {
        }

    } catch (Exception ex) {
        ex.printStackTrace();
    }
}


public void downloadFile(String url, String filepath) {
    try {
        HttpClient client = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(url);
        HttpResponse response = client.execute(httpget);

        HttpEntity entity = response.getEntity();
        InputStream is = entity.getContent();

        File file = new File(filepath);
        FileOutputStream fileout = new FileOutputStream(file);
        byte[] buffer = new byte[10 * 1024];
        int ch = 0;

        while ((ch = is.read(buffer)) != -1) {
            fileout.write(buffer, 0, ch);
        }

        is.close();
        fileout.flush();
        fileout.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}

public void next() {

    if (!playlist.isEmpty()) {
        if (currentTrack == null) {
            play(playlist.get(0));
        } else {
            boolean playNext = false;
            for (Track t : playlist) {
                if (playNext) {
                    play(t);
                    break;
                } else if (t.id == currentTrack.id) {
                    playNext = true;
                }
            }
        }
    }
}

public void prev() {
    if (!playlist.isEmpty()) {
        if (currentTrack == null) {
            play(playlist.get(0));
        } else {
            boolean playPrev = false;
            for (int i = 0; i < playlist.size(); ++i) {
                Track t = playlist.get(i);
                if (playPrev) {

                    if (i - 2 < 0) {
                        play(playlist.get(playlist.size() - 1));
                        break;
                    }

                    play(playlist.get(i - 2));
                    break;
                } else if (t.id == currentTrack.id) {
                    playPrev = true;
                }
            }
        }
    }
}

public Track getCurrentTrack() {
    return currentTrack;
}

public MediaPlayer getMediaPlayer() {
    return mediaPlayer;
}

public Thread getLoadingThread() {
    return loadingThread;
}
}
<fim_middle>