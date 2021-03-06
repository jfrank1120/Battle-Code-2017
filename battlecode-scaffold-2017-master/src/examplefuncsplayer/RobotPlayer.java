package examplefuncsplayer;
import battlecode.common.*;
import sun.reflect.generics.tree.Tree;

import java.util.*;
public strictfp class RobotPlayer {
    static RobotController rc;
    static Direction[] dirList = new Direction[4];
    static Direction goingDir;
    static Random rand;
    static Team enemy = rc.getTeam().opponent();
    static final int ARCHON_X_POS_CHANNEL = 0;
    static final int ARCHON_Y_POS_CHANNEL = 1;
    static final int ENEMY_ARCHON_X_CHANNEL = 2;
    static final int ENEMY_ARCHON_Y_CHANNEL = 3;
    static final int ENEMY_TREE_X_CHANNEL = 4;
    static final int ENEMY_TREE_Y_CHANNEL = 5;
    static final int NEUTRAL_TREE_X_CHANNEL = 6;
    static final int NEUTRAL_TREE_Y_CHANNEL = 7;
    static TreeInfo[] allyTrees = new TreeInfo[100];
    static TreeInfo[] enemyTrees = new TreeInfo[100];
    static TreeInfo[] neutralTrees = new TreeInfo[100];
    MapLocation[] enemyBroadcastedLocations[];
    MapLocation[] teamBroadcastedLocations[];
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
    **/
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        initDirList();
        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case TANK:
            	runTank();
            	break;
            case SCOUT:
            	runScout();
            	break;
        }
	}
    public static void initDirList(){
        for(int i=0;i<4;i++){
            float radians = (float)(-Math.PI + 2*Math.PI*((float)i)/4);
            dirList[i]=new Direction(radians);
            System.out.println("made new direction "+dirList[i]);
        }
    }
    static MapLocation findClosestEnemy(int robotID) throws GameActionException {
        RobotInfo closestEnemy;
        RobotInfo[] locationOfEnemys = new RobotInfo[100];
        for (int i = 0; i < 1000; i++) {
            locationOfEnemys[i] = rc.senseRobot(robotID);
        }
        float minDistance = (float) 0.0;
        int posInArrayOfClosestEnemy = 0;
        for (int i = 0; i < locationOfEnemys.length; i++)
        {
            if (locationOfEnemys[i].getLocation().distanceTo(rc.getLocation()) < minDistance){
                minDistance = locationOfEnemys[i].getLocation().distanceTo((rc.getLocation()));
                posInArrayOfClosestEnemy = i;
            }
            if (locationOfEnemys[i].getLocation().distanceTo(rc.getLocation()) == minDistance) {
                // TODO - Add action if there are two enemies that are the same distance from the robot
            }
        }
        closestEnemy = locationOfEnemys[posInArrayOfClosestEnemy];
        MapLocation locationOfClosestEnemy = closestEnemy.getLocation();
        return locationOfClosestEnemy;
    }
    static int treeDistance;
    // TODO  create method that finds the closest tree and either destroys it or hides behind it
    static MapLocation findClosestTree (int RobotID) throws GameActionException {
        MapLocation closestTree = new MapLocation((float)0.0,(float)0.0);
        int j = 0;
        int k = 0;
        int l = 0;
        TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
        for (int i = 0; i < nearbyTrees.length; i++)
        {
            if (nearbyTrees[i].team == enemy){
                enemyTrees[j] = nearbyTrees[i];
                j++;
            }
            else if (nearbyTrees[i].team.isPlayer() == true ) {
                allyTrees[k] = nearbyTrees[i];
                k++;
            }
            else {
                neutralTrees[l] = nearbyTrees[i];
                l++;
            }

        }
        return closestTree;
    }
    static MapLocation[] locateAllyArchons () throws GameActionException{
        MapLocation[] locationsOfArchons = new MapLocation[1];
        // Creates arrays to hold the X and Y cordinates of the Archons
        int[] allyArchonXPos = new int[1];
        int[] allyArchonYPos = new int[1];
        try {
            // Appends the X cordinates of ally archons to the array
            for (int i = 0; i < 2; i++) {
                allyArchonXPos[i] = rc.readBroadcast(ARCHON_X_POS_CHANNEL);
            }
            // Appends the Y cordinares of ally archones to the array
            for (int i = 0; i < 2; i++) {
                allyArchonYPos[i] = rc.readBroadcast(ARCHON_Y_POS_CHANNEL);
            }
        } catch (Exception e)
        {
            System.out.println("Unable to find ally archons");
        }
        MapLocation positionOfAllyArchon1;
        positionOfAllyArchon1 = new MapLocation((float)allyArchonXPos[0],(float)allyArchonYPos[0]);
        MapLocation positionOfAllyArchon2;
        positionOfAllyArchon2 = new MapLocation((float)allyArchonXPos[1],(float)allyArchonYPos[1]);
        locationsOfArchons[0] = positionOfAllyArchon1;
        locationsOfArchons[1] = positionOfAllyArchon2;
        return locationsOfArchons;
    }
    static BulletInfo findClosestBullet (MapLocation myLocation) throws GameActionException {
        int i;
        int j = 0;
        BulletInfo[] bullets = rc.senseNearbyBullets();
        BulletInfo[] willCollideBullets = new BulletInfo[100];
            for (i = 0; i < bullets.length; i++)
        {
            if (willCollideWithMe(bullets[i]) == true)
            {
                willCollideBullets[j] = bullets[i];
                j++;
            }
        }
        int positionInArrayOfMin = 0;
        float minDistance = bullets[0].location.distanceTo(myLocation);
        for (i = 1; i < bullets.length; i++) {
            if (willCollideBullets[i].location.distanceTo(myLocation) < minDistance) {
                minDistance = willCollideBullets[i].getLocation().distanceTo(myLocation);
                positionInArrayOfMin = i;
            }
        }
        BulletInfo closestBullet = willCollideBullets[positionInArrayOfMin];
        return closestBullet;
    }
    static boolean dodgeBullet (MapLocation currentLocation, RobotInfo id) throws GameActionException {
        BulletInfo incomingBullet = findClosestBullet(rc.getLocation());
        Direction towards = incomingBullet.getDir();
        MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);
        return(tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
    }
    static void attackArchons () throws GameActionException {
        // TODO finish method that makes robots go attack enemy archon whent they know it's position
        int enemyArchonX = rc.readBroadcast(ENEMY_ARCHON_X_CHANNEL);
        int enemyArchonY = rc.readBroadcast(ENEMY_ARCHON_Y_CHANNEL);
    }
    static void iLiveWithThePoolsAndTreeIs() throws GameActionException {
        //TODO create method that waters trees along with other common gardner actions
    }
    // Below is all run methods for the different types of robots
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    static void runScout () throws GameActionException {
    	System.out.println("Scout spawned");
    	Team enemy = rc.getTeam().opponent();
    	while (true)
        {
            try {
                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                for (int i = 0; i < robots.length; i++)
                {
                    // Checks to see if enemy robots are an archon
                    if (robots[i].type == RobotType.ARCHON)
                    {
                        // Calls out x and y position of the enemy archon
                        rc.broadcast(0, (int)robots[i].getLocation().x);
                        rc.broadcast(1, (int)robots[i].getLocation().y);
                    }
                }
            } catch (Exception e) {

            }
        }
    }
	static void runGardener() throws GameActionException {
        System.out.println("I'm a gardener!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos,yPos);
                tryToPlant();
                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a soldier or lumberjack in this direction
                if (rc.canBuildRobot(RobotType.SOLDIER, dir) && Math.random() < .01) {
                    rc.buildRobot(RobotType.SOLDIER, dir);
                } else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && Math.random() < .01 && rc.isBuildReady()) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }
    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }
    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");
        // The code you want your robot to perform every round should be in this loop
        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                // Randomly attempt to build a gardener in this direction
                if (rc.canHireGardener(dir) && Math.random() < .01) {
                    rc.hireGardener(dir);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(ARCHON_X_POS_CHANNEL,(int)myLocation.x);
                rc.broadcast(ARCHON_Y_POS_CHANNEL,(int)myLocation.y);

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }
    static void runTank () throws GameActionException {
        System.out.println("Tank Spawned");
        while (true) {
            try {
                wander();
            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
    }
    // =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
    public static boolean modGood(float number,float spacing, float fraction){
        return (number%spacing)<spacing*fraction;
    }
    public static void tryToPlant() throws GameActionException{
        //try to build gardeners
        //can you build a gardener?
        if(rc.getTeamBullets()>GameConstants.BULLET_TREE_COST) {//have enough bullets. assuming we haven't built already.
            for (int i = 0; i < 4; i++) {
                //only plant trees on a sub-grid
                MapLocation p = rc.getLocation().add(dirList[i],GameConstants.GENERAL_SPAWN_OFFSET+GameConstants.BULLET_TREE_RADIUS+rc.getType().bodyRadius);
                if(modGood(p.x,6,0.2f)&&modGood(p.y,6,0.2f)) {
                    if (rc.canPlantTree(dirList[i])) {
                        rc.plantTree(dirList[i]);
                        break;
                    }
                }
            }
        }
    }
    public static void wander() throws GameActionException {
        try {
            Direction dir = randomDirection();
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (!rc.hasMoved() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        //boolean moved = rc.hasMoved();
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(!rc.hasMoved() && rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                return true;
            }
            // Try the offset on the right side
            if(! rc.hasMoved() && rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
    
    /**
     *
     * @param map A MapLocation to convert to integer representation
     * @return An array arr such that:
     *          arr[0] - integer part of x
     *          arr[1] - decimal part of x * 10^6 and rounded
     *          arr[2] - integer part of y
     *          arr[3] - decimal part of y * 10^6 and rounded
     */
    static int[] convertMapLocation(MapLocation map) {
        float xcoord = map.x;
        float ycoord = map.y;
        int[] returnarray = new int[4];
        returnarray[0] = Math.round(xcoord - (xcoord % 1));
        returnarray[1] = Math.toIntExact(Math.round((xcoord % 1)*Math.pow(10,6)));
        returnarray[2] = Math.round(ycoord - (ycoord % 1));
        returnarray[3] = Math.toIntExact(Math.round((ycoord % 1)*Math.pow(10,6)));
        return(returnarray);
    }

    /**
     *
     * @param arr An array arr such that:
     *          arr[0] - integer part of x
     *          arr[1] - decimal part of x * 10^6 and rounded
     *          arr[2] - integer part of y
     *          arr[3] - decimal part of y * 10^6 and rounded
     * @return A MapLocation instantiated from the coordinates given by array
     */
    static MapLocation convertLocationInts(int[] arr) {
        float xcoord = (float)(arr[0] + arr[1]/Math.pow(10,6));
        float ycoord = (float)(arr[2] + arr[3]/Math.pow(10,6));
        return(new MapLocation(xcoord,ycoord));
    }

    static MapLocation readLocation(int firstChannel) throws GameActionException{
        int[] array = new int[4];
        array[0] = rc.readBroadcast(firstChannel);
        array[1] = rc.readBroadcast(firstChannel+1);
        array[2] = rc.readBroadcast(firstChannel+2);
        array[3] = rc.readBroadcast(firstChannel+3);
        return convertLocationInts(array);
    }

    static void writeLocation(MapLocation map, int firstChannel) throws GameActionException{
        int[] arr = convertMapLocation(map);
        rc.broadcast(firstChannel, arr[0]);
        rc.broadcast(firstChannel + 1, arr[1]);
        rc.broadcast(firstChannel+2, arr[2]);
        rc.broadcast(firstChannel+3, arr[3]);
    }

    static void goTowards(MapLocation map) throws GameActionException {
        tryMove(rc.getLocation().directionTo(map));
    }

}
