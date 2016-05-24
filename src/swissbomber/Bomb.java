package swissbomber;

import java.awt.Color;

public class Bomb extends Tile {

	private int x, y;
	private Character owner;
	private long timer;
	public final long TIMER_START;
	private boolean hasExploded = false;
	
	private int power;
	private boolean piercing, remote, remoteActivated = false;
	
	private int[] explosionSize = new int[4]; // Extends up, down, left, right
	
	Bomb(int x, int y, int armor, Color color, Character owner, int power, boolean piercing, boolean remote) {
		super(armor, color);
		
		this.x = x;
		this.y = y;
		this.owner = owner;
		this.power = power;
		this.piercing = piercing;
		this.remote = remote;
		
		if (!remote)
			TIMER_START = 3000000000l;
		else
			TIMER_START = 750000000l;
		timer = TIMER_START;
	}

	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
	
	public Color getColor() {
		if (!hasExploded)
			return new Color((int) Math.round((1 - timer / (double)TIMER_START) * 100), 0, 0);
		else
			return new Color((int) Math.round((1 - timer / -1000000000d) * 200), 0, 0);
	}
	
	public boolean hasExploded() {
		return hasExploded;
	}
	
	public int[] getExplosionSize() {
		return explosionSize;
	}
	
	public boolean step(Game game, long deltaTime) {
		if (!remote || remoteActivated)
			timer -= deltaTime;
		if (timer <= -1000000000) {
			return true;
		} else if (timer <= 0 && !hasExploded) {
			Tile[][] map = game.getMap();
			
			for (int x = 0; x < map.length; x++) {
				for (int y = 0; y < map[x].length; y++) {
					if (map[x][y] == this) {
						map[x][y] = null;
					}
				}
			}
			
			explosionSize[0] = y;
			explosionSize[1] = y;
			explosionSize[2] = x;
			explosionSize[3] = x;
			
			destroy(game, x, y);
			int[][] explodeDirections = { {0, 1}, {1, 0}, {0, -1}, {-1, 0} };
			for (int d = 0; d < explodeDirections.length; d++) {
				int destroyX = x, destroyY = y;
				for (int i = 0; i < power; i++) {
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
			
			owner.addBomb();
			hasExploded = true;
		}
		return false;
	}
	
	public int destroy(Game game, int x, int y) {
		for (Character character : game.getCharacters()) {
			if (character.collidesWithTile(x, y)) {
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
			    	if (Math.random() >= 0.75) {
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
		step(game, timer);
	}
	
	public void detonate() {
		remoteActivated = true;
	}
}
