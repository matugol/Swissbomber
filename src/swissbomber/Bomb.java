package swissbomber;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class Bomb extends Tile {

	private static BufferedImage[] animations = new BufferedImage[100];
	
	private int visualX, visualY;
	private float realX, realY;
	private Character owner;
	private long timer;
	public final long TIMER_START;
	private boolean exploded = false;
	
	private static final int[][] DIRECTIONS = { {1, 0}, {0, 1}, {-1, 0}, {0, -1} };
	private boolean sliding = false;
	private int slideDirection = 0;
	
	private int power;
	private boolean piercing, remote, remoteActivated = false, dangerous, powerful;
	
	private int[] explosionSize = new int[4]; // Extends up, down, left, right
	
	/**
	 * Loads the animations for all current and future instances of bombs.
	 * Required to run beforehand to run {@link #getAnimation() getAnimation()} on a bomb.
	 */
	static void loadAnimations() {
		for (int i = 1; i <= 100; i++) {
			try {
				animations[i - 1] = ImageIO.read(new File("bomb/" + String.format("%04d", i) + ".png"));
			} catch (IOException | NullPointerException e) {
				System.out.println("Error: Failed loading bomb animation image " + i);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Returns a BufferedImage of the current frame of the animation.
	 * {@link #Bomb.loadAnimations() Bomb.loadAnimations()} must have been called at some point beforehand.
	 * 
	 * @return	the current frame of the animation
	 */
	public BufferedImage getAnimation() {
		if (Math.round(100f * timer / TIMER_START) <= 0) return animations[99];
		return animations[100 - Math.round(100f * timer / TIMER_START)];
	}
	
	/**
	 * Creates a Bomb object at a specified location with specified attributes.
	 * 
	 * @param x			the x coordinate of the grid where the bomb is to be placed
	 * @param y			the y coordinate of the grid where the bomb is to be placed
	 * @param armor		the armor value of the bomb
	 * @param owner		the character who placed the bomb
	 * @param power		how far the bomb explosion range extends
	 * @param piercing	if the bomb pierces through blocks
	 * @param remote	if the bomb can be exploded remotely
	 * @param dangerous	if the bomb is an area of effect explosion
	 * @param powerful	if the bomb is maximum range
	 * @see				Powerup
	 */
	Bomb(int x, int y, int armor, Character owner, int power, boolean piercing, boolean remote, boolean dangerous, boolean powerful) {
		super(armor, null);
		
		this.visualX = x;
		this.visualY = y;
		this.realX = x + 0.5f;
		this.realY = y + 0.5f;
		this.owner = owner;
		this.power = power;
		this.piercing = piercing;
		this.remote = remote;
		this.dangerous = dangerous;
		this.powerful = powerful;
		
		if (!remote)
			TIMER_START = 3000000000l;
		else
			TIMER_START = 750000000l;
		timer = TIMER_START;
	}

	/**
	 * Returns the rounded pixel value of the x coordinate
	 * 
	 * @return rounded pixel value of the x coordinate
	 */
	public int getX() {
		return visualX;
	}
	
	/**
	 * Returns the rounded pixel value of the y coordinate
	 * 
	 * @return rounded pixel value of the y coordinate
	 */
	public int getY() {
		return visualY;
	}
	
	/**
	 * Returns the floating hidden value of the x coordinate
	 * 
	 * @return floating hidden value of the x coordinate
	 */
	public float getRealX() {
		return realX;
	}
	
	/**
	 * Returns the floating hidden value of the y coordinate
	 * 
	 * @return floating hidden value of the y coordinate
	 */
	public float getRealY() {
		return realY;
	}
	
	public Character getOwner() {
		return owner;
	}
	
	public boolean hasExploded() {
		return exploded;
	}
	
	public int[] getExplosionSize() {
		return explosionSize;
	}
	
	public boolean isRemote() {
		return remote;
	}
	
	/**
	 * Returns the color of the bomb or the blast depending on how far along the fuse it has run through or how long ago it has exploded.
	 * 
	 * @return	the color of the bomb or blast
	 */
	public Color getColor() {
		if (!exploded)
			return new Color((int) Math.round((1 - timer / (double)TIMER_START) * 100), 0, 0);
		else
			return new Color((int) Math.round((1 - timer / -1000000000d) * 200), 0, 0, (int) Math.round((1 - timer / -1000000000d) * 255));
	}
	
	/**
	 * Attempts to kick the bomb in a specified direction.
	 * 
	 * @param direction	the direction to kick the bomb (right, down, left, or up)
	 * @return			whether or not the kick was successful
	 */
	public boolean kick(int direction) {
		if (Game.game.getMap()[visualX + DIRECTIONS[direction][0]][visualY + DIRECTIONS[direction][1]] != null) return false;
		
		sliding = true;
		slideDirection = direction;
		
		mapLoop:
		for (int x = 0; x < Game.game.getMap().length; x++) {
			for (int y = 0; y < Game.game.getMap()[x].length; y++) {
				if (Game.game.getMap()[x][y] == this) {
					Game.game.getMap()[x][y] = null;
					break mapLoop;
				}
			}
		}
		
		return true;
	}
	
	public boolean isSliding() {
		return sliding;
	}
	
	public boolean isDangerous() {
		return dangerous;
	}
	
	/**
	 * Calculates the next step for the bomb, including the fuse, sliding, and explosion.
	 * 
	 * @param deltaTime	time passed since last step in nanoseconds
	 * @return			whether the bomb is to be deleted from memory
	 */
	public boolean step(long deltaTime) {
		if (!remote || remoteActivated)
			timer -= deltaTime;
		if (sliding) { // TODO: Detect and stop when hits player
			realX += DIRECTIONS[slideDirection][0] * 7.5f / 1000000000d * deltaTime;
			realY += DIRECTIONS[slideDirection][1] * 7.5f / 1000000000d * deltaTime;
			
			for (Bomb bomb : Game.game.getBombs().toArray(new Bomb[Game.game.getBombs().size()])) {
				if (bomb.isSliding() && bomb != this) {
					if (Math.abs(realX - bomb.getRealX()) < 1 && Math.abs(realY - bomb.getRealY()) < 1) {
						int newBombX = (int) ((realX + bomb.getRealX()) / 2);
						int newBombY = (int) ((realY + bomb.getRealY()) / 2);
						
						Game.game.getMap()[newBombX][newBombY] = new Bomb(newBombX, newBombY, 1, null, 1, false, false, true, false);
						
						if (bomb.isRemote())
							bomb.getOwner().detonateRemoteBomb(bomb);
						Game.game.getBombs().set(Game.game.getBombs().indexOf(bomb), (Bomb) Game.game.getMap()[newBombX][newBombY]);
						
						if (owner != null) owner.addBomb();
						if (bomb.getOwner() != null) bomb.getOwner().addBomb();
						
						return true;
					}
				}
			}
			
			visualX = (int) realX;
			visualY = (int) realY;
			int nextX = visualX + DIRECTIONS[slideDirection][0];
			int nextY = visualY + DIRECTIONS[slideDirection][1];
			boolean stop = false;
			if (DIRECTIONS[slideDirection][0] != 0)
				stop = (visualX + 0.5f) * DIRECTIONS[slideDirection][0] <= realX * DIRECTIONS[slideDirection][0];
			else  
				stop = (visualY + 0.5f) * DIRECTIONS[slideDirection][1] <= realY * DIRECTIONS[slideDirection][1];
			
			if (Game.game.getMap()[nextX][nextY] != null && stop) { // Should moving bombs go through powerups? (and should they consume them or not if they go through?)
				realX = visualX + 0.5f;
				realY = visualY + 0.5f;
				sliding = false;
				Game.game.getMap()[visualX][visualY] = this;
			}
		}
		if (timer <= -1000000000) {
			return true;
		} else if (timer <= 0 && !exploded) {
			calculateBlast();
		}
		return false;
	}
	
	/**
	 * Calculates the blast of the bomb and destroys what is appropriate 
	 */
	private void calculateBlast() { // TODO: Make dangerous explosion
		Tile[][] map = Game.game.getMap();
		
		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[x].length; y++) {
				if (map[x][y] == this) {
					map[x][y] = null;
				}
			}
		}
		
		explosionSize[0] = visualY;
		explosionSize[1] = visualY;
		explosionSize[2] = visualX;
		explosionSize[3] = visualX;
		
		destroy(visualX, visualY);
		int[][] explodeDirections = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };
		for (int d = 0; d < explodeDirections.length; d++) {
			int destroyX = visualX, destroyY = visualY;
			for (int i = 0; i < (powerful ? Integer.MAX_VALUE : power); i++) {
				destroyX += explodeDirections[d][0];
				destroyY += explodeDirections[d][1];

				int next = destroy(destroyX, destroyY);					
				if (next != 1) { 
					if (destroyY > explosionSize[0]) explosionSize[0] = destroyY;
					if (destroyY < explosionSize[1]) explosionSize[1] = destroyY;
					if (destroyX < explosionSize[2]) explosionSize[2] = destroyX;
					if (destroyX > explosionSize[3]) explosionSize[3] = destroyX;
					if (next == 0 && !piercing) break;
				} else {
					break;
				}
			}
		}
		
		if (timer > 0) timer = 0;
		if (owner != null) owner.addBomb();
		exploded = true;
		sliding = false;
	}
	
	/**
	 * Destroys everything in a given tile, including characters, powerups, and bombs.
	 * 
	 * @param x	x coordinate of tile
	 * @param y	y coordinate of tile
	 * @return	<code>-1&nbsp</code>— no tiles hit <br>
	 * 			<code>0&nbsp&nbsp</code>— tile hit and destroyed<br>
	 * 			<code>1&nbsp&nbsp</code>— tile hit and not destroyed
	 * @see		Tile
	 */
	private int destroy(int x, int y) {
		for (Character character : Game.game.getCharacters()) {
			if (character.collidesWithTile(x, y)) {
				System.out.println((owner != null ? owner.getColor().getRGB() : "Nobody") + " killed " + character.getColor().getRGB());
				System.out.println("Explosion Tile (" + x + ", " + y + ")");
				System.out.println("Victim (" + character.getX() + ", " + character.getY() + ")");
				character.kill();
			}
		}
		
		Tile tile = Game.game.getMap()[x][y];
		if (tile != null) {
			if (tile instanceof Bomb) {
				((Bomb) tile).explode();
				Game.game.getMap()[x][y] = null;
			} else if (tile instanceof Powerup) { 
				Game.game.getMap()[x][y] = null;
			} else {
				if (power >= tile.getArmor() && tile.getArmor() > 0) { // TODO: Better armor mechanics
			    	if (Math.random() >= 0.7) {
			    		Game.game.getMap()[x][y] = Tile.SURGE;
			    	} else {
			    		Game.game.getMap()[x][y] = Tile.ASH;
			    	}
					return 0; // Tile hit and destroyed
				}
				if (tile.getArmor() == 0 && piercing) return 0; // Do not stop at (but also do not destroy) temporary indestructibles if the bomb is piercing
				return 1; // Tile hit and not destroyed
			}
		}
		
		return -1; // No tiles hit
	}
	
	/**
	 * Instantly explodes bomb and calculates the blast.
	 */
	public void explode() {
		if (remote && !remoteActivated)
			owner.detonateRemoteBomb(this);	
		calculateBlast();
	}
	
	/**
	 * Detonates bomb if it is a remote bomb.
	 */
	public void detonate() {
		remoteActivated = true;
	}
}
