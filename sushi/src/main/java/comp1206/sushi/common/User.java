package comp1206.sushi.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class User extends Model {

	/**
	 * @param name the user's username
	 * @param password the user's password
	 * @param basket the dishes currently in the user's basket
	 * @param userOrders list of the user's past orders
	 */

	private String name;
	private String password;
	private String address;
	private Postcode postcode;
	private Map<Dish, Number> basket;
	private List<Order> userOrders;

	public User(String username, String password, String address, Postcode postcode) {
		this.name = username;
		this.password = password;
		this.address = address;
		this.postcode = postcode;
		this.basket = new HashMap<>();
		this.userOrders = new ArrayList<>();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Number getDistance() {
		return postcode.getDistance();
	}

	public Postcode getPostcode() {
		return this.postcode;
	}
	
	public void setPostcode(Postcode postcode) {
		this.postcode = postcode;
	}

	/**
	 * Verify user's login
	 * @param password
	 * @return true if verification is successful
	 */

	public boolean checkLogin(String password) {
		if(this.password.equals(password)) return true;

		return false;
	}

	/**
	 * Get contents of the user's basket
	 */

	public Map<Dish, Number> getBasket() {
		return this.basket;
	}

	/**
	 * Add a dish to the user's basket
	 * @param dish to be added
	 * @param quantity
	 */

	public void addDishToBasket(Dish dish, int quantity) {
		this.basket.put(dish, quantity);
	}

	/**
	 * Clear the user's basket
	 */

	public void clearBasket() {
		this.basket.clear();
	}

	/**
	 * Add an order to the list of the user's orders
	 * @param order to be added
	 */

	public void addUserOrder(Order order) {
		this.userOrders.add(order);
	}

	/**
	 * Remove an user order
	 * @param order to be removed
	 */

	public void removeUserOrder(Order order) {
		int index = this.userOrders.indexOf(order);
		this.userOrders.remove(index);
	}

	/**
	 * Getter for the user orders
	 * @return the user orders list
	 */

	public List<Order> getUserOrders() {
		return this.userOrders;
	}

	/**
	 * Set the user's order list
	 */

	public void setUserOrders(List<Order> orders) {this.userOrders = orders; }


}
