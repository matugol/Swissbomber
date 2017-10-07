package swissbomber;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputController implements Controller, KeyListener {

	Character character;
	/** The key codes for each of actions the player can take. <br><code>{up, down, left, right, bomb, special}</code> */
	private int[] keyCodes;
	/** Whether each key the player can press is pressed down or not. <br><code>{up, down, left, right, bomb, special}</code> */
	private boolean[] keyPressed = new boolean[6];
	/** Whether or not a key the player can press has been held down for longer than a frame. <br><code>{bomb, special}</code> */
	private boolean[] isHeld = new boolean[2];
	
	public InputController(Character character, int[] keyCodes) {
		this.character = character;
		this.keyCodes = keyCodes;
	}
	
	/**
	 * Processes all input for the character and updates them
	 * 
	 * @param deltaTime	the time passed since the last frame in nanoseconds
	 */
	@Override
	public void step(long deltaTime) {
		if (!character.isAlive()) return;
		
		if (keyPressed[4] && !isHeld[0] && character.getCurrentBombs() > 0) {
			if (Game.game.placeBomb((int) character.getX(), (int) character.getY(), character)) {
				character.removeBomb();
			}
			
			isHeld[0] = true;
		}
		
		if (keyPressed[5] && !isHeld[1]) {
			character.detonateRemoteBomb();
			isHeld[1] = true;
		}
		
		int horizontal = (keyPressed[2] ? -1 : 0) + (keyPressed[3] ? 1 : 0);
		int vertical = (keyPressed[0] ? 1 : 0) + (keyPressed[1] ? -1 : 0);
		
		if (horizontal == 0 && vertical == 0) {
			return;
		}
		
		double angle;
		if (horizontal == 1 && vertical == -1) {
			angle = 315;
		} else {
			angle = (horizontal == -1 ? 180 : 0) + (vertical == -1 ? 270 : (vertical == 1 ? 90 : 0));
			if (horizontal != 0 && vertical != 0) {
				angle /= 2;
			}
		}
				
		character.move(angle, deltaTime);
	}
	
	@Override
	public void keyPressed(KeyEvent e) {
		for (int i = 0; i < keyCodes.length; i++) {
			if (e.getKeyCode() == keyCodes[i]) {
				keyPressed[i] = true;
			}
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		for (int i = 0; i < keyCodes.length; i++) {
			if (e.getKeyCode() == keyCodes[i]) {
				keyPressed[i] = false;
				if (i >= 4)
					isHeld[i - 4] = false;
			}
		}
	}

	@Override
	public void keyTyped(KeyEvent e) {
		
	}

}
