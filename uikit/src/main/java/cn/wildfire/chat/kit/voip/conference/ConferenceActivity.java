package cn.wildfire.chat.kit.voip.conference;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import org.webrtc.StatsReport;

import java.util.ArrayList;
import java.util.List;

import cn.wildfire.chat.kit.group.PickGroupMemberActivity;
import cn.wildfire.chat.kit.voip.VoipBaseActivity;
import cn.wildfirechat.avenginekit.AVEngineKit;
import cn.wildfirechat.avenginekit.VideoProfile;
import cn.wildfirechat.message.ConferenceInviteMessageContent;
import cn.wildfirechat.model.Conversation;
import cn.wildfirechat.model.GroupInfo;
import cn.wildfirechat.remote.ChatManager;

public class ConferenceActivity extends VoipBaseActivity {

    private static final int REQUEST_CODE_ADD_PARTICIPANT = 100;
    private AVEngineKit.CallSessionCallback currentCallSessionCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void init() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            finish();
            return;
        }

        Fragment fragment;
        if (session.getState() == AVEngineKit.CallState.Incoming) {
            fragment = new ConferenceIncomingFragment();
        } else if (session.isAudioOnly()) {
            fragment = new ConferenceAudioFragment();
        } else {
            fragment = new ConferenceVideoFragment();
        }

        currentCallSessionCallback = (AVEngineKit.CallSessionCallback) fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .add(android.R.id.content, fragment)
            .commit();
    }


    // hangup 也会触发
    @Override
    public void didCallEndWithReason(AVEngineKit.CallEndReason callEndReason) {
        postAction(() -> {
            if (!isFinishing()) {
                finish();
            }
        });
    }

    // 自己的状态
    @Override
    public void didChangeState(AVEngineKit.CallState callState) {
        postAction(() -> {
            currentCallSessionCallback.didChangeState(callState);
        });
    }

    @Override
    public void didParticipantJoined(String userId) {
        postAction(() -> {
            currentCallSessionCallback.didParticipantJoined(userId);
        });
    }

    @Override
    public void didParticipantLeft(String userId, AVEngineKit.CallEndReason callEndReason) {
        postAction(() -> {
            currentCallSessionCallback.didParticipantLeft(userId, callEndReason);
        });
    }

    @Override
    public void didReportAudioVolume(String userId, int volume) {
        postAction(() -> {
            currentCallSessionCallback.didReportAudioVolume(userId, volume);
        });
    }

    @Override
    public void didChangeMode(boolean audioOnly) {
        postAction(() -> {
            if (audioOnly) {
                Fragment fragment = new ConferenceAudioFragment();
                currentCallSessionCallback = (AVEngineKit.CallSessionCallback) fragment;
                FragmentManager fragmentManager = getSupportFragmentManager();
                fragmentManager.beginTransaction()
                    .replace(android.R.id.content, fragment)
                    .commit();
            } else {
                // never called
            }
        });
    }

    //@Override
    public void didChangeInitiator(String initiator) {

    }

    @Override
    public void didCreateLocalVideoTrack() {
        postAction(() -> {
            currentCallSessionCallback.didCreateLocalVideoTrack();
        });
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        postAction(() -> {
            currentCallSessionCallback.didReceiveRemoteVideoTrack(userId);
        });
    }

    @Override
    public void didRemoveRemoteVideoTrack(String userId) {
        postAction(() -> {
            currentCallSessionCallback.didRemoveRemoteVideoTrack(userId);
        });
    }

    @Override
    public void didError(String reason) {
        postAction(() -> {
            currentCallSessionCallback.didError(reason);
        });
    }

    @Override
    public void didGetStats(StatsReport[] statsReports) {
        postAction(() -> {
            currentCallSessionCallback.didGetStats(statsReports);
        });
    }

    void hangup() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session != null && session.getState() != AVEngineKit.CallState.Idle) {
            session.endCall();
        }
        finish();
    }

    void accept() {
        AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
        if (session == null || session.getState() == AVEngineKit.CallState.Idle) {
            finish();
            return;
        }

        Fragment fragment;
        if (session.isAudioOnly()) {
            fragment = new ConferenceAudioFragment();
        } else {
            fragment = new ConferenceVideoFragment();
            List<String> participants = session.getParticipantIds();
            if (participants.size() >= 4) {
                AVEngineKit.Instance().setVideoProfile(VideoProfile.VP120P, false);
            } else if (participants.size() >= 6) {
                AVEngineKit.Instance().setVideoProfile(VideoProfile.VP120P_3, false);
            }
        }
        currentCallSessionCallback = (AVEngineKit.CallSessionCallback) fragment;
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit();

        session.answerCall(session.isAudioOnly());
    }

    @Override
    public void didVideoMuted(String s, boolean b) {
        postAction(() -> currentCallSessionCallback.didVideoMuted(s, b));
    }

    void addParticipant(int maxNewInviteParticipantCount) {
        isInvitingNewParticipant = true;

//        AVEngineKit.CallSession session = AVEngineKit.Instance().getCurrentSession();
//        if (session != null) {
//            ConferenceInviteMessageContent invite = new ConferenceInviteMessageContent();
//            invite.setCallId(session.getCallId());
//            invite.setTitle(session.getTitle());
//            invite.setPin(session.getPin());
//            invite.setHost(session.getHost());
//            String target = "7f7272XX";
//            ChatManager.Instance().sendMessage(new Conversation(Conversation.ConversationType.Group, "7f7272XX"), invite, null, 0, null);
//        }
        //Todo 发送邀请消息给参会者，可以使用转发类似UI
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_ADD_PARTICIPANT) {
            isInvitingNewParticipant = false;
            if (resultCode == RESULT_OK) {
                List<String> newParticipants = data.getStringArrayListExtra(PickGroupMemberActivity.EXTRA_RESULT);
                if (newParticipants != null && !newParticipants.isEmpty()) {
                    AVEngineKit.CallSession session = getEngineKit().getCurrentSession();
                    session.inviteNewParticipants(newParticipants);
                }
            }
        }
    }
}
