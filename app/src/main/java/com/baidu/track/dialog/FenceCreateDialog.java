package com.baidu.track.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.baidu.trace.api.fence.FenceShape;
import com.baidu.trace.api.fence.FenceType;
import com.baidu.track.R;

/**
 * 围栏创建对话框
 */
public class FenceCreateDialog extends Dialog implements View.OnClickListener {

    /**
     * 回调接口
     */
    private Callback callback = null;

    private View fenceRadiusLayout = null;
    private View fenceOffsetLayout = null;
    private Button cancelBtn = null;
    private Button sureBtn = null;

    private TextView titleText = null;

    private EditText fenceDenoiseText = null;
    private EditText fenceRadiusText = null;
    private EditText fenceOffsetText = null;

    private FenceType fenceType = FenceType.local;
    private FenceShape fenceShape = FenceShape.circle;

    private double radius = 100;
    private int denoise = 0;
    private int offset = 200;

    public FenceCreateDialog(Activity activity, Callback callback) {
        super(activity, android.R.style.Theme_Holo_Light_Dialog);
        this.callback = callback;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fence_create);

        fenceRadiusLayout = findViewById(R.id.layout_fenceCreate_radius);
        fenceOffsetLayout = findViewById(R.id.layout_fenceCreate_offset);
        titleText = (TextView) findViewById(R.id.tv_fenceCreate_title);

        fenceDenoiseText = (EditText) findViewById(R.id.edtTxt_fenceCreate_denoise);
        fenceRadiusText = (EditText) findViewById(R.id.edtTxt_fenceCreate_radius);
        fenceOffsetText = (EditText) findViewById(R.id.edtTxt_fenceCreate_offset);

        cancelBtn = (Button) findViewById(R.id.btn_fenceCreate_cancel);
        sureBtn = (Button) findViewById(R.id.btn_fenceCreate_sure);
        cancelBtn.setOnClickListener(this);
        sureBtn.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        switch (fenceShape) {
            case circle:
                if (FenceType.local == fenceType) {
                    titleText.setText(R.string.fence_create_local_circle);
                } else {
                    titleText.setText(R.string.fence_create_server_circle);
                }
                fenceRadiusLayout.setVisibility(View.VISIBLE);
                fenceOffsetLayout.setVisibility(View.GONE);
                break;

            case polygon:
                titleText.setText(R.string.fence_create_polygon);
                fenceRadiusLayout.setVisibility(View.GONE);
                fenceOffsetLayout.setVisibility(View.GONE);
                break;

            case polyline:
                titleText.setText(R.string.fence_create_polyline);
                fenceOffsetLayout.setVisibility(View.VISIBLE);
                fenceRadiusLayout.setVisibility(View.GONE);
                break;

            default:
                break;
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_fenceCreate_cancel:
                dismiss();
                if (null != callback) {
                    callback.onCancelCallback();
                }
                break;

            case R.id.btn_fenceCreate_sure:
                String denoiseStr = fenceDenoiseText.getText().toString();
                String radiusStr = fenceRadiusText.getText().toString();
                String offsetStr = fenceOffsetText.getText().toString();

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
                if (!TextUtils.isEmpty(offsetStr)) {
                    try {
                        offset = Integer.parseInt(offsetStr);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                if (null != callback) {
                    callback.onSureCallback(radius, denoise, offset);
                }
                dismiss();
                break;

            default:
                break;
        }
    }

    public void setFenceType(FenceType fenceType) {
        this.fenceType = fenceType;

    }

    public void setFenceShape(FenceShape fenceShape) {
        this.fenceShape = fenceShape;
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
