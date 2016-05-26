package swissbomber;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

public class Game extends JPanel {
	
	private static final long serialVersionUID = -7101890057819507949L;
	
	List<Character> characters = new ArrayList<>();
	List<Controller> controllers = new ArrayList<>();
	List<Bomb> bombs = new ArrayList<>();
	private Tile[][] map;
	
	private int currentFPS = 0;
	private int targetFPS = 60;
	private int tileLength = 50;
	
	Game(Tile[][] map, int playerCount) {	
		this.map = map;
		int[][] controls = {
				{KeyEvent.VK_W, KeyEvent.VK_S, KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_SPACE, KeyEvent.VK_SHIFT},
				{KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, KeyEvent.VK_END, KeyEvent.VK_CONTEXT_MENU},
				{KeyEvent.VK_I, KeyEvent.VK_K, KeyEvent.VK_J, KeyEvent.VK_L, KeyEvent.VK_B, KeyEvent.VK_SLASH},
				{}
		};
		Color[] colors = {Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA};
		float[][] positions = { {1.5f, 1.5f}, {13.5f, 11.5f}, {13.5f, 1.5f}, {1.5f, 11.5f} };
		for (int i = 0; i < playerCount; i++) {
			Character newCharacter = new Character(positions[i][0], positions[i][1], colors[i]);
			characters.add(newCharacter);
			InputController newInputController = new InputController(newCharacter, controls[i]);
			controllers.add(newInputController);
			addKeyListener(newInputController);
		}
		
		setPreferredSize(new Dimension(map.length * tileLength + 200, map[0].length * tileLength));
		setFocusable(true);
		requestFocusInWindow();
				
		new Thread(loop(this)).start();
	}
	
	public List<Character> getCharacters() {
		return characters;
	}
	
	public List<Controller> getControllers() {
		return controllers;
	}
	
	public Tile[][] getMap() {
		return map;
	}
	
	public void setCurrentFPS(int fps) {
		currentFPS = fps;
	}
	
	public int getTargetFPS() {
		return targetFPS;
	}
	
	public int getTileLength() {
		return tileLength;
	}
	
	boolean placeBomb(int x, int y, Character owner) {
		if (map[x][y] == null) {
			map[x][y] = new Bomb(x, y, 1, Color.BLACK, owner, owner.getBombPower(), owner.hasPiercingBombs(), owner.hasRemoteBombs());
			bombs.add((Bomb) map[x][y]);
			if (owner.hasRemoteBombs())
				owner.addRemoteBomb((Bomb) map[x][y]);
			for (Character character : characters) {
				if (character.collidesWithTile(x, y)) {
					character.addTempUncollidableTile(map[x][y]);
				}
			}
			return true;
		}
		return false;
	}
	
	public void paintComponent (Graphics g) {
		Graphics2D gg = ((Graphics2D) g);
		gg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		
		gg.setColor(Color.WHITE);
		gg.fillRect(0, 0, map.length * tileLength, map[0].length * tileLength);
		
		for (int x = 0; x < map.length; x++) {
			for (int y = 0; y < map[x].length; y++) {
				if (map[x][y] != null) {
					gg.setColor(map[x][y].getColor());
					if (map[x][y] instanceof Bomb) {
						gg.drawImage(((Bomb) map[x][y]).getAnimation(), x * tileLength, y * tileLength, tileLength, tileLength, null);
						//gg.fillOval(x * tileLength + Math.round(tileLength * 0.05f), y * tileLength + Math.round(tileLength * 0.05f), Math.round(tileLength * 0.9f), Math.round(tileLength * 0.9f));
					} else if (map[x][y] instanceof Powerup) {
						gg.fillOval(x * tileLength + Math.round(tileLength * (0.5f - ((Powerup)map[x][y]).RADIUS)), y * tileLength + Math.round(tileLength * (0.5f - ((Powerup)map[x][y]).RADIUS)), Math.round(((Powerup)map[x][y]).RADIUS * 2 * tileLength), Math.round(((Powerup)map[x][y]).RADIUS * 2 * tileLength));
					} else {
						if (map[x][y].getArmor() == 0) {
							if (map[x][y].getColor() == null) {
								map[x][y] = null;
								continue;
							} else if (map[x][y].getColor() == Color.CYAN) {
					    		int value = (int) (Math.random() * Powerup.getTotalRarity());
					    		for (Powerup powerup : Powerup.POWERUPS) {
					    			value -= powerup.RARITY;
					    			if (value < 0) {
					    				map[x][y] = powerup;
					    				break;
					    			}
					    		}
					    		gg.setColor(map[x][y].getColor());
								gg.fillOval(x * tileLength + Math.round(tileLength * (0.5f - ((Powerup)map[x][y]).RADIUS)), y * tileLength + Math.round(tileLength * (0.5f - ((Powerup)map[x][y]).RADIUS)), Math.round(((Powerup)map[x][y]).RADIUS * 2 * tileLength), Math.round(((Powerup)map[x][y]).RADIUS * 2 * tileLength));
								continue;
							}
						}
						gg.fillRect(x * tileLength, y * tileLength, tileLength, tileLength);
					}
				}
			}
		}
		
		for (Bomb bomb : bombs.toArray(new Bomb[bombs.size()])) {
			if (bomb.hasExploded()) {				
				BufferedImage img = new BufferedImage(this.getWidth(), this.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
				Graphics2D ig = img.createGraphics();
				//ig.setColor(new Color(bomb.getColor().getRGB() & 16777216)); // Remove alpha from color
				ig.setColor(new Color(bomb.getColor().getRed(), bomb.getColor().getGreen(), bomb.getColor().getBlue()));
				ig.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC, bomb.getColor().getAlpha() / 255f));

				ig.fillRect(bomb.getExplosionSize()[2] * tileLength, Math.round((bomb.getY() + 0.05f) * tileLength),
						   (bomb.getExplosionSize()[3] - bomb.getExplosionSize()[2] + 1) * tileLength, Math.round(0.9f * tileLength));
				ig.fillRect(Math.round((bomb.getX() + 0.05f) * tileLength), bomb.getExplosionSize()[1] * tileLength,
						    Math.round(0.9f * tileLength), (bomb.getExplosionSize()[0] - bomb.getExplosionSize()[1] + 1) * tileLength);
				
				ig.dispose();
				gg.drawImage(img, 0, 0, null);
			}
		}
		
