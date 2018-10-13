package test;

import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonConverter {
	private Gson gson;
	
	public GsonConverter() {
		gson = new GsonBuilder()
		.setLenient()
		.create();
	}
	public <T> T convert(String json,Class<T> cls){
//		return gson.fromJson(json, new TypeToken<T>(){}.getType());
		return gson.fromJson(json,cls);
	}
}
