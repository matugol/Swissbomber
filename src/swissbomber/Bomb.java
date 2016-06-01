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
	
	public BufferedImage getAnimation() {
		if (Math.round(100f * timer / TIMER_START) <= 0) return animations[99];
		return animations[100 - Math.round(100f * timer / TIMER_START)];
	}
	
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

	public int getX() {
		return visualX;
	}
	
	public int getY() {
		return visualY;
	}
	
	public float getRealX() {
		return realX;
	}
	
	public float getRealY() {
		return realY;
	}
	
	public Character getOwner() {
		return owner;
	}
	
	public Color getColor() {
		if (!exploded)
			return new Color((int) Math.round((1 - timer / (double)TIMER_START) * 100), 0, 0);
		else
			return new Color((int) Math.round((1 - timer / -1000000000d) * 200), 0, 0, (int) Math.round((1 - timer / -1000000000d) * 255));
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
	
	public boolean kick(Game game, int direction) {
		if (game.getMap()[visualX + DIRECTIONS[direction][0]][visualY + DIRECTIONS[direction][1]] != null) return false;
		
		sliding = true;
		slideDirection = direction;
		
		mapLoop:
		for (int x = 0; x < game.getMap().length; x++) {
			for (int y = 0; y < game.getMap()[x].length; y++) {
				if (game.getMap()[x][y] == this) {
					game.getMap()[x][y] = null;
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
	
	public boolean step(Game game, long deltaTime) {
		if (!remote || remoteActivated)
			timer -= deltaTime;
		if (sliding) { // TODO: Detect and stop when hits player, detect and transform into deadly bomb when hits other moving bomb
			realX += DIRECTIONS[slideDirection][0] * 7.5f / 1000000000d * deltaTime;
			realY += DIRECTIONS[slideDirection][1] * 7.5f / 1000000000d * deltaTime;
			
			for (Bomb bomb : game.getBombs().toArray(new Bomb[game.getBombs().size()])) {
				if (bomb.isSliding() && bomb != this) {
					if (Math.abs(realX - bomb.getRealX()) < 1 && Math.abs(realY - bomb.getRealY()) < 1) {
						int newBombX = (int) ((realX + bomb.getRealX()) / 2);
						int newBombY = (int) ((realY + bomb.getRealY()) / 2);
						
						game.getMap()[newBombX][newBombY] = new Bomb(newBombX, newBombY, 1, null, 1, false, false, true, false);
						
						if (bomb.isRemote())
							bomb.getOwner().detonateRemoteBomb(bomb);
						game.getBombs().set(game.getBombs().indexOf(bomb), (Bomb) game.getMap()[newBombX][newBombY]);
						
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
			
			if (game.getMap()[nextX][nextY] != null && stop) { // Should moving bombs go through powerups? (and should they consume them or not if they go through?)
				realX = visualX + 0.5f;
				realY = visualY + 0.5f;
				sliding = false;
				game.getMap()[visualX][visualY] = this;
			}
		}
		if (timer <= -1000000000) {
			return true;
		} else if (timer <= 0 && !exploded) { // TODO: Make dangerous explosion
			Tile[][] map = game.getMap();
			
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
			
			destroy(game, visualX, visualY);
			int[][] explodeDirections = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };
			for (int d = 0; d < explodeDirections.length; d++) {
				int destroyX = visualX, destroyY = visualY;
				for (int i = 0; i < (powerful ? Integer.MAX_VALUE : power); i++) {
					destroyX += explodeDirections[d][0];
					destroyY += explodeDirections[d][1];

					int next = destroy(game, destroyX, destroyY);					
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
			
			if (owner != null) owner.addBomb();
			exploded = true;
			sliding = false;
		}
		return false;
	}
	
	private int destroy(Game game, int x, int y) {
		for (Character character : game.getCharacters()) {
			if (character.collidesWithTile(x, y)) {
				System.out.println((owner != null ? owner.getColor().getRGB() : "Nobody") + " killed " + character.getColor().getRGB());
				System.out.println("Explosion Tile (" + x + ", " + y + ")");
				System.out.println("Victim (" + character.getX() + ", " + character.getY() + ")");
				character.kill();
			}
		}
		
		Tile tile = game.getMap()[x][y];
		if (tile != null) {
			if (tile instanceof Bomb) {
				((Bomb) tile).explode(game);
				game.getMap()[x][y] = null;
			} else if (tile instanceof Powerup) { 
				game.getMap()[x][y] = null;
			} else {
				if (power >= tile.getArmor() && tile.getArmor() > 0) { // TODO: Better armor mechanics
			    	if (Math.random() >= 0.7) {
			    		game.getMap()[x][y] = Tile.SURGE;
			    	} else {
			    		game.getMap()[x][y] = Tile.ASH;
			    	}
					return 0; // Tile hit and destroyed
				}
				if (tile.getArmor() == 0 && piercing) return 0; // Do not stop at (but also do not destroy) temporary indestructibles if the bomb is piercing
				return 1; // Tile hit and not destroyed
			}
		}
		
		return -1; // No tiles hit
	}
	
	public void explode(Game game) {
		if (remote && !remoteActivated)
			owner.detonateRemoteBomb(this);	
		step(game, timer);
	}
	
	public void detonate() {
		remoteActivated = true;
	}
}
