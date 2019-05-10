package comp1206.sushi.server;

import comp1206.sushi.common.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Configuration class
 * Parses a configuration file
 * Sets the relevant models and states
 */

public class Configuration {


    /**
     * parseConfiguration method
     * @param fileName the configuration file name
     * @param serverInstance the server instance
     * Used to parse the file passed to the constructor
     */

    public static void parseConfiguration(String fileName, Server serverInstance) throws IOException {

        BufferedReader bufferedRead; // use a buffered reader to read through the file
        File file; // the configuration file
        String line; // each line of the configuration file
        String[] splitLine; // hold the different parts of each line


            file = new File(fileName);
            bufferedRead = new BufferedReader(new FileReader(file));
            line = bufferedRead.readLine();

            // Loop until end of the file is reached
            while(line != null) {

                if (line.equals("")) {
                    line = bufferedRead.readLine();
                    continue; // Skip to the next line if current on is empty
                }

                splitLine = line.split(":"); // Split each line into its subparts

                /*
                 * Switch statement that sets up models as specified
                 */
                try {
                    switch (splitLine[0]) {
                        case "RESTAURANT":
                            // Create new restaurant
                            String restName = splitLine[1];
                            Postcode restPostcode = new Postcode(splitLine[2]);
                            serverInstance.restaurant = new Restaurant(restName, restPostcode);
                            break;
                        case "POSTCODE":
                            // Create new postcode
                            String newPostcode = splitLine[1];
                            serverInstance.addPostcode(newPostcode);
                            break;
                        case "SUPPLIER":
                            // Create new supplier
                            String supplierName = splitLine[1];
                            Postcode supplierPcode = new Postcode(splitLine[2]);
                            serverInstance.addSupplier(supplierName, supplierPcode);
                            break;
                        case "INGREDIENT":
                            // Create new ingredient

                            String ingrName = splitLine[1];
                            String ingrUnit = splitLine[2];

                            int threshold = Integer.parseInt(splitLine[4]);
                            int amount = Integer.parseInt(splitLine[5]);
                            int weight = Integer.parseInt(splitLine[6]);

                            Supplier supplier = null;

                            // Find the appropriate supplier

                            for (Supplier s : serverInstance.getSuppliers()) {
                                if (s.getName().equals(splitLine[3])) {
                                    supplier = s;
                                    break;
                                }
                            }

                            //
                            serverInstance.addIngredient(ingrName,
                                    ingrUnit,
                                    supplier,
                                    threshold, amount, weight);

                            break;
                        case "DISH":
                            // Create new dish

                            String dishName = splitLine[1];
                            String description = splitLine[2];
                            int dishPrice = Integer.parseInt(splitLine[3]);
                            int dishThreshold = Integer.parseInt(splitLine[4]);
                            int dishAmount = Integer.parseInt(splitLine[5]);

                            Dish dish = serverInstance.addDish(dishName, description,
                                    dishPrice, dishThreshold, dishAmount);

                            String[] dishIngredients = splitLine[6].split(",");

                            for(int i = 0; i < dishIngredients.length; i++){
                                String[] temp = dishIngredients[i].split(" * ");

                                int quantity = Integer.parseInt(temp[0]);
                                String ingredient = temp[2];
                                Ingredient dishIngr = null;

                                for(Ingredient ingr : serverInstance.getIngredients()) {
                                    if(ingr.getName().equals(ingredient)) {
                                        dishIngr = ingr;
                                    }
                                }

                                serverInstance.addIngredientToDish(dish, dishIngr, quantity);
                            }
                            break;
                        case "USER":
                            // Create new user

                            String username = splitLine[1];
                            String password = splitLine[2];
                            String location = splitLine[3];

                            Postcode userPostcode = new Postcode(splitLine[4]);

                            serverInstance.users.add(new User(username, password, location, userPostcode));
                            break;
                        case "ORDER":
                            // Create new order
                            Order newOrder = new Order();
                            String user = splitLine[1];
                            User orderUser = null;

                            for(User u : serverInstance.getUsers()) {
                                if(u.getName().equals(user)) orderUser = u;
                            }

                            String[] orderContents = splitLine[2].split(",");

                            for(int i = 0; i < orderContents.length; i++){
                                String[] temp = orderContents[i].split(" \\* ");

                                int quantity = Integer.parseInt(temp[0]);
                                String dishNam = temp[1];
                                Dish dish1 = null;

                                for(Dish dsh : serverInstance.getDishes()) {
                                    if(dsh.getName().equals(dishNam)) {
                                        dish1 = dsh;
                                    }
                                }

                                newOrder.addToOrder(dish1, quantity);
                            }

                            newOrder.setUser(orderUser);

                            serverInstance.addOrder(newOrder);
                            break;
                        case "STAFF":
                            // Create new staff

                            String staffName = splitLine[1];

                            serverInstance.addStaff(staffName);
                            break;
                        case "DRONE":
                            // Create new drone

                            int droneSpeed = Integer.parseInt(splitLine[1]);

                            serverInstance.addDrone(droneSpeed);
                            break;
                        case "STOCK":
                            // Create new stock
                            String placeholder = splitLine[1];
                            int quantity = Integer.parseInt(splitLine[2]);

                            for(Dish d : serverInstance.getDishes()) {
                                if(d.getName().equals(placeholder)) {
                                    serverInstance.setStock(d, quantity);
                                }
                            }

                            for(Ingredient in : serverInstance.getIngredients()) {
                                if(in.getName().equals(placeholder)) {
                                    serverInstance.setStock(in, quantity);
                                }
                            }
                            break;
                        default:
                            // Unrecognized command
                            // Skip the line
                            break;
                    }

                    line = bufferedRead.readLine(); // Read the next line

                } catch (NumberFormatException | NullPointerException e) {

                    // Skip to the next line in case of a formating error in the config file
                    // That has caused an exception

                    line = bufferedRead.readLine();
                }
            }


    }

