/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeCalculatorImpl2.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.gregor.travaletimecalc;

import java.util.HashMap;

import org.matsim.events.EventAgentArrival;
import org.matsim.events.EventLinkEnter;
import org.matsim.events.EventLinkLeave;
import org.matsim.events.handler.EventHandlerAgentArrivalI;
import org.matsim.events.handler.EventHandlerLinkEnterI;
import org.matsim.events.handler.EventHandlerLinkLeaveI;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.TravelTimeI;

public class TravelTimeCalculatorImpl2 implements EventHandlerLinkEnterI,
		EventHandlerLinkLeaveI, EventHandlerAgentArrivalI, TravelTimeI {
	
	// EnterEvent implements Comparable based on linkId and vehId. This means that the key-pair <linkId, vehId> must always be unique!
	private final HashMap<String, EnterEvent> enterEvents = new HashMap<String, EnterEvent>();
	
	private NetworkLayer network = null;
	final int roleIndex;
	private final int timeslice;
	private final int expectNumSlots;
	
	
	
	public TravelTimeCalculatorImpl2(final NetworkLayer network){
		this(network,15*60,30*3600); // default timeslot-duration: 15 minutes
	}
	

	public TravelTimeCalculatorImpl2(final NetworkLayer network, final int timeslice) {
		this(network, timeslice, 30*3600); // default: 30 hours at most
		
	}
	
	//compatibility constructor ...
	public TravelTimeCalculatorImpl2(final NetworkLayer network, final int timeslice, final int maxTime) {
		this.network  = network;
		this.timeslice = timeslice;
		this.expectNumSlots = (int) (0.25 * (maxTime / this.timeslice) + 1); // TODO hard-coded max-time
		this.roleIndex = network.requestLinkRole();
	}
	
	public void resetTravelTimes() {
		for (Link link : this.network.getLinks().values()) {
			TravelTimeRole r = getTravelTimeRole(link);
			r.resetTravelTimes();
		}
		this.enterEvents.clear();
	}
	

	public void reset(final int iteration) {
		resetTravelTimes();
	}


	//////////////////////////////////////////////////////////////////////
	// Implementation of EventAlgorithmI
	//////////////////////////////////////////////////////////////////////
	public void handleEvent(EventLinkEnter event) {
		EnterEvent e = new EnterEvent(event.linkId, event.time);
		this.enterEvents.put(event.agentId, e);
	}

	public void handleEvent(EventLinkLeave event) {
		EnterEvent e = this.enterEvents.remove(event.agentId);
		if (e != null && e.linkId.equals(event.linkId)) {
			double timediff = event.time - e.time;
			if (event.link == null) event.link = (Link)this.network.getLocation(event.linkId);
			if (event.link != null) {
				getTravelTimeRole(event.link).addTravelTime(e.time, timediff);
			}
		}
	}

	public void handleEvent(EventAgentArrival event) {
		// remove EnterEvents from list when an agent arrives.
		// otherwise, the activity duration would counted as travel time, when the
		// agent departs again and leaves the link!
		this.enterEvents.remove(event.agentId);

	}

	private TravelTimeRole getTravelTimeRole(final Link link) {
		TravelTimeRole r = (TravelTimeRole) link.getRole(this.roleIndex);
		if (null == r) {
			r = new TravelTimeRole(link, this.expectNumSlots);
			link.setRole(this.roleIndex, r);
		}
		return r;
	}
	
	private int getTimeSlotIndex(final double time) {
		int slice = ((int) time)/this.timeslice;
		return slice;
	}
	
	
	//////////////////////////////////////////////////////////////////////
	// Implementation of TravelTimeI
	//////////////////////////////////////////////////////////////////////

	/* (non-Javadoc)
	 * @see org.matsim.network.TravelCostI#getLinkTravelTime(org.matsim.network.Link, int)
	 */
	public double getLinkTravelTime(final Link link, final double time) {
		return getTravelTimeRole(link).getTravelTime(time);
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return this.getClass().getSimpleName();
	}
	
	//////////////////////////////////////////////////////////////////////
	// inner classes
	//////////////////////////////////////////////////////////////////////
	
	static private class EnterEvent {

		public final String linkId;
		public final double time;

		public EnterEvent(final String linkId, final double time) {
			this.linkId = linkId;
			this.time = time;
		}

	};

	private class TravelTimeRole {
	
		private HashMap<Integer,TimeStruct> travelTimes;
		private final double freetraveltime;
		private int currIdx;
		private int currCnt;
		private double currTimeSum;

		public TravelTimeRole(final Link link, final int numSlots) {

			this.travelTimes =  new HashMap<Integer,TimeStruct>(numSlots,(float) 0.5); 
			this.freetraveltime = link.getLength() / link.getFreespeed();
			resetTravelTimes();
		}

		public void resetTravelTimes() {
			this.currCnt = 0;
			this.currIdx = 0;
			this.currTimeSum = 0;
			this.travelTimes.clear();
		}

		public void addTravelTime(final double now, final double traveltime) {
			int index = getTimeSlotIndex(now);
			if (index != this.currIdx){
				changeCurrent(index);
				
			}
			this.currCnt++;
			this.currTimeSum += traveltime;
		}

		private void changeCurrent(int index) {
			TimeStruct curr = this.travelTimes.get(this.currIdx);
			// save old
			if (curr == null){
				this.travelTimes.put(this.currIdx, new TimeStruct(this.currTimeSum,this.currCnt));
			} else {
				curr.cnt += this.currCnt;
				curr.timeSum += this.currTimeSum;
			}
			
			// set new
			this.currIdx = index;
			curr = this.travelTimes.get(this.currIdx);
			if (curr == null){
				this.currCnt = 0;
				this.currTimeSum = 0;				
			} else {
				this.currCnt = curr.cnt;
				this.currTimeSum = curr.timeSum;				
			}
			
		}

		public double getTravelTime(final double now) {
			int index = getTimeSlotIndex(now);
			
			if (index == this.currIdx) {
				return this.currTimeSum / this.currCnt;
			}
	
			TimeStruct ts = this.travelTimes.get(index);
			if (ts == null){
				return this.freetraveltime;
			}
			
			return ts.timeSum / ts.cnt;
			
		}
		

		private class TimeStruct{
			public double timeSum;
			public int cnt;
			public TimeStruct(double timeSum, int cnt){
				this.cnt = cnt;
				this.timeSum = timeSum;
			}
		};

	};
	

	
}