		for (Character character : characters) {
			if (!character.isAlive()) continue;
			gg.setColor(character.getColor());
			gg.fillOval(Math.round(character.getX() * tileLength - character.getRadius() * tileLength), Math.round(character.getY() * tileLength - character.getRadius() * tileLength), Math.round(character.getRadius() * 2 * tileLength), Math.round(character.getRadius() * 2 * tileLength));
		}
		
		gg.setColor(Color.WHITE);
		gg.setFont(new Font("idk", gg.getFont().getStyle(), 30));
		gg.drawString(Integer.toString(currentFPS), 10, 30);
		
		gg.setColor(Color.LIGHT_GRAY);
		gg.fillRect(map.length * tileLength, 0, 200, map[0].length * tileLength);
		
		for (int i = 0; i < characters.size(); i++) {
			gg.setColor(characters.get(i).getColor());
			gg.fillRect(map.length * tileLength, Math.round(map[0].length * tileLength / 4f * i), 200, Math.round(map[0].length * tileLength / 4f));
			
			gg.setColor(Color.LIGHT_GRAY);
			gg.fillRect(map.length * tileLength + 20, map[0].length * tileLength / 4 * i + 20, 40, 60);
			gg.setColor(Color.BLACK);
			gg.drawString(Integer.toString(characters.get(i).getBombPower()), map.length * tileLength + 30, map[0].length * tileLength / 4 * i + 60);
	
			gg.setColor(Color.LIGHT_GRAY);
			gg.fillRect(map.length * tileLength + 20, map[0].length * tileLength / 4 * i + 90, 40, 60);
			gg.setColor(Color.BLACK);
			gg.drawString(Integer.toString((int) characters.get(i).getSpeed()), map.length * tileLength + 30, map[0].length * tileLength / 4 * i + 130);

			gg.setColor(Color.LIGHT_GRAY);
			gg.fillRect(map.length * tileLength + 120, map[0].length * tileLength / 4 * i + 20, 60, 60);
			gg.setColor(Color.BLACK);
			gg.drawString(characters.get(i).getCurrentBombs() + "/" + characters.get(i).getMaxBombs(), map.length * tileLength + 130, map[0].length * tileLength / 4 * i + 60);
	
			gg.setColor(Color.LIGHT_GRAY);
			gg.fillRect(map.length * tileLength + 120, map[0].length * tileLength / 4 * i + 90, 60, 60);
			if (characters.get(i).hasPiercingBombs()) {
				gg.setColor(Powerup.POWERUPS[3].getColor());
				gg.fillOval(map.length * tileLength + 125, map[0].length * tileLength / 4 * i + 95, 20, 20);
			}
			if (characters.get(i).hasRemoteBombs()) {
				gg.setColor(Powerup.POWERUPS[4].getColor());
				gg.fillOval(map.length * tileLength + 155, map[0].length * tileLength / 4 * i + 95, 20, 20);
			}
			if (characters.get(i).canKick()) {
				gg.setColor(Powerup.POWERUPS[5].getColor());
				gg.fillOval(map.length * tileLength + 125, map[0].length * tileLength / 4 * i + 125, 20, 20);
			}
		}
	}
	
	private Runnable loop(Game game) {
		return new Runnable() {

			@Override
			public void run() {
				long deltaTime, currentTime, previousTime = System.nanoTime(), deltaSecond, previousSecond = System.nanoTime();
				int fpsCount = 0;
				
				while (true) {
					currentTime = System.nanoTime();
					deltaTime = currentTime - previousTime;
					
					if (deltaTime >= 1000000000 / game.targetFPS) {
						previousTime = currentTime;
						fpsCount++;
						game.update(deltaTime);
						
						currentTime = System.nanoTime();
						deltaSecond = currentTime - previousSecond;
						
						if (deltaSecond >= 1000000000) {
							game.setCurrentFPS((int) (fpsCount / (deltaSecond / 1000000000)));
							previousSecond = currentTime;
							fpsCount = 0;
						}
					}
				}
			}
		};
	}
			
	private void update(long deltaTime) {
		for (Controller controller : controllers) {
			controller.step(this, deltaTime);
		}
		
		for (int i = 0; i < bombs.size(); i++) {
			if (bombs.get(i).step(this, deltaTime)) {
				bombs.remove(bombs.get(i));
				i--;
			}
		}
		
		repaint();
	}
	
}
