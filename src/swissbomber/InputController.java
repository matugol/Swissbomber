package swissbomber;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class InputController implements Controller, KeyListener {

	Character character;
	private int[] keyCodes; // upKC, downKC, leftKC, rightKC, bombKC, specialKC;
	private boolean[] keyPressed = new boolean[6];
	private boolean[] isHeld = new boolean[2];
	
	public InputController(Character character, int[] keyCodes) {
		this.character = character;
		this.keyCodes = keyCodes;
	}
	
	@Override
	public void step(Game game, long deltaTime) {
		if (!character.isAlive()) return;
		
		if (keyPressed[4] && !isHeld[0] && character.getCurrentBombs() > 0) {
			if (game.placeBomb((int) character.getX(), (int) character.getY(), character)) {
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
				
		character.move(game, angle, deltaTime);
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