    /**
     * Clean the current configuration of the server
     * @param serverInstance the server instance
     * @throws ServerInterface.UnableToDeleteException
     */

    public static void cleanConfiguration(Server serverInstance) throws ServerInterface.UnableToDeleteException  {

       for(Drone d : serverInstance.getDrones()) {
            if(!d.getStatus().equals("Idle"))
                throw new ServerInterface.UnableToDeleteException("Can't load a new config if there is a working drone");
        }

        for(Staff s : serverInstance.getStaff()) {
            if(!s.getStatus().equals("Idle"))
                throw new ServerInterface.UnableToDeleteException("Can't load a new config if there is a working staff member");
        }


        serverInstance.restaurant = null;
        for(Dish d : serverInstance.getDishes()) serverInstance.setStock(d, -1);
        for(Ingredient i : serverInstance.getIngredients()) serverInstance.setStock(i, -1);
        //serverInstance.clearStockLevels();

        // Remove all dishes
        int dishesSize = serverInstance.getDishes().size();
        for(int i = 0; i < dishesSize; i++) {
            Dish d = serverInstance.getDishes().get(0);
            serverInstance.removeDish(d);
        }

        // Remove all drones
        int dronesSize = serverInstance.getDrones().size();
        for(int i = 0; i < dronesSize; i++) {
            Drone d = serverInstance.getDrones().get(0);
            serverInstance.forceRemoveDrone(d);
        }

        // Remove all ingredients
        int ingredientsSize = serverInstance.getIngredients().size();
        for(int i = 0; i < ingredientsSize; i++) {
            Ingredient in = serverInstance.getIngredients().get(0);
            serverInstance.forceRemoveIngredient(in);
        }

        // Remove all orders
        int ordersSize = serverInstance.getOrders().size();
        for(int i = 0; i < ordersSize; i++) {
            Order o = serverInstance.getOrders().get(0);
            serverInstance.removeOrder(o);
        }

        // Remove all suppliers
        int suppliersSize = serverInstance.getSuppliers().size();
        for(int i = 0; i < suppliersSize; i++) {
            Supplier s = serverInstance.getSuppliers().get(0);
            serverInstance.forceRemoveSupplier(s);
        }

        // Remove all postcodes
        int postcodesSize = serverInstance.getPostcodes().size();
        for(int i = 0; i < postcodesSize; i++) {
            Postcode p = serverInstance.getPostcodes().get(0);
            serverInstance.forceRemovePostcode(p);
        }

        // Remove all staff members
        int staffSize = serverInstance.getStaff().size();
        for(int i = 0; i < staffSize; i++) {
            Staff s = serverInstance.getStaff().get(0);
            serverInstance.forceRemoveStaff(s);
        }

        // Remove all users
        int usersSize = serverInstance.getUsers().size();
        for(int i = 0; i< usersSize; i++) {
            User u = serverInstance.getUsers().get(0);
            serverInstance.forceRemoveUser(u);
        }
    }
}

