package com.example.satellite_num;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Iterator;

public class MainActivity extends AppCompatActivity {
    LocationManager locationManager;  //获取一个LocationManager对象，是提供位置定位服务的位置管理对象，中枢控制系统
    private TextView satellite;
    private TextView lal;
    private TextView use_satellite;
    private TextView all_satellite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        satellite = (TextView)this.findViewById(R.id.satellite);  //获取布局中的TextView
        lal = (TextView)this.findViewById(R.id.location);
        use_satellite = (TextView)findViewById(R.id.use_satellite);
        all_satellite = (TextView)findViewById(R.id.all_satellite);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);  //初始化LocationManager对象
        openGPSSettings();  //判断GPS是否打开

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){  //判断是否获得定位的运行时权限
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);  //若无，请求给与权限
        }
        else
        {
            Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            updateLaL(location);
        }

        LocationListener locationListener = new LocationListener() {  //位置监听器，不关心定位信息，所以为空实现，用于位置信息更新回调函数
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String newlal = updateLaL(location);
                lal.setText(null);
                lal.setText(newlal);
                int use_num = location.getExtras().getInt("satellites");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }
        };

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){  //判断是否获得定位的运行时权限
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);  //若无，请求给与权限
        }
        else
        {
            locationManager.addGpsStatusListener(statusListener);  //添加卫星状态监听器
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,1,locationListener);  //每隔1000ms回调一次位置信息
        }
    }

    private void openGPSSettings(){
        if(locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)){  //检测GPS状态
            Toast.makeText(this,"GPS模块正常",Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this,"请开启GPS!",Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_SEARCH_SETTINGS); //利用intent对象返回开启GPS导航设置
        startActivityForResult(intent,0);
    }

    private String updateLaL(Location loc){
        StringBuilder LaL = new StringBuilder();
        if(loc != null)
        {
            double Latitude = loc.getLatitude();
            double Longitude = loc.getLongitude();
            LaL.append("经度："+Latitude+"\n纬度："+Longitude);
        }
        else
        {
            LaL.append("无位置信息！");
        }
        return LaL.toString();
    }

    private final GpsStatus.Listener statusListener = new GpsStatus.Listener(){  //卫星状态监听器
        @Override
        public void onGpsStatusChanged(int event) {  //如果状态发生变化
            if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){  //同样要判断是否有权限
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
            }
            else
            {
                GpsStatus status = locationManager.getGpsStatus(null);  //获得当前卫星状态
                updateGpsStatus(event,status);  //更新卫星数函数
                Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                try{
                    int use_num = location.getExtras().getInt("satellites");
                    String use_text = "使用卫星数：" + use_num;
                    use_satellite.setText(use_text);
                }catch (NullPointerException e){
                    use_satellite.setText("使用卫星数：获取失败");
                }
            }
        }
    };

    private void updateGpsStatus(int event, GpsStatus status){
        StringBuilder Num = new StringBuilder();  //stringbuilder对象，类似于string
        StringBuilder unuse = new StringBuilder();
        if(event == GpsStatus.GPS_EVENT_SATELLITE_STATUS){  //与当前状态相符
            int maxSatellites = status.getMaxSatellites();  //获取最大卫星数
            Iterator<GpsSatellite> it = status.getSatellites().iterator();  //获取所有卫星对象
            int count = 0;
            int max_count = 0;
            while(it.hasNext() && count <= maxSatellites){  //循环遍历所有卫星
                max_count++;
                GpsSatellite s = it.next();
                if(s.getSnr() > 0)  //信噪比判断，能获取到的卫星很多，但是只有信噪比大于0的才是真正起作用的卫星
                {
                    count++;
                    float snr = s.getSnr();
                    int prn = s.getPrn();
                    float azimuth = s.getAzimuth();
                    float elevation = s.getElevation();
                    if(s.usedInFix()){
                        Num.append("*第").append(count).append("颗：").append(snr).append(" PRN：").append(prn)
                                .append(" 方位角：").append(azimuth).append(" 高度：").append(elevation).append("\n");  //提取信息
                    }else{
                        unuse.append("第").append(count).append("颗：").append(snr).append(" PRN：").append(prn)
                                .append(" 方位角：").append(azimuth).append(" 高度：").append(elevation).append("\n");  //提取信息
                    }
                }
            }
            Num.append(unuse);
            Num.append("SNR>0：").append(count).append("\n");  //设置输出语句
            satellite.setText(Num);  //设置给布局中的TextView
            String all_text = "总卫星数：" + max_count;
            all_satellite.setText(all_text);
        }
    }
}
