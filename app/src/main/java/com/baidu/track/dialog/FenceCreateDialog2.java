package com.baidu.track.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.baidu.trace.api.fence.FenceType;
import com.baidu.track.R;

/**
 * 围栏创建对话框2
 */
public class FenceCreateDialog2 extends Dialog implements View.OnClickListener {

    /**
     * 回调接口
     */
    private Callback callback = null;

    private Button cancelBtn = null;
    private Button sureBtn = null;
    private RadioButton localBtn = null;
    private RadioButton serverBtn = null;

    private TextView titleText = null;

    private EditText fenceDenoiseText = null;
    private EditText fenceRadiusText = null;

    private FenceType fenceType = FenceType.local;

    private double radius = 100;
    private int denoise = 0;

    /**
     * @param activity ：调用的父activity
     */
    public FenceCreateDialog2(Activity activity, Callback callback) {
        super(activity, android.R.style.Theme_Holo_Light_Dialog);
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fence_create2);

        titleText = (TextView) findViewById(R.id.tv_fenceCreate_title);

        localBtn = (RadioButton) findViewById(R.id.btn_local);
        serverBtn = (RadioButton) findViewById(R.id.btn_server);

        fenceDenoiseText = (EditText) findViewById(R.id.edtTxt_fenceCreate_denoise);
        fenceRadiusText = (EditText) findViewById(R.id.edtTxt_fenceCreate_radius);

        cancelBtn = (Button) findViewById(R.id.btn_fenceCreate_cancel);
        sureBtn = (Button) findViewById(R.id.btn_fenceCreate_sure);
        localBtn.setOnClickListener(this);
        serverBtn.setOnClickListener(this);
        cancelBtn.setOnClickListener(this);
        sureBtn.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        titleText.setText("创建圆形围栏");
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_local:
                fenceType = FenceType.local;
                break;

            case R.id.btn_server:
                fenceType = FenceType.server;
                break;

            case R.id.btn_fenceCreate_cancel:
                dismiss();
                if (null != callback) {
                    callback.onCancelCallback();
                }
                break;

            case R.id.btn_fenceCreate_sure:
                String denoiseStr = fenceDenoiseText.getText().toString();
                String radiusStr = fenceRadiusText.getText().toString();
                if (!TextUtils.isEmpty(denoiseStr)) {
                    try {
                        denoise = Integer.parseInt(denoiseStr);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (!TextUtils.isEmpty(radiusStr)) {
                    try {
                        radius = Double.parseDouble(radiusStr);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (null != callback) {
                    callback.onSureCallback(radius, denoise, 0);
                }
                dismiss();
                break;

            default:
                break;
        }
    }

    public FenceType getFenceType(){
        return this.fenceType;
    }

    /**
     * 创建回调接口
     */
    public interface Callback {
        /**
         * 确定回调
         */
        void onSureCallback(double radius, int denoise, int offset);

        /**
         * 取消回调
         */
        void onCancelCallback();
    }

}
