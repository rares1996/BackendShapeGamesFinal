package Assignment.RequestService.Interfaces;

import java.util.concurrent.CompletableFuture;
import org.json.JSONObject;

public interface IRequestService{
    CompletableFuture<JSONObject> getTemperatureOnLocation(String id)  throws Exception;
}
