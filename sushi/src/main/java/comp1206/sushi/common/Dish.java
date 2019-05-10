package comp1206.sushi.common;

import java.util.HashMap;
import java.util.Map;

public class Dish extends Model {

	/** Dish Class
	 * @param locked true if the dish is currently being restocked
	 *               used for synchronization between Staff threads
	 *               that might try to restock it
	 */

	private String name;
	private String description;
	private Number price;
	private Map <Ingredient,Number> recipe;
	private Number restockThreshold;
	private Number restockAmount;
	private volatile boolean locked;
	private String status;

	public Dish(String name, String description, Number price, Number restockThreshold, Number restockAmount) {
		this.name = name;
		this.description = description;
		this.price = price;
		this.restockThreshold = restockThreshold;
		this.restockAmount = restockAmount;
		this.recipe = new HashMap<Ingredient,Number>();
		this.locked = false;
		this.status = "Normal";
	}

    /**
     * Called when a staff member starts restocking a particular meal
     * @return true if the dish is ready to be restocked
     */

	public synchronized boolean startRestocking() {
		if(locked) {
			return false;
		} else {
			locked = true;
			return true;
		}
	}

	public synchronized boolean stopRestocking() {
		if(locked) {
			locked = false;
			return true;
		} else {
			return false;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Number getPrice() {
		return price;
	}

	public void setPrice(Number price) {
		this.price = price;
	}

	public Map <Ingredient,Number> getRecipe() {
		return recipe;
	}

	public void setRecipe(Map <Ingredient,Number> recipe) {
		this.recipe = recipe;
	}

	public void setRestockThreshold(Number restockThreshold) {
		this.restockThreshold = restockThreshold;
	}
	
	public void setRestockAmount(Number restockAmount) {
		this.restockAmount = restockAmount;
	}

	public Number getRestockThreshold() {
		return this.restockThreshold;
	}

	public Number getRestockAmount() {
		return this.restockAmount;
	}

	public String getStatus() {return this.status;}

	public void setStatus(String status) {this.status = status;}

}
