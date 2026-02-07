package com.odin568.helper;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class HttpHelper
{
    public static MonitoringResult isSiteRedirectedToHttps(String device, String url)
    {
        if (!url.startsWith("http://")) {
            throw new IllegalArgumentException("Invalid url for device " + device);
        }

        MonitoringResult result = new MonitoringResult(device);

        try {
            URL site = new URI(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) site.openConnection();
            conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM) {
                String location = conn.getHeaderField("Location");
                if (location.startsWith(url.replace("http", "https"))) {
                    result.HealthState = HealthState.Healthy;
                }
                else {
                    result.Information = "Redirected to " + location;
                }
            }
            else {
                result.Information = "Response code: " + conn.getResponseCode();
            }
        }
        catch (IOException | URISyntaxException ex) {
            result.Information = ex.getClass().getName() + ": " + ex.getMessage();
        }
        return result;
    }


    public static MonitoringResult isSiteUpViaHttp(String device, String url, boolean onlyHead)
    {
        if (!url.startsWith("http://")) {
            throw new IllegalArgumentException("Invalid url for device " + device);
        }

        MonitoringResult result = new MonitoringResult(device);

        try {
            URL site = new URI(url).toURL();
            HttpURLConnection conn = (HttpURLConnection) site.openConnection();
            if (onlyHead)
                conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result.HealthState = HealthState.Healthy;
            }
            else {
                result.Information = "Response code: " + conn.getResponseCode();
            }
        }
        catch (IOException | URISyntaxException ex) {
            result.Information = ex.getClass().getName() + ": " + ex.getMessage();
        }
        return result;
    }

    public static MonitoringResult isSiteUpViaHttps(String device, String url, boolean onlyHead, boolean ignoreCertificateIssues)
    {
        if (!url.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid url for device " + device);
        }

        MonitoringResult result = new MonitoringResult(device);

        try {
            URL site = new URI(url).toURL();
            HttpsURLConnection conn = (HttpsURLConnection) site.openConnection();
            if (onlyHead)
                conn.setRequestMethod("HEAD");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (ignoreCertificateIssues) {
                conn.setHostnameVerifier(getHostnameVerifier());
            }

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                result.HealthState = HealthState.Healthy;
            }
            else {
                result.Information = "Response code: " + conn.getResponseCode();
            }
        }
        catch (IOException | URISyntaxException ex) {
            result.Information = ex.getClass().getName() + ": " + ex.getMessage();
        }
        return result;
    }

    private static HostnameVerifier getHostnameVerifier() {
        return (hostname, session) -> true;
    }


    public static RestTemplate getRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);

        return new RestTemplate(factory);
    }
}
