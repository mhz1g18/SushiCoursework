package comp1206.sushi.client;

import comp1206.sushi.ClientComms;
import comp1206.sushi.MessageWrapper;
import comp1206.sushi.common.*;
import javafx.util.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Client implements ClientInterface, Serializable {

    private static final Logger logger = LogManager.getLogger("Client");

	public Restaurant restaurant;
	public ArrayList<Dish> dishes = new ArrayList<Dish>();
	public ArrayList<Ingredient> ingredients = new ArrayList<Ingredient>();
	public ArrayList<Order> orders = new ArrayList<Order>();
	public ArrayList<Staff> staff = new ArrayList<Staff>();
	public ArrayList<Dish> basket = new ArrayList<Dish>();
	public ArrayList<Postcode> postcodes = new ArrayList<Postcode>();
	private ArrayList<UpdateListener> listeners = new ArrayList<UpdateListener>();

	private User loggedInUser;

	private ClientComms clientComms;

	public Client() {
        logger.info("Starting up client...");

        clientComms  = new ClientComms(this);


	}

	@Override
	public Restaurant getRestaurant() {
		MessageWrapper message = new MessageWrapper("getRestaurant", null);

		try {
			boolean success = clientComms.sendMessage(message);
			if(success) {
				Object response =  clientComms.receiveMessage();

				while(!(response instanceof Restaurant))
					response = clientComms.receiveMessage();

				this.restaurant = (Restaurant) response;
				return this.restaurant;
			}


		} catch (IOException | ClassNotFoundException e) {
			System.out.println("Error connecting to server");
		}

		return null;
	}

	@Override
	public String getRestaurantName() {
		return  this.restaurant.getName();
	}

	@Override
	public Postcode getRestaurantPostcode() {
		return this.restaurant.getLocation();
	}

	@Override
	public User register(String username, String password, String address, Postcode postcode) {

		User user = null;

		user = new User(username, password, address, postcode);

		MessageWrapper message = new MessageWrapper("registerUser", user);

			try {
				boolean success = clientComms.sendMessage(message);
				if(success) {
					Thread.sleep(250);
					Object response =  clientComms.receiveMessage();

					while(!(response instanceof Boolean)) response = clientComms.receiveMessage();

					if (((Boolean)response).booleanValue()) {
						System.out.println("Successfully registered " + user.getName());
						return user;
					} else {
						return null;
					}
				}
			} catch (ClassNotFoundException | ClassCastException | IOException | InterruptedException e ) {
				System.out.println("Error connecting to server");
			}

		return null;
	}

	@Override
	public User login(String username, String password) {
		Pair<String, String> temp = new Pair(username, password);


		MessageWrapper message = new MessageWrapper("loginUser", temp);

		try {
			boolean success = clientComms.sendMessage(message);
			if(success) {
				Thread.sleep(100);
				Object response =  clientComms.receiveMessage();

				while(!((response instanceof User) || response == null))
					response = clientComms.receiveMessage();

				if (response != null)
					loggedInUser = (User)response;
					return loggedInUser;
			}
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			System.out.println("Error connecting to server");
		}


		return null;
	}

	@Override
	public List<Postcode> getPostcodes() {

		MessageWrapper message = new MessageWrapper("getPostcodes", null);
		try {
			boolean success = this.clientComms.sendMessage(message);
			if(success) {
				Thread.sleep(100);
				this.postcodes = (ArrayList<Postcode>) clientComms.receiveMessage();
			}
		} catch (IOException | ClassNotFoundException | InterruptedException e ) {
			System.out.println("Error connecting to server");
		}


		return this.postcodes;
	}

	@Override
	public List<Dish> getDishes() {
		System.out.println("Calling get dishes");
		MessageWrapper message = new MessageWrapper("getDishes", null);
		try {
			boolean success = this.clientComms.sendMessage(message);
			if(success) {
				Thread.sleep(100);

				Object response =  clientComms.receiveMessage();

				while(!((response instanceof List) || response == null))
					this.dishes = (ArrayList<Dish>) clientComms.receiveMessage();

				if(response != null)
					return (ArrayList<Dish>)response;
			}
		} catch (IOException | ClassNotFoundException | InterruptedException e ) {
			System.out.println("Error connecting to server");
		}


		return this.dishes;
	}

	@Override
	public String getDishDescription(Dish dish) {
		return dish.getDescription();
	}

	@Override
	public Number getDishPrice(Dish dish) {
		return dish.getPrice();
	}

	@Override
	public Map<Dish, Number> getBasket(User user) {
		return user.getBasket();
	}

	@Override
	public Number getBasketCost(User user) {

		double cost = 0;
		Map<Dish, Number> basket = user.getBasket();

		for(Dish dish : basket.keySet()) {
			cost += basket.get(dish).doubleValue() * dish.getPrice().doubleValue();
		}

		return cost;
	}


	@Override
	public void addDishToBasket(User user, Dish dish, Number quantity) {
		user.addDishToBasket(dish, quantity.intValue());
		this.notifyUpdate();

	}

	@Override
	public void updateDishInBasket(User user, Dish dish, Number quantity) {
		user.addDishToBasket(dish, quantity.intValue());
		this.notifyUpdate();

	}

	@Override
	public Order checkoutBasket(User user) {
		System.out.println("Calling checkout basket");
		Map<Dish, Number> basket = user.getBasket();
			Order newOrder = new Order();

			for(Dish dish : basket.keySet()) {
				if (basket.get(dish).intValue() > 100 || newOrder == null)
					return null;
				newOrder.addToOrder(dish, basket.get(dish).intValue());
			}

			newOrder.setUser(user);
			newOrder.setStatus("Pending");


			MessageWrapper message = new MessageWrapper("createOrder", newOrder);

			try {
				boolean success = this.clientComms.sendMessage(message);
				if (success) {
					Thread.sleep(100);

					Object response = clientComms.receiveMessage();

					while (!((response instanceof Boolean)))
						response =  clientComms.receiveMessage();

					if(((Boolean) response).booleanValue()) {
						user.addUserOrder(newOrder);
						user.clearBasket();
						return newOrder;
					}
				}
			} catch (IOException | ClassNotFoundException | InterruptedException e) {
				System.out.println("Error connecting to server");
			}
			//this.notifyUpdate();

			return null;
	}

	@Override
	public void clearBasket(User user) {
		user.clearBasket();
		this.notifyUpdate();
	}


	@Override
	public List<Order> getOrders(User user) {

			MessageWrapper message = new MessageWrapper("getOrders", user);

			try {
				boolean success = clientComms.sendMessage(message);
				if (success) {
					Thread.sleep(100);

					Object response = clientComms.receiveMessage();

					while (!((response instanceof List) || response == null))
						response = clientComms.receiveMessage();


					if (response != null) {
						user.setUserOrders((List<Order>)response);
						return (List<Order>) response;
					}
				}
			} catch (IOException | ClassNotFoundException | InterruptedException e) {
				System.out.println("Error connecting to server");
			}


		return user.getUserOrders();
	}

	@Override
	public boolean isOrderComplete(Order order) {

		if(order.getStatus().equals("Delivered")) return true;

		return false;
	}

	@Override
	public String getOrderStatus(Order order) {
		return order.getStatus();
	}

	@Override
	public Number getOrderCost(Order order) {
		return order.getOrderCost();
	}

	@Override
	public void cancelOrder(Order order) {
		try {

			if(order.getStatus().equals("Delivered"))
				return;

			order.setStatus("Cancelled");
			MessageWrapper message = new MessageWrapper("cancelOrder", order);
			clientComms.sendMessage(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addUpdateListener(UpdateListener listener) {
		this.listeners.add(listener);

	}

	@Override
	public void notifyUpdate() {
		this.listeners.forEach(listener -> listener.updated(new UpdateEvent()));

	}



}
