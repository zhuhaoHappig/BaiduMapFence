package com.baidu.track.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.baidu.trace.api.fence.FenceShape;
import com.baidu.trace.api.fence.FenceType;
import com.baidu.track.R;

import com.baidu.track.utils.Constants;

/**
 * 创建、查询围栏参数设置
 */
public class CreateFenceOptions extends BaseActivity implements View.OnClickListener {

    private View fenceShapeLayout = null;
    private View fenceNameLayout = null;
    private View vertexesNumberLayout = null;
    private Button cancelBtn = null;
    private Button sureBtn = null;
    private RadioButton localBtn = null;
    private RadioButton serverBtn = null;
    private RadioButton createBtn = null;
    private RadioButton listBtn = null;
    private RadioButton circleBtn = null;
    private RadioButton polylineBtn = null;
    private RadioButton polygonBtn = null;

    private TextView createCaptionText = null;

    private EditText fenceNameText = null;
    private EditText vertexesNumberText = null;

    private FenceType fenceType = FenceType.local;
    private FenceShape fenceShape = FenceShape.circle;
    private int operateType = R.id.btn_fence_list;

    // 返回结果
    private Intent result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fenceShapeLayout = findViewById(R.id.layout_fence_shape);
        fenceNameLayout = findViewById(R.id.layout_fence_name);
        vertexesNumberLayout = findViewById(R.id.layout_vertexes_number);
        cancelBtn = (Button) findViewById(R.id.btn_cancel);
        sureBtn = (Button) findViewById(R.id.btn_sure);
        localBtn = (RadioButton) findViewById(R.id.btn_local);
        serverBtn = (RadioButton) findViewById(R.id.btn_server);
        createBtn = (RadioButton) findViewById(R.id.btn_create_fence);
        listBtn = (RadioButton) findViewById(R.id.btn_fence_list);
        circleBtn = (RadioButton) findViewById(R.id.btn_circle);
        polylineBtn = (RadioButton) findViewById(R.id.btn_polyline);
        polygonBtn = (RadioButton) findViewById(R.id.btn_polygon);

        createCaptionText = (TextView) findViewById(R.id.tv_create_caption);

        fenceNameText = (EditText) findViewById(R.id.edtTxt_fence_name);
        vertexesNumberText = (EditText) findViewById(R.id.text_vertexes_number);
        cancelBtn.setOnClickListener(this);
        sureBtn.setOnClickListener(this);
        localBtn.setOnClickListener(this);
        serverBtn.setOnClickListener(this);
        createBtn.setOnClickListener(this);
        listBtn.setOnClickListener(this);
        circleBtn.setOnClickListener(this);
        polylineBtn.setOnClickListener(this);
        polygonBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_local:
                fenceType = FenceType.local;
                fenceShape = FenceShape.circle;
                fenceShapeLayout.setVisibility(View.GONE);
                vertexesNumberLayout.setVisibility(View.GONE);
                if (createBtn.isChecked()) {
                    fenceNameLayout.setVisibility(View.VISIBLE);
                }
                break;

            case R.id.btn_server:
                fenceType = FenceType.server;
                if (createBtn.isChecked()) {
                    fenceShapeLayout.setVisibility(View.VISIBLE);
                    fenceNameLayout.setVisibility(View.VISIBLE);
                }
                if (polylineBtn.isChecked() || polygonBtn.isChecked()) {
                    vertexesNumberLayout.setVisibility(View.VISIBLE);
                } else {
                    vertexesNumberLayout.setVisibility(View.GONE);
                }
                break;

            case R.id.btn_create_fence:
                operateType = R.id.btn_create_fence;
                fenceNameLayout.setVisibility(View.VISIBLE);
                if (FenceType.server == fenceType) {
                    fenceShapeLayout.setVisibility(View.VISIBLE);
                }
                createCaptionText.setVisibility(View.VISIBLE);
                if (polylineBtn.isChecked() || polygonBtn.isChecked()) {
                    vertexesNumberLayout.setVisibility(View.VISIBLE);
                } else {
                    vertexesNumberLayout.setVisibility(View.GONE);
                }
                break;

            case R.id.btn_fence_list:
                operateType = R.id.btn_fence_list;
                fenceShapeLayout.setVisibility(View.GONE);
                fenceNameLayout.setVisibility(View.GONE);
                vertexesNumberLayout.setVisibility(View.GONE);
                createCaptionText.setVisibility(View.GONE);
                break;

            case R.id.btn_circle:
                fenceShape = FenceShape.circle;
                vertexesNumberLayout.setVisibility(View.GONE);
                break;

            case R.id.btn_polyline:
                fenceShape = FenceShape.polyline;
                vertexesNumberLayout.setVisibility(View.VISIBLE);
                break;

            case R.id.btn_polygon:
                fenceShape = FenceShape.polygon;
                vertexesNumberLayout.setVisibility(View.VISIBLE);
                break;

            case R.id.btn_cancel:
                onCancel(v);
                break;

            case R.id.btn_sure:
                String vertexesNumberStr = vertexesNumberText.getText().toString();
                int vertexesNumber = 3;
                if (!TextUtils.isEmpty(vertexesNumberStr)) {
                    try {
                        vertexesNumber = Integer.parseInt(vertexesNumberStr);
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }

                String fenceName = fenceNameText.getText().toString();
                if (TextUtils.isEmpty(fenceName)) {
                    fenceName = getResources().getString(R.string.fence_name_hint);
                }

                result = new Intent();
                result.putExtra("fenceType", fenceType.name());
                result.putExtra("fenceShape", fenceShape.name());
                result.putExtra("fenceName", fenceName);
                result.putExtra("vertexesNumber", vertexesNumber);
                result.putExtra("operateType", operateType);
                setResult(Constants.RESULT_CODE, result);
                super.onBackPressed();

                break;

            default:
                break;
        }
    }

    public void onCancel(View v) {
        super.onBackPressed();
    }


    @Override
    protected int getContentViewId() {
        return R.layout.activity_fence_options;
    }
}
