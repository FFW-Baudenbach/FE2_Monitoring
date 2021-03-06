package com.odin568.service;

import com.odin568.helper.PingHelper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;

@Service
public class PushoverService
{
    @Value("${pushover.enabled:true}")
    private boolean enabled;

    private final String token;
    private final String user;
    private final Logger logger = LoggerFactory.getLogger(PingHelper.class);

    public PushoverService(@Value("${pushover.token}") String token, @Value("${pushover.user}") String user) {
        this.token = token;
        this.user = user;
    }

    public boolean sendToPushover(String title, String message, String priority)
    {
        if (!enabled) {
            logger.warn("Sending is disabled");
            return true;
        }

        try {
            HttpClient httpclient;
            HttpPost httpPost;
            ArrayList<NameValuePair> postParameters;

            httpclient = HttpClientBuilder.create().build();
            httpPost = new HttpPost("https://api.pushover.net/1/messages.json");


            postParameters = new ArrayList<>();
            postParameters.add(new BasicNameValuePair("token", token));
            postParameters.add(new BasicNameValuePair("user", user));
            postParameters.add(new BasicNameValuePair("message", message));
            postParameters.add(new BasicNameValuePair("title", title));
            postParameters.add(new BasicNameValuePair("priority", priority));
            postParameters.add(new BasicNameValuePair("monospace", "1"));
            postParameters.add(new BasicNameValuePair("timestamp", String.valueOf(LocalDateTime.now().atZone(ZoneId.systemDefault()).toEpochSecond())));

            httpPost.setEntity(new UrlEncodedFormEntity(postParameters, "UTF-8"));

            HttpResponse response = httpclient.execute(httpPost);

            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IOException("Pushover returned " + response.getStatusLine().getStatusCode());
            }
        }
        catch (IOException ex) {
            logger.error("Error sending pushover message: " + title, ex);
            return false;
        }
        return true;
    }
}
