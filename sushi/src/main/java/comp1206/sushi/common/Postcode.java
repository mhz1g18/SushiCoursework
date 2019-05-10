package comp1206.sushi.common;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;



public class Postcode extends Model {

	private String name;
	private Map<String,Double> latLong;
	private Number distance;

	public Postcode(String code) {
		this.name = code;
		calculateLatLong();
		this.distance = Integer.valueOf(0);
	}
	
	public Postcode(String code, Restaurant restaurant) {
		this.name = code;
		calculateLatLong();
		calculateDistance(restaurant);
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public Number getDistance() {
		return this.distance;
	}

	public Map<String,Double> getLatLong() {
		return this.latLong;
	}


	/**
	 * Calculates the distance from the postcode's location to the restaurant's location
	 * By using the lat/long coordinates of the source postcode (this) and the destination (restaurant)
	 * @param restaurant the destination
	 */

	protected void calculateDistance(Restaurant restaurant) {
		Postcode destination = restaurant.getLocation();

		double lat1 = this.getLatLong().get("lat");
		double lon1 = this.getLatLong().get("lon");

		double lat2 = destination.getLatLong().get("lat");
		double lon2 = destination.getLatLong().get("lon");

		double R = 6371; // Radius of the earth in km
		double dLat = Math.toRadians(lat2-lat1);  // deg2rad below
		double dLon = Math.toRadians(lon2-lon1);
		double a =
				Math.sin(dLat/2) * Math.sin(dLat/2) +
						Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
								Math.sin(dLon/2) * Math.sin(dLon/2)
				;
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		double d = R * c; // Distance in km

		// Round the result for 4 numbers after the decimal point
		DecimalFormat df = new DecimalFormat("##.####");

		this.distance = Double.parseDouble(df.format(d));
	}

	/**
	 * Uses the api provided from the first coursework to get lat/long data
	 * Parses the json to extract the lat/long and put them in the latLong hashmap
	 */
	
	protected void calculateLatLong() {

		this.latLong = new HashMap<String,Double>();

		try {

			String urlString = "https://www.southampton.ac.uk/~ob1a12/postcode/postcode.php?postcode=" + urlify(getName(), getName().length());

			// Establish connetion
			URL url = new URL(urlString);
			URLConnection connection = url.openConnection();
			connection.connect();

			// Create a buffered reader to read from the input stream
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;

			while((inputLine = in.readLine()) != null) {
				if(inputLine.equals("{\"error\":\"Invalid format of postcode\"}") ||
					inputLine.contains("resolve")) {
					latLong.put("error", -1d);
					break;
				}

				// Create a new json object
				JSONObject jsonObj = new JSONObject(inputLine);

				try {

					double lat = Double.parseDouble(jsonObj.get("lat").toString());
					double lon = Double.parseDouble(jsonObj.get("long").toString());

					// Round the result for 4 numbers after the decimal point
					DecimalFormat df = new DecimalFormat("##.####");

					latLong.put("lat", Double.parseDouble(df.format(lat)));
					latLong.put("lon", Double.parseDouble(df.format(lon)));
				} catch (NumberFormatException e) {
					System.out.println("Could not parse json data");
				}
			}


		} catch (IOException e) {
			System.out.println(e.getMessage());
		}


		this.distance = new Integer(0);
	}

	/**
	 * Parses the url to request the json data for lat/long
	 * @param url the url string to be parsed
	 * @param length the length of the string
	 * @return the parsed string
	 */

	private String urlify(String url, int length) {

		String newUrl = new String();

		for(int i = 0; i < length; i++) {
			if(url.charAt(i) == ' ') {
				newUrl += "%20";
			} else {
				newUrl += url.charAt(i);
			}
		}

		return newUrl;
	}
}
