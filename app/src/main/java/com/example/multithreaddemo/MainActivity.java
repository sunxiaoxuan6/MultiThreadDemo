package com.example.multithreaddemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    //    进度条常量：Message.what类型
    private static final int START_NUM = 101;
    private static final int ADDING_NUM = 102;
    private static final int ENDING_NUM = 103;
    private static final int CANCEL_NUM = 104;

    MyTask myTask;

    private static final String DOWNLOAD_URL = "https://b-ssl.duitang.com/uploads/blog/" +
            "201312/04/20131204184148_hhXUT.jpeg";

    private static final String DOWNLOAD_URLS = "http://www.nipic.com/show/1/9/9849628.html";

    private CalculateThread calculateThread;

    private MyHandler myHandler = new MyHandler(this);



    private TextView textView;
    private ImageView imageView;
    private ProgressBar progressBar;

    private Button multithreading;
    private Button asynchronous;
    private Button handler;
    private Button asyncTask;
    private Button other;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        multithreading = findViewById(R.id.bt_multithreading);
        asynchronous = findViewById(R.id.bt_asynchronous);
        handler = findViewById(R.id.bt_handler);
        asyncTask = findViewById(R.id.bt_async_task);
        other = findViewById(R.id.bt_other);

        progressBar = findViewById(R.id.progress_bar);

        textView = findViewById(R.id.tv_text);
        imageView = findViewById(R.id.iv_image);

        multithreading.setOnClickListener(this);
        asyncTask.setOnClickListener(this);
        asynchronous.setOnClickListener(this);
        handler.setOnClickListener(this);
        other.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
//            多线程
            case R.id.bt_multithreading:
                calculateThread = new CalculateThread();
                calculateThread.start();
                break;
//            异步任务
            case R.id.bt_asynchronous:
                myTask = new MyTask();
                myTask.execute();
//                显示“已取消”
//                myTask.cancel(true);
                break;
//            Handler下载图片
            case R.id.bt_handler:
                new Thread(new DownloadImageFetcher(DOWNLOAD_URL)).start();
                break;
//            AsyncTask下载图片
            case R.id.bt_async_task:
               new MyAsyncTask(this).execute(DOWNLOAD_URLS);

                break;
//            其他异步方式
            case R.id.bt_other:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        other.setText("runOnUiThread方式更新");
                        textView.setText("runOnUiThread方式更新TextView的内容");
                    }
                });
