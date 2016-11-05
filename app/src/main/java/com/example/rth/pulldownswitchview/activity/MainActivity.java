package com.example.rth.pulldownswitchview.activity;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.example.rth.pulldownswitchview.R;
import com.example.rth.pulldownswitchview.view.CameraGLSurfaceView;
import com.example.rth.pulldownswitchview.view.NormalView;
import com.example.rth.pulldownswitchview.view.PullDownSwitchView;
import com.example.rth.pulldownswitchview.view.interfaces.PullDownSwitchListener;

public class MainActivity extends AppCompatActivity implements PullDownSwitchListener {

    private PullDownSwitchView pullDownSwitchView;
    private CameraGLSurfaceView cameraGLSurfaceView;
    private NormalView normalView;
    private View curView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        normalView = new NormalView(this);
        cameraGLSurfaceView = new CameraGLSurfaceView(this,null);

        pullDownSwitchView = (PullDownSwitchView) findViewById(R.id.pull_down_view);
        pullDownSwitchView.setPullDownSwitchListener(this);
        pullDownSwitchView.addAFunctionView(cameraGLSurfaceView);
        pullDownSwitchView.addAFunctionView(normalView);
        curView = normalView;

    }

    @Override
    public void onViewSelected(View view) {
        curView = view;
    }

    @Override
    public void onSwitchState(int state) {

    }

    @Override
    public void onPullDownChanged(int incrementVertical, int onePageHeight) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.aty_main,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_switch) {
            switchView();
        }
        return true;
    }

    private void switchView() {
        pullDownSwitchView.showView(curView == cameraGLSurfaceView ? normalView : cameraGLSurfaceView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraGLSurfaceView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraGLSurfaceView.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pullDownSwitchView.setPullDownSwitchListener(null);
    }
}
