package synergyviewcore.projects;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CopyFilesAndFoldersOperation;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorMessages;
import org.eclipse.ui.internal.navigator.resources.plugin.WorkbenchNavigatorPlugin;
import org.eclipse.ui.navigator.CommonDropAdapter;
import org.eclipse.ui.navigator.CommonDropAdapterAssistant;

import synergyviewcore.media.model.MediaRootNode;

/**
 * TODO Fixme see issue 14.
 * 
 * @author phyo
 */
@SuppressWarnings("restriction")
public class ProjectsDropAdapterAssistant extends CommonDropAdapterAssistant {

    /**
     * Instantiates a new projects drop adapter assistant.
     */
    public ProjectsDropAdapterAssistant() {
	//
    }

    /**
     * Returns the actual target of the drop, given the resource under the mouse. If the mouse target is a file, then the drop actually occurs in its parent. If the drop location is before or after the mouse target and feedback is enabled, the target is also the parent.
     * 
     * @param mouseTarget
     *            the mouse target
     * @return the actual target
     */
    private IContainer getActualTarget(IResource mouseTarget) {

	/* if cursor is on a file, return the parent */
	if (mouseTarget.getType() == IResource.FILE) {
	    return mouseTarget.getParent();
	}
	/* otherwise the mouseTarget is the real target */
	return (IContainer) mouseTarget;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#handleDrop(org.eclipse .ui.navigator.CommonDropAdapter, org.eclipse.swt.dnd.DropTargetEvent, java.lang.Object)
     */
    @Override
    public IStatus handleDrop(CommonDropAdapter aDropAdapter, DropTargetEvent aDropTargetEvent, Object aTarget) {
	IStatus status = null;
	if ((aDropAdapter.getCurrentTarget() == null) || (aDropTargetEvent.data == null)) {
	    return Status.CANCEL_STATUS;
	}
	// TransferData currentTransfer = aDropAdapter.getCurrentTransfer();
	// if (FileTransfer.getInstance().isSupportedType(currentTransfer))
	status = performFileDrop(aDropAdapter, aDropTargetEvent.data);
	openError(status);
	IContainer target = getActualTarget(((MediaRootNode) aDropAdapter.getCurrentTarget()).getResource());
	if ((target != null) && target.isAccessible()) {
	    try {
		target.refreshLocal(IResource.DEPTH_ONE, null);
	    } catch (CoreException e) {
		//
	    }
	}
	return status;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#isSupportedType(org .eclipse.swt.dnd.TransferData)
     */
    @Override
    public boolean isSupportedType(TransferData aTransferType) {
	return super.isSupportedType(aTransferType) || FileTransfer.getInstance().isSupportedType(aTransferType);
    }

    /**
     * Adds the given status to the list of problems. Discards OK statuses. If the status is a multi-status, only its children are added.
     * 
     * @param status
     *            the status
     * @param toMerge
     *            the to merge
     */
    private void mergeStatus(MultiStatus status, IStatus toMerge) {
	if (!toMerge.isOK()) {
	    status.merge(toMerge);
	}
    }

    /**
     * Opens an error dialog if necessary. Takes care of complex rules necessary for making the error dialog look nice.
     * 
     * @param status
     *            the status
     */
    private void openError(IStatus status) {
	if (status == null) {
	    return;
	}

	String genericTitle = WorkbenchNavigatorMessages.DropAdapter_title;
	int codes = IStatus.ERROR | IStatus.WARNING;

	// simple case: one error, not a multistatus
	if (!status.isMultiStatus()) {
	    ErrorDialog.openError(getShell(), genericTitle, null, status, codes);
	    return;
	}

	// one error, single child of multistatus
	IStatus[] children = status.getChildren();
	if (children.length == 1) {
	    ErrorDialog.openError(getShell(), status.getMessage(), null, children[0], codes);
	    return;
	}
	// several problems
	ErrorDialog.openError(getShell(), genericTitle, null, status, codes);
    }

    /**
     * Performs a drop using the FileTransfer transfer type.
     * 
     * @param anAdapter
     *            the an adapter
     * @param data
     *            the data
     * @return the i status
     */
    private IStatus performFileDrop(CommonDropAdapter anAdapter, Object data) {

	MultiStatus problems = new MultiStatus(PlatformUI.PLUGIN_ID, 0, WorkbenchNavigatorMessages.DropAdapter_problemImporting, null);
	mergeStatus(problems, validateTarget(anAdapter.getCurrentTarget(), anAdapter.getCurrentTransfer(), anAdapter.getCurrentOperation()));

	final IContainer target = getActualTarget(((MediaRootNode) anAdapter.getCurrentTarget()).getResource());
	final String[] names = (String[]) data;
	// Run the import operation asynchronously.
	// Otherwise the drag source (e.g., Windows Explorer) will be blocked
	// while the operation executes. Fixes bug 16478.
	Display.getCurrent().asyncExec(new Runnable() {
	    public void run() {
		getShell().forceActive();
		CopyFilesAndFoldersOperation operation = new CopyFilesAndFoldersOperation(getShell());
		operation.copyFiles(names, target);
	    }
	});
	return problems;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.ui.navigator.CommonDropAdapterAssistant#validateDrop(java .lang.Object, int, org.eclipse.swt.dnd.TransferData)
     */
    @Override
    public IStatus validateDrop(Object target, int operation, TransferData transferType) {
	if (target instanceof MediaRootNode) {

	    return Status.OK_STATUS;
	} else {
	    return Status.CANCEL_STATUS;
	}
    }

    /**
     * Ensures that the drop target meets certain criteria.
     * 
     * @param target
     *            the target
     * @param transferType
     *            the transfer type
     * @param dropOperation
     *            the drop operation
     * @return the i status
     */

    private IStatus validateTarget(Object target, TransferData transferType, int dropOperation) {
	if (!(target instanceof IResource)) {
	    return WorkbenchNavigatorPlugin.createInfoStatus(WorkbenchNavigatorMessages.DropAdapter_targetMustBeResource);
	}
	IResource resource = (IResource) target;
	if (!resource.isAccessible()) {
	    return WorkbenchNavigatorPlugin.createErrorStatus(WorkbenchNavigatorMessages.DropAdapter_canNotDropIntoClosedProject);
	}
	IContainer destination = getActualTarget(resource);
	if (destination.getType() == IResource.ROOT) {
	    return WorkbenchNavigatorPlugin.createErrorStatus(WorkbenchNavigatorMessages.DropAdapter_resourcesCanNotBeSiblings);
	}
	String message = null;

	if (FileTransfer.getInstance().isSupportedType(transferType)) {
	    String[] sourceNames = (String[]) FileTransfer.getInstance().nativeToJava(transferType);
	    if (sourceNames == null) {
		// source names will be null on Linux. Use empty names to do
		// destination validation.
		// Fixes bug 29778
		sourceNames = new String[0];
	    }
	    CopyFilesAndFoldersOperation copyOperation = new CopyFilesAndFoldersOperation(getShell());
	    message = copyOperation.validateImportDestination(destination, sourceNames);
	}
	if (message != null) {
	    return WorkbenchNavigatorPlugin.createErrorStatus(message);
	}
	return Status.OK_STATUS;
    }

}