//
//                textView.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        other.setText("View.post方式更新");
//                        textView.setText("View.post方式更新TextView的内容");
//                    }
//                });

                other.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        other.setText("View.postDelayed方式延迟3秒更新");
                        textView.setText("View.postDelayed方式延迟3秒更新TextView的内容");
                    }
                },3000);
                break;
        }

    }

    //自定义Handler静态类
    static class MyHandler extends Handler {
        //        定义弱引用对象
        private WeakReference<Activity> ref;

        //        在构造方法中创建此对象
        public MyHandler(Activity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//1、获取弱引用指向的Activity对象
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
//            2、根据Message的what属性值处理消息
            switch (msg.what) {
                case START_NUM:
                    activity.progressBar.setVisibility(View.VISIBLE);
                    break;
                case ADDING_NUM:
                    activity.progressBar.setProgress(msg.arg1);
                    activity.textView.setText("计算已完成" + msg.arg1 + "%");
                    break;
                case ENDING_NUM:
                    activity.progressBar.setVisibility(View.GONE);
                    activity.textView.setText("计算已完成，结果为：" + msg.arg1);
                    activity.myHandler.removeCallbacks(activity.calculateThread);
                    break;
                case CANCEL_NUM:
                    activity.progressBar.setProgress(0);
                    activity.progressBar.setVisibility(View.GONE);
                    activity.textView.setText("计算已取消");
                    break;
            }
        }
    }

    class CalculateThread extends Thread {
        @Override
        public void run() {
            int result = 0;
            boolean isCancel = false;

            myHandler.sendEmptyMessage(START_NUM);

            for (int i = 0; i < 101; i++) {
                try {
                    Thread.sleep(100);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isCancel = true;
                    break;
                }
                if (i % 5 == 0) {
                    Message msg = Message.obtain();
                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    myHandler.sendMessage(msg);
                }
            }
            if (!isCancel) {
                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage(msg);
            }
        }
    }

    private static final int MSG_SHOW_PROGRESS = 11;
    private static final int MSG_SHOW_IMAGE = 12;

    static class MyUIHandler extends Handler {
        //        定义弱引用对象
        private WeakReference<Activity> ref;

        //        在构造方法中
        public MyUIHandler(Activity activity) {
            this.ref = new WeakReference<>(activity);
        }

        //        重写handler方法
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
//            1、获取弱引用指向的Activity对象
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
//            2、根据Message的what属性值处理消息
            switch (msg.what) {
                case MSG_SHOW_PROGRESS:
                    activity.progressBar.setVisibility(View.VISIBLE);
                    break;
                case MSG_SHOW_IMAGE:
                    activity.progressBar.setVisibility(View.GONE);
                    activity.imageView.setImageBitmap((Bitmap) msg.obj);
                    break;
            }
        }
    }

    private MyUIHandler uiHandler = new MyUIHandler(this);

    //        下载图片的线程
    private class DownloadImageFetcher implements Runnable {
        private String imgUrl;

        public DownloadImageFetcher(String strUrl) {
            this.imgUrl = strUrl;
        }

        @Override
        public void run() {
            InputStream in = null;
//                发一个空消息到handleMessage()去处理，显示进度条
            uiHandler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();

            try {
//                    1、将url字符串转为URL对象
                URL url = new URL(imgUrl);
//                    2、打开url对象的http连接
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                    3、获取这个连接的输入流
                in = connection.getInputStream();
//                    4、将输入流解码为Bitmap图片
                Bitmap bitmap = BitmapFactory.decodeStream(in);
//                    5、通过handler发送消息
//                    uiHandler.obtainMessage(MSG_SHOW_IMAGE,bitmap).sendToTarget();
                Message msg = uiHandler.obtainMessage();
                msg.what = MSG_SHOW_IMAGE;
                msg.obj = bitmap;
                uiHandler.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }



    private class MyTask extends AsyncTask<String, Integer, Integer> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
//                执行前显示提示
            textView.setText("计算中");
        }

        @Override
        protected Integer doInBackground(String... strings) {
            int result = 0;
            boolean isCancel = false;
            for (int i = 0; i < 101; i++) {
                try {
                    Thread.sleep(100);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isCancel = true;
                    break;
                }
                if (i % 5 == 0) {
                    Message msg = Message.obtain();
                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    myHandler.sendMessage(msg);
                }
            }
            if (!isCancel) {
                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage(msg);
            }
            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setVisibility(values[0]);
            textView.setText("计算已完成" + values[0] + "%");
        }

        @Override
        protected void onPostExecute(Integer s) {
            progressBar.setVisibility(View.GONE);
            textView.setText("计算已完成，结果为：" + s);
        }

        @Override
        protected void onCancelled() {
            progressBar.setVisibility(View.GONE);
            textView.setText("计算已取消");
            progressBar.setProgress(0);
        }
    }


