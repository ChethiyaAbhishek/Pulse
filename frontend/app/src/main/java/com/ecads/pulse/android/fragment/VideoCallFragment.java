package com.ecads.pulse.android.fragment;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import io.agora.rtc2.Constants;
import io.agora.rtc2.IRtcEngineEventHandler;
import io.agora.rtc2.RtcEngine;
import io.agora.rtc2.RtcEngineConfig;
import io.agora.rtc2.video.VideoCanvas;
import io.agora.rtc2.ChannelMediaOptions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

import com.ecads.pulse.android.R;
import com.ecads.pulse.android.databinding.FragmentVideoCallBinding;
import com.ecads.pulse.android.dto.AppointmentStatusUpdateDto;
import com.ecads.pulse.android.listener.CallBack;
import com.ecads.pulse.android.network.AppointmentApiService;
import com.ecads.pulse.android.network.ChannelingApiService;
import com.ecads.pulse.android.network.RetrofitClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class VideoCallFragment extends Fragment {

    private FragmentVideoCallBinding binding;
    private String myAppId = "bf77db5c8aea4ce8bc554316cb246b84";
    private RtcEngine mRtcEngine;
    private boolean micMute;
    private boolean videoOff;
    private ConstraintLayout videoCallView;
    private LinearLayout waitingRoomView;
    private String consultantName;
    private String appointmentId;
    private String slot;
    private boolean endSession;
    private Handler timerHandler;
    private Runnable timerRunnable;
    private Retrofit retrofit;
    private FirebaseAuth firebaseAuth;


    private String channelName;
    private String token;

    private final IRtcEngineEventHandler mRtcEventHandler = new IRtcEngineEventHandler() {
        //local user
        @Override
        public void onJoinChannelSuccess(String channel, int uid, int elapsed) {
            super.onJoinChannelSuccess(channel, uid, elapsed);
            Toast.makeText(requireActivity(), "Joined channel " + channel, Toast.LENGTH_SHORT).show();
            Log.i("VideoCallFragment", "Joined Channel: " + channel);

            LocalTime sessionStartsAt = LocalTime.parse(slot, DateTimeFormatter.ofPattern("hh:mm a"));
            long millisToSessionStart = Duration.between(LocalTime.now(), sessionStartsAt).toMillis();

            if (millisToSessionStart > 0) {
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        endSession = true;
                        startTimer();
                    }
                }, millisToSessionStart);
            } else {
                endSession = true;
                startTimer();
            }
        }

        //remote user
        @Override
        public void onUserJoined(int uid, int elapsed) {
            super.onUserJoined(uid, elapsed);
            requireActivity().runOnUiThread(() -> {
                setupRemoteVideo(uid);
                Toast.makeText(requireActivity(), "Consultant joined", Toast.LENGTH_SHORT).show();
                Log.i("VideoCallFragment", "Consultant joined: " + uid);
                videoCallView.setVisibility(View.VISIBLE);
                waitingRoomView.setVisibility(View.GONE);

                endSession = false;
                stopTimer();
                binding.consultantConnectionLost.setVisibility(View.GONE);
            });
        }

        //remote user left
        @Override
        public void onUserOffline(int uid, int reason) {
            super.onUserOffline(uid, reason);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireActivity(), "Consultant left", Toast.LENGTH_SHORT).show();
                Log.i("VideoCallFragment", "Consultant left: " + uid);
                videoCallView.setVisibility(View.GONE);
                waitingRoomView.setVisibility(View.VISIBLE);

                if (reason == 0) {
                    cleanupAgoraEngine();
                    requireActivity().finish();
                    notifySessionEnded(0);
                } else {
                    endSession = true;
                    startTimer();
                    binding.consultantConnectionLost.setVisibility(View.VISIBLE);
                }
            });
        }

        @Override
        public void onRemoteVideoStateChanged(int uid, int state, int reason, int elapsed) {

            requireActivity().runOnUiThread(() -> {
                if (state == Constants.REMOTE_VIDEO_STATE_STOPPED
                        || state == Constants.REMOTE_VIDEO_STATE_FROZEN) {
                    //video off
                    binding.remoteVideoViewContainer.setVisibility(View.GONE);
                    binding.remoteVideoOffView.setVisibility(View.VISIBLE);
                    binding.remoteVideoOffViewName.setText(consultantName);

                } else if (state == Constants.REMOTE_VIDEO_STATE_DECODING) {
                    //video on
                    binding.remoteVideoViewContainer.setVisibility(View.VISIBLE);
                    binding.remoteVideoOffView.setVisibility(View.GONE);
                }
            });
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() == null) {
            Toast.makeText(requireActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
            requireActivity().finish();
            return;
        }

        firebaseAuth = FirebaseAuth.getInstance();

        channelName = getArguments().getString("channel");
        token = getArguments().getString("token");
        consultantName = getArguments().getString("consultantName");
        appointmentId = getArguments().getString("appointmentId");
        slot = getArguments().getString("slot");

        endSession = false;
        timerHandler = new Handler(Looper.getMainLooper());

        retrieveToken(instantiated -> {
        });
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

        videoCallView = binding.videoCallContainer;
        waitingRoomView = binding.waitingRoomContainer;

        videoCallView.setVisibility(View.GONE);
        waitingRoomView.setVisibility(View.VISIBLE);
        binding.waitingRoomConsultantName.setText(consultantName);
        binding.consultantConnectionLost.setVisibility(View.GONE);


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
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                transaction.replace(R.id.fragmentContainer, new ChannelFragment());
                transaction.commit();
                notifySessionEnded(0);
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        cleanupAgoraEngine();
        notifySessionEnded(0);
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
        surfaceView.setZOrderMediaOverlay(true);
        container.addView(surfaceView);
        mRtcEngine.setupLocalVideo(new VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0));
    }

    private void setupRemoteVideo(int uid) {
        FrameLayout container = binding.remoteVideoViewContainer;
        SurfaceView surfaceView = new SurfaceView(getActivity().getBaseContext());
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

    private void notifySessionEnded(int reason) {
        if (retrofit == null) {
            retrieveToken(instantiated -> {
                if (instantiated) {
                    notifyServer(reason);
                }
            });
        } else {
            notifyServer(reason);
        }
    }

    private void notifyServer(int reason) {
        AppointmentApiService apiService = retrofit.create(AppointmentApiService.class);

        AppointmentStatusUpdateDto statusUpdateDto = new AppointmentStatusUpdateDto(appointmentId, reason);
        Call<Void> result = apiService.updateOnlineAppointmentStatus(statusUpdateDto);
        result.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(requireActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
                    Log.i("VideoCallFragment", "Server appointment status update failed: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable throwable) {
                Toast.makeText(requireActivity(), "An error occurred", Toast.LENGTH_SHORT).show();
                Log.i("VideoCallFragment", "Server appointment status update failed: " + throwable);
            }
        });
    }

    private void startTimer() {

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (endSession) {
                    Toast.makeText(requireActivity(), "Connection Timeout", Toast.LENGTH_SHORT).show();
                    cleanupAgoraEngine();
                    notifySessionEnded(1);
                    requireActivity().finish();
                    FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
                    transaction.replace(R.id.fragmentContainer, new ChannelFragment());
                    transaction.commit();
                }
            }
        };

        timerHandler.postDelayed(timerRunnable, 900000); //15 mins
    }

    private void stopTimer() {
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void retrieveToken(CallBack<Boolean> instantiated) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            currentUser.getIdToken(true)
                    .addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
                        @Override
                        public void onSuccess(GetTokenResult getTokenResult) {
                            Log.i("Firebase Auth", "Firebase Token retrieved");
                            retrofit = RetrofitClient.getInstance(getTokenResult.getToken());
                            instantiated.callBack(true);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("Firebase Auth", "Token retrieval failed: " + e.getMessage());
                        }
                    });
        } else {
            // exception
            Log.e("Firebase Auth", "No firebase auth user");
        }
    }

}