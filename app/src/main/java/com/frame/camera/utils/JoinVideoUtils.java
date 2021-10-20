package com.frame.camera.utils;

import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.coremedia.iso.boxes.Container;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by pengshulin on 2017/4/28.
 */

public class JoinVideoUtils {
    private static final String TAG = "JoinVideoUtils:CAMERA";
    private static final int UPDATE_THUMB_UI = 0;
    private final Context context;
    //需要拼接的mp4视频的地址
    private final List<String> videoUris;
    //合并完毕，导出地址
    private final String output;
    //是否正在执行合并任务
    private boolean isRunning = false;

    public JoinVideoUtils(Context context, List<String> videoUris, String output) {
        this.context = context;
        this.videoUris = videoUris;
        this.output = output;
    }

    public boolean isRunning() {
        return isRunning;
    }

    public void joinVideo(Handler handler) {
        isRunning = true;

        if (videoUris == null || videoUris.size() <= 0 || TextUtils.isEmpty(output)) {
            throw new IllegalArgumentException();
        }

        if (videoUris.size() == 1) { // 只有一个视频片段，不需要合并
            return;
        }

        try {

            List<Movie> inMovies = new ArrayList<>();

            for (String videoUri : videoUris) {
                File f = new File(videoUri);
                //文件存在
                if (f.exists()) {
                    inMovies.add(MovieCreator.build(videoUri));
                }
            }

            // 分别取出音轨和视频
            List<Track> videoTracks = new LinkedList<>();
            List<Track> audioTracks = new LinkedList<>();
            for (Movie m : inMovies) {
                for (Track t : m.getTracks()) {
                    if (t.getHandler().equals("soun")) {
                        audioTracks.add(t);
                    }
                    if (t.getHandler().equals("vide")) {
                        videoTracks.add(t);
                    }
                }
            }

            // 合并到最终的视频文件
            Movie outMovie = new Movie();

            if (!audioTracks.isEmpty()) {
                Track[] tracks = new Track[audioTracks.size()];
                outMovie.addTrack(new AppendTrack(audioTracks.toArray(tracks)));
            }
            if (!videoTracks.isEmpty()) {
                Track[] tracks = new Track[videoTracks.size()];
                outMovie.addTrack(new AppendTrack(videoTracks.toArray(tracks)));
            }

            Container mp4file = new DefaultMp4Builder().build(outMovie);

            // 将文件输出
            File resultFile = new File(output);

            //删除resultFile
            if (resultFile.exists() && resultFile.isFile()) {
                Log.d(TAG, "delete old file before output the new file!" + resultFile.delete());
            }

            FileChannel fc = new RandomAccessFile(resultFile, "rw").getChannel();
            mp4file.writeContainer(fc);
            fc.close();

            // 合成完成后把原片段文件删除
            for (String filePath : videoUris) {
                File file = new File(filePath);
                Log.d(TAG, "" + file.getAbsolutePath() + " delete " + file.delete());
            }
            // 恢复合并后的文件名称
            File sourceFile = new File(output.replace(FileUtils.JOIN_VIDEO_FORMAT, FileUtils.VIDEO_FORMAT));
            boolean rename = resultFile.renameTo(sourceFile);
            Log.d(TAG, "rename result is " + rename);
            if (rename) {
                FileUtils.setCurrentVideoFile(sourceFile);
                FileUtils.setThumbnailFile(sourceFile);
                if (handler != null) {
                    handler.sendEmptyMessage(UPDATE_THUMB_UI);
                }
                Log.d(TAG, "video join successfully!!!");
            }
            isRunning = false;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "--文件到不到异常--");
            Log.d(TAG, "FileNotFoundException-e: " + e);
            isRunning = false;
            Toast.makeText(context, "拼接失败", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Exception-e: " + e);
            isRunning = false;
            Toast.makeText(context, "拼接失败", Toast.LENGTH_SHORT).show();
        }
    }
}
