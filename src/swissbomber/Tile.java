package swissbomber;

import java.awt.Color;

public class Tile {
	
	public static final Tile ASH = new Tile(0, null); // Remains of a destroyed tile, gets destroyed before being painted (used to prevent piercing when a bomb triggers another bomb)
	public static final Tile SURGE = new Tile(0, Color.CYAN); // Remains of a destroyed tile, gets turned into a random power-up before being painted (same use as ASH)
	
	private int armor;
	private Color color;
	
	Tile(int armor, Color color) {
		this.armor = armor;
		this.color = color;
	}
	
	public int getArmor() {
		return armor;
	}
	
	public Color getColor() {
		return color;
	}
}
