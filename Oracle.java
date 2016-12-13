package spacecadets2016;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

import robocode.AdvancedRobot;
import robocode.RobotDeathEvent;
import robocode.ScannedRobotEvent;
import robocode.util.*;

public class Oracle extends AdvancedRobot
{
	private int direction = 1;
	private long lastTurn = 0;
	private double targetX = 300;
	private double targetY = 300;
	
	private Map<String, Victim> victims = new HashMap<String, Victim>();
	private Victim nextVictim;
	
	private static final double BULLET_POWER = 3;
	private static final double OFFSET = 0.95;
	
	
	private class Victim {
		public long lastScried;
		public double x;
		public double y;
		public double velX;
		public double velY;
		public double energy;
		public String name;
		public int shotsFired;
	}
	
	
	
	public void run()
	{
		setAdjustGunForRobotTurn(true);
		setAdjustRadarForGunTurn(true);
		while(true) {
			scry();
			stride();
			voodoo();
			execute();
		}
	}
	
	public void onRobotDeath(RobotDeathEvent event)
	{
		if(event.getName().equals(nextVictim.name)) {
			nextVictim = null;
		}
	}
	
	private void voodoo() {
		if(nextVictim == null) {
			return;
		}
		
		double bulletSpeed = 20 - (3 * BULLET_POWER);
		
		double t = Math.sqrt(Math.pow(nextVictim.x - getX(), 2) + Math.pow(nextVictim.y - getY(), 2)) / bulletSpeed;
		double[] prophecy = {nextVictim.x + nextVictim.velX * t * OFFSET, nextVictim.y + nextVictim.velY * t * OFFSET};
		double[] vector = {prophecy[0] - getX(), prophecy[1] - getY()};
		double angle = Math.toDegrees(Math.atan2(vector[0], vector[1]));
		double turn = Utils.normalRelativeAngleDegrees(angle - getGunHeading());
		setTurnGunRight(turn);
		
		System.out.println("Enemy prophesised to be at: " + prophecy[0] + ", " + prophecy[1]);
		System.out.println("Angle: " + angle);
		
		if(Math.abs(turn) < 10.0 && getGunHeat() == 0.0) {
			setFire(BULLET_POWER - (getEnergy() < 15.0 ? 2.0 : 0.0));
			nextVictim.shotsFired += 1;
		}
	}

	private void scry()
	{
		if(nextVictim == null) {
			setTurnRadarRight(45);
		} else if(getOthers() == 1) {
			scryOne();
		} else {
			scryMany();
		}
	}
	
	private void scryOne()
	{
		int scanDir = (int)(getTime() % 2) * -2 + 1;
		
		double[] vector = {nextVictim.x - getX(), nextVictim.y - getY()};
		double angle = Math.toDegrees(Math.atan2(vector[0], vector[1]));
		setTurnRadarRight(Utils.normalRelativeAngleDegrees(angle - getRadarHeading() + (scanDir * 20)));
	}
	
	private void scryMany()
	{
		setTurnRadarRight(45);
	}
	
	@Override
	public void onScannedRobot(ScannedRobotEvent e)
	{
		Victim scried;
		
		if(victims.containsKey(e.getName())) {
			scried = victims.get(e.getName());
		} else {
			scried = new Victim();
			victims.put(e.getName(), scried);
		}
		
		
		scried.lastScried = e.getTime();
		double angle = Utils.normalAbsoluteAngleDegrees(e.getBearing() + getHeading());
		scried.x = getX() + e.getDistance() * Math.sin(Math.toRadians(angle));
		scried.y = getY() + e.getDistance() * Math.cos(Math.toRadians(angle));
		scried.velX = e.getVelocity() * Math.sin(Math.toRadians(e.getHeading()));
		scried.velY = e.getVelocity() * Math.cos(Math.toRadians(e.getHeading()));
		double deltaEnergy = e.getEnergy() - scried.energy;
		scried.energy = e.getEnergy();
		scried.name = e.getName();
		
		System.out.println("Vel: " + e.getVelocity());
		System.out.println("Heading: " + e.getHeading());
		System.out.println("Enemy velX: " + scried.velX);
		System.out.println("Enemy velY: " + scried.velY);
		
		
		if(nextVictim != null) {
			if(scried != nextVictim) {
				
				if(Math.sqrt(Math.pow(scried.x - getX(), 2) + Math.pow(scried.y - getY(), 2)) 
						< Math.sqrt(Math.pow(nextVictim.x - getX(), 2) + Math.pow(nextVictim.y - getY(), 2))) {
					nextVictim = scried;
				}
			}
			
		} else {
			nextVictim = scried;
		}
		
		if(deltaEnergy < -0.1 && deltaEnergy >= -3.0) {
			angle = e.getBearing() + getHeading() + (direction * 90);
			direction *= -1;
			targetX = 80 * Math.sin(Math.toRadians(angle)) + getX();
			targetY = 80 * Math.cos(Math.toRadians(angle)) + getY();
		}
	}
	
	private void stride()
	{
		
		if(Math.abs(getX() - targetX) < 10 
				&& Math.abs(getY() - targetY) < 10 
				&& nextVictim != null) {
			System.out.println("Distance to enemy: " + Math.sqrt(Math.pow(getX() - nextVictim.x, 2) + Math.pow(getY() - nextVictim.y, 2)));
			if(Math.sqrt(Math.pow(getX() - nextVictim.x, 2) + Math.pow(getY() - nextVictim.y, 2)) > 200) {
				targetX = nextVictim.x;
				targetY = nextVictim.y;
				System.out.println("GET CLOSER " + targetX);
				System.out.println("GET CLOSER " + targetY);
			}
		}
		
		if(targetX < 100 || targetX > getBattleFieldWidth() - 100) {
			targetX = 400;
		}
		
		if(targetY < 100 || targetY > getBattleFieldHeight() - 100) {
			targetY = 400;
		}
		
		double[] vector = {targetX - getX(), targetY - getY()};
		double angle = Utils.normalRelativeAngleDegrees(Math.toDegrees(Math.atan2(vector[0], vector[1])) - getHeading());
		
		if(Math.abs(angle) <= 90) {
			setTurnRight(angle);
			setAhead(100);
		} else {
			angle = Utils.normalRelativeAngleDegrees(angle - 180);
			setTurnRight(angle);
			setAhead(-100);
		}
	}
}
