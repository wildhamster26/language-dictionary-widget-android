package com.example.glancedict;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class DictionaryWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new DictionaryRemoteViewsFactory(getApplicationContext());
    }
}
