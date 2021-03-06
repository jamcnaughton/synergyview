/**
 * File: ICaptionChangeListener.java Copyright (c) 2011 phyo This program is
 * free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version. This
 * program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 */

package synergyviewcore.annotations.ui.events;

/**
 * The listener interface for receiving ICaptionChange events. The class that is interested in processing a ICaptionChange event implements this interface, and the object created with that class is registered with a component using the component's <code>addICaptionChangeListener<code> method. When
 * the ICaptionChange event occurs, that object's appropriate
 * method is invoked.
 * 
 * @author phyo
 */
public interface ICaptionChangeListener {

    /**
     * The Enum CaptionPublishState.
     */
    public enum CaptionPublishState {
	/** The set. */
	SET,
	/** The unset. */
	UNSET
    };

    /**
     * Caption change.
     * 
     * @param source
     *            the source
     * @param eventArg
     *            the event arg
     */
    public void captionChange(Object source, CaptionChangeEvent eventArg);
}
