package com.csce482.atex.manmadeobjectdetection;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;


public class GaborActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        // Force full screen view.
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().clearFlags(
                WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gabor);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, PreviewFragment.newInstance())
                    .commit();

        }
    }

}
