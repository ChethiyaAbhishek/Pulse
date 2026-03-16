package com.ecads.pulse.telemedicine.util;

import com.ecads.pulse.channeling.enums.SlotDurations;
import com.ecads.pulse.common.enums.UserRole;
import com.ecads.pulse.common.exception.ShowErrorException;
import io.agora.media.RtcTokenBuilder2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public final class TeleMedicineHelper {

    @Value("${agora.app-id}")
    private String APP_ID;
    @Value("${agora.app-certificate}")
    private String APP_CERTIFICATE;

    private static final int EXPIRATION_IN_SECONDS = 5400;

    public static final int PATIENT_UID = 1000;
    public static final int CONSULTANT_UID = 2000;

    public String generateAgoraChannelName(String appointmentId) {
        return "telemed_" + appointmentId;
    }

    public String generateAgoraChannelToken(String channelName, UserRole role) {

        int currentTime = (int) (System.currentTimeMillis() / 1000);
        int expireTime = currentTime + EXPIRATION_IN_SECONDS;

        RtcTokenBuilder2 token = new RtcTokenBuilder2();

        int uid = 0;
        if(role == UserRole.PATIENT){
            uid = PATIENT_UID;
        }else if(role == UserRole.CONSULTANT){
            uid = CONSULTANT_UID;
        }

        try {
            return token.buildTokenWithUid(
                    APP_ID,
                    APP_CERTIFICATE,
                    channelName,
                    uid,
                    RtcTokenBuilder2.Role.ROLE_PUBLISHER,
                    expireTime,
                    expireTime
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
