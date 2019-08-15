package sogou.com.packageinstaller;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.liulishuo.filedownloader.FileDownloader;
import com.liulishuo.filedownloader.connection.FileDownloadUrlConnection;

import org.w3c.dom.Text;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button textView = findViewById(R.id.install);
        Button next = findViewById(R.id.next);
        //下载初始化
        FileDownloader.setupOnApplicationOnCreate(this.getApplication())
                .connectionCreator(new FileDownloadUrlConnection
                        .Creator(new FileDownloadUrlConnection.Configuration()
                        .connectTimeout(10000) // set connection timeout.
                        .readTimeout(10000) // set read timeout.
                ))
                .commit();

        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UpdateUtil.getInstance().download(MainActivity.this, "http://apkpackage2qtest-1253804688.file.myqcloud.com/apk/1.2.5/installmentloan_Signed_mainRelease_legu_main_signed_zipalign.apk",
                        "loan.apk");
            }
        });
        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, InstallApkSessionApi.class);
                startActivity(intent);
            }
        });
    }
}
