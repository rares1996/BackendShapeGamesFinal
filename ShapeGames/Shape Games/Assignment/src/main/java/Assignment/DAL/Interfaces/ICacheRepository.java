package Assignment.DAL.Interfaces;


import Assignment.Entities.LocationEntity;

public interface ICacheRepository {

    
    public LocationEntity getFromCache(String key);
    public void saveInCache(String key, LocationEntity location);
    public void removeFromCache(String key);
    public void emptyCache();
    public boolean checkIfEmpty();

}