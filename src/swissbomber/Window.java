package swissbomber;

import javax.swing.JFrame;

public class Window extends JFrame {
	
	private static final long serialVersionUID = 5857900679549302482L;
		
	Window(Game game) {	
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("Swissbomber");

		add(game);
		
		setIgnoreRepaint(true);
		setResizable(false);
		pack();
		setVisible(true);
	}
	
}
