package comp1206.sushi.common;


import comp1206.sushi.server.Server;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Staff extends Model implements Serializable, Runnable {

	private String name;
	private String status;
	private Number fatigue;
	private StockManagement stockManagement;
	private transient Server serverInstance;
	private boolean active;
	private Order mostRecentOrder;

	public Staff(String name, Server server) {
		this.serverInstance = server;
		this.setName(name);
		this.setFatigue(0);
		this.setStatus("Idle");
		this.stockManagement = server.getStockManagement();
		this.active = true;
		new Thread(this).start();

	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Number getFatigue() {
		return fatigue;
	}

	public void setFatigue(Number fatigue) {
		this.fatigue = fatigue;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		notifyUpdate("status", this.status, status);
		this.status = status;
	}

	@Override
	public void run() {


		try {
			this.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		/**
		 * Run the thread as long as it is not interrupted
		 * If the staff's fatigue is >= 100, recharge
		 * Look for potential jobs
		 */

		while (this.isActive()) {

			int staffFatigue = this.getFatigue().intValue();

			if (staffFatigue >= 100){
				this.setStatus("Recharging");
				while (staffFatigue > 0) {

					try {
						this.sleep(2);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					staffFatigue -= 5;

					if(staffFatigue < 0)
						staffFatigue = 0;

					this.setFatigue(staffFatigue);
				}

				this.setStatus("Idle");

			}

			if (this.getStatus().equals("Idle")) {
				if(serverInstance.isRestockingDishEnabled()) {
					checkDishesStock();
				}
				checkPendingOrders();
			}


			 /**
			  * Tmeout for 2 seconds
 			  */

			try  {
				this.sleep(2);
			} catch (InterruptedException e) {
				this.stop();
				//e.printStackTrace();
			}
		}
	}

	/**
	 * Stops the staff member
	 */

	public void stop() {
		this.active = false;
	}

	public boolean isActive() {
		return this.active;
	}

	/**
	 * Pauses thread execution for a period of time
	 *
	 * @param timeout sleep time in seconds
	 * @throws InterruptedException
	 */

	public void sleep(long timeout) throws InterruptedException {

		Thread.sleep(timeout * 1000);
	}

	/**
	 * Check the stocks of dishes and restock if needed
	 */

	private void checkDishesStock() {
		Map<Dish, Number> dishLevels = stockManagement.getDishStockLevels();

		for (Dish d : dishLevels.keySet()) {
			int stockLevel = stockManagement.getStock(d).intValue();
			int threshold = d.getRestockThreshold().intValue();


			if (stockLevel < threshold) {
				this.setStatus("Restocking");
				boolean testLocked = d.startRestocking();


				if (testLocked) {
					/*System.out.println("[" + this.getName() + "]" + " Restocking " + d.getName());
					System.out.println("[" + this.getName() + "]" + " Stock is " + stockLevel + " Threshold is " + threshold);*/
					this.restockDish(d);
					d.stopRestocking();
				}

				this.setStatus("Idle");
			}
		}
	}

	/**
	 * Restocks a particular dish
	 * Decrements the stocks of the ingredients that are used in the dish
	 *
	 * @param dish to be restocked
	 * @return true if the restock has been successful, false if it has failed
	 */

	private boolean restockDish(Dish dish) {


		int currentStock = stockManagement.getStock(dish).intValue();
		int restockThreshold = dish.getRestockThreshold().intValue();
		int restockAmount = dish.getRestockAmount().intValue();
		int staffFatigue = this.getFatigue().intValue();
		Map<Ingredient, Number> dishRecipe = dish.getRecipe();

		while (currentStock < restockThreshold) {

			currentStock = stockManagement.getStock(dish).intValue();

			/**
			 * Iterate over the ingredients in the recipe
			 * Return if there the stocks are not enough to fulfil a restock
			 * Decrement the used up ingredients
			 */

			for (Ingredient i : dishRecipe.keySet()) {
				// Get the current stock of the ingredient
				int currentIngredientStock = stockManagement.getStock(i).intValue();

				if (dishRecipe.get(i).intValue() > currentIngredientStock) {
					/*System.out.println("[" + this.getName() + "]" + " Insufficient stock for " + i.getName());
					System.out.println("[" + this.getName() + "]" + " Required: " + dishRecipe.get(i).intValue() * restockAmount
							+ " Available: " + currentIngredientStock);*/
					dish.stopRestocking();
					return false;
				} else {
					currentIngredientStock -= dishRecipe.get(i).intValue();
					stockManagement.setStock(i, currentIngredientStock);
				}
			}

			long randSleepTime = new Random().nextInt(40) + 20;


			try {
				this.sleep(randSleepTime);
				currentStock = stockManagement.getStock(dish).intValue();
				currentStock += restockAmount;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("[" + this.getName() + "]" + " Prepared " + dish.getName() + "in " + randSleepTime + " seconds");

			System.out.println("[" + this.getName() + "]" + " Current is: " + currentStock + " Threshold is: " + restockThreshold);

			stockManagement.setStock(dish, currentStock);
		}

		staffFatigue += 10;
		this.setFatigue(staffFatigue);
		return true;
	}

	/**
	 * Restocks a dish given a particular required amount for the dish
	 * Decrements the stocks of the ingredients that are used in the dish
	 * @param requiredAmount the required amount
	 * @param dish to be restocked
	 * @return true if the restock has been successful, false if it has failed
	 */

	private boolean restockDish(Dish dish, int requiredAmount) {


		int currentStock = stockManagement.getStock(dish).intValue();
		int restockAmount = dish.getRestockAmount().intValue();
		int staffFatigue = this.getFatigue().intValue();
		Map<Ingredient, Number> dishRecipe = dish.getRecipe();
		this.setStatus("Restocking");

		while (currentStock < requiredAmount) {

			/**
			 * Iterate over the ingredients in the recipe
			 * Return if there the stocks are not enough to fulfil a restock
			 * Decrement the used up ingredients
			 */

			for (Ingredient i : dishRecipe.keySet()) {
				// Get the current stock of the ingredient
				int currentIngredientStock = stockManagement.getStock(i).intValue();

				if (dishRecipe.get(i).intValue() > currentIngredientStock) {
					/*System.out.println("[" + this.getName() + "]" + " Insufficient stock for " + i.getName());
					System.out.println("[" + this.getName() + "]" + " Required: " + dishRecipe.get(i).intValue() * restockAmount
							+ " Available: " + currentIngredientStock);*/
					return false;
				} else {
					currentIngredientStock -= dishRecipe.get(i).intValue();
					stockManagement.setStock(i, currentIngredientStock);
				}
			}

			long randSleepTime = new Random().nextInt(40) + 20;


			try {
				this.sleep(randSleepTime);
				currentStock = stockManagement.getStock(dish).intValue();
				currentStock += restockAmount;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			System.out.println("[" + this.getName() + "]" + " Prepared " + dish.getName() + "in " + randSleepTime + " seconds");

			System.out.println("[" + this.getName() + "]" + " Current is: " + currentStock + " Required is: " + requiredAmount);

			stockManagement.setStock(dish, currentStock);


		}

		staffFatigue += 10;
		this.setFatigue(staffFatigue);
		return true;
	}


	/**
	 * Process an incoming order
	 *
	 * @param order to be processed
	 * @return true if the order has been processed successfully
	 */

	private boolean processOrder(Order order) {

		this.setStatus("Processing Order");

		Map<Dish, Number> orderContent = stockManagement.getCorrectReferences(order.getOrderContent());
		int staffFatigue = this.getFatigue().intValue();


		for (Dish dish : orderContent.keySet()) {

			int stockLevel = stockManagement.getStock(dish).intValue();
			int requiredAmount = orderContent.get(dish).intValue();

			// Restock the dish if the required amount is greater than the current stock levels
			if (requiredAmount > stockLevel) {

				boolean testLocked = dish.startRestocking();

				if (!testLocked) {
					this.setStatus("Idle");
					return false; //Dish is already being restocked
				}

				//System.out.println("[" + this.getName() + "]" + "Insufficient stocks of dish " + dish.getName() + " for order: " + order.getName());

				this.setStatus("Restocking");
				boolean successfulRestock = restockDish(dish, requiredAmount);
				dish.stopRestocking();
				this.setStatus("Processing order");

				// Dish was not restocked successfully
				if (!successfulRestock) {
					this.setStatus("Idle");
					return false;
				}
			}
		}

		if(!order.getStatus().equals( "Cancelled") && !order.isStockDecrementeed()) {
			for (Dish dish : orderContent.keySet()) {
				order.setStockDecrementeed(true);
				stockManagement.setStock(dish, stockManagement.getStock(dish).intValue() - orderContent.get(dish).intValue());
			}
		}

		staffFatigue += 10;
		this.setFatigue(staffFatigue);
		this.setStatus("Idle");
		return true;
	}

	/**
	 * Iterate over the pending orders in the server and look for a job
	 */

	private void checkPendingOrders() {
		List<Order> orderList = serverInstance.getPendingOrders();

		for (int i = 0; i < orderList.size(); i++) {

			Order curOrder = orderList.get(i);

			if (curOrder.getStatus().equals("Pending") && this.getStatus().equals("Idle")) {

				boolean orderIsProcessed = curOrder.startProcessing();

				if (orderIsProcessed) {
					boolean success = this.processOrder(curOrder);

					// If the order has been processed successfully, remove it from the pending orders list
					if (success && curOrder.getStatus().equals("Pending")) {
						curOrder.setStatus("Awaiting delivery");
						serverInstance.getPendingOrders().remove(orderList.get(i));
					}

					curOrder.setMoreThanCurStock(false);
					curOrder.stopProcessing();

				}
			}

		}
	}

	/**
	 * Used to restore the state of the thread after deserializing
	 *
	 */

	public void restoreStaffState() {

			this.setStatus("Idle");
	}

	public void setServerInstance(Server server) {
		this.serverInstance = server;
	}
}


