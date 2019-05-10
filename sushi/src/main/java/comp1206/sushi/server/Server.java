package comp1206.sushi.server;

import comp1206.sushi.DataPersistence;
import comp1206.sushi.ServerComms;
import comp1206.sushi.common.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

public class Server implements ServerInterface, Serializable {

    private static final Logger logger = LogManager.getLogger("Server");
	private static final long serialVersionUID = 1L;
	

	public   Restaurant restaurant;
	public   List<Dish> dishes = new ArrayList<Dish>();
	public  List<Drone> drones = new ArrayList<Drone>();
	public  List<Ingredient> ingredients = new ArrayList<Ingredient>();
	public  List<Order> orders = new ArrayList<Order>();
	public  List<Staff> staff = new ArrayList<Staff>();
	public  List<Supplier> suppliers = new ArrayList<Supplier>();
	public  List<User> users = new ArrayList<User>();
	public  List<Postcode> postcodes = new ArrayList<Postcode>();
	private transient ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();
	private  StockManagement stockManagement = new StockManagement(this);
	private  List<Order> pendingOrders = new ArrayList<>();

	private ServerComms serverComms = new ServerComms(this);
	private boolean restockingIngrEnabled;
	private boolean restockingDishEnabled;

	private final String saveFileName = "server_data.ser";
	private DataPersistence dp;

	private boolean stateRestored;

	
	public Server() {
        logger.info("Starting up server...");


		/**
		 * Try to load serialized data from a file
		 * If the attempt was unsuccessful, populate the server with placeholder data
		 */

		Server temp = (Server) DataPersistence.load(saveFileName);
			if(temp!= null) {
				this.restaurant = temp.getRestaurant();
				this.stateRestored = false;

				if (restaurant == null) {
					Postcode restaurantPostcode = new Postcode("SO17 1BJ");
					restaurant = new Restaurant("Mock Restaurant", restaurantPostcode);
				}

				this.postcodes = temp.getPostcodes();
				this.dishes = temp.getDishes();
				this.drones = temp.getDrones();
				this.ingredients = temp.getIngredients();
				this.orders = temp.getOrders();
				this.users = temp.getUsers();
				this.staff = temp.getStaff();
				this.stockManagement = temp.getStockManagement();
				this.stockManagement.setServerInstance(this);
				this.suppliers = temp.getSuppliers();
				this.pendingOrders = temp.getPendingOrders();
				this.restockingDishEnabled = temp.restockingDishEnabled;
				this.restockingIngrEnabled = temp.restockingIngrEnabled;

				/**
				 * Restore the state of the different models
				 */

				restoreModelState();

				this.dp = new DataPersistence(this, saveFileName);
			} else {

				restockingDishEnabled = true;
				restockingIngrEnabled = true;

				Postcode restaurantPostcode = new Postcode("SO17 1BJ");
				restaurant = new Restaurant("Mock Restaurant", restaurantPostcode);

				Postcode postcode1 = addPostcode("SO17 1TJ");
				Postcode postcode2 = addPostcode("SO17 1BX");
				Postcode postcode3 = addPostcode("SO17 2NJ");
				Postcode postcode4 = addPostcode("SO17 1TW");
				Postcode postcode5 = addPostcode("SO17 2LB");

				Supplier supplier1 = addSupplier("Supplier 1", postcode1);
				Supplier supplier2 = addSupplier("Supplier 2", postcode2);
				Supplier supplier3 = addSupplier("Supplier 3", postcode3);

				Ingredient ingredient1 = addIngredient("Ingredient 1", "grams", supplier1, 50, 5, 1);
				Ingredient ingredient2 = addIngredient("Ingredient 2", "grams", supplier2, 50, 5, 1);
				Ingredient ingredient3 = addIngredient("Ingredient 3", "grams", supplier3, 50, 5, 1);

				Dish dish1 = addDish("Dish 1", "Dish 1", 1, 10, 10);
				Dish dish2 = addDish("Dish 2", "Dish 2", 2, 10, 10);
				Dish dish3 = addDish("Dish 3", "Dish 3", 3, 10, 10);

				orders.add(new Order());

				addIngredientToDish(dish1, ingredient1, 1);
				addIngredientToDish(dish1, ingredient2, 2);
				addIngredientToDish(dish2, ingredient2, 3);
				addIngredientToDish(dish2, ingredient3, 1);
				addIngredientToDish(dish3, ingredient1, 2);
				addIngredientToDish(dish3, ingredient3, 1);

				addStaff("Staff 1");
				addStaff("Staff 2");
				addStaff("Staff 3");

				addDrone(1);
				addDrone(2);
				addDrone(3);

				// Initialize some random values for the stock levels

				Random random = new Random();
				List<Ingredient> ingredients = getIngredients();
				for (Ingredient ingredient : ingredients) {
					this.setStock(ingredient, random.nextInt(50));
				}

				List<Dish> dishes = getDishes();
				for (Dish dish : dishes) {
					this.setStock(dish, random.nextInt(50));
				}

				stockManagement.setStock(ingredient1, 100);
				stockManagement.setStock(ingredient2, 100);
				stockManagement.setStock(ingredient3, 100);

				dp = new DataPersistence(this, saveFileName);
			}

	}
	
