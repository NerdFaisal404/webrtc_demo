package com.dingsoft.webrtc.webrtcroom.activity;

import android.app.Activity;
import android.graphics.Point;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.dingsoft.webrtc.webrtcroom.R;
import com.dingsoft.webrtc.webrtcroom.webrtcmodule.PeerConnectionParameters;
import com.dingsoft.webrtc.webrtcroom.webrtcmodule.RtcListener;
import com.dingsoft.webrtc.webrtcroom.webrtcmodule.WebRtcClient;
import com.yanzhenjie.permission.Action;
import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.Permission;
import org.webrtc.EglBase;
import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoTrack;

import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity implements RtcListener,View.OnClickListener{
    //控件
    private EditText roomName;
    private Button openCamera;
    private Button switchCamera;
    private Button createRoom;
    private Button exitRoom;
    private SurfaceViewRenderer localSurfaceViewRenderer;
    private LinearLayout remoteVideoLl;
    private HashMap<String,View> remoteViews;
    //EglBase
    private EglBase rootEglBase;
    //WebRtcClient
    private WebRtcClient webRtcClient;
    //PeerConnectionParameters
    private PeerConnectionParameters peerConnectionParameters;
    //private String socketHost = "http://172.16.70.226:8081";
    private String socketHost = "https://172.16.70.226:8443";

    private long firstTime = 0;
    private boolean isCameraOpen = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        roomName =  findViewById(R.id.room);
        openCamera = findViewById(R.id.openCamera);
        openCamera.setOnClickListener(this);
        switchCamera =  findViewById(R.id.switchCamera);
        switchCamera.setOnClickListener(this);
        createRoom = findViewById(R.id.create);
        createRoom.setOnClickListener(this);
        exitRoom = findViewById(R.id.exit);
        exitRoom.setOnClickListener(this);
        localSurfaceViewRenderer = findViewById(R.id.localVideo);
        remoteVideoLl = findViewById(R.id.remoteVideoLl);
        remoteViews = new HashMap<>();
        createWebRtcClient();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isCameraOpen){
            webRtcClient.startCamera(localSurfaceViewRenderer,WebRtcClient.FONT_FACTING);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        localSurfaceViewRenderer.release();
        localSurfaceViewRenderer = null;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        switch (keyCode){
            case KeyEvent.KEYCODE_BACK:
                long secondTime=System.currentTimeMillis();
                if(secondTime-firstTime>2000){
                    Toast.makeText(MainActivity.this,"再按一次退出程序",Toast.LENGTH_SHORT).show();
                    firstTime = secondTime;
                    return true;
                }else{
                    System.exit(0);
                }
                break;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.openCamera:
                if(isCameraOpen){
                    webRtcClient.closeCamera();
                    localSurfaceViewRenderer.clearImage();
                    localSurfaceViewRenderer.setBackground(new ColorDrawable(getResources().getColor(R.color.colorBlack)));
                    //localSurfaceViewRenderer.setForeground(new ColorDrawable(R.color.colorBlack));
                    localSurfaceViewRenderer.release();
                    isCameraOpen = false;
                    openCamera.setText("");
                }else{
                    openCamera();
                }
                break;
            case R.id.switchCamera:
                switchCamera();
                break;
            case R.id.create:

                String roomId = roomName.getText().toString();
                if(isCameraOpen){
                    webRtcClient.createAndJoinRoom(roomId);
                    createRoom.setEnabled(false);
                }else{
                    Toast.makeText(this,"",Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.exit:
                webRtcClient.exitRoom();
                createRoom.setEnabled(true);
                break;
            default:
                break;
        }
    }

    private void createPeerConnectionParameters(){
        Point displaySize = new Point();
        this.getWindowManager().getDefaultDisplay().getSize(displaySize);
        displaySize.set(480,320);
        peerConnectionParameters =  new PeerConnectionParameters(true, false,
                    false, displaySize.x, displaySize.y, 30,
                    0, "VP8",
                    true,false,0,"OPUS",
                    false,false,false,false,false,false,
                    false,false,false,false);
    }

    //webRtcClient
    private void createWebRtcClient(){
        createPeerConnectionParameters();
        rootEglBase = EglBase.create();
        webRtcClient = new WebRtcClient(getApplicationContext(),
                rootEglBase,
                peerConnectionParameters,
                MainActivity.this,
                socketHost);
    }

    private void openCamera(){
        if(AndPermission.hasPermissions(this,Permission.Group.CAMERA)){
            startCamera();
        }else{
            AndPermission.with(this)
                    .runtime()
                    .permission(Permission.Group.CAMERA)
                    .onGranted(new Action<List<String>>() {
                        @Override
                        public void onAction(List<String> data) {
                            startCamera();
                        }
                    })
                    .onDenied(new Action<List<String>>() {
                        @Override
                        public void onAction(List<String> data) {
                            Toast.makeText(MainActivity.this, "", Toast.LENGTH_SHORT).show();
                        }
                    }).start();
        }
    }


    private void startCamera(){

        localSurfaceViewRenderer.init(rootEglBase.getEglBaseContext(), null);

        localSurfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
        localSurfaceViewRenderer.setZOrderMediaOverlay(true);
        localSurfaceViewRenderer.setEnableHardwareScaler(false);
        localSurfaceViewRenderer.setMirror(true);
        localSurfaceViewRenderer.setBackground(null);

        webRtcClient.startCamera(localSurfaceViewRenderer,WebRtcClient.FONT_FACTING);

        isCameraOpen = true;
        openCamera.setText("");
    }


    private void switchCamera(){
        if (webRtcClient != null){
            webRtcClient.switchCamera();
        }
    }


    public void clearRemoteCamera(){
        remoteVideoLl.removeAllViews();
    }

    /* RtcListener */
    @Override
    public void onAddRemoteStream(String peerId,VideoTrack videoTrack) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceViewRenderer remoteView = new SurfaceViewRenderer(MainActivity.this);
                remoteView.init(rootEglBase.getEglBaseContext(), null);
                remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FIT);
                remoteView.setZOrderMediaOverlay(true);
                remoteView.setEnableHardwareScaler(false);
                remoteView.setMirror(true);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(360,360);
                layoutParams.topMargin = 20;
                remoteVideoLl.addView(remoteView,layoutParams);
                remoteViews.put(peerId,remoteView);
                //VideoTrack videoTrack = mediaStream.videoTracks.get(0);
                videoTrack.addSink(remoteView);
            }
        });
    }

    @Override
    public void onRemoveRemoteStream(String peerId) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SurfaceViewRenderer remoteView = (SurfaceViewRenderer)remoteViews.get(peerId);
                if (remoteView != null){
                    remoteVideoLl.removeView(remoteView);
                    remoteViews.remove(peerId);
                    remoteView.release();
                    remoteView = null;
                }
            }
        });
    }
}
