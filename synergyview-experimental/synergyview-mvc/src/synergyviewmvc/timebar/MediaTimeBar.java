package synergyviewmvc.timebar;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.databinding.observable.map.IObservableMap;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;

import synergyviewmvc.collections.model.CollectionMedia;
import synergyviewmvc.collections.model.CollectionMediaClip;
import synergyviewmvc.collections.model.CollectionNode;
import synergyviewmvc.collections.ui.AbstractMediaCollectionControl;
import synergyviewmvc.media.model.AbstractMedia;
import synergyviewmvc.media.model.MediaRootNode;
import synergyviewmvc.timebar.component.MediaControlBar;
import synergyviewmvc.timebar.component.TimeScaleBar;
import synergyviewmvc.timebar.component.MediaControlBar.MediaControlListener;
import synergyviewmvc.timebar.component.TimeScaleBar.TimeScaleListener;
import synergyviewmvc.timebar.event.MediaListEvent;
import synergyviewmvc.timebar.model.MediaIntervalImpl;
import synergyviewmvc.timebar.model.MediaSegmentIntervalImpl;
import synergyviewmvc.timebar.model.MediaTimeBarMarkerImpl;
import synergyviewmvc.timebar.model.ModelBuilder;
import synergyviewmvc.timebar.render.TimeBarIntervalRenderer;
import synergyviewmvc.timebar.render.TimeBarSegmentIntervalRenderer;
import synergyviewmvc.timebar.render.TimeBarTimeScaleRender;
import de.jaret.util.date.Interval;
import de.jaret.util.date.JaretDate;
import de.jaret.util.ui.timebars.TimeBarMarker;
import de.jaret.util.ui.timebars.TimeBarMarkerListener;
import de.jaret.util.ui.timebars.mod.DefaultIntervalModificator;
import de.jaret.util.ui.timebars.model.DefaultTimeBarRowModel;
import de.jaret.util.ui.timebars.model.ITimeBarChangeListener;
import de.jaret.util.ui.timebars.model.TimeBarModel;
import de.jaret.util.ui.timebars.model.TimeBarModelListener;
import de.jaret.util.ui.timebars.model.TimeBarRow;
import de.jaret.util.ui.timebars.swt.TimeBarViewer;
import de.jaret.util.ui.timebars.swt.renderer.DefaultTitleRenderer;

public class MediaTimeBar extends AbstractMediaCollectionControl {
	
	public static final JaretDate MEDIA_START_TIME= new JaretDate().setTime(0, 0, 0, 0);
	
	protected List<TimeSlideListener> timeBarListeners = new ArrayList<TimeSlideListener>();
	
	protected Composite parent;
	
	protected TimeScaleBar timeScaleControlPanel;
	protected MediaControlBar mediaControlBar;
	
	protected int initialMediaLenght = 3600;	
	protected MediaTimeBarMarkerImpl timeMarker;	
	protected boolean isMarkerDragging=false;
	protected boolean isMediaDurationBarOn=true;
	protected boolean mediaStatus = false;
	protected boolean lock = false;
	protected TimeBarIntervalRenderer timeBarIntervalRenderer;
	
