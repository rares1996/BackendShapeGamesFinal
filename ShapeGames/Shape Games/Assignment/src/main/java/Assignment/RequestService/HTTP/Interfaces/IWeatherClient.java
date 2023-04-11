package Assignment.RequestService.HTTP.Interfaces;

import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public interface IWeatherClient {

    public CompletableFuture<JSONObject> getLocationTemperature(String cityId) throws InterruptedException;

    public Integer getLatestStatusCode();

}
