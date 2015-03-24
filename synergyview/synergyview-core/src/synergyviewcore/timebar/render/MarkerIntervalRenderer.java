/**
 * File: MarkerIntervalRender.java Copyright (c) 2010 Linxiao Ma This program is
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

package synergyviewcore.timebar.render;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.printing.Printer;

import synergyviewcore.timebar.model.MarkerInterval;
import de.jaret.util.date.Interval;
import de.jaret.util.swt.SwtGraphicsHelper;
import de.jaret.util.ui.timebars.TimeBarViewerDelegate;
import de.jaret.util.ui.timebars.TimeBarViewerInterface;
import de.jaret.util.ui.timebars.swt.renderer.RendererBase;
import de.jaret.util.ui.timebars.swt.renderer.TimeBarRenderer;
import de.jaret.util.ui.timebars.swt.renderer.TimeBarRenderer2;

/**
 * The Class MarkerIntervalRenderer.
 */
public class MarkerIntervalRenderer extends RendererBase implements
		TimeBarRenderer, TimeBarRenderer2 {
	/** pixeloffset for the label drawing. */
	private static final int LABELOFFSET = 3;
	/** extend for the label. */
	private static final int MAXLABELWIDTH = 50;
	/** size of the drawn element. */
	private static final int SIZE = 10;
	
	/** cache for the delegate supplying the orientation information. */
	protected TimeBarViewerDelegate _delegate;
	
	/** corrected size (for printing). */
	private int _size = SIZE;
	
	/**
	 * Construct renderer for screen use.
	 */
	public MarkerIntervalRenderer() {
		super(null);
	}
	
	/**
	 * Create renderer for printing.
	 * 
	 * @param printer
	 *            printer device
	 */
	public MarkerIntervalRenderer(Printer printer) {
		super(printer);
		_size = scaleX(SIZE);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public boolean contains(Interval interval, Rectangle drawingArea, int x,
			int y, boolean overlapping) {
		return contains(_delegate, interval, drawingArea, x, y, overlapping);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.jaret.util.ui.timebars.swt.renderer.TimeBarRenderer2#contains(de.jaret
	 * .util.ui.timebars.TimeBarViewerDelegate, de.jaret.util.date.Interval,
	 * org.eclipse.swt.graphics.Rectangle, int, int, boolean)
	 */
	public boolean contains(TimeBarViewerDelegate delegate, Interval interval,
			Rectangle drawingArea, int x, int y, boolean overlapping) {
		boolean horizontal = delegate.getOrientation() == TimeBarViewerInterface.Orientation.HORIZONTAL;
		Rectangle da = getDrawingRect(drawingArea, horizontal);
		return da.contains(drawingArea.x + x, drawingArea.y + y);
	}
	
	/**
	 * {@inheritDoc}. Will create print renderes for all registered renderers.
	 */
	public TimeBarRenderer createPrintrenderer(Printer printer) {
		MarkerIntervalRenderer renderer = new MarkerIntervalRenderer(printer);
		return renderer;
	}
	
	/**
	 * Drawing method for default rendering.
	 * 
	 * @param gc
	 *            GC
	 * @param drawingArea
	 *            drawingArea
	 * @param delegate
	 *            delegate
	 * @param interval
	 *            interval to draw
	 * @param selected
	 *            true for selected drawing
	 * @param printing
	 *            true for printing
	 * @param overlap
	 *            true if the interval overlaps with other intervals
	 */
	private void defaultDraw(GC gc, Rectangle drawingArea,
			TimeBarViewerDelegate delegate, Interval interval,
			boolean selected, boolean printing, boolean overlap) {
		
		boolean horizontal = delegate.getOrientation() == TimeBarViewerInterface.Orientation.HORIZONTAL;
		Rectangle da = getDrawingRect(drawingArea, horizontal);
		
		// draw focus
		drawFocus(gc, da, delegate, interval, selected, printing, overlap);
		
		Color bg = gc.getBackground();
		
		// draw the diamond
		gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
		
		int[] points = new int[] { da.x, da.y + (da.height / 2),
				da.x + (da.width / 2), da.y, da.x + da.width,
				da.y + (da.height / 2), da.x + (da.width / 2), da.y + da.height };
		
		gc.fillPolygon(points);
		gc.drawPolygon(points);
		
		if (selected) {
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_BLUE));
			gc.setAlpha(60);
			gc.fillPolygon(points);
			gc.setAlpha(255);
		}
		gc.setBackground(bg);
		
		MarkerInterval fe = (MarkerInterval) interval;
		
		// draw the label
		if (horizontal) {
			SwtGraphicsHelper.drawStringVCentered(gc, fe.getLabel(), da.x
					+ da.width + scaleX(LABELOFFSET), da.y, da.y + da.height);
		} else {
			SwtGraphicsHelper.drawStringCentered(gc, fe.getLabel(), da.x
					+ (da.width / 2), da.y + da.height + scaleY(LABELOFFSET)
					+ gc.textExtent(fe.getLabel()).y);
		}
		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void dispose() {
	}
	
	/**
	 * {@inheritDoc}
	 */
	public void draw(GC gc, Rectangle drawingArea,
			TimeBarViewerDelegate delegate, Interval interval,
			boolean selected, boolean printing, boolean overlap) {
		_delegate = delegate;
		defaultDraw(gc, drawingArea, delegate, interval, selected, printing,
				overlap);
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Rectangle getContainingRectangle(Interval interval,
			Rectangle drawingArea, boolean overlapping) {
		return getContainingRectangle(_delegate, interval, drawingArea,
				overlapping);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.jaret.util.ui.timebars.swt.renderer.TimeBarRenderer2#
	 * getContainingRectangle(de.jaret.util.ui.timebars.TimeBarViewerDelegate,
	 * de.jaret.util.date.Interval, org.eclipse.swt.graphics.Rectangle, boolean)
	 */
	public Rectangle getContainingRectangle(TimeBarViewerDelegate delegate,
			Interval interval, Rectangle drawingArea, boolean overlapping) {
		boolean horizontal = delegate.getOrientation() == TimeBarViewerInterface.Orientation.HORIZONTAL;
		Rectangle da = getDrawingRect(drawingArea, horizontal);
		return da;
	}
	
	/**
	 * Calculate the drawing area for the marking symbol.
	 * 
	 * @param drawingArea
	 *            drawing area as given for the time
	 * @param horizontal
	 *            the horizontal
	 * @return Rectangle for drawing the main symbol
	 */
	private Rectangle getDrawingRect(Rectangle drawingArea, boolean horizontal) {
		if (horizontal) {
			int y = drawingArea.y + ((drawingArea.height - (2 * _size)) / 2);
			return new Rectangle(drawingArea.x - _size, y, 2 * _size, 2 * _size);
		} else {
			int x = drawingArea.x + ((drawingArea.width - (2 * _size)) / 2);
			return new Rectangle(x, drawingArea.y - _size, 2 * _size, 2 * _size);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public Rectangle getPreferredDrawingBounds(Rectangle intervalDrawingArea,
			TimeBarViewerDelegate delegate, Interval interval,
			boolean selected, boolean printing, boolean overlap) {
		
		boolean horizontal = delegate.getOrientation() == TimeBarViewerInterface.Orientation.HORIZONTAL;
		if (horizontal) {
			return new Rectangle(intervalDrawingArea.x - _size,
					intervalDrawingArea.y, intervalDrawingArea.width
							+ (2 * _size) + scaleX(MAXLABELWIDTH),
					intervalDrawingArea.height);
		} else {
			return new Rectangle(intervalDrawingArea.x, intervalDrawingArea.y
					- _size, intervalDrawingArea.width,
					intervalDrawingArea.height + (2 * _size)
							+ scaleY(MAXLABELWIDTH));
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	public String getToolTipText(Interval interval, Rectangle drawingArea,
			int x, int y, boolean overlapping) {
		return getToolTipText(_delegate, interval, drawingArea, x, y,
				overlapping);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.jaret.util.ui.timebars.swt.renderer.TimeBarRenderer2#getToolTipText
	 * (de.jaret.util.ui.timebars.TimeBarViewerDelegate,
	 * de.jaret.util.date.Interval, org.eclipse.swt.graphics.Rectangle, int,
	 * int, boolean)
	 */
	public String getToolTipText(TimeBarViewerDelegate delegate,
			Interval interval, Rectangle drawingArea, int x, int y,
			boolean overlapping) {
		if (contains(delegate, interval, drawingArea, x, y, overlapping)) {
			return interval.toString();
		}
		return null;
	}
	
}
