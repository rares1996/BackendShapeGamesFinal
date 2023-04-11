package Assignment.Web;

import Assignment.RequestService.Exceptions.APILimitReachedException;
import Assignment.RequestService.HTTP.Exceptions.InvalidInputException;
import Assignment.RequestService.Interfaces.IRequestService;
import Assignment.Utilities.WeatherUtilities;
import Assignment.Web.Responses.LocationReturnEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Component
public class Controller extends Thread {

  @Autowired
  private IRequestService reqService;

  @ExceptionHandler(APILimitReachedException.class)
  public ResponseEntity<String> handleAPILimitReached(APILimitReachedException message) {
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(message.getMessage());
  }
  @ExceptionHandler(InvalidInputException.class)
  public ResponseEntity<String> InvalidInputException(InvalidInputException message) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(message.getMessage());
  }


  @GetMapping(value = "/weather/cities/{cityId}", produces = "application/json")
  public CompletableFuture<ResponseEntity<Object>> getTemperatureNextFiveDays(
    @PathVariable("cityId") String cityId
  ) throws Exception {
    return reqService
      .getTemperatureOnLocation(cityId)
      .thenApply(jsonObject ->
        new ResponseEntity<Object>(jsonObject.toString(), HttpStatus.OK)
      );
  }


  @GetMapping(value = "/weather/summary", produces = "application/json")
  public CompletableFuture<Object> getTemperatureOnLocations(
    @RequestParam(required = true) String unit,
    @RequestParam(required = true) int temperature,
    @RequestParam(required = true) String cities
  ) throws Exception {
    
    var citiesIdsList = cities.split(",", 0);
    var citiesToReturn = new ArrayList<LocationReturnEntity>();

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (String citiesId : citiesIdsList) {
      CompletableFuture<Void> future = reqService
        .getTemperatureOnLocation(citiesId)
        .thenAccept(cityObj -> {
          if (
            WeatherUtilities.IsLocationAboveTemperature(
              unit,
              temperature,
              cityObj
            )
          ) {
            LocationReturnEntity location = new LocationReturnEntity();
            location.payload = cityObj.toString();
            citiesToReturn.add(location);
          }
        });

      // we are adding the futures with all the cities
      futures.add(future);
    }

    return CompletableFuture
      .allOf(futures.toArray(new CompletableFuture[0]))
      .thenApply(v -> new ResponseEntity<Object>(citiesToReturn, HttpStatus.OK)
      );
  }
}
