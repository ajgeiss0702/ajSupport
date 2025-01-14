package us.ajg0702.bots.ajsupport.autorespond;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

public class EmbeddingUtils {

    public static BigDecimal[] embed(String string) throws IOException {
        final String token = System.getenv("CF_TOKEN");
        if(token == null || token.isEmpty()) throw new IOException("Missing CF Token!");

        URL url = new URL("https://api.cloudflare.com/client/v4/accounts/f55b85c8a963663b11036975203c63c0/ai/run/@cf/baai/bge-base-en-v1.5");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + token); // Replace YOUR_BEARER_TOKEN with your actual token
        con.setDoOutput(true);

        JsonObject payloadObject = new JsonObject();
        payloadObject.addProperty("text", string);
        String payload = new Gson().toJson(payloadObject);
        try (var outputStream = con.getOutputStream()) {
            outputStream.write(payload.getBytes());
            outputStream.flush();
        }

        if(con.getResponseCode() >= 400) {
            try (var inputStream = con.getErrorStream();
                 var reader = new InputStreamReader(inputStream)) {
                StringBuilder responseBuilder = new StringBuilder();
                int c;
                while ((c = reader.read()) != -1) {
                    responseBuilder.append((char) c);
                }
                System.out.println("(Embeddings) Error response body: " + responseBuilder);
            }
        }

        var inputStream = con.getInputStream();
        var reader = new InputStreamReader(inputStream);

        Gson gson = new Gson();
        JsonObject json = gson.fromJson(reader, JsonObject.class).getAsJsonObject("result");

        BigDecimal[] vec = new BigDecimal[768];

        int i = 0;
        for (JsonElement vectorElement : json.getAsJsonArray("data").get(0).getAsJsonArray()) {
            vec[i++] = vectorElement.getAsBigDecimal();
        }

        return vec;
    }

    public static @Nullable VectorizeResponse queryVectorize(BigDecimal[] query) throws IOException {
        List<VectorizeResponse> results = queryVectorize(query, 1);
        if(results.size() == 0) return null;
        return results.get(0);
    }

    public static List<VectorizeResponse> queryVectorize(BigDecimal[] query, int topK) throws IOException {
        final String token = System.getenv("CF_TOKEN");
        if(token == null || token.isEmpty()) throw new IOException("Missing CF Token!");

        URL url = new URL("https://api.cloudflare.com/client/v4/accounts/f55b85c8a963663b11036975203c63c0/vectorize/v2/indexes/support-autoresponse/query");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + token); // Replace YOUR_BEARER_TOKEN with your actual token
        con.setDoOutput(true);

        String payload = String.format(
            "{\"vector\": %s, \"returnMetadata\": \"all\", \"returnValues\": false, \"topK\": " + topK + "}",
            new Gson().toJson(query)
        );
        try (var outputStream = con.getOutputStream()) {
            outputStream.write(payload.getBytes());
            outputStream.flush();
        }
    
        var inputStream = con.getInputStream();
        var reader = new InputStreamReader(inputStream);
    
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(reader, JsonObject.class);
        var responses = new ArrayList<VectorizeResponse>();
        System.out.println(json.toString());
    
        for (JsonElement element : json.getAsJsonObject("result").getAsJsonArray("matches")) {
            responses.add(new VectorizeResponse(
                    element.getAsJsonObject().get("id").getAsString(),
                    element.getAsJsonObject().get("metadata").getAsJsonObject(),
                    element.getAsJsonObject().get("score").getAsDouble()
                    ));
        }
    
        return responses;
    }

    public static void insertIntoVectorize(String id, BigDecimal[] vector, String channelId, String response) throws IOException {
        final String token = System.getenv("CF_TOKEN");
        if(token == null || token.isEmpty()) throw new IOException("Missing CF Token!");

        final String boundary = new BigInteger(128, new Random()).toString();

        URL url = new URL("https://api.cloudflare.com/client/v4/accounts/f55b85c8a963663b11036975203c63c0/vectorize/v2/indexes/support-autoresponse/upsert");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
        con.setRequestProperty("Authorization", "Bearer " + token); // Replace YOUR_BEARER_TOKEN with your actual token
        con.setDoOutput(true);

        final String end = boundary + "\r\n";

        String payload = "--" + end + "Content-Disposition: form-data; name=\"vectors\"; filename=\"upsert.ndjson\"\r\nContent-Type: application/x-ndjson\r\n\r\n" +
                String.format(
                    "{\"id\": \"%s\", \"values\": %s, \"metadata\": { \"channelId\":\"%s\", \"response\": \"%s\" }}",
                    id,
                    new Gson().toJson(vector),
                    channelId,
                    response
                ) + "\r\n--" + boundary + "--";


        try (var outputStream = con.getOutputStream()) {
            outputStream.write(payload.getBytes());
            outputStream.flush();
        }

        System.out.println("(Vectorize: Upsert) Request returned status " + con.getResponseCode());
        try (var inputStream = con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST ? con.getInputStream() : con.getErrorStream();
             var reader = new InputStreamReader(inputStream)) {
            StringBuilder responseBuilder = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                responseBuilder.append((char) c);
            }
            System.out.println("(Vectorize: Upsert) Response body: " + responseBuilder);
        }
        
    }

    public static void deleteFromVectorize(String id) throws IOException {
        final String token = System.getenv("CF_TOKEN");
        if(token == null || token.isEmpty()) throw new IOException("Missing CF Token!");

        URL url = new URL("https://api.cloudflare.com/client/v4/accounts/f55b85c8a963663b11036975203c63c0/vectorize/v2/indexes/support-autoresponse/delete_by_ids");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Authorization", "Bearer " + token); // Replace YOUR_BEARER_TOKEN with your actual token
        con.setDoOutput(true);

        String payload = String.format(
                "{\"ids\": [\"%s\"]}",
                id
        );
        try (var outputStream = con.getOutputStream()) {
            outputStream.write(payload.getBytes());
            outputStream.flush();
        }

        System.out.println("(Vectorize: Delete) Request returned status " + con.getResponseCode());
        try (var inputStream = con.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST ? con.getInputStream() : con.getErrorStream();
             var reader = new InputStreamReader(inputStream)) {
            StringBuilder responseBuilder = new StringBuilder();
            int c;
            while ((c = reader.read()) != -1) {
                responseBuilder.append((char) c);
            }
            System.out.println("(Vectorize: Delete) Response body: " + responseBuilder);
        }
    }

    public static class VectorizeResponse {
        private final String id;
        private final JsonObject metadata;
        private final double score;

        public VectorizeResponse(String id, JsonObject metadata, double score) {
            this.id = id;
            this.metadata = metadata;
            this.score = score;
        }
    
        public String getId() {
            return id;
        }
    
        public JsonObject getMetadata() {
            return metadata;
        }

        public double getScore() {
            return score;
        }

        @Override
        public String toString() {
            return score + " " + id + ": " + metadata.toString();
        }
    }

    public static void main(String[] args) {
        try {
//            insertIntoVectorize("1326592604510617620", embed("can anyone tell me how to reset AjLeaderboards"), "810277316298408007", "reset");
//            insertIntoVectorize("1326229772476485633", embed("how do i reset people on leaderboard even tho they arent online?"), "810277316298408007", "reset");
            BigDecimal[] vec = embed("how do I reset my kills leaderboard");
//            System.out.println(vec);

            VectorizeResponse responses = queryVectorize(vec);
            System.out.println(responses);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
