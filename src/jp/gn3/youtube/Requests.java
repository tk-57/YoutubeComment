package jp.gn3.youtube;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.Map;

public class Requests {

    private Gson gson;
    private String encoding = "UTF-8";

    public Requests(){
        gson = new GsonBuilder().create();
    }

    public <T> T doGet(String url,Class<T> classOfT) throws IOException {
        return request(url, null,null, classOfT);
    }

    public <T> T doPost(String url,String post,Class<T> classOfT) throws IOException {
        return request(url, post,null, classOfT);
    }

    public <T> T doPost(String url,LinkedTreeMap json,Class<T> classOfT) throws IOException {
        return request(url, gson.toJson(json),null, classOfT);
    }

    public <T> T doGet(String url,LinkedTreeMap<String,String> header,Class<T> classOfT) throws IOException {
        return request(url, null,header, classOfT);
    }

    public <T> T doPost(String url,String post,LinkedTreeMap<String,String> header,Class<T> classOfT) throws IOException {
        return request(url, post,header, classOfT);
    }

    public <T> T doPost(String url,LinkedTreeMap json,LinkedTreeMap<String,String> header,Class<T> classOfT) throws IOException {
        return request(url, gson.toJson(json),header, classOfT);
    }

    public <T> T request(String url, String jsonString,LinkedTreeMap<String,String> headers, Class<T> classOfT) throws IOException {
        final okhttp3.MediaType mediaTypeJson = okhttp3.MediaType.parse("application/json; charset=" + encoding);


        Request.Builder builder = new Request.Builder().url(url);
        if(headers != null){
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(),header.getValue());
            }
        }
        if(jsonString != null) {
            RequestBody requestBody = RequestBody.create(mediaTypeJson, jsonString);
            builder.post(requestBody);
        }
        Request request = builder.build();

        final OkHttpClient client = new OkHttpClient.Builder()
                .build();
        final Response response = client.newCall(request).execute();
        final String resultStr = response.body().string();
        if(classOfT == String.class){
            return (T) resultStr;
        }
        T t = gson.fromJson(resultStr, classOfT);
        return t;
    }
}
