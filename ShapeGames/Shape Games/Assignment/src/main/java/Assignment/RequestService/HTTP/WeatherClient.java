package Assignment.RequestService.HTTP;

import org.json.JSONObject;
import java.util.concurrent.CompletableFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import Assignment.RequestService.HTTP.Exceptions.InvalidInputException;
import Assignment.RequestService.HTTP.Interfaces.IWeatherClient;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class WeatherClient implements IWeatherClient {


    private final String requestURL = "https://api.openweathermap.org/data/2.5/forecast?id=%s&appid=%s";
    private final String apiKey = "a8193543ecf27622013b0f9265986190";
    Logger logger = LoggerFactory.getLogger(WeatherClient.class);

    HttpClient client = HttpClient.newHttpClient();
    private volatile Integer  latestStatusCode;
    @Override
    @Async("taskExecutor")
    public CompletableFuture<JSONObject> getLocationTemperature(String cityId) throws InterruptedException{
 
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(String.format(requestURL, cityId, apiKey))).build();
        var future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
        .thenApply((response -> {
            logger.info(" statuscode in WC: {}", response.statusCode());

            latestStatusCode = response.statusCode();
            if (latestStatusCode == 404) {
                throw new InvalidInputException("City ID not found: " + cityId);
            } else {
                return response.body();
            }
        }))
        .thenApply((r -> new JSONObject(r)));
        return future;
    };


    public Integer  getLatestStatusCode() {
        return latestStatusCode;
    }
    

}
