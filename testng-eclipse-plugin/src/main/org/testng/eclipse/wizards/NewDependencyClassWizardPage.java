package org.testng.eclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import static org.testng.eclipse.wizards.WizardConstants.*;
/**
 * Generate a new TestNG class and optionally, the corresponding XML suite file.
 */
public class NewDependencyClassWizardPage extends WizardPage {
  private Text m_sourceFolderText;
  private Text m_packageNameText;
  private Text m_classNameText;

  private List<JavaElement> m_elements;
  
  //Add enhancement
  private org.eclipse.swt.widgets.Combo modifierNames;
  private org.eclipse.swt.widgets.Combo m_returnTypeText;
//  private Text m_returnTypeText;
  private Text m_methodNameText;
  private Text m_methodParamsText;  
  public static final String[] MODIFIERS = new String[] {
      "public", "private", "protected", "package"
    }; 
  private Map<Integer, Map<String, String>> m_methodSignature = new HashMap<>();  
  private AtomicInteger atomicInteger = new AtomicInteger(1);
  private Button b_static;
  private Button b_final;
  private Button b_throws;
  public static final String[] RETURN_TYPES = new String[] {
      "void", "Integer", "Double", "Object", "Boolean"
    };  

  public NewDependencyClassWizardPage() {
    super(ResourceUtil.getString("NewDependencyClassWizardPage.title"));
    setTitle(ResourceUtil.getString("NewDependencyClassWizardPage.title"));
    setDescription(ResourceUtil.getString("NewDependencyClassWizardPage.description"));
  }

  /**
   * @see IDialogPage#createControl(Composite)
   */
  public void createControl(Composite parent) {
    Composite container = new Composite(parent, SWT.NULL);
    GridLayout layout = new GridLayout();
    layout.numColumns = 1;
    container.setLayout(layout);

    createTop(container);
    //Test methods section
    createMethod(container);
    
    initialize();
    dialogChanged();
    setControl(container);
  }

  private void createTop(Composite parent) {
    final Composite container = SWTUtil.createGridContainer(parent, 3);

    //
    // Source folder
    //
    {
      m_sourceFolderText = SWTUtil.createPathBrowserText(container, "&Source folder:",
          new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              dialogChanged();
            }
          });
    }

    //
    // Package name
    //
    {
      Label label = new Label(container, SWT.NULL);
      label.setText("&Package name:");
      m_packageNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
      m_packageNameText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          dialogChanged();
        }
      });
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      m_packageNameText.setLayoutData(gd);
      Button button = new Button(container, SWT.PUSH);
      button.setText("Browse...");
      button.addSelectionListener(new SelectionAdapter() {
        public void widgetSelected(SelectionEvent e) {
          handleBrowsePackages(container.getShell());
        }
      });
    }

    //
    // Class name
    //
    {
      Label label = new Label(container, SWT.NULL);
      label.setText("&Class name:");
      m_classNameText = new Text(container, SWT.BORDER | SWT.SINGLE);
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      m_classNameText.setLayoutData(gd);
      m_classNameText.addModifyListener(new ModifyListener() {
        public void modifyText(ModifyEvent e) {
          dialogChanged();
        }
      });
    }
  }


  private void createMethod(Composite parent) {
    {
      createTestGroupSection(parent);
    }
    
  }
  
  private Composite createTestGroupSection(final Composite parent){
    
    Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
    g.setText("Method Signature");  
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    //GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    //gd.verticalSpan = 2;      
    g.setLayoutData(gd);
    
    GridLayout layout = new GridLayout();
    g.setLayout(layout);
    layout.numColumns = 16;     
    
    b_static = new Button(g, SWT.CHECK);
    b_static.setText("static");
    b_static.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
          dialogChanged();
      }
    });    
    
    b_final = new Button(g, SWT.CHECK);
    b_final.setText("final");    
    b_final.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
          dialogChanged();
      }
    });       
    
    Label label1 = new Label(g, SWT.NULL);
    label1.setText("&ModifierName:");   
    modifierNames = new org.eclipse.swt.widgets.Combo(g,
        SWT.BORDER | SWT.SINGLE);
    for(String modifier : MODIFIERS){
      modifierNames.add(modifier);  
    }
    GridData modifierName = new GridData(GridData.FILL_HORIZONTAL);
    modifierNames.setLayoutData(modifierName);  
    modifierNames.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        dialogChanged();
      }
    });       
    
    Label label2 = new Label(g, SWT.NULL);
    label2.setText("&ReturnType:");  
