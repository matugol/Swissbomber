package swissbomber;

import java.awt.Color;

public class Main {
	
	public static void main(String[] args) {	
		Tile w = new Tile(-1, Color.GRAY);
		Tile c = new Tile(1, new Color(180, 160, 90));
		Tile n = null;
		Tile[][] grid = {
			{w,w,w,w,w,w,w,w,w,w,w,w,w},
			{w,n,n,c,c,c,c,c,c,c,n,n,w},
			{w,n,w,c,w,c,w,c,w,c,w,n,w},
			{w,c,c,c,c,c,c,c,c,c,c,c,w},
			{w,c,w,c,w,c,w,c,w,c,w,c,w},
			{w,c,c,c,c,c,c,c,c,c,c,c,w},
			{w,c,w,c,w,c,w,c,w,c,w,c,w},
			{w,c,c,c,c,c,c,c,c,c,c,c,w},
			{w,c,w,c,w,c,w,c,w,c,w,c,w},
			{w,c,c,c,c,c,c,c,c,c,c,c,w},
			{w,c,w,c,w,c,w,c,w,c,w,c,w},
			{w,c,c,c,c,c,c,c,c,c,c,c,w},
			{w,n,w,c,w,c,w,c,w,c,w,n,w},
			{w,n,n,c,c,c,c,c,c,c,n,n,w},
			{w,w,w,w,w,w,w,w,w,w,w,w,w}
		};

		Bomb.loadAnimations();
		new Window(new Game(grid, 2));
	}
	
}
