package com.Edoctor.activity;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ZoomControls;

import com.Edoctor.activity.map.overlayutil.DrivingRouteOverlay;
import com.Edoctor.activity.map.overlayutil.TransitRouteOverlay;
import com.Edoctor.activity.map.overlayutil.WalkingRouteOverlay;
import com.baidu.location.BDAbstractLocationListener;
import com.baidu.location.BDLocation;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;

import butterknife.BindView;
import butterknife.ButterKnife;


public class MainActivity extends Activity {


    @BindView(R.id.nav_map)
    MapView mNavMap;
    @BindView(R.id.hospital_tv)
    TextView mHospitalTv;
    @BindView(R.id.car_tv)
    TextView mCarTv;
    @BindView(R.id.bus_tv)
    TextView mBusTv;
    @BindView(R.id.walk_tv)
    TextView mWalkTv;
    private BaiduMap mBaiduMap;
    private LocationClient mLocationClient;
    private BDAbstractLocationListener myLitenner = new MyLocationListener();
    private boolean isFirstLocation = true;
    private RoutePlanSearch mSearch;
    private BDLocation mLocation;
    private int mDistance;
    private int mDuration;
    Dialog dialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        ButterKnife.bind(this);
        initDialog();
        initMapView();
        initLocationClient();

    }

    private void initLocationClient() {
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(myLitenner);//注册监听函数
        setLocationOption();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationClient.start();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, 100);
        }
    }

    private void initMapView() {
        // 隐藏百度的LOGO
        View child = mNavMap.getChildAt(1);
        if (child != null && (child instanceof ImageView || child instanceof ZoomControls)) {
            child.setVisibility(View.INVISIBLE);
        }

        // 不显示地图上比例尺
        mNavMap.showScaleControl(false);

        // 不显示地图缩放控件（按钮控制栏）
        mNavMap.showZoomControls(false);
        mBaiduMap = mNavMap.getMap();
        //开启定位图层
        mBaiduMap.setMyLocationEnabled(true);
    }

    private void initDialog() {
        dialog = new Dialog(this);
        dialog.setContentView(R.layout.progress_layout);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        dialog.getWindow().getDecorView().setBackgroundColor(Color.argb(0, 0, 0, 0));
    }

    @butterknife.OnClick({R.id.nav_map, R.id.hospital_tv, R.id.car_tv, R.id.bus_tv, R.id.walk_tv})
    public void onClick(View v) {
        switch (v.getId()) {
            default:
                break;
            case R.id.nav_map:
                break;
            case R.id.hospital_tv:
                break;
            case R.id.car_tv:
                driving();
                break;
            case R.id.bus_tv:
                transit();
                break;
            case R.id.walk_tv:
                walking();
                break;
        }
    }

    private void walking() {

        dialog.show();
        PlanNode dNode = PlanNode.withCityNameAndPlaceName("武汉", "光谷广场");
        PlanNode sNode = PlanNode.withLocation(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        mSearch.walkingSearch(new WalkingRoutePlanOption()
                .from(sNode).to(dNode));
    }

    private void transit() {
        PlanNode dNode = PlanNode.withCityNameAndPlaceName("武汉", "光谷广场");
        PlanNode sNode = PlanNode.withLocation(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        mSearch.transitSearch(new TransitRoutePlanOption().city("武汉")
                .from(sNode).to(dNode));
    }

    private void driving() {
        PlanNode dNode = PlanNode.withCityNameAndPlaceName("武汉", "光谷广场");
        PlanNode sNode = PlanNode.withLocation(new LatLng(mLocation.getLatitude(), mLocation.getLongitude()));
        mSearch.drivingSearch(new DrivingRoutePlanOption()
                .from(sNode).to(dNode));
    }

    /**
     * 设置定位参数
     */
    private void setLocationOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true);//打开GPS
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);//设置定位模式
        option.setCoorType("bd09ll");//返回的定位结果是百度经纬度，默认值是gcj02
        option.setScanSpan(5000);//设置发起定位请求的时间间隔为5000ms
        option.setIsNeedAddress(true);//返回的定位结果饱饭地址信息
        option.setNeedDeviceDirect(true);// 返回的定位信息包含手机的机头方向
        mLocationClient.setLocOption(option);
    }


    private class MyLocationListener extends BDAbstractLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            //MAP VIEW 销毁后不在处理新接收的位置
            if (location == null || mNavMap == null)
                return;
            MyLocationData locData = new MyLocationData.Builder()
                    //此处设置开发者获取到的方向信息，顺时针0-360
                    .accuracy(location.getRadius())
                    .direction(100).latitude(location.getLatitude())
                    .longitude(location.getLongitude()).build();
            mBaiduMap.setMyLocationData(locData);
            //设置定位数据
            if (isFirstLocation) {
                isFirstLocation = false;
                LatLng ll = new LatLng(location.getLatitude(), location.getLongitude());
                MapStatusUpdate mapStatusUpdate = MapStatusUpdateFactory.newLatLngZoom(ll, 16);
                //设置地图中心点以及缩放级别
                mBaiduMap.animateMapStatus(mapStatusUpdate);

            }
            Log.d("aa", "onReceiveLocation: " + location.getLatitude() + "--->" + location.getLongitude());
            mLocation = location;
            mSearch = RoutePlanSearch.newInstance();
            mSearch.setOnGetRoutePlanResultListener(new OnGetRoutePlanResultListener() {
                @Override
                public void onGetWalkingRouteResult(WalkingRouteResult walkingRouteResult) {
                    dialog.dismiss();
                    if (walkingRouteResult == null || walkingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                        Toast.makeText(MainActivity.this, "路很长，慢慢走",
                                Toast.LENGTH_SHORT).show();
                    }
                    if (walkingRouteResult.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                        return;
                    }
                    if (walkingRouteResult.error == SearchResult.ERRORNO.NO_ERROR) {
                        WalkingRouteOverlay overlay = new MyWalkingRouteOverlay(
                                mBaiduMap);
                        mBaiduMap.setOnMarkerClickListener(overlay);
                        WalkingRouteLine walkingRouteLine = walkingRouteResult.getRouteLines().get(0);
                        mDistance = walkingRouteLine.getDistance();
                        mDuration = walkingRouteLine.getDuration();
                        overlay.setData(walkingRouteLine);
                        overlay.addToMap();
                        overlay.zoomToSpan();
                        mHospitalTv.setText("出行方式：驾车\t" + "距离：" + mDistance / 1000.0 + "km\t" + "需要时间：" + getTime(mDuration));
                    }
                }

                @Override
                public void onGetTransitRouteResult(TransitRouteResult transitRouteResult) {
                    dialog.dismiss();
                    if (transitRouteResult == null || transitRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                        Toast.makeText(MainActivity.this, "路很长，慢慢走",
                                Toast.LENGTH_SHORT).show();
                    }
                    if (transitRouteResult.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                        return;
                    }
                    if (transitRouteResult.error == SearchResult.ERRORNO.NO_ERROR) {
                        TransitRouteOverlay overlay = new MyTransitRouteOverlay(
                                mBaiduMap);
                        mBaiduMap.setOnMarkerClickListener(overlay);
                        TransitRouteLine transitRouteLine = transitRouteResult.getRouteLines().get(0);
                        mDistance = transitRouteLine.getDistance();
                        mDuration = transitRouteLine.getDuration();
                        for (TransitRouteLine routeLine : transitRouteResult.getRouteLines()) {
                            overlay.setData(routeLine);
                            overlay.addToMap();
                            overlay.zoomToSpan();
                        }
//                        overlay.setData(transitRouteLine);
//                        overlay.addToMap();
//                        overlay.zoomToSpan();
                        mHospitalTv.setText("出行方式：驾车\t" + "距离：" + mDistance / 1000.0 + "km\t" + "需要时间：" + getTime(mDuration));
                    }

                }

                @Override
                public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

                }

                @Override
                public void onGetDrivingRouteResult(DrivingRouteResult drivingRouteResult) {
                    dialog.dismiss();
                    if (drivingRouteResult == null || drivingRouteResult.error != SearchResult.ERRORNO.NO_ERROR) {
                        Toast.makeText(MainActivity.this, "路很长，慢慢走",
                                Toast.LENGTH_SHORT).show();
                    }
                    if (drivingRouteResult.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
                        return;
                    }
                    if (drivingRouteResult.error == SearchResult.ERRORNO.NO_ERROR) {
                        DrivingRouteOverlay overlay = new MyDrivingRouteOverlay(
                                mBaiduMap);
                        mBaiduMap.setOnMarkerClickListener(overlay);
                        DrivingRouteLine drivingRouteLine = drivingRouteResult.getRouteLines().get(0);
                        mDistance = drivingRouteLine.getDistance();
                        mDuration = drivingRouteLine.getDuration();
                        overlay.setData(drivingRouteLine);
                        overlay.addToMap();
                        overlay.zoomToSpan();
                        mHospitalTv.setText("出行方式：驾车\t" + "距离：" + mDistance / 1000.0 + "km\t" + "需要时间：" + getTime(mDuration));
                        dialog.dismiss();
                    }

                }

                @Override
                public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

                }

                @Override
                public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

                }
            });


        }
    }


    public String getTime(int duration) {
        if (duration < 60) {
            return duration + "s";
        } else if (duration < 3600) {
            int min = duration / 60;
            int second = duration % 60;
            return min + "m" + second + "s";
        } else {
            int hour = duration / 3600;
            int min = 0;
            int second = 0;
            if ((duration % 3600) < 60) {
                second = duration % 3600;
            } else {
                second = (duration % 3600) % 60;
                min = (duration - hour * 3600 - second) / 60;
            }
            return hour + "h" + min + "m" + second + "s";
        }
    }


    class MyWalkingRouteOverlay extends WalkingRouteOverlay {

        public MyWalkingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromAsset("Icon_start.png");
        }


        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromAsset("Icon_end.png");
        }
    }

    class MyTransitRouteOverlay extends TransitRouteOverlay {

        /**
         * 构造函数
         *
         * @param baiduMap 该TransitRouteOverlay引用的 BaiduMap 对象
         */
        public MyTransitRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromAsset("Icon_start.png");
        }


        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromAsset("Icon_end.png");
        }
    }

    class MyDrivingRouteOverlay extends DrivingRouteOverlay {

        /**
         * 构造函数
         *
         * @param baiduMap 该DrivingRouteOvelray引用的 BaiduMap
         */
        public MyDrivingRouteOverlay(BaiduMap baiduMap) {
            super(baiduMap);
        }

        @Override
        public BitmapDescriptor getStartMarker() {
            return BitmapDescriptorFactory.fromAsset("Icon_start.png");
        }


        @Override
        public BitmapDescriptor getTerminalMarker() {
            return BitmapDescriptorFactory.fromAsset("Icon_end.png");
        }
    }


    //三个状态实现地图生命周期管理
    @Override
    protected void onDestroy() {
        //退出销毁
        mLocationClient.stop();
        mBaiduMap.setMyLocationEnabled(false);
        super.onDestroy();
        mNavMap.onDestroy();
        mNavMap = null;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mNavMap.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mNavMap.onPause();
    }


}