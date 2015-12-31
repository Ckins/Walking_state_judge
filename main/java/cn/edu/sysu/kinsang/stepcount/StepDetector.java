package cn.edu.sysu.kinsang.stepcount;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by kinsang on 15-12-31.
 */
public class StepDetector implements SensorEventListener {
    public static int CURRENT_SETP = 0;
    public static Double SENSITIVITY = 0.0;   //SENSITIVITY灵敏度

    private float mLastValues[] = new float[3 * 2];
    private float mScale[] = new float[2];
    private float mYOffset;
    private static long end = 0;
    private static long start = 0;

    //用于实现状态记录的变量
    public static boolean isWalking = false;
    private static long mark_end = 0;
    private static long mark_start = 0;
    private static int mark_last_step = 0;

    /**
     * 最后加速度方向
     */
    private float mLastDirections[] = new float[3 * 2];
    private float mLastExtremes[][] = { new float[3 * 2], new float[3 * 2] };
    private float mLastDiff[] = new float[3 * 2];
    private int mLastMatch = -1;

    /**
     * 传入上下文的构造函数
     *
     * @param context
     */
    public StepDetector(Context context) {
        // TODO Auto-generated constructor stub
        super();
        int h = 480;
        mYOffset = h * 0.5f;
        mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
        mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
        SENSITIVITY = 2.96; // SENSITIVITY range : 1.97 2.96 4.44 6.66 10.00 15.00 22.50 33.75 50.62
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Log.i(Constant.STEP_SERVER, "StepDetector");
        Sensor sensor = event.sensor;
        // Log.i(Constant.STEP_DETECTOR, "onSensorChanged");
        synchronized (this) {
            if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
                // do nothing
            } else {

                if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    float vSum = 0;
                    for (int i = 0; i < 3; i++) {
                        final float v = mYOffset + event.values[i] * mScale[1];
                        vSum += v;
                    }
                    int k = 0;
                    float v = vSum / 3;

                    float direction = (v > mLastValues[k] ? 1: (v < mLastValues[k] ? -1 : 0));
                    if (direction == -mLastDirections[k]) {
                        // Direction changed
                        int extType = (direction > 0 ? 0 : 1); // minumum or
                        // maximum?
                        mLastExtremes[extType][k] = mLastValues[k];
                        float diff = Math.abs(mLastExtremes[extType][k]- mLastExtremes[1 - extType][k]);

                        if (diff > SENSITIVITY) {
                            boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
                            boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
                            boolean isNotContra = (mLastMatch != 1 - extType);

                            if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
                                end = System.currentTimeMillis();
                                if (end - start > 500) {// 此时判断为走了一步
                                    // MainActivity.step_text.setText("calculating possible step: "+ Integer.toString(CURRENT_SETP));
                                    CURRENT_SETP++;
                                    mLastMatch = extType;
                                    start = end;
                                }
                            } else {
                                mLastMatch = -1;
                            }
                        }
                        mLastDiff[k] = diff;
                    }
                    mLastDirections[k] = direction;
                    mLastValues[k] = v;
                    checkState();
                }
            }
        }
    }

    public void checkState() {
        mark_end = System.currentTimeMillis();
        // 如果三十秒 内走了40步，算是在走路状态

        if (mark_end - mark_start >= 3000) {
            Log.d("----------->", Long.toString(mark_start));
            Log.d("step-----delta>", Long.toString(CURRENT_SETP-mark_last_step));
            if (CURRENT_SETP-mark_last_step >= 4) {
                MainActivity.title_text.setText("walking");
                isWalking = true;
            } else {
                MainActivity.title_text.setText("Notwalking");
                isWalking = false;
            }
            mark_last_step = CURRENT_SETP;
            mark_start = mark_end;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}
