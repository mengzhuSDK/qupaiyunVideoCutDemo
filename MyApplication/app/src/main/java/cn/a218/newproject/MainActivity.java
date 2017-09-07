package cn.a218.newproject;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.aliyun.common.utils.ToastUtil;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Intent recorder = new Intent("com.duanqu.qupai.action.recorder");
        startActivity(recorder);

//        if(key == R.mipmap.demo_camera_icon)
//            if(permissionGranted){
////                                Intent camera = new Intent(DemoActivity.this, SettingActivity.class);
////                                startActivity(camera);
//                Intent camera = new Intent("com.duanqu.qupai.action.camera");
//                startActivity(camera);
//            }else{
//                ToastUtil.showToast(DemoActivity.this,getString(R.string.need_permission));
//            }
//        else if(key == R.mipmap.demo_crop_icon) {
//            Intent crop = new Intent("com.duanqu.qupai.action.crop");
//            startActivity(crop);
//        } else if(key == R.mipmap.demo_edit_icon) {
//            Intent edit = new Intent("com.duanqu.qupai.action.import");
//            startActivity(edit);
//        } else if(key == R.mipmap.demo_vedio_icon){
//            Intent recorder = new Intent("com.duanqu.qupai.action.recorder");
//            startActivity(recorder);
//        } else if(key == R.mipmap.demo_ui_icon) {
//            Intent recorder = new Intent("com.duanqu.qupai.action.help");
//            startActivity(recorder);
//        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId())
        {
            case R.id.btn1:
                Intent camera = new Intent("com.duanqu.qupai.action.camera");
                startActivity(camera);
                break;
            case R.id.btn2:
                Intent crop = new Intent("com.duanqu.qupai.action.crop");
            startActivity(crop);
                break;
            case R.id.btn3:
                Intent edit = new Intent("com.duanqu.qupai.action.import");
            startActivity(edit);
            break;
            case R.id.btn4:
                Intent recorder = new Intent("com.duanqu.qupai.action.recorder");
            startActivity(recorder);
            break;
            case R.id.btn5:
                Intent help = new Intent("com.duanqu.qupai.action.help");
            startActivity(help);
            break;
        }
    }
}
