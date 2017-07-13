package com.baidu.track.activity;

import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.CircleOptions;
import com.baidu.mapapi.map.MapPoi;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;
import com.baidu.mapapi.map.Overlay;
import com.baidu.mapapi.map.OverlayOptions;
import com.baidu.mapapi.map.PolygonOptions;
import com.baidu.mapapi.map.PolylineOptions;
import com.baidu.mapapi.map.Stroke;
import com.baidu.mapapi.model.LatLng;
import com.baidu.trace.LBSTraceClient;
import com.baidu.trace.api.fence.CircleFence;
import com.baidu.trace.api.fence.CreateFenceRequest;
import com.baidu.trace.api.fence.CreateFenceResponse;
import com.baidu.trace.api.fence.DeleteFenceRequest;
import com.baidu.trace.api.fence.DeleteFenceResponse;
import com.baidu.trace.api.fence.FenceInfo;
import com.baidu.trace.api.fence.FenceListRequest;
import com.baidu.trace.api.fence.FenceListResponse;
import com.baidu.trace.api.fence.FenceShape;
import com.baidu.trace.api.fence.FenceType;
import com.baidu.trace.api.fence.HistoryAlarmRequest;
import com.baidu.trace.api.fence.HistoryAlarmResponse;
import com.baidu.trace.api.fence.MonitoredStatusByLocationResponse;
import com.baidu.trace.api.fence.MonitoredStatusResponse;
import com.baidu.trace.api.fence.OnFenceListener;
import com.baidu.trace.api.fence.PolygonFence;
import com.baidu.trace.api.fence.PolylineFence;
import com.baidu.trace.api.fence.UpdateFenceRequest;
import com.baidu.trace.api.fence.UpdateFenceResponse;
import com.baidu.trace.model.CoordType;
import com.baidu.trace.model.StatusCodes;
import com.baidu.track.R;
import com.baidu.track.TrackApplication;
import com.baidu.track.dialog.FenceCreateDialog;
import com.baidu.track.dialog.FenceOperateDialog;
import com.baidu.track.utils.BitmapUtil;
import com.baidu.track.utils.Constants;
import com.baidu.track.utils.MapUtil;
import com.baidu.track.utils.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 围栏操作：创建、查询、更新、删除围栏，查询围栏历史报警信息
 */
