package swissbomber;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Character {

	private boolean alive = true;
	private float positionX, positionY;
	private Color color;
	
	private int bombPower = 1;
	private float speed = 5; // Tiles per second
	private int maxBombs = 1, currentBombs = maxBombs;
	private boolean piercingBombs = false;
	private boolean remoteBombs = false;
	private List<Bomb> activeRemoteBombs = new ArrayList<Bomb>();
	private boolean kicks = false;
	private boolean nextDangerous = false, nextPowerful = false;
	
	private float radius = 0.4f;
	
	/** List of tiles which this character is allowed to phase through until they move away away. */
	private List<Tile> tempUncollidableTiles = new ArrayList<Tile>();
	
	Character(float positionX, float positionY, Color color) {
		this.positionX = positionX;
		this.positionY = positionY;
		this.color = color;
	}
	
	public boolean isAlive() {
		return alive;
	}
	
	public void kill() {
		alive = false;
	}
	
	public float getX() {
		return positionX;
	}
	
	public float getY() {
		return positionY;
	}
	
	public Color getColor() {
		return color;
	}
	
	public int getBombPower() {
		return bombPower;
	}
	
	public int getCurrentBombs() {
		return currentBombs;
	}
	
	public int getMaxBombs() {
		return maxBombs;
	}
	
	public void removeBomb() {
		currentBombs = Math.max(0, currentBombs - 1);
	}
	
	public void addBomb() {
		currentBombs = Math.min(maxBombs, currentBombs + 1);
	}
	
	public float getSpeed() {
		return speed;
	}
	
	public boolean hasPiercingBombs() {
		return piercingBombs;
	}
	
	public boolean hasRemoteBombs() {
		return remoteBombs;
	}
	
	public void addRemoteBomb(Bomb bomb) {
		activeRemoteBombs.add(bomb);
	}
	
	public boolean detonateRemoteBomb() {
		if (activeRemoteBombs.size() <= 0)
			return false;
		activeRemoteBombs.remove(0).detonate();
		return true;
	}
	
	public boolean detonateRemoteBomb(Bomb bomb) {
		if (!activeRemoteBombs.contains(bomb))
			return false;
		activeRemoteBombs.remove(activeRemoteBombs.indexOf(bomb)).detonate();
		return true;
	}
	
	public boolean canKick() {
		return kicks;
	}
	
	public boolean useDangerous() {
		if (nextDangerous) {
			nextDangerous = false;
			return true;
		}
		return false;
	}
	
	public boolean usePowerful() {
		if (nextPowerful) {
			nextPowerful = false;
			return true;
		}
		return false;
	}
	
	public float getRadius() {
		return radius;
	}
	
	/**
	 * Allows character to phase through the specified tile until they move away from it's area.
	 * 
	 * @param tile the tile which the character can phase through
	 */
	public void addTempUncollidableTile(Tile tile) {
		tempUncollidableTiles.add(tile);
	}
	
	/**
	 * Moves this character along a specified angle for a certain amount of time according to their speed.
	 * Also checks for all collision when doing so.
	 * 
	 * @param angle		angle of movement in degrees
	 * @param deltaTime	time passed since last frame in nanoseconds
	 */
	public void move(double angle, long deltaTime) {
		if (!alive) return;
		
		double distance = speed / 1000000000 * deltaTime;
		positionX += Math.cos(Math.toRadians(angle)) * distance;
		positionY -= Math.sin(Math.toRadians(angle)) * distance;
		
		for (Bomb bomb : Game.game.getBombs()) {
			if (bomb.isSliding()) {
				float distanceX = Math.abs(positionX - bomb.getRealX());
				float distanceY = Math.abs(positionY - bomb.getRealY());
				
				if (distanceX > 0.5f + radius) continue;
				if (distanceY > 0.5f + radius) continue;

				if (distanceY <= 0.5f && distanceX >= distanceY) { positionX = properPosition(positionX, bomb.getRealX()); continue;}
				if (distanceX <= 0.5f && distanceX <= distanceY) { positionY = properPosition(positionY, bomb.getRealY()); continue;}
								
				if (Math.pow(distanceX - 0.5f, 2) + Math.pow(distanceY - 0.5f, 2) <= Math.pow(radius, 2)) {
					System.out.println("collide");
					if (distanceY > distanceX)
						positionY = properPosition(positionY, bomb.getRealY());
					else
						positionX = properPosition(positionX, bomb.getRealX());
				}
			}
		}
		
		final int[][] collidableTiles = { {0, 0},  {-1, -1}, {-1, 0}, {-1, 1}, {0, -1}, {0, 1}, {1, -1}, {1, 0}, {1, 1} };
		
		collidableTiles:
		for (int[] collidableTile : collidableTiles) {
			Tile tile = Game.game.getMap()[(int) (positionX + collidableTile[0])][(int) (positionY + collidableTile[1])];
			if (tile != null) {
				for (Tile tempUTile : tempUncollidableTiles) {
					if (tile == tempUTile) {
						if (!collidesWithTile((int) (positionX + collidableTile[0]), (int) (positionY + collidableTile[1]))) {
							tempUncollidableTiles.remove(tile);
						}
						continue collidableTiles;
					}
				}
				
				float distanceX = Math.abs(positionX - ((int) (positionX + collidableTile[0]) + 0.5f));
				float distanceY = Math.abs(positionY - ((int) (positionY + collidableTile[1]) + 0.5f));
				
				if (tile instanceof Powerup) {
					if (collidesWithPowerup((int) (positionX + collidableTile[0]), (int) (positionY + collidableTile[1]), (Powerup) tile)) {
						activatePowerup((Powerup) tile);
						Game.game.getMap()[(int) (positionX + collidableTile[0])][(int) (positionY + collidableTile[1])] = null;
					}
					continue;
				} else if (tile instanceof Bomb && kicks) {
					if (collidesWithTile((int) (positionX + collidableTile[0]), (int) (positionY + collidableTile[1]))) {
						int direction;
						
						if (distanceX > distanceY) {
							direction = (Math.signum(positionX - ((int) (positionX + collidableTile[0]) + 0.5f)) == 1 ? 2 : 0);
						} else {
							direction = (Math.signum(positionY - ((int) (positionY + collidableTile[1]) + 0.5f)) == 1 ? 3 : 1);
						}
						
						if (((Bomb) tile).kick(direction)) continue;
					}
				}
				
				if (distanceX > 0.5f + radius) continue;
				if (distanceY > 0.5f + radius) continue;

				if (distanceY <= 0.5f && distanceX >= distanceY) { positionX = properPosition(positionX, (float) (Math.floor(positionX) + 0.5f + collidableTile[0])); continue;}
				if (distanceX <= 0.5f && distanceX <= distanceY) { positionY = properPosition(positionY, (float) (Math.floor(positionY) + 0.5f + collidableTile[1])); continue;}
				
				if (Math.pow(distanceX - 0.5f, 2) + Math.pow(distanceY - 0.5f, 2) <= Math.pow(radius, 2)) {
					if (distanceY > distanceX)
						positionY = (float) (Math.floor(positionY) + 0.5f + collidableTile[1] * (0.5f - radius));
					else
						positionX = (float) (Math.floor(positionX) + 0.5f + collidableTile[0] * (0.5f - radius));
				}
			}
		}
	}
	
	private float properPosition(float position, float tile) {
		return tile + (position > tile ? 1 : -1) * (radius + 0.5f);
	}
	
	/**
	 * Checks if this character collides with a tile at given coordinates.
	 * 
	 * @param x	x coordinate in the map of the tile
	 * @param y	y coordinate in the map of the tile
	 * @return	whether the character collides with the tile or not
	 * @see		Tile
	 */
	public boolean collidesWithTile(int x, int y) {
		float distanceX = Math.abs(positionX - (x + 0.5f));
		float distanceY = Math.abs(positionY - (y + 0.5f));

		if (distanceX >= 0.499f + radius) return false;
		if (distanceY >= 0.499f + radius) return false;
		
		if (distanceX <= 0.5f) return true;
		if (distanceY <= 0.5f) return true;
		
		return Math.pow(distanceX - 0.5f, 2) + Math.pow(distanceY - 0.5f, 2) <= Math.pow(radius, 2);
	}
	
	/**
	 * Checks if this character collides with a certain powerup at given coordinates
	 * @param x			x coordinate in the map of the powerup
	 * @param y			y coordinate in the map of the powerup
	 * @param powerup	the <code>Powerup</code> to test collision with
	 * @return			whether or not the character collides with the powerup or not
	 * @see				Powerup
	 */
	public boolean collidesWithPowerup(int x, int y, Powerup powerup) {
		return Math.sqrt(Math.pow(positionX - (x + 0.5f), 2) + Math.pow(positionY - (y + 0.5f), 2)) <= radius + powerup.RADIUS;
	}
	
	/**
	 * Activates a given powerup and applies it's effects onto this character.
	 * 
	 * @param powerup	the powerup to activate
	 * @see				Powerup
	 */
	public void activatePowerup(Powerup powerup) {
		switch (powerup.EFFECT) {
		case "power+":
			if (bombPower < 9)
				bombPower++;
			break;
		case "speed+":
			if (speed < 9)
				speed++;
			break;
		case "bombs+":
			if (maxBombs < 9) {
				currentBombs++;
				maxBombs++;
			}
			break;
		case "pierce":
			piercingBombs = true;
			break;
		case "remote":
			remoteBombs = true;
			break;
		case "kick":
			kicks = true;
			break;
		case "nextDangerous":
			nextDangerous = true;
			break;
		case "nextPowerful":
			nextPowerful = true;
			break;
		default:
			System.out.println("Error: Undefined powerup \"" + powerup.EFFECT + "\"");
			break;
		}
	}
}
