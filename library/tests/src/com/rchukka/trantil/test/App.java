package com.rchukka.trantil.test;
 
import com.rchukka.trantil.content.DataStore;

import android.app.Application;

public class App extends Application{  
	@Override 
    public void onCreate() { 
        DataStore.init(this, 2);
    }
}
