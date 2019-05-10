package comp1206.sushi.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class Order extends Model {

	/**
	 * @param status the order status
	 * @param orderContent mapping of the contents of the order
	 * @param user that has placed the order
	 * @param locked used as a switch to indicate if the order is currently
	 *               being delivered by a drone
	 * @param processed used as a switch to indicated if the order is currently
	 *                  being processed by a staff member
	 */

	private String status;
	private Map<Dish, Number> orderContent;
	private User user;
	private boolean locked;
	private boolean processed;
	private boolean moreThanCurStock;



	private boolean stockDecrementeed;

	public Order() {
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/YYYY HH:mm:ss");  
		LocalDateTime now = LocalDateTime.now();  
		this.name = dtf.format(now);
		this.orderContent = new HashMap<>();
		this.locked = false;
		this.processed = false;
		this.moreThanCurStock = true;
		this.stockDecrementeed = false;

	}

	public Number getDistance() {
		return 1;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public synchronized String getStatus() {
		return status;
	}

	public synchronized void setStatus(String status) {
		notifyUpdate("status",this.status,status);
		this.status = status;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public User getUser() { return this.user; }

	/**
	 * Add a dish to the order
	 * @param dish to be added
	 * @param quantity
	 */

	public void addToOrder(Dish dish, Number quantity) {
		this.orderContent.put(dish, quantity);
	}

	public Map<Dish, Number> getOrderContent() {return this.orderContent; }

	/**
	 * Calculate the cost of the order
	 * @return the cost
	 */

	public Number getOrderCost() {
		int cost = 0;

		for(Dish dish : orderContent.keySet()) {
			cost += dish.getPrice().intValue() * orderContent.get(dish).intValue();
		}

		return cost;
	}

	/**
	 * Called when a drone attempts to deliver an order
	 * @return true if the order is ready to be delivered
	 */

	public synchronized boolean startDelivery() {
		if(locked) {
			return false;
		} else {
			locked = true;
			return true;
		}
	}

	public synchronized boolean stopDelivery() {
		if(locked) {
			locked = false;
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean startProcessing() {
		if(processed) {
			return false;
		} else {
			processed = true;
			return true;
		}
	}

	public synchronized boolean stopProcessing() {
		if(processed) {
			processed = false;
			return true;
		} else {
			return false;
		}
	}

	public synchronized boolean getMoreThanCurStock() {return this.moreThanCurStock;}

	public synchronized void setMoreThanCurStock(boolean bool) { this.moreThanCurStock = bool; }

	public boolean isStockDecrementeed() {
		return stockDecrementeed;
	}

	public void setStockDecrementeed(boolean stockDecrementeed) {
		this.stockDecrementeed = stockDecrementeed;
	}


}


