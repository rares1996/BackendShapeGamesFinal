package Assignment.RequestService;

import Assignment.DAL.Interfaces.ICacheRepository;
import Assignment.Entities.LocationEntity;
import Assignment.RequestService.Exceptions.APILimitReachedException;
import Assignment.RequestService.HTTP.Exceptions.InvalidInputException;
import Assignment.RequestService.HTTP.Interfaces.IWeatherClient;
import Assignment.RequestService.Interfaces.IRequestService;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;

@Service
@EnableScheduling
public class RequestService implements IRequestService {

  @Autowired
  private IWeatherClient client;

  @Autowired
  private ICacheRepository cacheRepository;

  private final AtomicInteger APIlimit = new AtomicInteger(1000);
  private AtomicInteger APIcallcount = new AtomicInteger(0);
  Logger logger = LoggerFactory.getLogger(RequestService.class);

  //reseting the daily timer for API calls made
  public void resetcounter() {
    APIcallcount = new AtomicInteger(0);
    if (!cacheRepository.checkIfEmpty()) {
      cacheRepository.emptyCache();
    }
  }

  @PostConstruct
  public void initScheduler() {
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    Runnable task = new Runnable() {
      @Override
      public void run() {
        resetcounter();
      }
    };

    // Schedule the task to run every day
    long delay = 100; // 24 hours in seconds
    scheduler.scheduleAtFixedRate(task, delay, delay, TimeUnit.SECONDS);
  }

  ///
  @Async("taskExecutor")
  private boolean incrementAndCheckAPIcallcount() {
  
    if (APIlimit.get() > APIcallcount.get()) {
      APIcallcount.incrementAndGet();
      return true;
    }
    return false;
  }
  
  @Override
  @Async("taskExecutor")
  public CompletableFuture<JSONObject> getTemperatureOnLocation(String id)
    throws Exception {
        var element = cacheRepository.getFromCache(id);

        if (element == null) {
          return client
            .getLocationTemperature(id)
            .thenApply(locationTemperature -> {
              if (client.getLatestStatusCode() != 404) {
                if (incrementAndCheckAPIcallcount()) {
                  LocationEntity resultElement = new LocationEntity(
                    locationTemperature,
                    LocalDate.now()
                  );
                  cacheRepository.saveInCache(id, resultElement);
                  return resultElement.getPayLoad();
                } else {
                  throw new APILimitReachedException("API limit has been reached.");
                }
              } else {
                throw new InvalidInputException("City ID not found: " + id);
              }
            });
        }
    //if the element is not null, but has expired
    else if (
      element != null && LocalDate.now().isAfter(element.getExpiryDay())
    ) {
      //we remove the item from the cache
      cacheRepository.removeFromCache(id);

      //and then check if the APIlimit has been reached
      //and subsequently return the payload / or not
      if (incrementAndCheckAPIcallcount()) {

        return client
          .getLocationTemperature(id)
          .thenApply(locationTemperature -> {
            LocationEntity resultElement = new LocationEntity(
              locationTemperature,
              LocalDate.now()
            );
            cacheRepository.saveInCache(id, resultElement);

            return resultElement.getPayLoad();
          });
      } else {
        throw new APILimitReachedException("API limit has been reached.");
      }
    }
    //if the element is in the cache and has not expired, we will return it from the cache
    else if (
      element != null && !LocalDate.now().isAfter(element.getExpiryDay())
    ) {
      return CompletableFuture.completedFuture(element.getPayLoad());
    } else if(client.getLatestStatusCode() == 404 && !incrementAndCheckAPIcallcount()) {
        throw new InvalidInputException("City ID not found: " + id);

    } else {
        throw new APILimitReachedException("API limit has been reached.");
    }

  }
}


