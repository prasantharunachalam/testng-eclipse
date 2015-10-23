package org.testng.eclipse.util;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.util.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;


/**
 * Class usage XXX
 * 
 * @version $Revision$
 */
public class SWTUtil {
	private SWTUtil(){}

	public static void setButtonGridData(Button button) {
		GridData gridData= new GridData();
		button.setLayoutData(gridData);
		setButtonDimensionHint(button);
	}
	
	/**
	 * Returns a width hint for a button control.
	 */
	public static int getButtonWidthHint(Button button) {
		button.setFont(JFaceResources.getDialogFont());
		PixelConverter converter= new PixelConverter(button); // FIXME
		int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
	}
	
	public static void setButtonDimensionHint(Button button) {
		Assert.isNotNull(button);
		Object gd= button.getLayoutData();
		if (gd instanceof GridData) {
			((GridData)gd).widthHint= getButtonWidthHint(button);		 
		}
	}

	public static Display getDisplay() {
		Display display= Display.getCurrent();
		if (display == null) {
			display= Display.getDefault();
		}
		return display;
	}

	/**
	 * Returns the active workbench window
	 * 
	 * @return the active workbench window
	 */
	public static IWorkbenchWindow getActiveWorkbenchWindow(IWorkbench workBench) {
		if(null == workBench) {
			return null;
		}
		
		return workBench.getActiveWorkbenchWindow();
	}		
	
	public static IWorkbenchPage getActivePage(IWorkbench workBench) {
		IWorkbenchWindow activeWorkbenchWindow = getActiveWorkbenchWindow(workBench);
		
		if(null == activeWorkbenchWindow) {
			return null;
		}
		
		return activeWorkbenchWindow.getActivePage();
	}

  /**
   * Create a container with a GridData layout and columns.
   */
  public static Composite createGridContainer(Composite parent, int columns) {
    Composite result = new Composite(parent, SWT.NULL);
    createGridLayout(result, columns);
    return result;
  }
  
  public static void createGridLayout(Composite result, int columns) {
    GridLayout layout = new GridLayout();
    layout.numColumns = columns;
    result.setLayout(layout);

    GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    result.setLayoutData(gd);
  }

  /**
   * @return a Text field that contains a path. The file system can be browsed by pressing
   * the "Browse" button.
   */
  public static Text createPathBrowserText(final Composite container, String text,
      ModifyListener listener) {
    final Text result = createLabelText(container, text, listener);
    Button button = new Button(container, SWT.PUSH);
    button.setText("Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ContainerSelectionDialog dialog = new ContainerSelectionDialog(container.getShell(),
            ResourcesPlugin.getWorkspace().getRoot(), false, "Select new file container");
        dialog.showClosedProjects(false);
        if (dialog.open() == ContainerSelectionDialog.OK) {
          Object[] res = dialog.getResult();
          if (res.length == 1) {
            result.setText(((Path) res[0]).toString());
          }
        }
      }
    });
    return result;
  }

  /**
   * @return a Label+Text.
   */
  public static Text createLabelText(Composite container, String text, ModifyListener listener) {
    Label label = new Label(container, SWT.NULL);
    label.setText(text);
    final Text result = new Text(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    result.setLayoutData(gd);
    if (listener != null) result.addModifyListener(listener);

    return result;
  }

  public static GridData createGridData() {
    return new GridData(SWT.FILL, SWT.TOP, true, false);
  }
  
  public static Text createFileBrowserText(final Group group, final Composite container, String text,
      ModifyListener listener) {
    final Text result = createLabelText(group, text, listener);
    Button button = new Button(group, SWT.PUSH);
    button.setText("Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ResourceFileSelectionDialog dialog = new ResourceFileSelectionDialog("Test Class Dependency File Selection", "Select any dependent Java file required for the Test Class", new String[] { "java" });
        if (dialog.open() == dialog.OK.getCode()) {
          Object[] res = dialog.getResult();
          if (res.length >= 1) {
            result.setText(((File) res[0]).toString().trim());
          }
        }
      }
    });
    return result;
  }  
  
  public static Combo createFileBrowserCombo(final Group group, final Composite container, String text,
      ModifyListener listener) {
    final Combo result = createCombo(group, text, listener);
    Button button = new Button(group, SWT.PUSH);
    button.setText("Browse...");
    button.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        ResourceFileSelectionDialog dialog = new ResourceFileSelectionDialog("Return Type Selection", "Select any dependent Java file required for ReturnType", new String[] { "java" });
        if (dialog.open() == dialog.OK.getCode()) {
          Object[] res = dialog.getResult();
          if (res.length >= 1) {
            result.setText(((File) res[0]).toString().trim());
          }
        }
      }
    });
    return result;
  }  
  
  /**
   * @return a Label+Text.
   */
  public static Combo createCombo(Composite container, String text, ModifyListener listener) {
    Label label = new Label(container, SWT.NULL);
    label.setText(text);      
    final Combo result = new Combo(container, SWT.BORDER | SWT.SINGLE);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 3;
    result.setLayoutData(gd);
    if (listener != null) result.addModifyListener(listener);
    return result;
  }  
  
  public static String getJavaClassNameFromFullPath(String source){
      int startIndex = source.lastIndexOf("/");
      int lastIndex = source.lastIndexOf(".");
//    if (!(startIndex == -1 || lastIndex == -1)){
      return source.substring(startIndex + 1, lastIndex);
//    }
//    return source;w
  }
  
  public static String getJavaPackageNameFromFullPath(String source){
    int startIndex = source.lastIndexOf("src/");
    int lastIndex = source.lastIndexOf("/");
    return source.substring(startIndex+1, lastIndex).replaceAll("/", ".");
  }  
  
  public static String getPackageNameFromFullPath(String className) {
    int startIndex1 = className.lastIndexOf("src/main/");
    int startIndex2 = className.lastIndexOf("src/");
    int lastIndex = className.lastIndexOf(".");
    int startIndex = (startIndex1 == -1 ? startIndex2 + 4 : startIndex1 + 9);
    return className.substring(startIndex, lastIndex).replaceAll("/", ".");
  }  
  
}
