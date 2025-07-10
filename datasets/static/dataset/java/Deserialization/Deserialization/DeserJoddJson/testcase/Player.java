<filename>Player.java<fim_prefix>

package com.theironyard.textadventure;

import jodd.json.JsonParser;
import jodd.json.JsonSerializer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

/**
 * Created by jeff on 11/30/16.
 */
public class Player extends Character{
    private String weapon;
    private String location;
    private Scanner input = new Scanner(System.in);
    private ArrayList<String> weapons = new ArrayList<>();
    private HashMap<String, String> locations = new HashMap<>();

    public Player(){
        weapons.add("sword");
        weapons.add("mace");
        weapons.add("axe");

        locations.put("forest", "Now entering the spooky old forest...");
        locations.put("cave", "Now enter the cave of the dark side of the force");
    }

    public String getWeapon() {
        return weapon;
    }

    public void setWeapon(String weapon) {
        this.weapon = weapon;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void chooseName(){
        System.out.println("What is your name foolish traveler?");
        setName(this.input.nextLine());
        System.out.println("Welcome, " + getName());
    }

    public void chooseWeapon() {
        while(this.weapon == null){
            //Ask for what weapon
            System.out.println("Choose your weapon: Mace or Sword");
            String weapon = input.nextLine();

            if(this.weapons.contains(weapon.toLowerCase())) {
                System.out.format("A %s is a fine choice.\n", weapon);
                this.weapon = weapon;
            }

        }
    }

    public void chooseLocation() {
        while(this.location == null) {
            System.out.println("Choose your location [forest/cave]");
            String location = input.nextLine().toLowerCase();

            if(locations.containsKey(location)){
                System.out.println(locations.get(location));
                this.location = location;
            }
        }
    }

    public void savePlayer() throws IOException {
        JsonSerializer serializer = new JsonSerializer();
        String json = serializer.serialize(this);

        File f = new File("game.json");
        FileWriter fw = new FileWriter(f);
        fw.write(json);
        fw.close();
    }

    public static Player loadPlayer() throws FileNotFoundException {
        File f = new File("game.json");
        Scanner scanner = new Scanner(f);
        scanner.useDelimiter("\\Z");
        String contents = scanner.next();

        JsonParser parser = new JsonParser();
        <fim_suffix>
    }
}
<fim_middle>