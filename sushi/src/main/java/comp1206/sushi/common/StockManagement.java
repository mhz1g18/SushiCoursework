package comp1206.sushi.common;

import comp1206.sushi.server.Server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * StockManagement Class
 * Manages stock amounts for dishes and ingredients
 * Restock Amount - how much can be restocked in one go
 * Restock Threshold - falling below this means the ingredient/dish has to be restocked
 */

public class StockManagement implements Serializable {

    /**
     * @param dishLevels maps dishes to their current stock level
     * @param ingredientLevels maps ingredients to their current stock level
     */

    private transient Server serverInstance;
    private Map<Dish, Number> dishLevels;
    private Map<Ingredient, Number> ingredientLevels;

    public StockManagement(Server serverInstance) {
        this.dishLevels = new ConcurrentHashMap<>();
        this.ingredientLevels = new ConcurrentHashMap<>();
        this.serverInstance = serverInstance;
    }

    /**
     * Get the stock levels of ingredients/dishes
     * @return the relevant mapping of dishes/ingredients to their stock levels
     */

    public Map<Ingredient, Number> getIngredientStockLevels() {
        return this.ingredientLevels;
    }

    public Map<Dish, Number> getDishStockLevels() {
        return this.dishLevels;
    }

    /**
     * Clear the stocks for all dishes and ingredients
     */

    public void clearStocks() {
        this.dishLevels.clear();
        this.ingredientLevels.clear();
    }

    /**
     * Change the stock levels of a dish
     * @param dish - dish to set the stock level
     * @param quantity - the stock level to be set, if quantity == -1, delete the dish
     */

    public synchronized void setStock(Dish dish, Number quantity) {
        if (quantity == Integer.valueOf(-1)) {
            this.dishLevels.remove(dish);
        } else {
            this.dishLevels.put(dish, quantity);
        }

        serverInstance.notifyUpdate();
    }

    /**
     * Change the stock levels of an ingredient
     * @param ingredient - ingredient to set the stock level
     * @param quantity - the stock level to be set, if quantity == -1, delete the ingredient
     */

    public synchronized void setStock(Ingredient ingredient, Number quantity) {
        if(quantity == Integer.valueOf(-1)) {
            this.ingredientLevels.remove(ingredient);
        } else {
            this.ingredientLevels.put(ingredient, quantity);
        }

        serverInstance.notifyUpdate();
    }

    /**
     * Get the stock level for a particular dish by looking it up in the dishLevels map
     * @param dish to be lookedup
     * @return current stock level, 0 if not found in the hashmap
     */

    public synchronized Number getStock(Dish dish) {
        for(Dish d : this.dishLevels.keySet()) {
            if(d.getName().equals(dish.getName())){
                return this.dishLevels.get(d);
            }
        }

        return 0;
    }

    public synchronized Number getStock(Ingredient ingredient) {
        for(Ingredient i : this.ingredientLevels.keySet()) {
            if(i.getName().equals(ingredient.getName())){
                return this.ingredientLevels.get(i);
            }
        }

        return 0;
    }

    /**
     * Get the correct references for the dishes stored in the server by matching the names
     * in the order sent from the client
     * @param temp dishes send from the client
     * @return a new hashmap with the correct local references
     */

    public Map<Dish, Number> getCorrectReferences(Map<Dish, Number> temp) {

        Map<Dish, Number> newMap = new HashMap<>();

        for(Dish d : temp.keySet()) {
            for(Dish localDish : serverInstance.getDishes()) {
                if(d.getName().equals(localDish.getName())){
                    newMap.put(localDish, temp.get(d).intValue());
                }
            }
        }

        return newMap;
    }

    public void setServerInstance(Server server) {
        this.serverInstance = server;
    }


}