public class CreateFenceActivity extends BaseActivity implements View.OnClickListener,
        BaiduMap.OnMapClickListener, BaiduMap.OnMapLoadedCallback,BaiduMap.OnMarkerClickListener{

    private TrackApplication trackApp = null;

    private ViewUtil viewUtil = null;

    private FenceType fenceType = FenceType.server;//围栏类型，默认为服务端
    private FenceShape fenceShape = FenceShape.circle;//围栏形状，默认为圆形
    private String fenceName = null;//创建、查询围栏时，输入的围栏名称
    private OnFenceListener fenceListener = null;//围栏监听器
    private FenceCreateDialog fenceCreateDialog = null;//围栏创建对话框

    private FenceCreateDialog.Callback createCallback = null;//围栏创建对话框回调接口
    private FenceOperateDialog fenceOperateDialog = null;//围栏操作对话框（查询历史报警、删除、更新）

    private MapUtil mapUtil = null;

    /**
     * 围栏覆盖物集合
     * key : fenceType_fenceId（如local_24, server_100）
     * value : Overlay实例
     */
    private Map<String, Overlay> overlays = new HashMap<>();

    /**
     * 围栏标识集合
     */
    private Map<String, Marker> markers = new HashMap<>();

    private long beginTime = 0;//围栏历史报警开始时间
    private long endTime = 0;//围栏历史报警结束时间

    private LatLng circleCenter = null;//圆形围栏中心点坐标（地图坐标类型）

    private List<com.baidu.trace.model.LatLng> traceVertexes = new ArrayList<>();//顶点坐标（轨迹坐标类型）

    private List<LatLng> mapVertexes = new ArrayList<>();//顶点坐标（地图坐标类型）

    private int vertexesNumber = 3;//多边形、多段线围栏默认顶点数
    private double radius = 100;//圆形围栏默认半径
    private int denoise = 0;//去噪（默认不去噪）
    private int offset = 200;//偏离距离（默认200米）

    private Map<Integer, LatLng> tempLatLngs = new HashMap<>();//创建多边形、多段线围栏时，临时存储顶点坐标
    private Map<Integer, Overlay> tempOverlays = new HashMap<>();// 创建多边形、多段线围栏时，临时存储围栏覆盖物
    private List<Overlay> tempMarks = new ArrayList<>();//创建多边形、多段线围栏时，临时存储顶点覆盖物

    private String fenceKey;//操作围栏时，正在操作的围栏标识，格式：fenceType_fenceId

    private int vertexIndex = 0;//创建多边形、多段线围栏时，正在操作的顶点下标

    private LBSTraceClient mClient = null;//轨迹客户端

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.fence_title);
        setOnClickListener(this);
        initTrace();//初始化轨迹服务
        initMap();//初始化地图
        initListener();
    }

    private void initTrace() {
        trackApp = (TrackApplication) getApplication();
        mClient = new LBSTraceClient(this);
    }

    private void initMap() {
        mapUtil = MapUtil.getInstance();
        mapUtil.init((MapView) findViewById(R.id.fence_mapView));
        // 设置地图加载监听
        mapUtil.baiduMap.setOnMapLoadedCallback(this);
        // 设置maker点击时的响应
        mapUtil.baiduMap.setOnMarkerClickListener(this);

        fenceOperateDialog = new FenceOperateDialog(this);
        viewUtil = new ViewUtil();
    }

    @Override
    public void onClick(View view) {
        long fenceId;
        String[] fenceKeys;
        switch (view.getId()) {
            // 围栏设置（创建、查询围栏）
            case R.id.btn_activity_options:
                Intent intent = new Intent(CreateFenceActivity.this,CreateFenceOptions.class);
                startActivityForResult(intent, Constants.REQUEST_CODE);
                break;

            // 围栏报警
            case R.id.btn_fenceOperate_alarm:

                break;

            // 更新围栏
            case R.id.btn_fenceOperate_update:

                break;

            // 删除围栏
            case R.id.btn_fenceOperate_delete:
                List<Long> deleteFenceIds = new ArrayList<>();
                fenceKeys = fenceKey.split("_");
                fenceType = FenceType.valueOf(fenceKeys[0]);
                fenceId = Long.parseLong(fenceKeys[1]);
                deleteFenceIds.add(fenceId);
                DeleteFenceRequest deleteRequest;
                if (FenceType.server == fenceType) {
                    deleteRequest = DeleteFenceRequest.buildServerRequest(trackApp.getTag(),
                            trackApp.serviceId, trackApp.entityName, deleteFenceIds);
                } else {
                    deleteRequest = DeleteFenceRequest.buildLocalRequest(trackApp.getTag(),
                            trackApp.serviceId, trackApp.entityName, deleteFenceIds);
                }
                mClient.deleteFence(deleteRequest, fenceListener);
                break;

            default:
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (null == data) {
            return;
        }

        if (data.hasExtra("fenceType")) {
            FenceType fenceType = FenceType.valueOf(data.getStringExtra("fenceType"));
            this.fenceType = fenceType;
        }

        if (data.hasExtra("fenceShape")) {
            FenceShape fenceShape = FenceShape.valueOf(data.getStringExtra("fenceShape"));
            this.fenceShape = fenceShape;
        }

        if (data.hasExtra("fenceName")) {
            this.fenceName = data.getStringExtra("fenceName");
        }

        if (data.hasExtra("vertexesNumber")) {
            this.vertexesNumber = data.getIntExtra("vertexesNumber",3);
        }

        int operateType = 0;
        if (data.hasExtra("operateType")) {
            operateType = data.getIntExtra("operateType",3);
        }

        switch (operateType) {
            case R.id.btn_create_fence:
                mapUtil.baiduMap.setOnMapClickListener(CreateFenceActivity.this);
                break;

            case R.id.btn_fence_list:
                queryFenceList(fenceType);
                break;
        }

    }

    /**
     * 地图加载成功后，查询本地与服务端围栏
     */
    @Override
    public void onMapLoaded() {
        queryFenceList(FenceType.local);
        queryFenceList(FenceType.server);
    }

    /**
     * @param latLng
     */
    @Override
    public void onMapClick(LatLng latLng) {

        switch (fenceShape) {
            case circle:
                circleCenter = latLng;
                break;

            case polygon:
            case polyline:
                mapVertexes.add(latLng);//本地画图点集合
                traceVertexes.add(mapUtil.convertMap2Trace(latLng));//发送服务端点集合
                vertexIndex++;
                BitmapUtil.getMark(trackApp, vertexIndex);
                OverlayOptions overlayOptions = new MarkerOptions().position(latLng)
                        .icon(BitmapUtil.getMark(trackApp, vertexIndex)).zIndex(9).draggable(true);
                tempMarks.add(mapUtil.baiduMap.addOverlay(overlayOptions));
                break;
            default:
                break;
        }

        if (null == fenceCreateDialog) {
            fenceCreateDialog = new FenceCreateDialog(this, createCallback);
        }
        if (FenceShape.circle == fenceShape || vertexIndex == vertexesNumber) {//定点数相同
            fenceCreateDialog.setFenceType(fenceType);
            fenceCreateDialog.setFenceShape(fenceShape);
            fenceCreateDialog.show();
        }
    }

    @Override
    public boolean onMapPoiClick(MapPoi mapPoi) {
        return false;
    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        Iterator<Map.Entry<String, Marker>> markerIt = markers.entrySet().iterator();
        while (markerIt.hasNext()) {
            Map.Entry<String, Marker> entry = markerIt.next();

            if(entry.getValue() == marker){
                fenceKey = entry.getKey();//找到marker的唯一标识
            }

        }

        fenceOperateDialog.showAtLocation(findViewById(R.id.layout_top),
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 0);

        // 处理PopupWindow在Android N系统上的兼容性问题
        if (Build.VERSION.SDK_INT < 24) {
            fenceOperateDialog.update(fenceOperateDialog.getWidth(), fenceOperateDialog.getHeight());
        }

        return false;
    }

    //创建围栏
    private void createFence(int tag) {
        CreateFenceRequest request = null;
        switch (fenceType) {
            case local:
                request = CreateFenceRequest.buildLocalCircleRequest(tag, trackApp.serviceId, fenceName,
                        trackApp.entityName, mapUtil.convertMap2Trace(circleCenter), radius, denoise, CoordType.bd09ll);
                break;

            case server:
                switch (fenceShape) {
                    case circle:
                        request = CreateFenceRequest.buildServerCircleRequest(tag, trackApp.serviceId, fenceName,
                                trackApp.entityName, mapUtil.convertMap2Trace(circleCenter), radius, denoise,
                                CoordType.bd09ll);
                        break;

                    case polygon:
                        request = CreateFenceRequest.buildServerPolygonRequest(tag,
                                trackApp.serviceId, fenceName, trackApp.entityName, traceVertexes, denoise,
                                CoordType.bd09ll);
                        break;

                    case polyline:
                        request = CreateFenceRequest.buildServerPolylineRequest(tag,
                                trackApp.serviceId, fenceName, trackApp.entityName, traceVertexes, offset, denoise,
                                CoordType.bd09ll);
                        break;

                    default:
                        break;
                }
                break;

            default:
                break;
        }

        mClient.createFence(request, fenceListener);
    }

    //请求围栏列表
    private void queryFenceList(FenceType fenceType) {
        FenceListRequest request = null;
        switch (fenceType) {
            case local:
                request = FenceListRequest.buildLocalRequest(trackApp.getTag(),
                        trackApp.serviceId, trackApp.entityName, null);
                break;

            case server:
                request = FenceListRequest.buildServerRequest(trackApp.getTag(),
                        trackApp.serviceId, trackApp.entityName, null, CoordType.bd09ll);
                break;

            default:
                break;
        }

        mClient.queryFenceList(request, fenceListener);
    }

    private void clearOverlay() {
        if (null != overlays) {
            for (Map.Entry<String, Overlay> entry : overlays.entrySet()) {
                entry.getValue().remove();
            }
            overlays.clear();
        }

        if (null != markers) {
            for (Map.Entry<String, Marker> entry : markers.entrySet()) {
                entry.getValue().remove();
            }
            markers.clear();
        }
    }

    private void initListener() {

        createCallback = new FenceCreateDialog.Callback() {

            private int tag;

            @Override
            public void onSureCallback(double radius, int denoise, int offset) {
                CreateFenceActivity.this.radius = radius;
                CreateFenceActivity.this.denoise = denoise;
                CreateFenceActivity.this.offset = offset;

                OverlayOptions overlayOptions = null;
                tag = trackApp.getTag();

                if (FenceShape.circle == fenceShape) {
                    if (FenceType.local == fenceType) {//地图上画图层
                        overlayOptions = new CircleOptions().fillColor(0x000000FF).center(circleCenter)
                                .stroke(new Stroke(5, Color.rgb(0x23, 0x19, 0xDC))).radius((int) radius);
                    } else {
                        overlayOptions = new CircleOptions().fillColor(0x000000FF).center(circleCenter)
                                .stroke(new Stroke(5, Color.rgb(0xFF, 0x06, 0x01))).radius((int) radius);
                    }
                    tempLatLngs.put(tag, circleCenter);
                } else if (FenceShape.polygon == fenceShape) {
                    overlayOptions = new PolygonOptions().points(mapVertexes)
                            .stroke(new Stroke(mapVertexes.size(), Color.rgb(0xFF, 0x06, 0x01)))
                            .fillColor(0x30FFFFFF);
                    tempLatLngs.put(tag, mapVertexes.get(0));
                } else if (FenceShape.polyline == fenceShape) {
                    overlayOptions = new PolylineOptions().points(mapVertexes).width(10)
                            .color(Integer.valueOf(Color.RED));
                    tempLatLngs.put(tag, mapVertexes.get(0));
                }

                tempOverlays.put(tag, mapUtil.baiduMap.addOverlay(overlayOptions));
                mapUtil.baiduMap.setOnMapClickListener(null);

                createFence(tag);
            }

            @Override
            public void onCancelCallback() {
                if (tempOverlays.containsKey(tag)) {
                    tempOverlays.get(tag).remove();
                    tempOverlays.remove(tag);
                }
                for (Overlay overlay : tempMarks) {
                    overlay.remove();
                }
                tempMarks.clear();//取消创建，则清空
                mapVertexes.clear();
                traceVertexes.clear();
                vertexIndex = 0;
                mapUtil.baiduMap.setOnMapClickListener(null);
            }
        };

        fenceListener = new OnFenceListener() {
            @Override
            public void onCreateFenceCallback(CreateFenceResponse response) {

                int tag = response.getTag();
                if (StatusCodes.SUCCESS == response.getStatus()) {
                    String fenceKey = response.getFenceType() + "_" + response.getFenceId();

                    Overlay overlay = tempOverlays.get(tag);
                    //overlay.setVisible(false);
                    overlays.put(fenceKey, overlay);
                    tempOverlays.remove(tag);

                    if (tempLatLngs.containsKey(tag)) {
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(tempLatLngs.get(tag)).icon(BitmapUtil.bmGcoding)
                                .draggable(false);
                        Marker marker = (Marker) mapUtil.baiduMap.addOverlay(markerOptions);
                        markers.put(fenceKey, marker);
                        tempLatLngs.remove(tag);
                    }

                    viewUtil.showToast(CreateFenceActivity.this,
                            response.getMessage() + "," + getString(R.string.fence_operate_caption));
                } else {
                    tempOverlays.get(tag).remove();
                    tempOverlays.remove(tag);
                }
                for (Overlay overlay : tempMarks) {
                    overlay.remove();
                }
                tempMarks.clear();//创建完成，清空
                mapVertexes.clear();
                traceVertexes.clear();
                vertexIndex = 0;

            }

            @Override
            public void onUpdateFenceCallback(UpdateFenceResponse response) {

            }

            @Override
            public void onDeleteFenceCallback(DeleteFenceResponse response) {
                viewUtil.showToast(CreateFenceActivity.this, response.getMessage());
                List<Long> fenceIds = response.getFenceIds();
                if (null == fenceIds || fenceIds.isEmpty()) {
                    return;
                }

                FenceType fenceType = response.getFenceType();

                Iterator<Map.Entry<String, Overlay>> overlayIt = overlays.entrySet().iterator();
                while (overlayIt.hasNext()) {
                    Map.Entry<String, Overlay> entry = overlayIt.next();
                    long fenceId = Long.parseLong(entry.getKey().split("_")[1]);
                    String fenceKey = fenceType + "_" + fenceId;
                    if (fenceIds.contains(fenceId) && entry.getKey().equals(fenceKey)) {
                        entry.getValue().remove();
                        overlayIt.remove();//删除围栏对应地图的图层
                    }
                }

                Iterator<Map.Entry<String, Marker>> markerIt = markers.entrySet().iterator();
                while (markerIt.hasNext()) {
                    Map.Entry<String, Marker> entry = markerIt.next();
                    long fenceId = Long.parseLong(entry.getKey().split("_")[1]);
                    String fenceKey = fenceType + "_" + fenceId;
                    if (fenceIds.contains(fenceId) && entry.getKey().equals(fenceKey)) {
                        entry.getValue().remove();
                        markerIt.remove();//删除围栏对应地图的图层
                    }
                }

                mapUtil.refresh();
            }

            @Override
            public void onFenceListCallback(FenceListResponse response) {

                System.out.println(response.getStatus());
                if (StatusCodes.SUCCESS != response.getStatus()) {
                    viewUtil.showToast(CreateFenceActivity.this, response.getMessage());
                    return;
                }
                if (0 == response.getSize()) {
                    StringBuffer message = new StringBuffer("未查询到");
                    if (FenceType.local == response.getFenceType()) {
                        message.append("本地围栏");
                    } else {
                        message.append("服务端围栏");
                    }
                    viewUtil.showToast(CreateFenceActivity.this, message.toString());

                    return;
                }

                viewUtil.showToast(CreateFenceActivity.this, getString(R.string.fence_operate_caption));

                clearOverlay();

                FenceType fenceType = response.getFenceType();

                List<FenceInfo> fenceInfos = response.getFenceInfos();
                List<LatLng> points = new ArrayList<>();
                String fenceKey;
                for (FenceInfo fenceInfo : fenceInfos) {
                    Overlay overlay;
                    switch (fenceInfo.getFenceShape()) {
                        case circle:
                            CircleFence circleFence = fenceInfo.getCircleFence();
                            fenceKey = fenceType + "_" + circleFence.getFenceId();

                            LatLng latLng = MapUtil.convertTrace2Map(circleFence.getCenter());
                            double radius = circleFence.getRadius();
                            CircleOptions circleOptions = new CircleOptions().fillColor
                                    (0x000000FF).center(latLng)
                                    .radius((int) radius);
                            if (FenceType.local == fenceType) {
                                circleOptions.stroke(new Stroke(5, Color.rgb(0x23,
                                        0x19, 0xDC)));
                                overlay = mapUtil.baiduMap.addOverlay(circleOptions);
                                //overlay.setVisible(false);
                                overlays.put(fenceKey, overlay);
                            } else {
                                circleOptions.stroke(new Stroke(5, Color.rgb(0xFF,
                                        0x06, 0x01)));
                                overlay = mapUtil.baiduMap.addOverlay(circleOptions);
                                //overlay.setVisible(false);
                                overlays.put(fenceKey, overlay);
                            }
                            MarkerOptions circleMarker = new MarkerOptions()
                                    .position(latLng).icon(BitmapUtil.bmGcoding)
                                    .draggable(false);
                            Marker marker1 = (Marker) mapUtil.baiduMap.addOverlay(circleMarker);
                            markers.put(fenceKey,marker1);
                            points.add(latLng);
                            break;

                        case polygon:
                            PolygonFence polygonFence = fenceInfo.getPolygonFence();
                            fenceKey = fenceType + "_" + polygonFence.getFenceId();
                            List<com.baidu.trace.model.LatLng> polygonVertexes = polygonFence.getVertexes();
                            List<LatLng> mapVertexes1 = new ArrayList<>();
                            for (com.baidu.trace.model.LatLng ll : polygonVertexes) {
                                mapVertexes1.add(MapUtil.convertTrace2Map(ll));
                            }
                            PolygonOptions polygonOptions = new PolygonOptions().points(mapVertexes1)
                                    .stroke(new Stroke(mapVertexes1.size(), Color.rgb(0xFF, 0x06, 0x01)))
                                    .fillColor(0x30FFFFFF);
                            overlay = mapUtil.baiduMap.addOverlay(polygonOptions);
                            overlays.put(fenceKey, overlay);

                            MarkerOptions polygonMarker = new MarkerOptions()
                                    .position(mapVertexes1.get(0)).icon(BitmapUtil.bmGcoding)
                                    .draggable(false);
                            Marker marker2 = (Marker) mapUtil.baiduMap.addOverlay(polygonMarker);
                            markers.put(fenceKey,marker2);
                            points.add(mapVertexes1.get(0));
                            break;

                        case polyline:
                            PolylineFence polylineFence = fenceInfo.getPolylineFence();
                            fenceKey = fenceType + "_" + polylineFence.getFenceId();
                            List<com.baidu.trace.model.LatLng> polylineVertexes = polylineFence.getVertexes();
                            List<LatLng> mapVertexes2 = new ArrayList<>();
                            for (com.baidu.trace.model.LatLng ll : polylineVertexes) {
                                mapVertexes2.add(MapUtil.convertTrace2Map(ll));
                            }
                            PolylineOptions polylineOptions = new PolylineOptions().points(mapVertexes2)
                                    .color(Color.rgb(0xFF, 0x06, 0x01));
                            overlay = mapUtil.baiduMap.addOverlay(polylineOptions);
                            overlays.put(fenceKey, overlay);

                            MarkerOptions polylineMarker = new MarkerOptions()
                                    .position(mapVertexes2.get(0)).icon(BitmapUtil.bmGcoding)
                                    .draggable(false);
                            Marker marker3 = (Marker) mapUtil.baiduMap.addOverlay(polylineMarker);
                            markers.put(fenceKey,marker3);

                            points.add(mapVertexes2.get(0));
                            break;

                        default:
                            break;
                    }

                }

                mapUtil.animateMapStatus(points);//
            }

            @Override
            public void onMonitoredStatusCallback(MonitoredStatusResponse response) {

            }

            @Override
            public void onMonitoredStatusByLocationCallback(MonitoredStatusByLocationResponse response) {

            }

            @Override
            public void onHistoryAlarmCallback(HistoryAlarmResponse response) {

            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapUtil.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapUtil.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        clearOverlay();
        if (null != fenceCreateDialog) {
            fenceCreateDialog.dismiss();
            fenceCreateDialog = null;
        }
        if (null != fenceOperateDialog) {
            fenceOperateDialog.dismiss();
            fenceOperateDialog = null;
        }

        mapUtil.clear();
        mClient.clear();
    }

    @Override
    protected int getContentViewId() {
        return R.layout.activity_fence;
    }

}
