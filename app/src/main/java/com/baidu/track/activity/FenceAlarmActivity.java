package com.baidu.track.activity;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.content.res.ResourcesCompat;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.Trace;
import com.baidu.trace.api.entity.LocRequest;
import com.baidu.trace.api.entity.OnEntityListener;
import com.baidu.trace.api.fence.CreateFenceRequest;
import com.baidu.trace.api.fence.CreateFenceResponse;
import com.baidu.trace.api.fence.DeleteFenceRequest;
import com.baidu.trace.api.fence.DeleteFenceResponse;
import com.baidu.trace.api.fence.FenceAlarmPushInfo;
import com.baidu.trace.api.fence.FenceType;
import com.baidu.trace.api.fence.MonitoredAction;
import com.baidu.trace.api.fence.OnFenceListener;
import com.baidu.trace.api.track.LatestPoint;
import com.baidu.trace.api.track.LatestPointRequest;
import com.baidu.trace.api.track.LatestPointResponse;
import com.baidu.trace.api.track.OnTrackListener;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.OnTraceListener;
import com.baidu.trace.model.ProcessOption;
import com.baidu.trace.model.PushMessage;
import com.baidu.trace.model.StatusCodes;
import com.baidu.trace.model.TraceLocation;
import com.baidu.trace.model.TransportMode;
import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.dialog.FenceCreateDialog2;
import com.baidu.track.model.CurrentLocation;
import com.baidu.track.utils.AbstractFenceListener;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.CommonUtil;
import com.baidu.track.utils.Constants;
import com.baidu.track.utils.MapUtil;
import com.baidu.track.utils.NetUtil;
import com.baidu.track.utils.TrackReceiver;
import com.baidu.track.utils.ViewUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by zhuhao on 2017/7/8.
 */
public class FenceAlarmActivity extends BaseActivity implements View.OnClickListener, SensorEventListener, BaiduMap.OnMarkerClickListener {

    private TrackApplication trackApp = null;

    private ViewUtil viewUtil = null;

    private Button traceBtn = null;

    private Button gatherBtn = null;

    private PowerManager powerManager = null;

    private PowerManager.WakeLock wakeLock = null;

    private SensorManager mSensorManager;
    private Double lastX = 0.0;//上一个方向值
    private int mCurrentDirection = 0;//传感器当前方向

    private TrackReceiver trackReceiver = null;

    private MapUtil mapUtil = null;

    /**
     * 轨迹服务监听器
     */
    private OnTraceListener traceListener = null;

    /**
     * 轨迹监听器(用于接收纠偏后实时位置回调)
     */
    private OnTrackListener trackListener = null;

    /**
     * Entity监听器(用于接收实时定位回调)
     */
    private OnEntityListener entityListener = null;
    /**
     * 围栏监听器
     */
    private OnFenceListener fenceListener = null;

    /**
     * 实时定位任务
     */
    private RealTimeHandler realTimeHandler = new RealTimeHandler();

    private RealTimeLocRunnable realTimeLocRunnable = null;

    /**
     * 轨迹点集合
     */
    private List<LatLng> trackPoints;

    /**
     * 围栏创建对话框
     */
    private FenceCreateDialog2 fenceCreateDialog2 = null;

    /**
     * 圆形围栏中心点坐标（地图坐标类型）
     */
    private LatLng circleCenter = null;
    private double radius;//半径
    private String fenceKey;//围栏key

    /**
     * 轨迹客户端
     */
    private LBSTraceClient mClient = null;

    /**
     * 轨迹服务
     */
    private Trace mTrace = null;

    private LocRequest locRequest = null;//本地定位请求

    private boolean traceStarted = false;
    private boolean gatherStarted = false;

    /**
     * 围栏创建对话框回调接口
     */
    private FenceCreateDialog2.Callback createCallback = null;

    //围栏图层相关
    private OverlayOptions overlayOptions;
    private OverlayOptions markerOptions;
    private Marker currentMarkerA;
    private Overlay currentOverlay;

