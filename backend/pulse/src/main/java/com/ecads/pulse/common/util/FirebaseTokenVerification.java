package com.ecads.pulse.common.util;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.stereotype.Service;

@Service
public class FirebaseTokenVerification {

    public FirebaseToken verify(String token) throws Exception {
        FirebaseAuth firebaseAuth = FirebaseAuth.getInstance();
        return firebaseAuth.verifyIdToken(token);
    }
}
