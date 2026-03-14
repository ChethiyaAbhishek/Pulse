package com.ecads.pulse.android.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.FragmentVideoCallBinding;

public class VideoCallFragment extends Fragment {

    private FragmentVideoCallBinding binding;
    private String myAppId = "bf77db5c8aea4ce8bc554316cb246b84";
    private RtcEngine mRtcEngine;
    private boolean micMute;
    private boolean videoOff;

    private String channelName = "PatientVideoConsultationChannel";
    private String token = "007eJxTYPjvJTS36f9Xa58plY5WHVe57GWjPnyefsrqrXzUlb9Z3y8oMCSlmZunJJkmWySmJpokp1okJZuamhgbmiUnGZmYJVmYTFy/NbMhkJHho2kyEyMDBIL48gwBiSWZqXklYZkpqfnO+XnFpTklQJH8POeMxLy81BwGBgBY2C1M";

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        // Triggered when the local user successfully joins the specified channel.
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            Toast.makeText(requireActivity(), "Joined channel " + channel, Toast.LENGTH_SHORT).show();
            Log.i("AGORA", " ------------------------------------------------------");
            Log.i("AGORA", "Joined channel " + channel + " ------------------------------------------------------");
            Log.i("AGORA", " ------------------------------------------------------");

        }

        // Triggered when a remote user/host joins the channel.
        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            getActivity().runOnUiThread(() -> {
                // Initialize and display remote video view for the new user.
                setupRemoteVideo(uid);
                Toast.makeText(requireActivity(), "User joined: " + uid, Toast.LENGTH_SHORT).show();
                Log.i("AGORA", " ------------------------------------------------------");
                Log.i("AGORA", "User joined: " + uid + " ------------------------------------------------------");
                Log.i("AGORA", " ------------------------------------------------------");

            });
        }

        // Triggered when a remote user/host leaves the channel.
        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            getActivity().runOnUiThread(() -> {
                Toast.makeText(requireActivity(), "User offline: " + uid, Toast.LENGTH_SHORT).show();
                Log.i("AGORA", "User offline: " + uid + " ------------------------------------------------------");
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {

            getActivity().runOnUiThread(() -> {
                if (state == Constants.REMOTE_VIDEO_STATE_STOPPED
                        || state == Constants.REMOTE_VIDEO_STATE_FROZEN) {
                    //video off
                    binding.remoteVideoViewContainer.setVisibility(View.GONE);
                    binding.remoteVideoOffView.setVisibility(View.VISIBLE);

                } else if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                    //video off
                    binding.remoteVideoViewContainer.setVisibility(View.VISIBLE);
                    binding.remoteVideoOffView.setVisibility(View.GONE);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentVideoCallBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        startVideoCalling();
        micMute = false;

        binding.videoCallMicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (micMute) {
                    mRtcEngine.muteLocalAudioStream(false);
                    micMute = false;
                    binding.videoCallMicBtn.setImageResource(R.drawable.audio_icon);
                } else {
                    mRtcEngine.muteLocalAudioStream(true);
                    micMute = true;
                    binding.videoCallMicBtn.setImageResource(R.drawable.mic_mute_icon);
                }
            }
        });

        binding.videoCallVideoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (videoOff) {
                    mRtcEngine.muteLocalVideoStream(false);
                    videoOff = false;
                    mRtcEngine.startPreview();
                    binding.localVideoOffView.setVisibility(View.GONE);
                    binding.localVideoViewContainer.setVisibility(View.VISIBLE);
                    binding.videoCallVideoBtn.setImageResource(R.drawable.video_icon);
                } else {
                    mRtcEngine.muteLocalVideoStream(true);
                    videoOff = true;
                    mRtcEngine.stopPreview();
                    binding.localVideoOffView.setVisibility(View.VISIBLE);
                    binding.localVideoViewContainer.setVisibility(View.GONE);
                    binding.videoCallVideoBtn.setImageResource(R.drawable.video_off_icon);
                }
            }
        });

        binding.videoCallHangUpBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cleanupAgoraEngine();
                requireActivity().finish();
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupAgoraEngine();
    }

    private void startVideoCalling() {
        initializeAgoraVideoSDK();
        enableVideo();
        setupLocalVideo();
        joinChannel();
    }

    private void initializeAgoraVideoSDK() {
        try {
            RtcEngineConfig config = new RtcEngineConfig();
            config.mContext = getActivity().getBaseContext();
            config.mAppId = myAppId;
            config.mEventHandler = mRtcEventHandler;
            mRtcEngine = RtcEngine.create(config);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing RTC engine: " + e.getMessage());
        }
    }

    private void joinChannel() {
        ChannelMediaOptions options = new ChannelMediaOptions();
        options.clientRoleType = Constants.CLIENT_ROLE_BROADCASTER;
        options.channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION;
        options.publishCameraTrack = true;
        options.publishMicrophoneTrack = true;
        mRtcEngine.joinChannel(token, channelName, 0, options);
    }

    private void enableVideo() {
        mRtcEngine.enableVideo();
        mRtcEngine.startPreview();
    }

    private void setupLocalVideo() {
        FrameLayout container = binding.localVideoViewContainer;
        SurfaceView surfaceView = new SurfaceView(getActivity().getBaseContext());
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = binding.remoteVideoViewContainer;
        SurfaceView surfaceView = new SurfaceView(getActivity().getBaseContext());
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupRemoteVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid));
    }

    private void cleanupAgoraEngine() {
        if (mRtcEngine != null) {
            mRtcEngine.stopPreview();
            mRtcEngine.leaveChannel();
            mRtcEngine = null;
        }
    }

}