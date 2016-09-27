package com.dyalog.apldev.debug.core.util;

import org.eclipse.core.resources.IWorkspaceRoot;

public class FileUtilities {
//	public static FileEditorInput getFileEditorInput(IPath fPath)
//    {
//        IWorkspaceRoot root = PerlEditorPlugin.getWorkspace().getRoot();
//
//        try
//        {
//            IFile[] files = root.findFilesForLocation(fPath);
//    		if (files.length > 0) return new FileEditorInput(files[0]); // found
//
//            // not found, let's create a link to its parent folder
//            // and search again
//            createFolderLink(fPath, getEpicLinksProject(root));
//    
//    		files = root.findFilesForLocation(fPath);    
//            if (files.length > 0) return new FileEditorInput(files[0]); // found
//            
//            // we have the link and the file still can't be found??
//            throw new CoreException(new Status(
//                IStatus.ERROR,
//                PerlEditorPlugin.getPluginId(),
//                IStatus.OK,
//                fPath.toOSString() + " could not be found through epic-links", 
//                null));
//        }
//        catch (CoreException e)
//        {
//            IStatus[] status;
//            IPath folderPath = fPath.removeLastSegments(1);
//            
//            if (root.getLocation().isPrefixOf(folderPath) ||
//                folderPath.isPrefixOf(root.getLocation()))    
//            {
//                status = new IStatus[] {
//                    e.getStatus(),
//                    new Status(
//                        IStatus.ERROR,
//                        PerlEditorPlugin.getPluginId(),
//                        IStatus.OK,
//                        "EPIC cannot access files located in folders on the path " +
//                        "to the workspace folder, nor within the workspace folder itself.",
//                        null)
//                    };
//            }
//            else
//            {
//                status = new IStatus[] { e.getStatus() };   
//            }
//            
//            PerlEditorPlugin.getDefault().getLog().log(
//                new MultiStatus(
//                    PerlEditorPlugin.getPluginId(),
//                    IStatus.OK,
//                    status,
//                    "An unexpected exception occurred while creating a link to " +
//                    fPath.toString(),
//                    e));
//            
//            // TODO: propagate this exception and/or update client code
//            return null; 
//        }
//	}

}
