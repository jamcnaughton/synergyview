/*
 *  File: TimeBarMarkerListener.java 
 *  Copyright (c) 2004-2007  Peter Kliem (Peter.Kliem@jaret.de)
 *  A commercial license is available, see http://www.jaret.de.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package de.jaret.util.ui.timebars;

import de.jaret.util.date.JaretDate;

/**
 * A listener for registering changes on a TimeBarMarker.
 * 
 * @author Peter Kliem
 * @version $Id: TimeBarMarkerListener.java 821 2009-02-04 21:12:16Z kliem $
 */
public interface TimeBarMarkerListener {
    /**
     * Callback method indicating a move of a marker.
     * 
     * @param marker the marker moved
     * @param oldDate date before movement
     * @param currentDate new date of the marker
     */
    void markerMoved(TimeBarMarker marker, JaretDate oldDate, JaretDate currentDate);

    /**
     * Callback method indicating the description of a marker changed.
     * 
     * @param marker the marker of which the description changed
     * @param oldValue previous description
     * @param newValue current description
     */
    void markerDescriptionChanged(TimeBarMarker marker, String oldValue, String newValue);

    
    
}
