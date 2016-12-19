package com.fanserg.ffmpegtest;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.bytedeco.javacv.FFmpegFrameFilter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.FFmpegLogCallback;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameFilter;
import org.bytedeco.javacv.FrameGrabber;
import org.bytedeco.javacv.FrameRecorder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_H264;
import static org.bytedeco.javacpp.avcodec.AV_CODEC_ID_MPEG4;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private Button bRun;
    private TextView tvReport;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        initReLinker();

        bRun = (Button) findViewById(R.id.b_run);
        tvReport = (TextView) findViewById(R.id.tv_report);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);

        tvReport.setText(getString(R.string.main_activity_report, ""));

        bRun.setOnClickListener(this);
    }

//    private void initReLinker() {
//        ReLinker.Logger logger = new ReLinker.Logger() {
//            @Override
//            public void log(String message) {
//                Log.v(TAG, "initReLinker, message : " + message);
//            }
//        };
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniavutil");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniswresample");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniavcodec");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniavformat");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniswscale");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jnipostproc");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniavfilter");
//        ReLinker.log(logger).recursively().loadLibrary(getApplicationContext(), "jniavdevice");
//    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.b_run :
                buttonRunClick();
                break;
        }
    }

    private void buttonRunClick() {
        tvReport.setText(getString(R.string.main_activity_report, getString(R.string.main_activity_processing)));
        progressBar.setVisibility(View.VISIBLE);
        bRun.setEnabled(false);

        new RecordAsyncTask().execute();
    }

    private class RecordAsyncTask extends AsyncTask<Integer, Void, String> {
        protected String doInBackground(Integer... params) {
            try {
                return runGrab();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return null;
        }

        protected void onPostExecute(String result) {
            finishGrab(result);
        }
    }

    private void finishGrab(String result) {
        progressBar.setVisibility(View.GONE);
        tvReport.setText(getString(R.string.main_activity_report, result));
        bRun.setEnabled(true);
    }

    private String runGrab() {
        FFmpegLogCallback.set();

        //String mainVideoLink = "/storage/emulated/0/video.mp4";
        String mainVideoLink = copyFileToExternal("video");
        Log.d(TAG, "buttonRunClick, mainVideoLink : " + mainVideoLink);
        if (mainVideoLink == null) return "mainVideoLink not found";
        String imageLink = "/storage/emulated/0/image.png";
        String outputVideoLink = "/storage/emulated/0/out.mp4";

        String reportResult = "";


        int videoWidth = 480;
        int videoHeight = 640; // 640, 850

        //int inFormat = avutil.AV_PIX_FMT_YUV420P;

        try {

            FFmpegFrameGrabber grabberMain = new FFmpegFrameGrabber(mainVideoLink);
            grabberMain.setFormat("mp4");
            //grabberMain.setVideoCodec(AV_CODEC_ID_MPEG4);
            //grabberMain.setPixelFormat(inFormat);
            grabberMain.start();

            FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputVideoLink, videoWidth, videoHeight, grabberMain.getAudioChannels());
            //recorder.setPixelFormat(inFormat);
            //recorder.setVideoCodec(AV_CODEC_ID_H264);
            recorder.setVideoQuality(3);
            recorder.start();

//            String filterStr = "movie=" + imageLink + " [effect];[in][effect]overlay=format=rgb [out]";
//            String filterStr = "color=color=red@.3:size=320x480 [over]; [in][over] overlay=format=yuv420 [out]";
            String filterStr = "color=color=red@.3:size=320x480 [over]; [in][over] overlay=format=rgb [out]";
//            String filterStr = "[in]transpose=1[out]";
//            String filterStr = "[in]rotate=PI[out]";
            FFmpegFrameFilter filter = new FFmpegFrameFilter(filterStr, videoWidth, videoHeight);
            filter.start();

            Frame frame;
            Frame frame2;
            Log.v(TAG, "buttonRunClick, start");
            while ((frame = grabberMain.grab()) != null) {
                if (frame.image != null) {
                    filter.push(frame, grabberMain.getPixelFormat());
                    while ((frame2 = filter.pull()) != null) {
                        recorder.setTimestamp(grabberMain.getTimestamp());
                        recorder.record(frame2, grabberMain.getPixelFormat());
                    }
                } else {
                    recorder.setTimestamp(grabberMain.getTimestamp());
                    recorder.record(frame, grabberMain.getPixelFormat());
                }
            }
            Log.v(TAG, "buttonRunClick, stop");

            filter.stop();
            recorder.stop();
            grabberMain.stop();

            reportResult = getString(R.string.main_activity_successful_result);

        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
            reportResult = e.getMessage();
        } catch (FrameRecorder.Exception e) {
            e.printStackTrace();
            reportResult = e.getMessage();
        } catch (FrameFilter.Exception e) {
            e.printStackTrace();
            reportResult = e.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            reportResult = e.getMessage();
        }

        return reportResult;
    }

    public String copyFileToExternal(String fileName) {
        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/");
        if (!folder.exists()){
            return null;
        }

        String filePath = folder.getAbsolutePath() + "/" + fileName + ".mp4";
        InputStream in = getResources().openRawResource(R.raw.video);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(filePath);

            byte[] buff = new byte[1024];
            int read = 0;
            while ((read = in.read(buff)) > 0 ) {
                out.write(buff, 0, read);
            }
            in.close();
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return filePath;
    }
}
