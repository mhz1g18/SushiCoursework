package comp1206.sushi.common;

import comp1206.sushi.server.Server;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class Drone extends Model implements Serializable, Runnable {

	private Number speed;
	private Number progress;

	private Number capacity;
	private Number battery;

	private String status;

	private Postcode source;
	private Postcode destination;

	private transient Server server;
	private StockManagement stockManagement;

	private boolean active;

	//private double travelledDistance;


	public Drone(Number speed, Server server) {
		this.server = server;
		this.active = true;
		this.setSpeed(speed);
		this.setStatus("Idle");
		this.setCapacity(1);
		this.setBattery(100);
		this.stockManagement = server.getStockManagement();
		new Thread(this).start();
	}

	public Number getSpeed() {
		return speed;
	}


	public Number getProgress() {
		if(this.getStatus().equals("Idle")) return null;

		return progress;
	}

	public void setProgress(Number progress) {
		if(progress.doubleValue() > 1)
			progress = 1;

		this.progress = progress;
	}

	public void setSpeed(Number speed) {
		this.speed = speed;
	}

	@Override
	public String getName() {
		return "Drone (" + getSpeed() + " speed)";
	}

	public Postcode getSource() {
		return source;
	}

	public void setSource(Postcode source) {
		this.source = source;
	}

	public Postcode getDestination() {
		return destination;
	}

	public void setDestination(Postcode destination) {
		this.destination = destination;
	}

	public Number getCapacity() {
		return capacity;
	}

	public void setCapacity(Number capacity) {
		this.capacity = capacity;
	}

	public Number getBattery() {
		return battery;
	}

	public void setBattery(Number battery) {
		if(battery.doubleValue()>100)
			battery = 100;
		else if(battery.doubleValue() < 0)
			battery = 0;

		this.battery = battery;
	}

	/**
	 * Calculate the distance between the source and the destination
	 * @return the distance
	 */

	public double calculateDistance() {
		double lat1 = this.getSource().getLatLong().get("lat");
		double lon1 = this.getSource().getLatLong().get("lon");

		double lat2 = this.getDestination().getLatLong().get("lat");
		double lon2 = this.getDestination().getLatLong().get("lon");

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

		return Double.parseDouble(df.format(d));
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	@Override
	public void run() {
		try {
			this.sleep(5);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/**
		 * Run the thread as long as it is active
		 * If the status is idle, check for potential jobs
		 */

		while (this.isActive()) {

			int battery = this.getBattery().intValue();

			if (battery <= 0){
				this.recharge(battery);
			}


			if(this.getStatus().equals("Idle") && battery > 0) {
				if(server.isRestockingIngrEnabled())
					this.checkIngredientsStock();

				this.checkOrders();
			}

			/**
			 * Timeout for 2 seconds
			 */

			try {
				this.sleep(2);
			} catch (InterruptedException e) {
				this.stop();
			}
		}
	}

	/**
	 * Stops the drone
	 */

	public void stop() {
		this.active = false;
	}

	public boolean isActive() {
		return this.active;
	}

	/**
	 * Pauses thread execution for a period of time
	 * @param timeout sleep time in seconds
	 * @throws InterruptedException
	 */

	public void sleep(long timeout) throws InterruptedException {

		Thread.sleep(timeout * 1000);
	}

	/**
	 * Check the stocks of dishes and restock if needed
	 */

	private void checkIngredientsStock() {
		Map<Ingredient, Number> ingredientLevels = stockManagement.getIngredientStockLevels();

		if(ingredientLevels == null || ingredientLevels.keySet().isEmpty())
			return;

		for(Ingredient i : ingredientLevels.keySet()) {

			int stockLevel = stockManagement.getStock(i).intValue();
			int threshold = i.getRestockThreshold().intValue();


			if(stockLevel < threshold & this.getStatus().equals("Idle")) {

				boolean testLocked = i.startRestocking();

				if(testLocked) {
					this.setStatus("Restocking");
					System.out.println("[" + this.getName() + "]" + " Restocking " + i.getName());
					this.restockIngredient(i);
					i.stopRestocking();
					this.setStatus("Idle");
				}
			}
		}
	}

	/**
	 * Restocks a particular ingredient
	 * @param ingredient to be restocked
	 */

	private void restockIngredient(Ingredient ingredient) {

		int restockAmount = ingredient.getRestockAmount().intValue();
		int pickedUpAmount = 0;

		DecimalFormat df = new DecimalFormat("##.####");


		/**
		 * Set the destination are source for the drone
		 * Assumption is made that an idle drone starts from the restaurant
		 * The source is the ingredient's supplier postcode
		 */

		this.setSource(server.getRestaurantPostcode());
		this.setDestination(ingredient.getSupplier().getPostcode());

		/**
		 * Travelling to a destination
		 * @param distance to the destination
		 * @param traveledDistance distance already traveled
		 * @param timeToDest the time it will take to travel to the destination
		 */

		double distance = this.calculateDistance() * 1000;
		System.out.println("[" + this.getName() + "] " + destination.getName() + " is " + distance + " metres away");
		double traveledDistance = 0;
		double timeToDest = distance / this.getSpeed().doubleValue();

		while(timeToDest > 0) {
			try {
				this.sleep(1);
				timeToDest--;
				traveledDistance += this.getSpeed().doubleValue();
				this.setProgress(Double.parseDouble(df.format(traveledDistance/distance)));
				this.setBattery(this.getBattery().doubleValue() - this.getSpeed().doubleValue()/100);
			} catch (InterruptedException e) {

			}
		}


		System.out.println("[" + this.getName() + "]" + " Reached destination " + this.getDestination().getName());

		/**
		 * Once the destination is reached, pick up the needed stock of the ingredient
		 * (Later a capacity could be implemented at this point)
		 */

		pickedUpAmount += restockAmount;

		/**
		 * Travel back to the restaurant from the supplier
		 */

		this.setSource(ingredient.getSupplier().getPostcode());
		this.setDestination(server.getRestaurantPostcode());

		traveledDistance = 0;
		timeToDest = distance / this.getSpeed().doubleValue();

		while(timeToDest > 0) {
			try {
				this.sleep(1);
				timeToDest--;
				traveledDistance += this.getSpeed().doubleValue();
				this.setProgress(Double.parseDouble(df.format(traveledDistance/distance)));
				this.setBattery(this.getBattery().doubleValue() - this.getSpeed().doubleValue()/100);

			} catch (InterruptedException e) {

			}
		}

		System.out.println("[" + this.getName() + "]" + " Delivered " + ingredient.getName());

		// Refresh the current stock in case the ingredient was uses while the drone was restocking it
		int currentStock = stockManagement.getStock(ingredient).intValue();
		stockManagement.setStock(ingredient, currentStock + pickedUpAmount);

	}

	/**
	 * Check for orders that are awaiting delivery
	 */

	private void checkOrders() {
		List<Order> orderList = server.getOrders();

		if(orderList == null || orderList.size() == 0)
			return;

		for(int i = 0; i < orderList.size(); i++) {

			Order o = orderList.get(i);

			if(o.getStatus() != null && o.getStatus().equals("Awaiting delivery")) {
				boolean testLocked = o.startDelivery();
				if(testLocked) {
					this.deliverOrder(o);
					o.stopDelivery();
					break;
				}

			}
		}
	}

	/**
	 * Delivers an order
	 * @param order to be delivered
	 */

	private boolean deliverOrder(Order order) {

		DecimalFormat df = new DecimalFormat("##.####");
		Postcode userPostcode = order.getUser().getPostcode();


		/**
		 * Set the destination are source for the drone
		 * Assumption is made that an idle drone starts from the restaurant
		 * The source is the ingredient's supplier postcode
		 */

		this.setSource(server.getRestaurantPostcode());
		this.setDestination(userPostcode);
		this.setStatus("Delivering order");
		//this.orderCancelled = false;
		order.setStatus("Being delivered");
		//order.setDroneDelivering(this);

		/**
		 * Travelling to a destination
		 * @param distance to the destination
		 * @param traveledDistance distance already traveled
		 * @param timeToDest the time it will take to travel to the destination
		 */

		double distance = this.calculateDistance() * 1000;
		System.out.println("[" + this.getName() + "] " + " delivering " + order.getName());
		System.out.println("[" + this.getName() + "] " + destination.getName() + " is " + distance + " metres away");
		double travelledDistance = 0;
		double timeToDest = distance / this.getSpeed().doubleValue();

		while(timeToDest > 0) {
			try {

				if(order.getStatus().equals("Cancelled")) {
					System.out.println("Order " + order.getName() + " Cancelled");
					distance = travelledDistance; // Set the distance back to the restaurant to the distance already travelled
					travelledDistance = 0;
					timeToDest = distance / this.getSpeed().doubleValue();
					this.setSource(null);
					this.setDestination(server.getRestaurantPostcode());
					this.setStatus("Returning to restaurant");

					//Start journey back to restaurant
					while(timeToDest > 0) {
						this.sleep(1);
						timeToDest--;
						travelledDistance += this.getSpeed().doubleValue();
						this.setProgress(Double.parseDouble(df.format(travelledDistance/distance)));
						this.setBattery(this.getBattery().doubleValue() - this.getSpeed().doubleValue()/100);

					}

					this.setDestination(null);
					this.setStatus("Idle");
					return true;
				}

				this.sleep(1);
				timeToDest--;
				travelledDistance += this.getSpeed().doubleValue();
				this.setProgress(Double.parseDouble(df.format(travelledDistance/distance)));
				this.setBattery(this.getBattery().doubleValue() - this.getSpeed().doubleValue()/100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		order.setStatus("Delivered");
		System.out.println("[" + this.getName() + "]" + " Reached destination " + this.getDestination().getName());
		System.out.println("[" + this.getName() + "]" + " Delivered " + order.getName());

		this.setStatus("Returning to restaurant");

		/**
		 * Travel back to the restaurant from the customer
		 */

		this.setSource(userPostcode);
		this.setDestination(server.getRestaurantPostcode());

		travelledDistance = 0;
		timeToDest = distance / this.getSpeed().doubleValue();

		while(timeToDest > 0) {
			try {
				this.sleep(1);
				timeToDest--;
				travelledDistance += this.getSpeed().doubleValue();
				this.setProgress(Double.parseDouble(df.format(travelledDistance/distance)));
				this.setBattery(this.getBattery().doubleValue() - this.getSpeed().doubleValue()/3);
			} catch (InterruptedException e) {

			}
		}

		System.out.println("[" + this.getName() + "]" + " Returned to restaurant");
		this.setStatus("Idle");
		this.setDestination(null);
		this.setSource(null);
		return true;


	}

	/**
	 * Recharge the drone
	 * @param curBattery the current battery level
	 */

	private void recharge(int curBattery) {
		this.setStatus("Recharging");
		while (curBattery < 100) {

			try {
				this.sleep(2);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			curBattery += 5;

			if(curBattery > 100)
				curBattery = 100;

			this.setBattery(curBattery);
		}

		this.setStatus("Idle");
	}



	/**
	 * If the status is set to delivering order
	 * This means that the drone has just been loaded after
	 * the application most likely crashed
	 * In this case, we send reset the drones position to the restaurant
	 */

	public void restoreDroneState() {

				this.setProgress(0);
				this.setSource(null);
				this.setDestination(null);
				this.setStatus("Idle");
	}

	public void setServerInstance(Server server) {
		this.server = server;
	}



}
