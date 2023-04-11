package Assignment.Entities;

import java.time.LocalDate;
import org.json.JSONObject;

public class LocationEntity {

    private JSONObject payload;
    private LocalDate addedDay;


    public LocationEntity(JSONObject payload, LocalDate addedDay) {
        
        this.payload = payload;
        this.addedDay = addedDay;

    }

    public JSONObject getPayLoad() {
        return payload;
    }

    public LocalDate getExpiryDay()  {
        
        return addedDay;
    }



}