//    m_returnTypeText = new Text(g, SWT.BORDER | SWT.SINGLE);  
    m_returnTypeText = new org.eclipse.swt.widgets.Combo(g,
        SWT.BORDER | SWT.SINGLE);  
    m_returnTypeText.setToolTipText("Please select any of the below Method Return types. If not available, type any valid customType");
    for(String returnType : RETURN_TYPES){
      m_returnTypeText.add(returnType);  
    }    
    GridData grid = new GridData(GridData.FILL_HORIZONTAL);
    grid.horizontalSpan = 2;
    m_returnTypeText.setLayoutData(grid);   
    m_returnTypeText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });    
    
    Label label3 = new Label(g, SWT.NULL);
    label3.setText("&MethodName:");  
    m_methodNameText = new Text(g, SWT.BORDER | SWT.SINGLE);
    GridData methodGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodGrid.horizontalSpan = 2;
    m_methodNameText.setLayoutData(methodGrid);
    m_methodNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });       
    
    Label label4 = new Label(g, SWT.NULL);
    label4.setText("&MethodParams:");   
    m_methodParamsText = new Text(g, SWT.BORDER | SWT.SINGLE);
    GridData methodParamsGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodParamsGrid.horizontalSpan = 2;
    m_methodParamsText.setLayoutData(methodParamsGrid);  
    m_methodParamsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });       
    
    b_throws = new Button(g, SWT.CHECK);
    b_throws.setText("throwsClause");    
    b_throws.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
         dialogChanged();
      }
    });       
    
    Button addMore = new Button(g, SWT.PUSH);
    addMore.setText("Add More Method...");
    addMore.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createMethod(parent);
        parent.layout();
        atomicInteger.addAndGet(1);
      }
      
    });
    return parent;
  }  
  
  /**
   * Tests if the current workbench selection is a suitable container to use.
   */
  private void initialize() {
    m_elements = Utils.getSelectedJavaElements();
    if (m_elements.size() > 0) {
      JavaElement sel = m_elements.get(0);
      if (sel.sourceFolder != null) {
        m_sourceFolderText.setText(sel.sourceFolder);
      }
      if (sel.getPackageName() != null) {
        m_packageNameText.setText(sel.getPackageName());
      }
      String className = sel.getClassName();
//      StringUtils.isEmptyString(sel.getClassName())
//          ? "NewTest" : sel.getClassName() + "Test";
      m_classNameText.setText("" );
    }
  }

  public List<JavaElement> getJavaElements() {
    return m_elements;
  }

  private void handleBrowsePackages(Shell dialogParrentShell) {
    try {
      IResource sourceContainer = ResourcesPlugin.getWorkspace().getRoot().findMember(
          new Path(getSourceFolder()));
      IJavaProject javaProject = JDTUtil.getJavaProject(sourceContainer.getProject().getName());
      
      SelectionDialog dialog = JavaUI.createPackageDialog(dialogParrentShell, javaProject, 0);
      dialog.setTitle("Package selection");
      dialog.setMessage("&Choose a package:");
      
      if (dialog.open() == SelectionDialog.OK) {
        Object[] selectedPackages = dialog.getResult();
        if (selectedPackages.length == 1) {
          m_packageNameText.setText(((IPackageFragment) selectedPackages[0]).getElementName());
        }
      }
    } catch (JavaModelException e) {
      updateStatus("Failed to list packages.");
    }
  }

  /**
   * Ensures that both text fields are set.
   */
  private void dialogChanged() {
    IResource container = ResourcesPlugin.getWorkspace().getRoot().findMember(
        new Path(getSourceFolder()));
    String className = getClassName();

    if (container.getProject() == null || container.getProject().getName() == null || container.getProject().getName().length() == 0) {
      updateStatus("The source folder of an existing project must be specified.");
      return;
    }    
    if (getPackageName().length() == 0) {
      updateStatus("The package must be specified");
      return;
    }
    if (container != null && !container.isAccessible()) {
      updateStatus("Project must be writable");
      return;
    }
    if (className.length() == 0) {
      updateStatus("Class name must be specified");
      return;
    }
    if (className.replace('\\', '/').indexOf('/', 1) > 0) {
      updateStatus("Class name must be valid");
      return;
    }

    int dotLoc = className.lastIndexOf('.');
    if (dotLoc != -1) {
      String ext = className.substring(dotLoc + 1);
      if (ext.equalsIgnoreCase("java") == false) {
        updateStatus("File extension must be \"java\"");
        return;
      }
    }
    //validations for method signature
    if(b_static.getSelection() || b_final.getSelection() || b_throws.getSelection() || !StringUtils.isEmptyString(modifierNames.getText()) 
        || !StringUtils.isEmptyString(m_methodParamsText.getText()) || !StringUtils.isEmptyString(m_returnTypeText.getText()) 
        || !StringUtils.isEmptyString(m_methodNameText.getText())) {
      if(!validateAndSetMethodSignature())
        return;
    }
    /*
    else if(!StringUtils.isEmptyString(m_returnTypeText.getText()) || !StringUtils.isEmptyString(m_methodNameText.getText())){
      validateAndSetMethodSignature();
    }
    */
    updateStatus(null);
  }
  
  private boolean validateAndSetMethodSignature(){
    if(StringUtils.isEmptyString(m_returnTypeText.getText())){
      updateStatus("Method Return Type cannot be empty");
      return false;        
    } 
    if(StringUtils.isEmptyString(m_methodNameText.getText())){
      updateStatus("Method Name cannot be empty");
      return false;        
    }
    
    m_methodSignature.put(atomicInteger.get(), new HashMap<String, String> () {{
      put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
      put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
      put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
      put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
      put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
      put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
      put(METHOD_MODIFIER, PACKAGE.equals(modifierNames.getText())?EMPTY:modifierNames.getText());
    }});    
    
    return true;
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getSourceFolder() {
    return m_sourceFolderText.getText();
  }

  public String getPackageName() {
    return m_packageNameText.getText();
  }

  public String getClassName() {
    return m_classNameText.getText();
  }

  public String getPackage() {
    return m_packageNameText.getText();
  }

  public String getMethodModifierName() {
    return modifierNames.getText();
  }  
  
  public String getMethodReturnType() {
    return m_returnTypeText.getText();
  }
  
  public String getMethodName() {
    return m_methodNameText.getText();
  }
  
  public String getMethodParams() {
    return m_methodParamsText.getText();
  }  
  
  public Map<Integer, Map<String, String>> getMethodSignature(){
    return m_methodSignature;
  }
  
  public AtomicInteger getAtomicInteger(){
    return atomicInteger;
  }
}