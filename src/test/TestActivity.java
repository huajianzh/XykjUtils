package test;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import test.bean.Article;
import test.bean.BaseData;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.xykj.xykjutils.R;
import com.xyy.net.FileRequestItem;
import com.xyy.net.NetManager;
import com.xyy.net.RequestItem;
import com.xyy.net.ResponceItem;
import com.xyy.net.StringRequestItem;
import com.xyy.net.imp.Callback;

public class TestActivity extends Activity implements OnClickListener {
	private TextView tvResult;
	private String token;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test_activity);
		tvResult = (TextView) findViewById(R.id.tv_result);
		findViewById(R.id.btn_get).setOnClickListener(this);
		findViewById(R.id.btn_post).setOnClickListener(this);
		findViewById(R.id.btn_update_base).setOnClickListener(this);
		findViewById(R.id.btn_load_articles).setOnClickListener(this);
		//以下https的配置可以根据自己需要来配置,没有安全证书可以不设置，不配置则表示客户端的https请求将会全部信任
//		try {
//			//单项认证，客户端有公钥文件(一般放在assets目录下)
//			//加载公钥文件
//			InputStream is = getAssets().open("clent.cer");
//			//创建配置文件
//			HttpsConfig config = HttpsConfig.createConfig(is);
//			NetManager.getInstance().setHttpsConfig(config);
//			
////			//双向认证
////			//加载服务端公钥
////			InputStream serverCer = getAssets().open("server.cer");
////			//加载客户端私钥
////			InputStream clientBks = getAssets().open("client.bks");
////			//创建配置(这里假如密码为123456)
////			HttpsConfig config = HttpsConfig.createConfig(serverCer, clientBks, "123456");
////			NetManager.getInstance().setHttpsConfig(config);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_get:
			NetManager
					.getInstance()
					.execute(
							new RequestItem.Builder()
									.url("https://192.168.2.113:8443/MyServletDemo001/test?opt=2&name=abc&psw=11233")
									.addHead("token",
											"abcdef12344skdjkdjskjdsk546567")
									.build(), new Callback<String>() {

								@Override
								public String changeData(ResponceItem responce) {
									Map<String, List<String>> heads = responce
											.getHeads();
									for (String name : heads.keySet()) {
										System.out.print("====>" + name + ":");
										for (String s : heads.get(name)) {
											System.out.println(s);
										}
									}
									System.out.println();
									return responce.getString();
								}

								@Override
								public void onResult(String result) {
									tvResult.setText(result);
								}
							});
			break;
		case R.id.btn_post:
			StringRequestItem request = new StringRequestItem.Builder()
					.url("https://192.168.2.108:8443/login")
					.addStringParam("name", "abc")
					.addStringParam("psw", "123456").build();
			NetManager.getInstance().execute(request, new Callback<String>() {

				@Override
				public String changeData(ResponceItem responce) {
					String json = responce.getString();
					Map<String, List<String>> heads = responce
							.getHeads();
					for (String name : heads.keySet()) {
						System.out.print("====>" + name + ":");
						for (String s : heads.get(name)) {
							System.out.println(s);
						}
					}
					try {
						JSONObject obj = new JSONObject(json);
						if(obj.optInt("code")==1){
							token = responce.getHeads().get("token").get(0);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return json;
				}

				@Override
				public void onResult(String result) {
					tvResult.setText(result);
				}
			});
			break;
		case R.id.btn_update_base:
			NetManager.getInstance().execute(new FileRequestItem.Builder()
			.url("https://192.168.2.108:8443/api/update_u_base")
			.addStringParam("name", "qq11")
			.addStringParam("sex", "女")
			.addStringParam("sign", "哈哈，风景很好啊")
			.addStringParam("birth", String.valueOf(System.currentTimeMillis()))
			.addFileParam("photo", new File("/mnt/sdcard/3.jpg"))
			.addHead("token", token)
			.build(), new Callback<String>() {

				@Override
				public String changeData(ResponceItem responce) {
					String json = responce.getString();
					return json;
				}

				@Override
				public void onResult(String result) {
					tvResult.setText(result);
				}
			});
			break;
		case R.id.btn_load_articles:
			NetManager.getInstance().execute(
					new RequestItem.Builder()
					.url("http://192.168.2.179:8080/getArticles?page=1")
					.build(), articleCallback);
					/*new GsonConverterCallback<BaseData<List<Article>>>() {

						@Override
						public void onResult(BaseData<List<Article>> data) {
							if(null != data){
								List<Article> result = data.getData();
								tvResult.setText("size:"+result.size()+"\n");
								for(int i = 0 ; i < result.size();i++){
									tvResult.append(result.get(i).toString()+"\n");
								}
							}
						}
					});*/
			break;
		}
	}
	
	private Callback<BaseData<List<Article>>> articleCallback = new Callback<BaseData<List<Article>>>() {

		@Override
		public BaseData<List<Article>> changeData(ResponceItem responce) {
			String json = responce.getString();
			Gson gson = new GsonBuilder()
			.setLenient()
			.create();
			BaseData<List<Article>> data = gson.fromJson(json, new TypeToken<BaseData<List<Article>>>(){}.getType());
			return data;
//			GsonConverter c = new GsonConverter();
//			return c.convert(json, );
		}

		@Override
		public void onResult(BaseData<List<Article>> data) {
			if(null != data){
				List<Article> result = data.getData();
				tvResult.setText("size:"+result.size()+"\n");
				for(int i = 0 ; i < result.size();i++){
					tvResult.append(result.get(i).toString()+"\n");
				}
			}
		}
	};
}
