package org.testng.eclipse.wizards;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.testng.eclipse.ui.util.Utils;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.SuiteGenerator;
import org.testng.eclipse.util.Utils.JavaElement;
import static org.testng.eclipse.wizards.WizardConstants.*;

/**
 *
 * @author sairam
 */
public class NewDependencyClassWizard extends Wizard implements INewWizard {
	private NewDependencyClassWizardPage m_page;
//  private TestNGMethodWizardPage m_methodPage;

	/**
	 * Constructor for NewTestNGClassWizard.
	 */
	public NewDependencyClassWizard() {
		super();
		setNeedsProgressMonitor(true);
	}

	/**
	 * Adding the pages to the wizard.
	 */
	@Override
  public void addPages() {
		m_page = new NewDependencyClassWizardPage();
		addPage(m_page);
	}

	/**
	 * This method is called when 'Finish' button is pressed in
	 * the wizard. We will create an operation and run it
	 * using wizard as execution context.
	 */
	@Override
  public boolean performFinish() {
	  
	   //set Map for method ui controls with impl
    m_page.setMethodSignatures();
	  
		String containerName = m_page.getSourceFolder();
		String className = m_page.getClassName();
		String packageName = m_page.getPackageName();
//		List<IMethod> methods = m_methodPage != null
//		    ? m_methodPage.getSelectedMethods() : Collections.<IMethod>emptyList();
		    List<String> methods = null;
    try {
      return doFinish(containerName, packageName, className, /*m_page.getXmlFile()*/null, methods,
          new NullProgressMonitor());
    } catch (CoreException e) {
      e.printStackTrace();
    }
		return true;
	}
	
	/**
	 * The worker method. It will find the container, create the
	 * file(s) if missing or just replace its contents, and open
	 * the editor on the newly created file.
	 *
	 * @return true if the operation succeeded, false otherwise.
	 */
	private boolean doFinish(String containerName, String packageName, String className,
	    String xmlPath, /*List<IMethod> methods,*/ List<String> methods, IProgressMonitor monitor) throws CoreException {
	  boolean result = true;

	  //
	  // Create Java file
	  //
	  if (result) {
  	  IFile file = createFile(containerName, packageName, className + ".java",
          createJavaContentStream(className, methods), monitor);
  	  if (file != null) org.testng.eclipse.util.Utils.openFile(getShell(), file, monitor);
  	  else result = false;
	  }

	  return result;
	}

  private IFile createFile(String containerName, String packageName, String fileName,
	    InputStream contentStream, IProgressMonitor monitor) throws CoreException {
    monitor.beginTask("Creating " + fileName, 2);
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    String fullPath = fileName;
    if (packageName != null && ! "".equals(packageName)) {
      fullPath = packageName.replace(".", File.separator) + File.separatorChar + fileName;
    }
    Path absolutePath = new Path(containerName + File.separatorChar + fullPath);
    final IFile result = root.getFile(absolutePath);
    Utils.createFileWithDialog(getShell(), result, contentStream);

    return result;
	}

	/**
	 * Create the content for the Java file.
	 * @param testMethods 
	 */
	private InputStream createJavaContentStream(String className, /*List<IMethod> testMethods,*/ List<String> implMethods) {
	  StringBuilder imports = new StringBuilder("");
	  StringBuilder methods = new StringBuilder();
	  String dataProvider = "";
	  String signature = "()";


	  //
	  // Test methods
	  //
	  Set<String> overloadedMethods = Sets.newHashSet();
	  Set<String> temp = Sets.newHashSet();
	  Map<Integer, Map<String, String>> map =  m_page.getMethodSignature();
	  for(int i = 1 ; i <= m_page.getAtomicInteger().get(); i++){
	    Map<String, String> obj = map.get(i);
	    if(obj != null){
        methods.append("\n"
            + TAB + TAB
            + obj.get(METHOD_MODIFIER)+SPACE
            + (!StringUtils.isEmptyString(obj.get(METHOD_STATIC))?obj.get(METHOD_STATIC)+SPACE:EMPTY)
            + (!StringUtils.isEmptyString(obj.get(METHOD_FINAL))?obj.get(METHOD_FINAL)+SPACE:EMPTY)
            +obj.get(METHOD_RETURN_TYPE)+ SPACE
            + obj.get(METHOD_NAME)+SPACE
            + OPEN_BRACE
            + (!StringUtils.isEmptyString(obj.get(METHOD_PARAMS_TYPE)) ? obj.get(METHOD_PARAMS_TYPE)+SPACE : EMPTY)
            + CLOSE_BRACE    
            + (!StringUtils.isEmptyString(obj.get(METHOD_THROWS_CLAUSE)) ? SPACE+obj.get(METHOD_THROWS_CLAUSE) : EMPTY)
            + TAB+"{\n"
            + TAB+TAB+TAB+TAB+"throw new RuntimeException(\"Method not implemented\");\n"
            + TAB+TAB+"}\n");	    
	    }
	  }
/*	  
	  for (IMethod m : testMethods) {
	    String name = m.getElementName();
      if (temp.contains(name)) overloadedMethods.add(name);
	    temp.add(name);
	  }

    for (IMethod m : testMethods) {
      methods.append("\n"
          + "  @Test\n"
          + "  public void " + createSignature(m, overloadedMethods) + " {\n"
          + "    throw new RuntimeException(\"Test not implemented\");\n"
          + "  }\n");
    }
*/
    String contents =
	      "package " + m_page.getPackage() + ";\n\n"
	      + imports
	      + "\n"
	      + "public class " + className + " {\n"
	      ;
/*
    if (testMethods.size() == 0 || ! StringUtils.isEmptyString(dataProvider)) {
      contents +=
          "  @Test" + dataProvider + "\n"
  	      + "  public void f" + signature + " {\n"
  	      + "  }\n";
    }
*/
    contents += methods + "}\n";

	  return new ByteArrayInputStream(contents.getBytes());
	}

	/**
	 * @return a suitable signature, possible with its name mangled if it's
	 * overloaded in the class (e.g foo() -> foo(), foo(Integer) -> fooInteger()).
	 */
  private String createSignature(IMethod m, Set<String> overloadedMethods) {
    String elementName = m.getElementName();
    StringBuilder result = new StringBuilder(elementName);
    if (overloadedMethods.contains(elementName)) {
      for (String type : m.getParameterTypes()) {
        result.append(sanitizeSignature(Signature.toString(type)));
      }
    }
    result.append("()");
    return result.toString();
  }

  /**
   * @return a string that can be used as a method name.
   */
  private String sanitizeSignature(String string) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < string.length(); i++) {
      char c = string.charAt(i);
      if (Character.isJavaIdentifierPart(c)) {
        result.append(c);
      }
    }

    return result.toString();
  }


	private String toMethod(String a) {
    return Character.toLowerCase(a.charAt(0)) + a.substring(1);
  }

	/**
	 * We will accept the selection in the workbench to see if
	 * we can initialize from it.
	 * @see IWorkbenchWizard#init(IWorkbench, IStructuredSelection)
	 */
  public void init(IWorkbench workbench, IStructuredSelection selection) {
	}

}