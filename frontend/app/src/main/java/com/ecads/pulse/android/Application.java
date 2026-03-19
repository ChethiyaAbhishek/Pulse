package com.ecads.pulse.android;

import com.google.android.libraries.places.api.Places;

public class Application extends android.app.Application {
    @Override
    public void onCreate() {
        super.onCreate();
        if(!Places.isInitialized()){
            Places.initialize(getApplicationContext(), BuildConfig.MAPS_API_KEY);
        }
    }
}