	@Override
	public List<Dish> getDishes() {
		return this.dishes;
	}

	@Override
	public Dish addDish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		Dish newDish = new Dish(name,description,price,restockThreshold,restockAmount);
		this.dishes.add(newDish);
		this.setStock(newDish, 0);
		//this.serverComms.updateClients(this.dishes);
		this.notifyUpdate();
        return newDish;
	}
	
	@Override
	public void removeDish(Dish dish) {
		this.dishes.remove(dish);
		this.stockManagement.setStock(dish, -1);
        this.notifyUpdate();
	}

	@Override
	public void setRestockingIngredientsEnabled(boolean enabled) {
		this.restockingIngrEnabled = enabled;
	}

	/**
	 * @return true if restocking ingredients is enabled,
	 * 		   false otherwise
	 */

	public boolean isRestockingIngrEnabled() {
		return this.restockingIngrEnabled;
	}

	/**
	 * @return true if restocking dishes is enabled,
	 * 		   false otherwise
	 */

	public boolean isRestockingDishEnabled() {
		return this.restockingDishEnabled;
	}


	@Override
	public void setRestockingDishesEnabled(boolean enabled) {
		this.restockingDishEnabled = enabled;
		
	}
	
	@Override
	public void setStock(Dish dish, Number stock) {
        this.stockManagement.setStock(dish, stock);
	}

	@Override
	public void setStock(Ingredient ingredient, Number stock) {
        this.stockManagement.setStock(ingredient, stock);
	}

	@Override
	public List<Ingredient> getIngredients() {
		return this.ingredients;
	}

	@Override
	public Ingredient addIngredient(String name, String unit, Supplier supplier,
			Number restockThreshold, Number restockAmount, Number weight) {
		Ingredient mockIngredient = new Ingredient(name,unit,supplier,restockThreshold,restockAmount,weight);
		this.ingredients.add(mockIngredient);
		this.setStock(mockIngredient, 0);
		this.notifyUpdate();
        return mockIngredient;
	}

	@Override
	public void removeIngredient(Ingredient ingredient) throws UnableToDeleteException {

		for(Dish d : this.dishes) {
			if(d.getRecipe().containsKey(ingredient))
				throw new UnableToDeleteException("Ingredient in use");
		}

		int index = this.ingredients.indexOf(ingredient);
		this.ingredients.remove(index);
		this.stockManagement.setStock(ingredient, -1);
        this.notifyUpdate();
	}

	public void forceRemoveIngredient(Ingredient ingredient) {
		int index = this.ingredients.indexOf(ingredient);
		this.ingredients.remove(index);
		this.stockManagement.setStock(ingredient, -1);
		this.notifyUpdate();
	}

	@Override
	public List<Supplier> getSuppliers() {
		return this.suppliers;
	}

	@Override
	public Supplier addSupplier(String name, Postcode postcode) {
		Supplier mock = new Supplier(name,postcode);
		this.suppliers.add(mock);
        return mock;
	}


	@Override
	public void removeSupplier(Supplier supplier) throws UnableToDeleteException{

		for(Ingredient i : this.ingredients){
			if(i.getSupplier().getName().equals(supplier.getName()))
				throw new UnableToDeleteException("Supplier in use");
		}

		int index = this.suppliers.indexOf(supplier);
		this.suppliers.remove(index);
        this.notifyUpdate();
	}

	public void forceRemoveSupplier(Supplier supplier) {
		int index = this.suppliers.indexOf(supplier);
		this.suppliers.remove(index);
		this.notifyUpdate();
	}

	@Override
	public List<Drone> getDrones() {
		return this.drones;
	}

	@Override
	public Drone addDrone(Number speed) {
		Drone mock = new Drone(speed, this);
		this.drones.add(mock);
        return mock;
	}

	@Override
	public void removeDrone(Drone drone) throws UnableToDeleteException {

		if(!drone.getStatus().equals("Idle"))
			throw new UnableToDeleteException("Can't delete a working drone");

		int index = this.drones.indexOf(drone);
		drone.stop();
		this.drones.remove(index);
        this.notifyUpdate();
	}

	public void forceRemoveDrone(Drone drone) {
		int index = this.drones.indexOf(drone);
		drone.stop();
		this.drones.remove(index);
		this.notifyUpdate();
	}

	@Override
	public List<Staff> getStaff() {
		return this.staff;
	}

	@Override
	public Staff addStaff(String name) {
		Staff mock = new Staff(name, this);
		//new Thread(mock).start();
		this.staff.add(mock);
        return mock;
	}

	@Override
	public void removeStaff(Staff staff) throws UnableToDeleteException {

		if(!staff.getStatus().equals("Idle"))
			throw new UnableToDeleteException("Can't delete a working staff mamber");

		this.staff.remove(staff);
		staff.stop();
        this.notifyUpdate();
	}

	public void forceRemoveStaff(Staff staff) {
		this.staff.remove(staff);
		staff.stop();
		this.notifyUpdate();
	}

	@Override
	public List<Order> getOrders() {
		return this.orders;
	}

	/**
	 * Add an order to the list
	 * @param order - order to be added
	 * @return the successfully added order or null if the order failed
	 */

	public Order addOrder(Order order) {

		User orderUser = order.getUser();

		for(User u : this.getUsers()) {
			if(u.getName().equals(orderUser.getName())){
				u.addUserOrder(order);
				break;
			}
		}

		if(order.getStatus() == null)
			order.setStatus("Pending");

		this.pendingOrders.add(order);
		this.notifyUpdate();
		this.orders.add(order);
        return order;
	}

	/**
	 * Cancel an order
	 */

	public void cancelOrder(Order order) {

		String orderStatus = order.getStatus();

		if(orderStatus.equals("Pending")) {
			this.pendingOrders.remove(order);
		} else if(orderStatus.equals("Cancelled")) {
			return;
		}

		order.setStatus("Cancelled");

		/**
		 * Restore the stock levels of the dishes in the order
		 */

		Map<Dish, Number> orderContent = this.stockManagement.getCorrectReferences(order.getOrderContent());

		/**
		 * Do not increment stocks if the order had been cancelled while the dishes
		 * were being prepared
		 */

		if(order.getMoreThanCurStock())
			return;

		for(Dish d : orderContent.keySet()) {
			int currentStock = this.getStock(d).intValue();
			int orderAmount = orderContent.get(d).intValue();
			this.setStock(d, currentStock + orderAmount);
		}


    }

	/**
	 * Get the stock levels for a particular dish
	 * @param dish - dish to lookup
	 * @return the stock level
	 */

	public Number getStock(Dish dish) {
		return this.stockManagement.getStock(dish);
	}

	/**
	 * Get the stock levels for a particular ingredient
	 * @param ingredient - dish to lookup
	 * @return the stock level
	 */

	public Number getStock(Ingredient ingredient) {
		return this.stockManagement.getStock(ingredient);
	}

	@Override
	public void removeOrder(Order order) throws UnableToDeleteException {

		int index = this.orders.indexOf(order);


		if(order.getStatus() != null && order.getStatus().equals("Being delivered"))
			throw new UnableToDeleteException("Order is being processed/delivered");

		if(order.getStatus() != null && order.getStatus().equals("Awaiting delivery")) {
			Map<Dish, Number> recipe = order.getOrderContent();
			for(Dish d : recipe.keySet()) {
				int curStock = this.getStock(d).intValue();
				this.setStock(d, curStock + recipe.get(d).intValue());
			}
		}


		this.orders.remove(index);
		if(order.getUser() != null && order.getUser().getUserOrders().size() > 0)
			try {
				order.getUser().removeUserOrder(order);
			} catch (NullPointerException e) {

			}
		this.notifyUpdate();
	}

	public void forceRemoveOrder(Order order) {
		int index = this.orders.indexOf(order);
		this.orders.remove(index);
		this.notifyUpdate();
	}
	
	@Override
	public Number getOrderCost(Order order) {
		return order.getOrderCost();
	}

	@Override
	public Map<Ingredient, Number> getIngredientStockLevels() {
		return this.stockManagement.getIngredientStockLevels();
	}



	@Override
	public Number getSupplierDistance(Supplier supplier) {
		return supplier.getDistance();
	}

	@Override
	public Number getDroneSpeed(Drone drone) {
		return drone.getSpeed();
	}

	@Override
	public Number getOrderDistance(Order order) {
		Order mock = (Order)order;
		return mock.getDistance();
	}

	@Override
	public void addIngredientToDish(Dish dish, Ingredient ingredient, Number quantity) {

			if(quantity.intValue() == 0) {
				dish.getRecipe().remove(ingredient);
			} else {
				dish.getRecipe().put(ingredient, quantity);
			}
	}

	@Override
	public void removeIngredientFromDish(Dish dish, Ingredient ingredient) {
		dish.getRecipe().remove(ingredient);
		this.notifyUpdate();
	}

	@Override
	public Map<Ingredient, Number> getRecipe(Dish dish) {
		return dish.getRecipe();
	}

	@Override
	public Map<Dish, Number> getDishStockLevels() {
		return this.stockManagement.getDishStockLevels();
	}

	@Override
	public List<Postcode> getPostcodes() {
		return this.postcodes;
	}

	@Override
	public Postcode addPostcode(String code) {
		Postcode mock;
		if(restaurant != null) {
			mock = new Postcode(code, restaurant);
		} else {
			mock = new Postcode(code);
		}
		this.postcodes.add(mock);
		//this.serverComms.updateClients(this.postcodes);
		this.notifyUpdate();
		return mock;
	}

	@Override
	public void removePostcode(Postcode postcode) throws UnableToDeleteException {

		for(User u : this.users) {
			if(u.getPostcode().getName().equals(postcode.getName())){
				throw new UnableToDeleteException("Postcode used by " + u.getName());
			}
		}

		for(Supplier s : this.suppliers) {
			if(s.getPostcode().getName().equals(postcode.getName())){
				throw new UnableToDeleteException("Postcode used by " + s.getName());
			}
		}

		this.postcodes.remove(postcode);
		//this.serverComms.updateClients(this.postcodes);
		this.notifyUpdate();
	}

	public void forceRemovePostcode(Postcode postcode) {
		this.postcodes.remove(postcode);
		this.notifyUpdate();
	}

	@Override
	public List<User> getUsers() {
		return this.users;
	}
	
	@Override
	public void removeUser(User user) throws UnableToDeleteException{

		for(User u : serverComms.getConnectedUsers()) {
			if(u.getName().equals(user.getName()))
				throw new UnableToDeleteException("User is currently logged in");
		}

		this.users.remove(user);
		this.notifyUpdate();
	}

	public void forceRemoveUser(User user) {
		this.users.remove(user);
		this.notifyUpdate();
	}

	/**
	 * Register a user
	 * @param user to be added to the list
	 * @return true if the user is regsitered successfully
	 */

	public boolean registerUser(User user) {
		for(User u : this.getUsers()) {
			if(user.getName().equals(u.getName())) {
				return false;
			}
		}
		DataPersistence.save(this, saveFileName);
		this.users.add(user);
		return true;
	}

	public User checkLogin(String username, String password) {
		for(User u : this.getUsers()) {
			if(u.getName().equals(username) && u.checkLogin(password)) {
				return u;
			}
		}
		return null;
	}

	@Override
	public void loadConfiguration(String filename) {
		try {
			Configuration.cleanConfiguration(this);
			Configuration.parseConfiguration(filename, this);
			System.out.println("Loaded configuration: " + filename);
		} catch (IOException| UnableToDeleteException e) {
			System.out.println("Error loading configuration");
			System.out.println(e.getMessage());
		}
	}

	@Override
	public void setRecipe(Dish dish, Map<Ingredient, Number> recipe) {
		for(Entry<Ingredient, Number> recipeItem : recipe.entrySet()) {
			addIngredientToDish(dish,recipeItem.getKey(),recipeItem.getValue());
		}

		this.notifyUpdate();
	}

	@Override
	public boolean isOrderComplete(Order order) {
		return true;
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}
	
	@Override
	public String getDroneStatus(Drone drone) {
		return drone.getStatus();
	}
	
	@Override
	public String getStaffStatus(Staff staff) {
		return staff.getStatus();
	}

	@Override
	public void setRestockLevels(Dish dish, Number restockThreshold, Number restockAmount) {
		dish.setRestockThreshold(restockThreshold);
		dish.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public void setRestockLevels(Ingredient ingredient, Number restockThreshold, Number restockAmount) {
		ingredient.setRestockThreshold(restockThreshold);
		ingredient.setRestockAmount(restockAmount);
		this.notifyUpdate();
	}

	@Override
	public Number getRestockThreshold(Dish dish) {
		return dish.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Dish dish) {
		return dish.getRestockAmount();
	}

	@Override
	public Number getRestockThreshold(Ingredient ingredient) {
		return ingredient.getRestockThreshold();
	}

	@Override
	public Number getRestockAmount(Ingredient ingredient) {
		return ingredient.getRestockAmount();
	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);
	}
	
	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));
	}

	@Override
	public Postcode getDroneSource(Drone drone) {
		return drone.getSource();
	}

	@Override
	public Postcode getDroneDestination(Drone drone) {
		return drone.getDestination();
	}

	@Override
	public Number getDroneProgress(Drone drone) {
		return drone.getProgress();
	}

	@Override
	public String getRestaurantName() {
		return restaurant.getName();
	}

	@Override
	public Postcode getRestaurantPostcode() {
		return restaurant.getLocation();
	}
	
	@Override
	public Restaurant getRestaurant() {
		return restaurant;
	}

	public StockManagement getStockManagement () {
		return this.stockManagement;
	}

	public List<Order> getPendingOrders() { return this.pendingOrders; }

	public String getSaveFileName() {
		return saveFileName;
	}

	public ServerComms getServerComms() {
		return this.serverComms;
	}


	private void restoreModelState() {
		/**
		 * If any orders were currently being delivered, reset their status to pending
		 * and return the stock levels back to where they were
		 * Check if any order has its lock set to true and set it to false
		 */

		if (!stateRestored) {

			for (Order o : this.getOrders()) {

				if(o.getStatus() == null)
					continue;

				if (o.getStatus().equals("Being delivered") || o.getStatus().equals("Awaiting delivery")) {
					o.setStatus("Pending");

					if(!o.isStockDecrementeed()) {
						System.out.println(o.isStockDecrementeed());
						Map<Dish, Number> orderContent = o.getOrderContent();

						for (Dish d : orderContent.keySet()) {
							int orderAmount = orderContent.get(d).intValue();
							int stockAmount = this.getStock(d).intValue();
							this.setStock(d, stockAmount + orderAmount);
						}

						o.setStockDecrementeed(true);
						System.out.println(o.isStockDecrementeed());

					}
					this.pendingOrders.add(o);

				}


				o.stopProcessing();

				o.stopDelivery();

			}

			/**
			 * Check for any dishes / ingredients that had been locked and unlock them
			 */

			for (Dish d : this.getDishes()) {
				d.stopRestocking();
			}

			for (Ingredient i : this.getIngredients()) {
				i.stopRestocking();
			}

			/**
			 * Start all the drone and staff threads and restore their states
			 */

			for (Drone d : this.getDrones()) {
				d.setServerInstance(this);
				d.restoreDroneState();
				new Thread(d).start();
			}

			for (Staff s : this.getStaff()) {
				s.setServerInstance(this);
				s.restoreStaffState();
				new Thread(s).start();
			}
		}

		stateRestored = true;
	}

}
