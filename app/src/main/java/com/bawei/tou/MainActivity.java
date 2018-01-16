package com.bawei.tou;

        import android.content.Intent;
        import android.database.Cursor;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.net.Uri;
        import android.os.Environment;
        import android.provider.MediaStore;
        import android.support.v7.app.AppCompatActivity;
        import android.os.Bundle;
        import android.util.Base64;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageView;

        import java.io.ByteArrayOutputStream;
        import java.io.File;
        import java.io.FileNotFoundException;
        import java.io.FileOutputStream;
        import java.io.IOException;
        import java.io.InputStream;

public class MainActivity extends AppCompatActivity {
    private static int CAMERA_REQUEST_CODE = 1;//摄像头返回data
    private static int GALLERY_REQUEST_CODE = 2;//图库返回data
    private static int CROP_REQUEST_CODE = 3;//裁剪返回data
    private Button btn_camera;//摄像头点击按钮
    private Button btn_gallery;//图库点击按钮
    private String url_getHeadImage = "http://www.jcpeixun.com/app_client_api/userinfo.aspx?uid=450894";//获得头像
    private String url_postHeadImage = "http://www.jcpeixun.com/app_client_api/upload_uimg.aspx";//上传头像
    private File tmpDir;//图片文件夹
    private File picture;//图片文件
    private Uri uri_picture;//统一资源标识符
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_camera = (Button) findViewById(R.id.btn_camera);//拍照
        btn_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, CAMERA_REQUEST_CODE);//启动摄像头
            }
        });

        btn_gallery = (Button) findViewById(R.id.btn_gallery);//图库
        btn_gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_REQUEST_CODE);//启动系统图库
            }
        });
    }
    //将头像保存到sd卡（返回值是一个file类型的uri）
    private Uri saveBitmap(Bitmap bm) {
        tmpDir = new File(Environment.getExternalStorageDirectory() + "/com.jikexueyuan.avater");
        if (!tmpDir.exists()) {
            tmpDir.mkdir();
        }
        picture = new File(tmpDir.getAbsolutePath() + "avater.png");
        try {
            FileOutputStream fos = new FileOutputStream(picture);
            bm.compress(Bitmap.CompressFormat.PNG, 85, fos);
            fos.flush();
            fos.close();
            return Uri.fromFile(picture);//返回uri
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    //将头像保存到sd卡并将content类型的uri装换成file类型的uri( uri(content) - bitmap - uri(file) )
    private Uri convertUri(Uri uri) {
        InputStream is = null;
        try {
            is = getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            is.close();
            return saveBitmap(bitmap);//将头像保存到sd卡
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //图片裁剪
    private void startImageZoom(Uri uri) {
        Intent intent = new Intent("com.android.camera.action.CROP");//启动裁剪界面
        intent.setDataAndType(uri, "image/*");//传入uri资源，类型为image
        intent.putExtra("crop", "true");//设置为可裁剪
        intent.putExtra("aspectX", 1);//aspect要裁剪的宽高比例（这里为1:1）
        intent.putExtra("aspectY", 1);
        intent.putExtra("outputX", 150);//output最终输出图片的宽和高
        intent.putExtra("outputY", 150);
        intent.putExtra("return-data", true);//设置裁剪之后的数据通过intent返回回来
        startActivityForResult(intent, CROP_REQUEST_CODE);
    }
    //数据返回
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CAMERA_REQUEST_CODE) {//摄像头
            if (data == null) {
                return;//用户点击取消则直接返回
            } else {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap bm = extras.getParcelable("data");
                    uri_picture = saveBitmap(bm);//将文件保存到sd卡（直接是file类型的bitmap）
                    startImageZoom(uri_picture);
                }
            }
        } else if (requestCode == GALLERY_REQUEST_CODE) {//图库
            if (data == null) {
                return;
            }
            uri_picture = data.getData();
            Uri fileUri = convertUri(uri_picture);//将content类型的uri转换成file类型的uri，，必须是file类型的uri(一般uri分file和content两种类型)
            startImageZoom(fileUri);
        } else if (requestCode == CROP_REQUEST_CODE) {//得到图片裁剪后的数据
            if (data == null) {//用户点击取消则直接返回
                return;
            }
            Bundle extras = data.getExtras();
            if (extras == null) {
                return;
            }
            Bitmap bm = extras.getParcelable("data");
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageBitmap(bm);//将图片显示在界面
            sendImage(bm);//将数据发送到服务器
        }
    }
    //将Bitmap转换成字符串发送到服务器
    private void sendImage(Bitmap bm) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.PNG, 60, stream);
        byte[] bytes = stream.toByteArray();
        String img = new String(Base64.encodeToString(bytes, Base64.DEFAULT));//接口为base64字符串时调用

        File img2 = new File(getRealPathFromURI(uri_picture.fromFile(picture)));//接口为file时调用
//        getVolley(img2);//xUtils方式
//        getAsync(img2);//async方式

    }

    //接口为file时调用该方法
    private String getRealPathFromURI(Uri contentURI) {
        String result;
        Cursor cursor = getContentResolver().query(contentURI, null, null, null, null);
        if (cursor == null) { // Source is Dropbox or other similar local file path
            result = contentURI.getPath();
        } else {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }
        return result;
    }


}