    private boolean firstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.alarm_title);
        setOptionsButtonInVisible();
        initTrace();//初始化轨迹服务
        initMap();//初始化地图
        initView();
        initListener();
    }

    private void initTrace() {
        trackApp = (TrackApplication) getApplicationContext();
        mClient = new LBSTraceClient(this);
        mTrace = new Trace(trackApp.serviceId, trackApp.entityName);
        locRequest = new LocRequest();//本地定位请求
        trackPoints = new ArrayList<>();
    }

    private void initMap() {
        mapUtil = MapUtil.getInstance();
        mapUtil.init((MapView) findViewById(R.id.tracing_mapView));
        mapUtil.baiduMap.setOnMapClickListener(new BaiduMap.OnMapClickListener() {//设置点击地图监听
            @Override
            public void onMapClick(LatLng latLng) {

                circleCenter = latLng;//点击位置的经纬度
                showDialog();

            }

            @Override
            public boolean onMapPoiClick(MapPoi mapPoi) {

                circleCenter = mapPoi.getPosition();//点击位置的经纬度
                showDialog();
                return false;
            }
        });
        mapUtil.baiduMap.setOnMarkerClickListener(this);//红色marker标识点击监听
        mapUtil.setCenter(mCurrentDirection);//设置地图中心点
    }

    private void initView() {
        viewUtil = new ViewUtil();
        powerManager = (PowerManager) trackApp.getSystemService(Context.POWER_SERVICE);
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);// 获取传感器管理服务

        traceBtn = (Button) findViewById(R.id.btn_trace);
        gatherBtn = (Button) findViewById(R.id.btn_gather);
        traceBtn.setOnClickListener(this);
        gatherBtn.setOnClickListener(this);
        setTraceBtnStyle();
        setGatherBtnStyle();
    }


    private void initListener() {

        //对话框回调
        createCallback = new FenceCreateDialog2.Callback() {

            @Override
            public void onSureCallback(double radius, int denoise, int offset) {

                FenceAlarmActivity.this.radius = radius;

                //创建围栏
                CreateFenceRequest request = CreateFenceRequest.buildServerCircleRequest(trackApp.getTag(), trackApp.serviceId, "myFence",
                        trackApp.entityName, mapUtil.convertMap2Trace(circleCenter), radius, denoise,
                        CoordType.bd09ll);
                mClient.createFence(request, fenceListener);
            }

            @Override
            public void onCancelCallback() {

            }
        };

        //围栏操作监听
        fenceListener = new AbstractFenceListener() {
            @Override
            public void onCreateFenceCallback(CreateFenceResponse response) {
                //围栏创建成功回调
                try {
                    if (StatusCodes.SUCCESS != response.getStatus()) {
                        return;
                    }

                    if (fenceKey != null) {
                        deleteFence();//删除旧的围栏
                        removeOverlay();//清除围栏图层
                    }

                    //围栏的key
                    fenceKey = response.getFenceType() + "_" + response.getFenceId();//

                    Stroke stroke;
                    if (fenceCreateDialog2.getFenceType() == FenceType.local) {
                        stroke = new Stroke(5, Color.rgb(0x23, 0x19, 0xDC));
                    } else {
                        stroke = new Stroke(5, Color.rgb(0xFF, 0x06, 0x01));
                    }

                    overlayOptions = new CircleOptions().fillColor(0x000000FF).center(circleCenter)
                            .stroke(stroke).zIndex(trackApp.getTag()).radius((int) radius);
                    markerOptions = new MarkerOptions()
                            .position(circleCenter).icon(BitmapUtil.bmGcoding)
                            .zIndex(trackApp.getTag()).draggable(false);
                    currentOverlay = mapUtil.baiduMap.addOverlay(overlayOptions);//画圆
                    currentMarkerA = (Marker) mapUtil.baiduMap.addOverlay(markerOptions);//画圆心图层
                } catch (Exception e) {

                }
            }

            @Override
            public void onDeleteFenceCallback(DeleteFenceResponse deleteFenceResponse) {
                //删除围栏成功回调
                if (deleteFenceResponse.getStatus() == StatusCodes.SUCCESS) {
                    viewUtil.showToast(FenceAlarmActivity.this,
                            "围栏旧删除成功");
                }

            }
        };

        //查询服务端纠偏最新位置回调监听
        trackListener = new OnTrackListener() {

            @Override
            public void onLatestPointCallback(LatestPointResponse response) {
                //经过服务端纠偏后的最新的一个位置点，回调
                System.out.println("onLatestPointCallback");
                try {
                    if (StatusCodes.SUCCESS != response.getStatus()) {
                        return;
                    }

                    LatestPoint point = response.getLatestPoint();
                    if (null == point || CommonUtil.isZeroPoint(point.getLocation().getLatitude(), point.getLocation()
                            .getLongitude())) {
                        return;
                    }

                    LatLng currentLatLng = mapUtil.convertTrace2Map(point.getLocation());
                    if (null == currentLatLng) {
                        return;
                    }

                    if(firstLocate){
                        firstLocate = false;
                        Toast.makeText(FenceAlarmActivity.this,"起点获取中，请稍后...",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //当前经纬度
                    CurrentLocation.locTime = point.getLocTime();
                    CurrentLocation.latitude = currentLatLng.latitude;
                    CurrentLocation.longitude = currentLatLng.longitude;

                    if (trackPoints == null) {
                        return;
                    }
                    trackPoints.add(currentLatLng);

                    mapUtil.drawHistoryTrack(trackPoints, false, mCurrentDirection);//时时动态的画出运动轨迹

                    if (overlayOptions != null) {
                        currentOverlay = mapUtil.baiduMap.addOverlay(overlayOptions);//会被删除，重新绘画围栏图层
                    }

                    if (markerOptions != null) {
                        currentMarkerA = (Marker) mapUtil.baiduMap.addOverlay(markerOptions);
                    }

                } catch (Exception e) {

                }
            }
        };

        //本地位置回调监听
        entityListener = new OnEntityListener() {

            @Override
            public void onReceiveLocation(TraceLocation location) {
                //本地LBSTraceClient客户端获取的位置
                try {
                    System.out.println("onReceiveLocation");

                    if (StatusCodes.SUCCESS != location.getStatus() || CommonUtil.isZeroPoint(location.getLatitude(),
                            location.getLongitude())) {
                        return;
                    }
                    LatLng currentLatLng = mapUtil.convertTraceLocation2Map(location);
                    if (null == currentLatLng) {
                        return;
                    }
                    CurrentLocation.locTime = CommonUtil.toTimeStamp(location.getTime());
                    CurrentLocation.latitude = currentLatLng.latitude;
                    CurrentLocation.longitude = currentLatLng.longitude;

                    if (null != mapUtil) {
                        mapUtil.updateMapLocation(currentLatLng, mCurrentDirection);//显示当前位置
                        mapUtil.animateMapStatus(currentLatLng);//缩放
                    }
                } catch (Exception e) {

                }

            }

        };

        //轨迹服务监听
        traceListener = new OnTraceListener() {

            @Override
            public void onBindServiceCallback(int errorNo, String message) {
                viewUtil.showToast(FenceAlarmActivity.this,
                        String.format("onBindServiceCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            @Override
            public void onStartTraceCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.START_TRACE_NETWORK_CONNECT_FAILED <= errorNo) {
                    traceStarted = true;
                    setTraceBtnStyle();
                    registerReceiver();
                }
                viewUtil.showToast(FenceAlarmActivity.this,
                        String.format("onStartTraceCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            @Override
            public void onStopTraceCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.CACHE_TRACK_NOT_UPLOAD == errorNo) {
                    traceStarted = false;
                    gatherStarted = false;
                    setTraceBtnStyle();
                    setGatherBtnStyle();
                    unregisterPowerReceiver();
                    firstLocate = true;
                }
                viewUtil.showToast(FenceAlarmActivity.this,
                        String.format("onStopTraceCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            @Override
            public void onStartGatherCallback(int errorNo, String message) {
                if (StatusCodes.SUCCESS == errorNo || StatusCodes.GATHER_STARTED == errorNo) {
                    gatherStarted = true;
                    setGatherBtnStyle();

                    stopRealTimeLoc();
                    startRealTimeLoc(Constants.DEFAULT_PACK_INTERVAL);
                }
                viewUtil.showToast(FenceAlarmActivity.this,
                        String.format("onStartGatherCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            @Override
            public void onStopGatherCallback(int errorNo, String message) {
                try {
                    if (StatusCodes.SUCCESS == errorNo || StatusCodes.GATHER_STOPPED == errorNo) {
                        gatherStarted = false;
                        setGatherBtnStyle();
                        if (trackPoints.size() >= 1) {
                            mapUtil.drawEndPoint(trackPoints.get(trackPoints.size() - 1));
                        }
                        firstLocate = true;
                        stopRealTimeLoc();
                        startRealTimeLoc(Constants.LOC_INTERVAL);

                    }
                } catch (Exception e) {

                }
                viewUtil.showToast(FenceAlarmActivity.this,
                        String.format("onStopGatherCallback, errorNo:%d, message:%s ", errorNo, message));
            }

            @Override
            public void onPushCallback(byte messageType, PushMessage pushMessage) {
                //进出围栏警报信息回调
                if (messageType < 0x03 || messageType > 0x04) {
                    viewUtil.showToast(FenceAlarmActivity.this, pushMessage.getMessage());
                    return;
                }
                FenceAlarmPushInfo alarmPushInfo = pushMessage.getFenceAlarmPushInfo();
                if (null == alarmPushInfo) {
                    return;
                }
                StringBuffer alarmInfo = new StringBuffer();
                alarmInfo.append("你")
                        .append(alarmPushInfo.getMonitoredAction() == MonitoredAction.enter ? "进入" : "离开")
                        .append(messageType == 0x03 ? "服务端" : "本地")
                        .append("围栏了");

                viewUtil.showToast(FenceAlarmActivity.this,
                        alarmInfo.toString());

            }
        };

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_trace:
                if (traceStarted) {
                    mClient.stopTrace(mTrace, traceListener);//停止服务
                } else {
                    mClient.startTrace(mTrace, traceListener);//开始服务
                }
                break;

            case R.id.btn_gather:
                if (gatherStarted) {
                    mClient.stopGather(traceListener);
                } else {
                    mClient.setInterval(Constants.DEFAULT_GATHER_INTERVAL, Constants.DEFAULT_PACK_INTERVAL);
                    mClient.startGather(traceListener);//开启采集
                }
                break;
            default:
                break;
        }

    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        if (marker.getZIndex() == currentMarkerA.getZIndex()) {//红色marker标识点击回调
            if (fenceKey != null) {
                deleteFence();//删除围栏
                removeOverlay();
            }
        }

        return false;
    }

    //清除围栏图层
    private void removeOverlay() {
        if (currentMarkerA != null) {
            currentMarkerA.remove();
            markerOptions = null;
        }

        if (currentOverlay != null) {
            currentOverlay.remove();
            overlayOptions = null;
        }
    }

    //删除围栏请求
    public void deleteFence() {
        List<Long> deleteFenceIds = new ArrayList<>();
        String[] fenceKeys = fenceKey.split("_");
        FenceType fenceType = FenceType.valueOf(fenceKeys[0]);
        long fenceId = Long.parseLong(fenceKeys[1]);
        deleteFenceIds.add(fenceId);//围栏id

        DeleteFenceRequest deleteRequest;
        if (FenceType.server == fenceType) {//删除服务端围栏
            deleteRequest = DeleteFenceRequest.buildServerRequest(trackApp.getTag(),
                    trackApp.serviceId, trackApp.entityName, deleteFenceIds);
        } else {//删除客户端围栏
            deleteRequest = DeleteFenceRequest.buildLocalRequest(trackApp.getTag(),
                    trackApp.serviceId, trackApp.entityName, deleteFenceIds);
        }
        mClient.deleteFence(deleteRequest, fenceListener);
    }

    /**
     * 获取当前位置
     */
    public void getCurrentLocation(OnEntityListener entityListener, OnTrackListener trackListener) {
        // 网络连接正常，开启服务及采集，则查询纠偏后实时位置；否则进行实时定位
        if (NetUtil.isNetworkAvailable(this)
                && traceStarted
                && gatherStarted) {
            LatestPointRequest request = new LatestPointRequest(trackApp.getTag(), trackApp.serviceId, trackApp.entityName);
            ProcessOption processOption = new ProcessOption();
            processOption.setRadiusThreshold(50);
            processOption.setTransportMode(TransportMode.walking);
            processOption.setNeedDenoise(true);
            processOption.setNeedMapMatch(true);
            request.setProcessOption(processOption);
            mClient.queryLatestPoint(request, trackListener);//请求服务端最新轨迹点
        } else {
            mClient.queryRealTimeLoc(locRequest, entityListener);
        }
    }

    private void showDialog() {
        if (null == fenceCreateDialog2) {
            fenceCreateDialog2 = new FenceCreateDialog2(FenceAlarmActivity.this, createCallback);//创建dialog
        }

        if (!fenceCreateDialog2.isShowing()) {
            fenceCreateDialog2.show();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //每次方向改变，重新给地图设置定位数据，用上一次的经纬度
        double x = sensorEvent.values[SensorManager.DATA_X];
        if (Math.abs(x - lastX) > 1.0) {// 方向改变大于1度才设置，以免地图上的箭头转动过于频繁
            mCurrentDirection = (int) x;
            if (!CommonUtil.isZeroPoint(CurrentLocation.latitude, CurrentLocation.longitude)) {
                mapUtil.updateMapLocation(new LatLng(CurrentLocation.latitude, CurrentLocation.longitude), (float) mCurrentDirection);
            }
        }
        lastX = x;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    /**
     * 设置服务按钮样式
     */
    private void setTraceBtnStyle() {

        if (traceStarted) {
            traceBtn.setText(R.string.stop_trace);
            traceBtn.setTextColor(ResourcesCompat.getColor(getResources(), R.color
                    .white, null));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                traceBtn.setBackground(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_sure, null));
            } else {
                traceBtn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_sure, null));
            }
        } else {
            traceBtn.setText(R.string.start_trace);
            traceBtn.setTextColor(ResourcesCompat.getColor(getResources(), R.color.layout_title, null));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                traceBtn.setBackground(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_cancel, null));
            } else {
                traceBtn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_cancel, null));
            }
        }
    }

    /**
     * 设置采集按钮样式
     */
    private void setGatherBtnStyle() {
        if (gatherStarted) {
            gatherBtn.setText(R.string.stop_gather);
            gatherBtn.setTextColor(ResourcesCompat.getColor(getResources(), R.color.white, null));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                gatherBtn.setBackground(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_sure, null));
            } else {
                gatherBtn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_sure, null));
            }
        } else {
            gatherBtn.setText(R.string.start_gather);
            gatherBtn.setTextColor(ResourcesCompat.getColor(getResources(), R.color.layout_title, null));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                gatherBtn.setBackground(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_cancel, null));
            } else {
                gatherBtn.setBackgroundDrawable(ResourcesCompat.getDrawable(getResources(),
                        R.mipmap.bg_btn_cancel, null));
            }
        }
    }

    /**
     * 实时定位任务
     */
    class RealTimeLocRunnable implements Runnable {

        private int interval = 0;

        public RealTimeLocRunnable(int interval) {
            this.interval = interval;
        }

        @Override
        public void run() {
            getCurrentLocation(entityListener, trackListener);
            realTimeHandler.postDelayed(this, interval * 1000);
        }
    }

    public void startRealTimeLoc(int interval) {
        realTimeLocRunnable = new RealTimeLocRunnable(interval);
        realTimeHandler.post(realTimeLocRunnable);
    }

    public void stopRealTimeLoc() {
        if (null != realTimeHandler && null != realTimeLocRunnable) {
            realTimeHandler.removeCallbacks(realTimeLocRunnable);
        }
        mClient.stopRealTimeLoc();
    }

    static class RealTimeHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }

    }

    /**
     * 注册广播（电源锁、GPS状态）
     */
    private void registerReceiver() {
        if (trackApp.isRegisterReceiver) {
            return;
        }

        if (null == wakeLock) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "track upload");
        }
        if (null == trackReceiver) {
            trackReceiver = new TrackReceiver(wakeLock);
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(StatusCodes.GPS_STATUS_ACTION);
        trackApp.registerReceiver(trackReceiver, filter);
        trackApp.isRegisterReceiver = true;

    }

    private void unregisterPowerReceiver() {
        if (!trackApp.isRegisterReceiver) {
            return;
        }
        if (null != trackReceiver) {
            trackApp.unregisterReceiver(trackReceiver);
        }
        trackApp.isRegisterReceiver = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (traceStarted && gatherStarted) {
            startRealTimeLoc(Constants.DEFAULT_PACK_INTERVAL);//开始采集后定位间隔
        } else {
            startRealTimeLoc(Constants.LOC_INTERVAL);//普通定位间隔
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapUtil.onResume();

        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_UI);

        // 在Android 6.0及以上系统，若定制手机使用到doze模式，请求将应用添加到白名单。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String packageName = trackApp.getPackageName();
            boolean isIgnoring = powerManager.isIgnoringBatteryOptimizations(packageName);
            if (!isIgnoring) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                try {
                    startActivity(intent);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapUtil.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopRealTimeLoc();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopRealTimeLoc();
        trackPoints.clear();
        trackPoints = null;

        if (fenceCreateDialog2 != null && fenceCreateDialog2.isShowing()) {
            fenceCreateDialog2.dismiss();
        }


        if (traceStarted) {
            mClient.setOnTraceListener(null);//停止服务
            mClient.stopTrace(mTrace, null);
        }


        mClient.clear();//释放资源

        mapUtil.clear();//清除地图所有图层释放资源

    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_alarm;
    }
}