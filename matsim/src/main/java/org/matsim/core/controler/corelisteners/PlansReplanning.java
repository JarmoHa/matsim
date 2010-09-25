/* *********************************************************************** *
 * project: org.matsim.*
 * PlansReplanner.java.java
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

package org.matsim.core.controler.corelisteners;

import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.listener.ReplanningListener;

/**
 * A {@link org.matsim.core.controler.listener.ControlerListener} that manages the
 * replanning of plans in every iteration. Basically it integrates the
 * {@link org.matsim.core.replanning.StrategyManagerImpl} with the
 * {@link org.matsim.core.controler.Controler}.
 *
 * @author mrieser
 */
public class PlansReplanning implements ReplanningListener {

	public void notifyReplanning(final ReplanningEvent event) {
		Controler controler = event.getControler();
		controler.getStrategyManager().run(controler.getPopulation(), event.getIteration());
	}

}