	public MediaTimeBar(Composite parent, int style, IObservableMap mediaMap,  CollectionNode collectionFolderValue) {
		super(parent, style, mediaMap, collectionFolderValue);
		this.parent = parent;
		
		this.createControls(parent, (int) this.getDuration());
		this.buildIntervals();
		this.addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				disposeResources();

			}

		});
	}
		
	/**
	 * 
	 */
	protected void disposeResources() {
		if (timeBarIntervalRenderer!=null)
			timeBarIntervalRenderer.disposeResource();
		
	}

	protected void createControls(Composite parent, int mediaLength){
		
		if (mediaLength<=0) mediaLength = initialMediaLenght;
		
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		this.setLayout(gridLayout);
		
		this.addMediaControlBar(this);
		
		GridData gd = new GridData();
		gd.horizontalAlignment = SWT.FILL;
		gd.grabExcessHorizontalSpace = true;
		gd.verticalAlignment = SWT.FILL;
		gd.grabExcessVerticalSpace = true;
		Group border = new Group(this,SWT.SHADOW_OUT);
		border.setLayoutData(gd);
		border.setLayout(new FillLayout());
		timeBarViewer = new TimeBarViewer(border, SWT.H_SCROLL);
		timeBarViewer.setVisible(true);
		
		//build timebar row
		TimeBarModel model = ModelBuilder.createModel();
		timeBarViewer.setModel(model);
		timeBarViewer.setYAxisWidth(0);
		timeBarViewer.setAutoscrollEnabled(false);
		timeBarViewer.setSelectionDelta(6);
		timeBarViewer.setMilliAccuracy(true);
	    
		//build timebar title
		DefaultTitleRenderer titleRenderer = new DefaultTitleRenderer();
		timeBarViewer.setTitleRenderer(titleRenderer);
		timeBarViewer.setTitle("Time Bar");

		//set time scale
		timeBarViewer.setTimeScaleRenderer(new TimeBarTimeScaleRender());
		timeBarViewer.setTimeScalePosition(TimeBarViewer.TIMESCALE_POSITION_TOP);
		timeBarViewer.setDrawRowGrid(true);
		
		//set initial setting
		timeBarViewer.setInitialDisplayRange(MEDIA_START_TIME.copy(), mediaLength/1000);
		timeBarViewer.setMaxDate(MEDIA_START_TIME.copy().advanceMillis(mediaLength));
		timeBarViewer.setMinDate(MEDIA_START_TIME.copy());
		
		timeBarViewer.setPixelPerSecond((double)(timeBarViewer.getSize().x*1000)/(double)(mediaLength));
 	   	timeBarViewer.setAdjustMinMaxDatesByModel(false);
		
	    timeBarViewer.setRowHeight(40);  
	    
	    timeBarIntervalRenderer = new TimeBarIntervalRenderer();
        timeBarViewer.registerTimeBarRenderer(MediaIntervalImpl.class, timeBarIntervalRenderer);
        timeBarViewer.registerTimeBarRenderer(MediaSegmentIntervalImpl.class, new TimeBarSegmentIntervalRenderer());

        this.addTimeScaleBar(this);
		   
        registerIntervalModifier();
	    registerChangeListener(timeBarViewer);
	    registerMediaPlayingListener();
	    registerSizeUpdate();
	    this.addMarkers();
	    
	    this.setVisible(true);  
	    
	    if (this.collectionNode.getResource().getCollectionMediaList().size()>0){
	    	lock = true;
	    	this.mediaControlBar.setLockButtonEnabled(true);
	    	
	    }
	    else
	    {
	    	lock = false;
	    	this.mediaControlBar.setLockButtonEnabled(false);
	    }
	    
	    updateMediaClips();
	    
	    this.timeBarViewer.getModel().addTimeBarModelListener(new TimeBarModelListener() {
			
			public void rowRemoved(TimeBarModel arg0, TimeBarRow arg1) {
				// TODO Auto-generated method stub
				
			}
			
			public void rowDataChanged(TimeBarModel arg0, TimeBarRow arg1) {
				// TODO Auto-generated method stub
				
			}
			
			public void rowAdded(TimeBarModel arg0, TimeBarRow arg1) {
				// TODO Auto-generated method stub
				
			}
			
			public void modelDataChanged(TimeBarModel arg0) {
				// TODO Auto-generated method stub
				
			}
			
			public void headerChanged(TimeBarModel arg0, TimeBarRow arg1, Object arg2) {
				// TODO Auto-generated method stub
				
			}
			
			public void elementRemoved(TimeBarModel arg0, TimeBarRow arg1, Interval arg2) {
				if (arg2 instanceof MediaIntervalImpl) {
					MediaTimeBar.this.removeMedia(((MediaIntervalImpl) arg2).getCollectionMedia());
				} else if (arg2 instanceof MediaSegmentIntervalImpl) {
					MediaTimeBar.this.removeMediaClip(((MediaSegmentIntervalImpl) arg2).getCollectionMediaClip());
				} 
				
			}
			
			public void elementChanged(TimeBarModel arg0, TimeBarRow arg1, Interval arg2) {
				if (arg2 instanceof MediaIntervalImpl) {
					MediaTimeBar.this.collectionNode.updateMedia(((MediaIntervalImpl) arg2).getCollectionMedia());
				}
			}
			
			public void elementAdded(TimeBarModel arg0, TimeBarRow arg1, Interval arg2) {
				// TODO Auto-generated method stub
				
			}
		});
	}
		
	protected void showDurationBar(boolean showDurationBar){
		this.isMediaDurationBarOn = showDurationBar;
	}
	
	protected void registerChangeListener(TimeBarViewer tbv) {

        tbv.addTimeBarChangeListener(new ITimeBarChangeListener() {
            public void intervalChangeCancelled(TimeBarRow row, Interval interval) {}

            public void intervalChangeStarted(TimeBarRow row, Interval interval) {        
            	isMarkerDragging = true;
            }

            public void intervalChanged(TimeBarRow row, Interval interval, JaretDate oldBegin, JaretDate oldEnd) {
                isMarkerDragging = false;
                if (interval instanceof MediaIntervalImpl) {
                	updateDuration();
                	updateMediaListener(getTime());
                	updatePixelsPerSecond();
                	MediaTimeBar.this.collectionNode.updateMedia(((MediaIntervalImpl) interval).getCollectionMedia());
                }
                else if (interval instanceof MediaSegmentIntervalImpl)
                {
                	updateMediaClips((MediaSegmentIntervalImpl) interval);
                	MediaTimeBar.this.collectionNode.findCollectionMediaClipNode(((MediaSegmentIntervalImpl) interval).getCollectionMediaClip()).updateResource();
                }
                
            }

            public void intervalIntermediateChange(TimeBarRow row, Interval interval, JaretDate oldBegin,
                    JaretDate oldEnd) {
                System.out.println("CHANGE INTERMEDIATE " + row + " " + interval); // TODO removed?
               
                if (interval instanceof MediaIntervalImpl){
                	if (interval.getBegin().getDate().getTime()-MEDIA_START_TIME.copy().getDate().getTime()<=0){
    					interval.setBegin(MEDIA_START_TIME.copy()); 
    					interval.setEnd(MEDIA_START_TIME.copy().advanceMillis(((AbstractMedia)(MediaTimeBar.this._mediaMap.get(((MediaIntervalImpl)interval).getCollectionMedia().getId()))).getDuration()));
    				}
                }
                else if (interval instanceof MediaSegmentIntervalImpl){
                	if (interval.getBegin().getDate().getTime()-MEDIA_START_TIME.copy().getDate().getTime()<=0){
    					interval.setBegin(oldBegin);
    					interval.setEnd(oldEnd);    					
    				}
                	else{
                		if (interval.getBegin().getMillisInDay()!=oldBegin.getMillisInDay()){
                			long value = interval.getBegin().copy().getDate().getTime()-MEDIA_START_TIME.copy().getDate().getTime();
        					setTime(value);
                		}
                		else {
                			long value = interval.getEnd().copy().getDate().getTime()-MEDIA_START_TIME.copy().getDate().getTime();
        					setTime(value);
                		}
                		
                	}
                	
                	
                }
                
            }

            public void markerDragStarted(TimeBarMarker marker) {
                System.out.println("Marker drag started "+marker);
                isMarkerDragging = true;
                stopMedia();
                mediaControlBar.setPlayButtonEnabled(true);
            }

            public void markerDragStopped(TimeBarMarker marker) {
                System.out.println("Marker drag stopped "+marker);
                isMarkerDragging = false;  
                updateMediaListener(getTime());
                if (mediaStatus){
                	playMedia();
                    mediaControlBar.setPlayButtonEnabled(false);
                }
                else {
                	stopMedia();
                    mediaControlBar.setPlayButtonEnabled(true);
                }

            }

        });
        
    }

	protected void registerIntervalModifier(){
		timeBarViewer.addIntervalModificator(new DefaultIntervalModificator(){

			@Override
			public boolean isShiftingAllowed(TimeBarRow row, Interval interval) {
				if ( lock )
					return false;
				else
					return true;
				
			}

			@Override
			public boolean isSizingAllowed(TimeBarRow row, Interval interval) {
				if ( lock ) return false;
				if (interval instanceof MediaIntervalImpl)
					return false;
				else
					return true;
			}

			@Override
			public boolean newBeginAllowed(TimeBarRow row, Interval interval,
					JaretDate newBegin) {
				if ( lock ) return false;
				if (interval instanceof MediaIntervalImpl)
					return false;
				else
					return true;
			}

			@Override
			public boolean newEndAllowed(TimeBarRow row, Interval interval,
					JaretDate newEnd) {
				if ( lock ) return false;
				if (interval instanceof MediaIntervalImpl)
					return false;
				else
					return true;
			}

			@Override
			public boolean shiftAllowed(TimeBarRow row, Interval interval,
					JaretDate newBegin) {
				
				if ( lock )
					return false;
				else
					return true;
			}
			
			
			
		});
	}
	
	protected void registerMediaPlayingListener(){

		//add media playing listenr
	    final Display display = Display.getCurrent();	 
	    this.addCollectionMediaListener(new CollectionMediaListener(){
	    	public void mediaPlaying(final long currentTime){
	    		display.asyncExec(new Runnable(){
	    			public void run() {
	    				
	    				if (isMarkerDragging) return;
	    				
	    				JaretDate newDate = MEDIA_START_TIME.copy().advanceMillis(currentTime);
	    				timeMarker.setDate(newDate.advanceMillis(_currentListenedMedia.getOffSet()));	
	    				    				
	    				int markerTime = (int) (currentTime+_currentListenedMedia.getOffSet());    				
	    				setTimeRange(markerTime);	
	    				
	    			}				
	    		});	
	    	}

			public void mediaCollectionChanged(MediaListEvent arg) {
				updateTimeBar();
				
			}

			public void MediaClipChanged() {
				
				
			}

	    });
	}
	
	private void updateTimeBar(){
		
		long mediaLength = initialMediaLenght;
		if (!isEmpty()){
			mediaLength = getDuration();
		}
		else{
			return;
		}
		timeBarViewer.setMaxDate(MEDIA_START_TIME.copy().advanceMillis(mediaLength));
		timeBarViewer.setMinDate(MEDIA_START_TIME.copy());
		timeBarViewer.setStartDate(MEDIA_START_TIME.copy());
		updatePixelsPerSecond();			
		buildIntervals();
		syncMedia();
	}
	
	protected void timeBarScaled(){
		 if (this.collectionNode.getResource().getCollectionMediaList().isEmpty()) return;
		 long markerTime = timeMarker.getDate().getDate().getTime() - MEDIA_START_TIME.copy().getDate().getTime();
		 setTimeRange(markerTime);        	
	}
	
	private void addMarkers(){
		//add Marker
	    timeMarker = new MediaTimeBarMarkerImpl(true, new JaretDate().setTime(0, 0, 0));
	    timeMarker.setDescription("Time slider bar marker");
	    timeMarker.setDraggable(true);
	    timeMarker.addTimeBarMarkerListener(new TimeBarMarkerListener(){
	     
			public void markerDescriptionChanged(TimeBarMarker arg0, String arg1, String arg2) {}
			public void markerMoved(TimeBarMarker arg0, JaretDate arg1, JaretDate arg2) {
				if (isMarkerDragging){

					if (MediaTimeBar.this.collectionNode.getResource().getCollectionMediaList().isEmpty()) return;
					long value = arg2.copy().getDate().getTime()-MEDIA_START_TIME.copy().getDate().getTime();
					setTime(value);
					
					
					long medialength = getDuration();
					long markerTime = value;
					if (markerTime<0){
						markerTime=0;
						timeMarker.setDate(MEDIA_START_TIME.copy());
						System.out.println("marker time <0");
					}
					else if (markerTime>medialength){
						markerTime=medialength;
						timeMarker.setDate(MEDIA_START_TIME.copy().advanceMillis(medialength));
						System.out.println("marker time > max");
					}
					
					setTimeRange(markerTime);	
				
					
					
				}
				
				
			}    	
	    });
	    timeBarViewer.addMarker(timeMarker);
	}
	
	private void setTimeRange(long markerTime){
		
		long timeSpanForCurrentTimeScale = getTimeSpanForCurrentTimeScale();			
		JaretDate currentDispalyStartDate = timeBarViewer.getStartDate().copy();
		long currentDisplayStartTime = currentDispalyStartDate.getDate().getTime() - MEDIA_START_TIME.copy().getDate().getTime();
		long currentDisplayEndTime = timeBarViewer.getEndDate().getDate().getTime() - MEDIA_START_TIME.copy().getDate().getTime();
		long length = getDuration();
		
		//marker go over the right side
		if (markerTime-currentDisplayStartTime-timeSpanForCurrentTimeScale>=0){
	
			if (markerTime+timeSpanForCurrentTimeScale/2<length)
				this.displayMarkerAtCenter(markerTime);
			else
				timeBarViewer.setStartDate(MEDIA_START_TIME.copy().advanceMillis(length-timeSpanForCurrentTimeScale));
		}
		//marker go over the left side
		else if ((markerTime)<=currentDisplayStartTime){
			if (markerTime-timeSpanForCurrentTimeScale/2>0)
				this.displayMarkerAtCenter(markerTime);
			else
				timeBarViewer.setStartDate(MEDIA_START_TIME.copy());
		}
		
		if (currentDisplayEndTime>length){
			timeBarViewer.setStartDate(MEDIA_START_TIME.copy().advanceMillis(length-timeSpanForCurrentTimeScale));

		}
	}
	
	private void syncMedia(){
		if (isEmpty()) 
			timeMarker.setDate(MEDIA_START_TIME.copy());
		for (CollectionMedia media: this.collectionNode.getResource().getCollectionMediaList()){
			((AbstractMedia)(this._mediaMap.get(media.getId()))).setTime((int) (timeMarker.getDate().getMillisInDay()-media.getOffSet()));
		}
	}
	
	protected void updateIntervals(){
		
		DefaultTimeBarRowModel timeBarRow = (DefaultTimeBarRowModel)(timeBarViewer.getModel().getRow(0));
		if (!this.isMediaDurationBarOn) return;
		
		for (Interval interval: timeBarRow.getIntervals()){
			MediaIntervalImpl mediaIntervalImpl = (MediaIntervalImpl)interval;
			mediaIntervalImpl.setBegin(MEDIA_START_TIME.copy().advanceMillis(mediaIntervalImpl.getCollectionMedia().getOffSet()));
			mediaIntervalImpl.setEnd(MEDIA_START_TIME.copy().advanceMillis(mediaIntervalImpl.getCollectionMedia().getOffSet()+((AbstractMedia)(this._mediaMap.get(mediaIntervalImpl.getCollectionMedia().getId()))).getDuration()));
			mediaIntervalImpl.setLabel(((AbstractMedia)(this._mediaMap.get(mediaIntervalImpl.getCollectionMedia().getId()))).getName());
		}
		
	}
	
	protected void buildIntervals(){
		DefaultTimeBarRowModel timeBarRow = (DefaultTimeBarRowModel)(timeBarViewer.getModel().getRow(0));
		timeBarRow.clear();
		if (!this.isMediaDurationBarOn) return;
		for (CollectionMedia media:this.collectionNode.getResource().getCollectionMediaList()){		
			final MediaIntervalImpl interval = new MediaIntervalImpl(timeBarViewer, media, timeBarRow,  (AbstractMedia)(this._mediaMap.get(media.getId())));
			interval.setBegin(MEDIA_START_TIME.copy().advanceMillis(media.getOffSet()));
			interval.setEnd(MEDIA_START_TIME.copy().advanceMillis(media.getOffSet()+((AbstractMedia)(this._mediaMap.get(media.getId()))).getDuration()));
			interval.setLabel(((AbstractMedia)(this._mediaMap.get(media.getId()))).getName());
			timeBarRow.addInterval(interval);
			
		}
	}
	
	protected void updateMediaClips(MediaSegmentIntervalImpl mediaSegmentInterval){
		for (CollectionMediaClip segment: this.collectionNode.getResource().getCollectionMediaClipList()){
			if (segment.getClipName().equals(mediaSegmentInterval.getLabel())){
				segment.setStartOffset((int) (mediaSegmentInterval.getBegin().getDate().getTime()-MEDIA_START_TIME.copy().getDate().getTime()));
				segment.setDuration((int) (mediaSegmentInterval.getEnd().getDate().getTime()-mediaSegmentInterval.getBegin().getDate().getTime()));
			}
		}
		
		for (CollectionMediaListener l:listeners){	
			l.MediaClipChanged();
		}
	}
	
	protected void updateMediaClips(){
		DefaultTimeBarRowModel timeBarRow = (DefaultTimeBarRowModel)(timeBarViewer.getModel().getRow(1));
		timeBarRow.clear();

		for (CollectionMediaClip clip:this.collectionNode.getResource().getCollectionMediaClipList()){		
			MediaSegmentIntervalImpl interval = new MediaSegmentIntervalImpl(MEDIA_START_TIME.copy().advanceMillis(clip.getStartOffset()), MEDIA_START_TIME.copy().advanceMillis(clip.getStartOffset()+clip.getDuration()), clip, timeBarRow);
			interval.setLabel(clip.getClipName());
			timeBarRow.addInterval(interval);
		}
		
		
	}
	
	private void displayMarkerAtCenter(long markerTime){
		timeBarViewer.setStartDate(MEDIA_START_TIME.copy().advanceMillis(markerTime-getTimeSpanForCurrentTimeScale()/2));
	}
	
	protected void addTimeScaleBar(Composite parent){
		 timeScaleControlPanel = new TimeScaleBar(parent, SWT.NULL, (double)(300)/(double)this.initialMediaLenght);
		    timeScaleControlPanel.addTimeScaleListener(new TimeScaleListener(){
				 public void timeScaleChanged(double newPixelsPerSecond) {
					 
					 timeBarViewer.setPixelPerSecond(newPixelsPerSecond);	
					 timeBarViewer.setStartDate(MEDIA_START_TIME.copy());
					 timeBarViewer.setMinDate(MEDIA_START_TIME.copy());
					 timeBarScaled();
				 }
					
		     });
	}
		
	protected void addMediaControlBar(Composite parent){
		mediaControlBar=new MediaControlBar(parent, SWT.NULL);
		mediaControlBar.addMediaControlListener(new MediaControlListener(){
			public void play() {
				playMedia();	
				mediaStatus = true;
			}

			public void stop() {
				stopMedia();	
				mediaStatus = false;
			}

			public void setMute(boolean mute) {
				setMute(mute);
				
			}

			public void setLock(boolean lockStatus) {
				
				lock = lockStatus;
			}
			
		});
	}
	

	
	protected void updatePixelsPerSecond(){
		if (isEmpty()) return;	
		long mediaLength=initialMediaLenght;	//TODO if media is empty all rows and intervals should be removed?
		if (getDuration()>0){
			mediaLength = getDuration();
		}

		timeBarViewer.setMaxDate(MEDIA_START_TIME.copy().advanceMillis(mediaLength));
		if (timeBarViewer.getSize().x>0) {
			timeBarViewer.setPixelPerSecond((double)(timeBarViewer.getSize().x*1000)/(double)(mediaLength));
			timeScaleControlPanel.setPixelsPerSecond((double)(timeBarViewer.getSize().x*1000)/(double)(mediaLength));
		}

	}
	
	protected void registerSizeUpdate(){
		timeBarViewer.addControlListener(new ControlListener(){

			public void controlMoved(ControlEvent e) {}

			public void controlResized(ControlEvent e) {
				updatePixelsPerSecond();
				updateIntervals();
				updateRowHeight();
			}
			
		});
	}
	
	protected long getTimeSpanForCurrentTimeScale(){
		return (long) ((double)(1000*timeBarViewer.getBounds().width)/(double)(timeBarViewer.getPixelPerSecond()));
	}
	
	protected void updateRowHeight(){
		if (timeBarViewer.getSize().y>50){
			int rowsAreaHeight = timeBarViewer.getSize().y-50;
			if (timeBarViewer.getModel().getRowCount()>1){
				timeBarViewer.setRowHeight(rowsAreaHeight/2); 
			}
				
			else
				timeBarViewer.setRowHeight(rowsAreaHeight); 
		}
	}
	
	public interface TimeSlideListener {
	}

	
		
}