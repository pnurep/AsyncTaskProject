package com.example.gold.asynctaskproject;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {


    public static final int SET_TEXT = 100;

    boolean flag = false;

    TextView result;

    ProgressBar progressBar;

    Button btnStart, btnStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        result = (TextView) findViewById(R.id.textView);
        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (flag) {
                    Toast.makeText(MainActivity.this, "실행중입니다", Toast.LENGTH_SHORT).show();
                } else {
                    String filename = "BlockB - YESTERDAY.mp4";
                    new TestAsync().execute(filename);
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delFile("BlockB - YESTERDAY.mp4");
            }
        });
    }

    //파일삭제
    public void delFile(String filename) {
        String fullPath = getFullPath(filename);
        File file = new File(fullPath);
        if (file.exists()) {
            file.delete();
        }
    }


    public class TestAsync extends AsyncTask<String, Integer, Boolean> {
        //AsyncTask 제네릭이 가리키는것
        // 1. doInBackGround 의 파라미터
        // 2. onProgressUpdate 파라미터

        //AsyncTask의 백그라운드 프로세스 전에 호출되는 함수. 준비단계, 어떤인자도 받지 않는다.
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            flag = true;
            progressBar.setProgress(0);

            // 파일사이즈를 입력
            AssetManager manager = getAssets();
            try {
                // 파일 사이즈를 가져오기 위해 파일 스트림 생성
                InputStream is = manager.open("big.avi");
                int fileSize = is.available(); // stream 에 연결된 파일사이즈를 리턴해준다
                // 프로그래스바의 최대값에 파일사이즈 입력
                progressBar.setMax(fileSize);
                is.close();
            }catch(Exception e){
                e.printStackTrace();
            }

        }


        //sub Thread에서 실행되는 함수
        @Override
        protected Boolean doInBackground(String... params) { //스트링이 배열로 넘어온다
            String filename = params[0];
            assetToDisk(filename);
            return true;
        }


        //doInBackGround가 종료된후에 호출되는 함수
        //doInBackGround의 리턴값을 받는다다
        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if (aBoolean) {
                result.setText("완료되었습니다");
            }
        }

        //main Thread에서 실행되는 함수 -> 핸들러역할!!!
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            int sec = values[0];
            result.setText(sec + "sec");
            progressBar.setProgress(sec);
        }


        //assets에 있는 파일을 쓰기가능한 internal Storage 복사한다
        // - Internal Storage 의 경로구조
        //  /data/data/패키지명
        public void assetToDisk(String filename) { //경로 + 파일이름

            //스트림선언, try문 안에 선언을 하게되면 예외발생시 close함수를 호출할 방법이 없게된다
            InputStream is = null;
            BufferedInputStream bis = null;
            FileOutputStream fos = null;
            BufferedOutputStream bos = null;

            try {
                //1. assets에 있는 파일을 filename으로 읽어온다
                AssetManager manager = getAssets();
                //2. 파일스트림 생성
                is = manager.open(filename);
                //3. 버퍼스트림으로 래핑(한번에 여러개의 데이터를 가져오기 위한 준비작업)
                bis = new BufferedInputStream(is);

                //쓰기위한 준비작업
                //4. 저장할 위치에 파일이 없으면 생성
                String targetFile = getFullPath(filename);
                File file = new File(targetFile);
                if (!file.exists()) {
                    file.createNewFile();
                }

                //5. 쓰기 스트림을 생성
                fos = new FileOutputStream(file);
                //6. 버퍼스트림으로 동시에 여러가지 데이터를 쓰기위한 래핑
                bos = new BufferedOutputStream(fos);

                //읽어둘 데이터를 담아 둘 변수
                int read = -1; // 모두 읽어오면 -1이 저장된다
                //한번에 읽을 버퍼의 크기를 지정
                byte buffer[] = new byte[1024];
                //읽어올 데이터가 없을때까지 반복문을 돌면서 읽고 쓴다.

                while ((read = bis.read(buffer, 0, 1024)) != -1) { //0부터 1024까지 배열에 적재, read에는 데이터의 개수가 저장
                    bos.write(buffer, 0, read); //마지막은 읽어온 만큼만 쓴다.

                }

                //남아있는 데이터를 다 흘려보낸다
                bos.flush(); // write를 해도 완벽히 다 쓰기가 되는 것이 아니기때문에 플러쉬를 해서 쓰기로 밀어준다.
                // 버퍼가 없으면 플러쉬는 쓸필요 없다.
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    //사용한 역순으로 닫아준다. 물론 스트림만 닫아도 같이 닫히긴 한다
                    if (bos != null) bos.close(); // 클로즈에 플러쉬가 같이 있다 == 클로즈만 써주어도 된다.
                    if (bos != null) fos.close();
                    if (bos != null) bis.close();
                    if (bos != null) is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //파일의 전체 경로를 만들어주는 함수
    private String getFullPath(String filename) {
        //  /data/data/패키지명/files + / + 파일명
        return getFilesDir().getAbsolutePath() + File.separator + filename;
    }
}
