package jp.gn3.youtube;

import com.google.gson.*;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoutubeComment {

    //チャンネルID
    String channelId = "";

    public static void main(String[] args) throws IOException, InterruptedException  {
        new YoutubeComment();
    }

    private Gson gson;
    private Requests requests;

    public YoutubeComment() throws IOException, InterruptedException {
        gson = new GsonBuilder().create();
        requests = new Requests();

        //APIに必要なデータを取得
        Options options = getOptions(channelId);

        //APIに使うPOSTデータ
        JsonObject body = new JsonObject();
        JsonObject context = new JsonObject();
        JsonObject client = new JsonObject();
        client.addProperty("clientVersion",options.clientVersion);
        client.addProperty("clientName","WEB");
        context.add("client",client);
        body.add("context",context);

        //かぶり対策
        List<String> last_comment_ids = new ArrayList<>();

        while(true) {
            //APIをたたく
            JsonObject comment_data = requestLiveChatAPI(body.deepCopy(),options);

            //コメントの一覧
            JsonArray comment_list = comment_data.getAsJsonObject("continuationContents").getAsJsonObject("liveChatContinuation")
                    .getAsJsonArray("actions");

            if(comment_list != null) {
                List<String> comment_ids = new ArrayList<>();
                for (int i = 0; i < comment_list.size(); i++) {
                    JsonObject comment;
                    try {
                        comment = comment_list.get(i).getAsJsonObject().getAsJsonObject("addChatItemAction")
                                .getAsJsonObject("item").getAsJsonObject("liveChatTextMessageRenderer");
                    } catch (Exception e) {
                        continue;
                    }
                    if (comment == null) continue;
                    //コメントのID
                    String id = comment.get("id").getAsString();
                    comment_ids.add(id);
                    if (last_comment_ids.contains(id)) continue;
                    //コメントしたアカウントの名前
                    String authorName = comment.getAsJsonObject("authorName").get("simpleText").getAsString();
                    String msg = "";
                    JsonArray messages = comment.getAsJsonObject("message").getAsJsonArray("runs");
                    for (int j = 0; j < messages.size(); j++) {
                        for (Map.Entry<String, JsonElement> message : messages.get(j).getAsJsonObject().entrySet()) {
                            if (message.getKey().equalsIgnoreCase("text")) {
                                msg += message.getValue().getAsString();
                            } else if (message.getKey().equalsIgnoreCase("emoji")) {
                                JsonArray shortcuts = message.getValue().getAsJsonObject().getAsJsonArray("shortcuts");
                                if (shortcuts != null) {
                                    msg += shortcuts.get(shortcuts.size() - 1);
                                } else {
                                    msg += message.getValue().getAsJsonObject().get("emojiId").getAsString();
                                }
                            }
                        }
                    }
                    //Println
                    System.out.println("\u001b[00;36m"+authorName+": \u001b[00;32m"+msg);
                }
                last_comment_ids = comment_ids;
            }

            //次のContinuationを入れる
            String nextContinuation = getNextContinuation(comment_data);
            if (nextContinuation != null) {
                options.continuation = nextContinuation;
            }

            //一秒スリープ
            Thread.sleep(1000);
        }
    }

    //LiveChatのAPIをたたく
    public JsonObject requestLiveChatAPI(JsonObject post_data,Options options) throws IOException {
        post_data.addProperty("continuation", options.continuation);
        //headerデータ
        LinkedTreeMap<String, Object> headers = new LinkedTreeMap<>();
        headers.put("Content-Type", "application/json");
        //apiをたたく
        return requests.doPost("https://www.youtube.com/youtubei/v1/live_chat/get_live_chat?key=" + options.innertubeApiKey,
                        post_data.toString(), JsonObject.class);
    }


    //生放送のURLを取得し、データを取得
    public Options getOptions(String channel_id){
        try {
            String url = "https://www.youtube.com/channel/" + channel_id + "/live";
            String data = requests.doGet(url, String.class);
            return getOptionData(data);
        }catch (Exception e){
            return null;
        }
    }

    //次に使用するContinuationを取得
    public String getNextContinuation(JsonObject comment_data){
        JsonObject continuation_data = comment_data.getAsJsonObject("continuationContents").getAsJsonObject("liveChatContinuation")
                .getAsJsonArray("continuations").get(0).getAsJsonObject();
        if(continuation_data.get("invalidationContinuationData") != null){
            return continuation_data.getAsJsonObject("invalidationContinuationData").get("continuation").getAsString();
        }
        else if(continuation_data.get("timedContinuationData") != null){
            return continuation_data.getAsJsonObject("timedContinuationData").get("continuation").getAsString();
        }
        return null;
    }

    //HTMLから必要なデータを抜き取る
    private Options getOptionData(String data){
        String liveId;
        String innertubeApiKey;
        String clientVersion;
        String continuation;
        Pattern p = Pattern.compile("<link rel=\"canonical\" href=\"https://www.youtube.com/watch[?]v=(.+?)\">");
        Matcher m = p.matcher(data);
        if (m.find()) {
            Pattern.compile("https://www.youtube.com/watch[?]v=(.+?)");
            Matcher m2 = p.matcher(m.group());
            m2.find();
            liveId = m2.group();
        } else {
            throw new Error("Live Stream was not found");
        }

        p = Pattern.compile("['\"]isReplay['\"]:\s*(true)");
        m = p.matcher(data);
        if (m.find()) {
            throw new Error("url is finished live");
        }

        p = Pattern.compile("['\"]INNERTUBE_API_KEY['\"]:\s*['\"](.+?)['\"]");
        m = p.matcher(data);
        if (m.find()) {
            innertubeApiKey = m.group();
        } else {
            throw new Error("API Key was not found");
        }

        p = Pattern.compile("['\"]clientVersion['\"]:\s*['\"]([\\d.]+?)['\"]");
        m = p.matcher(data);
        if (m.find()) {
            clientVersion = m.group();
        } else {
            throw new Error("Client Version was not found");
        }

        p = Pattern.compile("['\"]continuation['\"]:\s*['\"](.+?)['\"]");
        m = p.matcher(data);
        if (m.find()) {
            continuation = m.group();
        } else {
            throw new Error("Continuation was not found");
        }
        String json_text = "{"+innertubeApiKey+","+clientVersion+","+continuation+"}";
//        System.out.println(json_text);
        LinkedTreeMap<String,String> map = gson.fromJson(json_text, LinkedTreeMap.class);
        Options options = new Options();
        options.liveId = liveId;
        options.innertubeApiKey = map.get("INNERTUBE_API_KEY");
        options.clientVersion = map.get("clientVersion");
        options.continuation = map.get("continuation");
        return options;
    }
}

