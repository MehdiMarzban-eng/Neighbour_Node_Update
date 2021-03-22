/*
 * Complete this class.
 * Student Name: Mehdi Marzban
 * Student ID No.: 30062768
 */
import java.lang.reflect.Array;
import java.util.Arrays;

import javax.swing.*;

public class RouterNode {
  private int myID;
  private GuiTextArea myGUI;
  private RouterSimulator sim;
  private int[] costs = new int[RouterSimulator.NUM_NODES];
  // need to add several more parameters here (some of these can be optional based on your way of implementation):
  // - a switch to toggle PoisonReverse
  boolean PoisonReverse =true;
  // - a 2D array to store neighbor table
  int [][] DistTable=new int[RouterSimulator.NUM_NODES][RouterSimulator.NUM_NODES];
  // - an array to store the first hops for each hop
  int[] route=new int[RouterSimulator.NUM_NODES];
  //a boolean array to store if a node is a neighbour or not 
  boolean[] isNeighbour= new boolean[RouterSimulator.NUM_NODES];
  
  //--------------------------------------------------
  public RouterNode(int ID, RouterSimulator sim, int[] costs) {
    myID = ID;
    this.sim = sim;
    myGUI =new GuiTextArea("  Output window for Router #"+ ID + "  ");

    System.arraycopy(costs, 0, this.costs, 0, RouterSimulator.NUM_NODES);
    // initialize the distance vector array
    for(int i=0;i<RouterSimulator.NUM_NODES;i++) {
    	for(int j=0; j<RouterSimulator.NUM_NODES;j++) {
    		DistTable[i][j]=RouterSimulator.INFINITY;
    	}
    }
    //sets the distance table at the current nodes ID to costs
    System.arraycopy(costs, 0, DistTable[myID], 0, RouterSimulator.NUM_NODES);
    
    //this for loops initializes the initial routes, and also sets the isNeighbour array checking to see if there is a direct path to the node
    for(int i=0;i<RouterSimulator.NUM_NODES;i++) {
    	if(costs[i]==RouterSimulator.INFINITY) {
    		route[i]=RouterSimulator.INFINITY;
    		isNeighbour[i]=false;
    	}
    	else {
    		printDistanceTable();
    		route[i]=i;
    		isNeighbour[i]=true;
    	
    	}
    }
    isNeighbour[myID]=false;
    route[myID]=myID;
    broadcastDistance();
  }

  //--------------------------------------------------
  public void recvUpdate(RouterPacket pkt) {
    // Basically you should perform bellman-ford equation which is:
    // having this packet, for each node of the of topology:
    //    - handle any updates that have (1) a cost increase (2) packet sourceId is the first_hop
    //    - handle any updates that shows a path with lower cost
    // broadcast the updates if anything has changed
	//sets DistTable[pkt.sourceid] to pkt.mincost and then calls the costChange array which checks to see if there are any changes to the values
	System.arraycopy(pkt.mincost, 0, DistTable[pkt.sourceid], 0, RouterSimulator.NUM_NODES);
	costChange();
	printDistanceTable();
 }


  //----------------------------------------------------
  private void sendUpdate(RouterPacket pkt) {
    // NOTE: NEVER SEND ANY UPDATES TO NON-NEIGHBORS
    // no need to update if cost of the packet's desitination is INFINITY
    // if PoisonReverse is enabled: set the packet.mincost accordingly
	//I implement the poison reverse in the BroadcastDistance() method
    sim.toLayer2(pkt);
  }


  //--------------------------------------------------
  public void printDistanceTable() {
	  myGUI.println("Current table for " + myID +
			"  at time " + sim.getClocktime());
	  myGUI.println("DistanceTable");
	  myGUI.print("dist | ");
	  for(int i=0;i<RouterSimulator.NUM_NODES;i++) {
		  myGUI.print(i+" ");
	  }
	  myGUI.println("\n----------------------------------------");
	  myGUI.print("cost  | ");
	  for(int i=0;i<RouterSimulator.NUM_NODES;i++) {
		  myGUI.print(DistTable[myID][i]+" ");
	  }
	  myGUI.print("\nroute | ");
	  for(int i=0;i<RouterSimulator.NUM_NODES;i++) {
		  myGUI.print(route[i]+" ");
	  }
	  myGUI.println();
	  myGUI.println();
    // NOTE: Please provide a self-explanatory and human-readable logging report here
  }

  //--------------------------------------------------
  public void updateLinkCost(int dest, int newcost) {
    // update costs (declared above) array
    // update diestance vector (declared above) array
    // create a RouterPacket
    // handle cost increase
	 //calls costChange() to see if the new link cost changes any of the routes
	costs[dest]=newcost;
	costChange();
	printDistanceTable();
	broadcastDistance();
    // call recvUpdate
  }

  // You can add two helper functions:
  //    - one to broadcast the distances
  //    - one to handle cost increase
  private void broadcastDistance() {
	  // here we are going send the new distance vector to neighbor nodes 
	  for(int i =0;i<RouterSimulator.NUM_NODES; i++) {
		  if(myID==i||RouterSimulator.INFINITY==costs[i]) {
			  continue; // skip the self, and non-neighbour cases;
		  }
		  int[] sendVector=new int[RouterSimulator.NUM_NODES];
		  for(int j=0; j<RouterSimulator.NUM_NODES;j++) {
			  //when poison reverse is enabled and fastest route of the current index is equal to i we set that route to infinity to ignore it 
			  if(PoisonReverse==true&&i==route[j]) {
				  sendVector[j]=RouterSimulator.INFINITY;
			  }
			  else {
				  sendVector[j]=DistTable[myID][j];
			  }
		  }
		  sendUpdate(new RouterPacket(myID,i,sendVector));
	  }
  }
  //calculates the new distance vector
  private void costChange() {
		int[] holder=new int[RouterSimulator.NUM_NODES];
		int costHolder;
		boolean change=false;
			
			for(int i=0;i<RouterSimulator.NUM_NODES;i++) {
				costHolder=costs[i];
				//holder=DistTable[myID];
				System.arraycopy(DistTable[myID], 0, holder, 0, RouterSimulator.NUM_NODES);
			
				int fastRoute; // setting to the fastest route possible to the dest
				if(costs[i]!=RouterSimulator.INFINITY) { //
					fastRoute=i;
				}
				else {
					fastRoute=RouterSimulator.INFINITY;
				}
				//this for loop goes through every possible route j, and compares it to costs[i] and sees if it is a faster route, if so we change the route array
				for(int j=0;j<RouterSimulator.NUM_NODES;j++) {
					if(myID==j||costs[j]==RouterSimulator.INFINITY) {
						continue;
					}
					if(costs[j]+DistTable[j][i]<costHolder&&costs[j]!=RouterSimulator.INFINITY&&DistTable[j][i]!=RouterSimulator.INFINITY)
					{
						costHolder=costs[j]+DistTable[j][i];
						fastRoute=j;
					}
				}
				if(fastRoute!=RouterSimulator.INFINITY) {
				route[i]=fastRoute;
				}
				holder[i]=costHolder;
		
				//check to see if the new holder array actually different from the stored distance table 
				if(!Arrays.equals(holder, DistTable[myID])) {
					//DistTable[myID]=holder;
					System.arraycopy(holder, 0, DistTable[myID], 0, RouterSimulator.NUM_NODES);
					change=true;
				    broadcastDistance();
				}
			
			}
			if(change) {
				broadcastDistance();
			}
		
  }
 }


