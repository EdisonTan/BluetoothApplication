package com.example.administrator.bluetoothapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by Administrator on 2017/3/5 0005.
 */

public class WaveView extends View {
    private Canvas waveCanvas;
    public WaveView(Context context, AttributeSet set){
        super(context,set);
    }
    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);

    }
}
