package com.baidu.track.utils;

import com.baidu.trace.api.fence.CreateFenceResponse;
import com.baidu.trace.api.fence.DeleteFenceResponse;
import com.baidu.trace.api.fence.FenceListResponse;
import com.baidu.trace.api.fence.HistoryAlarmResponse;
import com.baidu.trace.api.fence.MonitoredStatusByLocationResponse;
import com.baidu.trace.api.fence.MonitoredStatusResponse;
import com.baidu.trace.api.fence.OnFenceListener;
import com.baidu.trace.api.fence.UpdateFenceResponse;

/**
 * Created by zhuhao on 2017/7/9.
 */
public class AbstractFenceListener implements OnFenceListener {

    @Override
    public void onCreateFenceCallback(CreateFenceResponse createFenceResponse) {

    }

    @Override
    public void onUpdateFenceCallback(UpdateFenceResponse updateFenceResponse) {

    }

    @Override
    public void onDeleteFenceCallback(DeleteFenceResponse deleteFenceResponse) {

    }

    @Override
    public void onFenceListCallback(FenceListResponse fenceListResponse) {

    }

    @Override
    public void onMonitoredStatusCallback(MonitoredStatusResponse monitoredStatusResponse) {

    }

    @Override
    public void onMonitoredStatusByLocationCallback(MonitoredStatusByLocationResponse monitoredStatusByLocationResponse) {

    }

    @Override
    public void onHistoryAlarmCallback(HistoryAlarmResponse historyAlarmResponse) {

    }
}

