package com.example.Courier.controllers;



import com.example.Courier.models.WeatherInput;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import java.util.Objects;
import static com.example.Courier.CourierApplication.repository;

@RestController
public class DeliveryFeeController {

    @Autowired
    public Environment environment;

    public DeliveryFeeController(Environment environment) {
        this.environment = environment;
    }

    public double getDeliveryFee(String location, String vehicle){

        WeatherInput station = getStationData(location);
        if (Objects.equals(station, null) && !Objects.equals(location, ""))
            return -1.0;// If this station wasn't in the database, but a valid station was entered.

        double fee = calculateRegionalBaseFee(location,vehicle);
        double extraFees = 0;

        if (fee != -200)
            extraFees = calculateExtraFees(station,vehicle);

        if (extraFees == -1)
            return -2.0;// If weather is hazardous for this vehicle.

        fee += extraFees;
        return fee;// If XML webpage malfunction(then about -200) or standard output.
    }
    public double calculateExtraFees(WeatherInput station, String vehicle){
        double extraFees = 0.0; // Starts adding to it, depending on conditions.

        int weatherSeverity = determineWeatherSeverity(station.getPhenomenon());

        if (vehicle.equals("Scooter") || vehicle.equals("Bike")){

            // Checking for ait temperature.
            if (station.getAirTemperature() <= 0 && station.getAirTemperature() >= -10)
                extraFees += 0.5;

            if (station.getAirTemperature() < -10)
                extraFees += 1.0;

            //Checking for weather phenomenons, such as rain or snow.

            if (weatherSeverity == 2)// If raining,
                extraFees += 0.5;

            if (weatherSeverity == 3)// If snow or sleet.
                extraFees += 1.0;

            if (weatherSeverity == 4) // If hazardous weather conditions.
                return -1;// Send out a negative value, that "getFeeRequestResponse()" function would notice it.

        }
        if (vehicle.equals("Bike")){
            double windspeed = station.getWindSpeed();

            if (windspeed >= 10 && windspeed <= 20)
                return extraFees + 0.5;

            if (windspeed > 20) {// Hazardous weather conditions.
                return -1;// Send out a negative value, that "getFeeRequestResponse()" function would notice it.
            }

        }

        return extraFees;
    }

    public int determineWeatherSeverity(String phenomenon){
        // Returns numbers 4-1. The larger the number, the more hazardous the weather.
        // The hazard level is classified by the extra fee phenomenon requirements.

        if (phenomenon.equals("Glaze") || phenomenon.equals("Hail") || phenomenon.equals("Thunder") || phenomenon.equals("Thunderstorm"))
            return 4;

        if (phenomenon.contains("snow") || phenomenon.contains("sleet"))
            return 3; // All possible values related to snow or sleet have these words in them.

        if (phenomenon.contains("rain")||phenomenon.contains("shower"))
            return 2; // All possible values related to rain have these words in them.

        return 1;// If none of the above.

    }
    public double calculateRegionalBaseFee(String location, String vehicle) {
        String propertyName = String.format("location.fees.%s.%s", location, vehicle);
        String feeValue = environment.getProperty(propertyName);

        if (feeValue != null) {
            return Double.parseDouble(feeValue);
        }
        return -200.0; // If none of the options apply.

    }

    private static WeatherInput getStationData(String location) {
        long repositorySize = repository.count();

        for (long i = 0; i < 3; i++) { // Getting the last 3 elements, for they will be the most recent elements of the repository.

            Integer weatherInputIndex = Math.toIntExact(repositorySize - i);
            WeatherInput weatherInput = repository.findById(weatherInputIndex).orElse(null);

            if (weatherInput != null && (Objects.equals(weatherInput.getStationName(), location)))
                return weatherInput;
        }
        return null; // Case, where an element was requested, that wasn't in the database.
        // Refers to either HttpStatus.BAD_REQUEST or HttpStatus.INTERNAL_SERVER_ERROR.
    }

}