//    private final class MyAsyncTask extends AsyncTask<String, Integer, Bitmap> {
//
//        private String imgUrl;
//
//        public MyAsyncTask(String strUrl) {
//            this.imgUrl = strUrl;
//        }
//
//        InputStream in = null;
//
//        @Override
//        protected void onPreExecute() {
//            progressDialog.setMessage("正在下载网络图片，请稍等。。。");
//            progressDialog.show();
//        }
//
//        @Override
//        protected Bitmap doInBackground(String... strings) {
//
//            try {
////                    1、将url字符串转为URL对象
//                URL url = new URL(imgUrl);
////                    2、打开url对象的http连接
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//
//                connection.setConnectTimeout(3000);
////                    3、获取这个连接的输入流
//                InputStream inputStream = connection.getInputStream();
//
//                int size=connection.getContentLength();
//                publishProgress(0,size);
//
//                ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();
//                byte[] buffer=new byte[1024];
//
//                int len=0;
//                while ((len=inputStream.read(buffer))!=-1){
//                    byteArrayOutputStream.write(buffer,0,len);
//                    publishProgress(1,len);
//                }
//                byte[] data=byteArrayOutputStream.toByteArray();
////                    4、将字节数组还原成原始图片对象
////                Bitmap bitmap = BitmapFactory.decodeByteArray(data,0,data.length);
//                Bitmap bitmap=BitmapFactory.decodeStream(inputStream);
//
////                    5、通过handler发送消息
////                    uiHandler.obtainMessage(MSG_SHOW_IMAGE,bitmap).sendToTarget();
//                Message msg = uiHandler.obtainMessage();
//                msg.what = MSG_SHOW_IMAGE;
//                msg.obj = bitmap;
//                uiHandler.sendMessage(msg);
//
//
//                return bitmap;
//            } catch (IOException e) {
//                e.printStackTrace();
//            } finally {
//                if (in != null) {
//                    try {
//                        in.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            return null;
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            switch (values[0]) {
//                case 0:
//                    progressDialog.setMax(values[1]);
//                    break;
//                case 1:
//                    progressDialog.incrementProgressBy(values[1]);
//                    break;
//            }
////            super.onProgressUpdate(values);
//        }
//
//        @Override
//        protected void onPostExecute(Bitmap bitmap) {
//            //            关闭对话框
//            progressDialog.dismiss();
//            super.onPostExecute(bitmap);
//
//            if(bitmap==null){
//                Toast.makeText(MainActivity.this,"图片下载失败",Toast.LENGTH_LONG).show();
//                return;
//            }else {
////            显示图片
//                imageView.setImageBitmap(bitmap);
//            }
//        }
//    }

//    private class MyAsyncTask extends AsyncTask<String,Void,Bitmap>{
//
//        InputStream inputStream=null;
//        @Override
//        protected Bitmap doInBackground(String... strings) {
//            URL url=null;
//            Bitmap bitmap=null;
//            InputStream inputStream=null;
//            HttpURLConnection connection=null;
//
//            try{
//                url=new URL(strings[0]);
//            } catch (MalformedURLException e) {
//                e.printStackTrace();
//            }
//            try {
//                connection= (HttpURLConnection) url.openConnection();
//                connection.setDoInput(true);
//                connection.connect();
//                inputStream=connection.getInputStream();
//                bitmap=BitmapFactory.decodeStream(inputStream);
//                inputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }finally {
//                try {
//                    if(inputStream!=null)
//                    inputStream.close();
//                    if(connection!=null)
//                    {connection.disconnect();}
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//            return bitmap;
//        }
//
//        @Override
//        protected void onCancelled() {
//            super.onCancelled();
//        }
//
//        @Override
//        protected void onPostExecute(Bitmap bitmap) {
//            super.onPostExecute(bitmap);
//        }
//    }


//    AsyncTask下载图片
     private class MyAsyncTask extends AsyncTask<String,Bitmap,Bitmap> {
        private WeakReference<AppCompatActivity> reference;

        public MyAsyncTask(AppCompatActivity activity){
            this.reference=new WeakReference<>(activity);
        }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MainActivity activity= (MainActivity) this.reference.get();
    }

    @Override
    protected Bitmap doInBackground(String... strings) {
            String url=strings[0];
        return downloadImage(url);
    }

    private Bitmap downloadImage(String strUrl){
            InputStream stream=null;
            Bitmap bitmap=null;

            MainActivity activity= (MainActivity) this.reference.get();

            ByteArrayOutputStream byteArrayOutputStream=new ByteArrayOutputStream();

        try {
            URL url = new URL(strUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            int totalLen = connection.getContentLength();
            if (totalLen == 0) {
                activity.progressBar.setProgress(0);
            }

            if (connection.getResponseCode() == 200) {
                stream = connection.getInputStream();
                int len = -1;
                int progress = 0;
                byte[] tmps = new byte[1024];
                while ((len = stream.read(tmps)) != -1) {
                    progress += len;
                    activity.progressBar.setProgress(progress);
                    byteArrayOutputStream.write(tmps, 0, len);
                }
                bitmap = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(),
                        0, byteArrayOutputStream.size());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    @Override
    protected void onPostExecute(Bitmap bitmap) {
        super.onPostExecute(bitmap);

        MainActivity activity = (MainActivity) this.reference.get();
        if (bitmap != null) {
            activity.imageView.setImageBitmap(bitmap);
        }
    }
  }
}
