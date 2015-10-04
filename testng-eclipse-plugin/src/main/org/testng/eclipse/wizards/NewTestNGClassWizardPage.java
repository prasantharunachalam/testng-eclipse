package org.testng.eclipse.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.IDialogPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate a new TestNG class and optionally, the corresponding XML suite file.
 */
public class NewTestNGClassWizardPage extends WizardPage {
  private Text m_sourceFolderText;
  private Text m_packageNameText;
  private Text m_classNameText;
  private Text m_xmlFilePath;
  
  //Added for enhancement
  private org.eclipse.swt.widgets.Combo frameWorks;
  private org.eclipse.swt.widgets.Combo modifierNames;
  private Text m_returnTypeText;
  private Text m_methodNameText;
  private Text m_methodParamsText;
  public static final String[] MODIFIERS = new String[] {
      "public", "private", "protected", "package"
    }; 
  private Text m_dependentClassNameText;
  private org.eclipse.swt.widgets.Combo methods;
  private org.eclipse.swt.widgets.Combo assignVariableTypes;
  private Text m_dependentMethodParamsText;
  private Text m_assignVariableNameText; 
  private org.eclipse.swt.widgets.Combo assertions;
  public static final String[] ASSERTIONS = new String[] {
      "Assert Assigned Variable to true", "Assert Assigned Variable to false",  "Assert Statement(Class.method) to true", "Assert Statement(Class.method) to false"
    };   

  private Map<String, Button> m_annotations = new HashMap<String, Button>();
  private List<JavaElement> m_elements;
  public static final String[] ANNOTATIONS = new String[] {
    "BeforeMethod", "AfterMethod", "DataProvider",
    "BeforeClass", "AfterClass", "",
    "BeforeTest",  "AfterTest", "",
    "BeforeSuite", "AfterSuite", ""
  };

  public NewTestNGClassWizardPage() {
    super(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setTitle(ResourceUtil.getString("NewTestNGClassWizardPage.title"));
    setDescription(ResourceUtil.getString("NewTestNGClassWizardPage.description"));
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
    createBottom(container);
    //Test methods section
    createTestMethods(container);

    initialize();
    dialogChanged();
    setControl(container);
  }

  private void createTop(Composite parent) {
    final Composite container = SWTUtil.createGridContainer(parent, 3);
    
    //TestNG Framework
    {
      Label fmwkLabel = new Label(container, SWT.NULL);
      fmwkLabel.setText("&Testing Framework:");
      frameWorks = new org.eclipse.swt.widgets.Combo(container,
          SWT.BORDER | SWT.SINGLE);
      frameWorks.add("TestNG");
      frameWorks.select(0);
      GridData grid = new GridData(GridData.FILL_HORIZONTAL);
      grid.horizontalSpan = 2;
      frameWorks.setLayoutData(grid);
    }
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

  
  private void createBottom(Composite parent) {
    //
    // Annotations
    //
    {
      Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
      g.setText("Annotations");
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      g.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      g.setLayout(layout);
      layout.numColumns = 12;

      for (String label : ANNOTATIONS) {
        if ("".equals(label)) {
          new Label(g, SWT.NONE);
        } else {
          Button b = new Button(g, "".equals(label) ? SWT.None : SWT.CHECK);
          m_annotations.put(label, b);
          b.setText("@" + label);
        }
      }
    }

    //
    // XML suite file
    //
    {
      Composite container = SWTUtil.createGridContainer(parent, 2);

      //
      // Label
      //
      Label label = new Label(container, SWT.NULL);
      label.setText(ResourceUtil.getString("TestNG.newClass.suitePath"));

      //
      // Text widget
      //
      m_xmlFilePath = new Text(container, SWT.SINGLE | SWT.BORDER);
      GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
      gd.grabExcessHorizontalSpace = true;
      m_xmlFilePath.setLayoutData(gd);
    }
    //Add Dependency class creation section
    {
      Group g = new Group(parent, SWT.SHADOW_ETCHED_OUT);
      g.setText("Dependency Class Creation Section");
      g.setToolTipText("Add any new Dependency Class required for Test Class by clicking the below Add Dependency Class button");
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      g.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      g.setLayout(layout);
      layout.numColumns = 1;
      
      Button b = new Button(g, SWT.PUSH);
      b.setText("Add Dependency Class...");
      b.addSelectionListener(toSelectionAdapterForAddDependency(g, parent));
    }
    
    
  }

  private void createTestMethods(Composite parent) {
    //Test Methods
    {
      
      //Composite container = SWTUtil.createGridContainer(parent, 2);
      /*
      Table m_table = new Table(parent, SWT.CHECK | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
      TableItem item1 = new TableItem(m_table, SWT.NONE);
      item1.setData(createTestGroupSection(parent));
      *
      Canvas canvas = new Canvas (parent, SWT.SHADOW_ETCHED_OUT  | SWT.V_SCROLL | SWT.H_SCROLL);
      canvas.addPaintListener(new PaintListener() {
        public void paintControl(PaintEvent e) {
          // Do some drawing
          Rectangle rect = ((Canvas) e.widget).getBounds();
          e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
          e.gc.drawFocus(5, 5, rect.width - 10, rect.height - 10);
          e.gc.drawText("You can draw text directly on a canvas", 60, 60);
        }
      });   
      */   
      //createTestGroupSection(canvas);
//      ScrolledComposite child = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
//      SWTUtil.createGridLayout(child, 1);
//      child.setContent(createTestGroupSection(child));
    //child.setExpandHorizontal(true);
    //child.setExpandVertical(true); 
      createTestGroupSection(parent);
    }
    
  }
  
  private Composite createTestGroupSection(final Composite child){
    
    //final Composite child = SWTUtil.createGridContainer(source, 1);
    
    
    Group g = new Group(child, SWT.SHADOW_ETCHED_OUT);
    g.setText("Test Method Signature");  
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    //GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    //gd.verticalSpan = 2;      
    g.setLayoutData(gd);
    
    //ScrolledComposite g = new ScrolledComposite(group, SWT.V_SCROLL | SWT.H_SCROLL);
    
    GridLayout layout = new GridLayout();
    g.setLayout(layout);
    layout.numColumns = 16;     
    
    
    Button b_static = new Button(g, SWT.CHECK);
    b_static.setText("static");
    
    Button b_final = new Button(g, SWT.CHECK);
    b_final.setText("final");    
    
    Label label1 = new Label(g, SWT.NULL);
    label1.setText("&ModifierName:");   
    modifierNames = new org.eclipse.swt.widgets.Combo(g,
        SWT.BORDER | SWT.SINGLE);
    for(String modifier : MODIFIERS){
      modifierNames.add(modifier);  
    }
    GridData modifierName = new GridData(GridData.FILL_HORIZONTAL);
    modifierNames.setLayoutData(modifierName);      
    
    Label label2 = new Label(g, SWT.NULL);
    label2.setText("&ReturnType:");  
    m_returnTypeText = new Text(g, SWT.BORDER | SWT.SINGLE);    
    GridData grid = new GridData(GridData.FILL_HORIZONTAL);
    grid.horizontalSpan = 2;
    m_returnTypeText.setLayoutData(grid);       
    
    Label label3 = new Label(g, SWT.NULL);
    label3.setText("&MethodName:");  
    m_methodNameText = new Text(g, SWT.BORDER | SWT.SINGLE);
    GridData methodGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodGrid.horizontalSpan = 2;
    m_methodNameText.setLayoutData(methodGrid);       
    
    Label label4 = new Label(g, SWT.NULL);
    label4.setText("&MethodParams:");   
    m_methodParamsText = new Text(g, SWT.BORDER | SWT.SINGLE);
    GridData methodParamsGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodParamsGrid.horizontalSpan = 2;
    m_methodParamsText.setLayoutData(methodParamsGrid);         
    
    Button b1 = new Button(g, SWT.CHECK);
    b1.setText("throwsClause");    
    
    Button addImpl = new Button(g, SWT.CHECK);
    addImpl.setText("Add test implementation");     
    addImpl.addSelectionListener(toSelectionAdapter(g, child));
    
    Button addMore = new Button(g, SWT.PUSH);
    addMore.setText("Add More Test Method...");
    addMore.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        createTestMethods(child);
        child.layout();
      }
      
    });
    
    // Set the child as the scrolled content of the ScrolledComposite
//    child.setContent(child);

    // Set the minimum size
    //child.setMinSize(800, 800);

    // Expand both horizontally and vertically
//    child.setExpandHorizontal(true);
//    child.setExpandVertical(true);    
    
    return child;
    
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
      String className = StringUtils.isEmptyString(sel.getClassName())
          ? "NewTest" : sel.getClassName() + "Test";
      m_classNameText.setText(className);
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
    updateStatus(null);
  }

  private void updateStatus(String message) {
    setErrorMessage(message);
    setPageComplete(message == null);
  }

  public String getSourceFolder() {
    return m_sourceFolderText.getText();
  }

  public String getXmlFile() {
    return m_xmlFilePath.getText();
  }

  public String getPackageName() {
    return m_packageNameText.getText();
  }

  public String getClassName() {
    return m_classNameText.getText();
  }

  public boolean containsAnnotation(String annotation) {
    Button b = m_annotations.get(annotation);
    return b.getSelection();
  }

  public String getPackage() {
    return m_packageNameText.getText();
  }
  
  public SelectionAdapter toSelectionAdapter(final Group g, final Composite container){
    return new SelectionAdapter() {
      Group methodImpl ;
      public void widgetSelected(SelectionEvent e) {
        Button btn = (Button) e.getSource();
        if(btn.getSelection()){    
          if(methodImpl == null){
            methodImpl = new Group(container, SWT.SHADOW_ETCHED_OUT);
            methodImpl.setText("Method Implementation");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            methodImpl.setLayoutData(gd);
            GridLayout layout = new GridLayout();
            methodImpl.setLayout(layout);
            layout.numColumns = 15;   
          }
          createTestMethodImplementationSection(methodImpl, container);
          methodImpl.setVisible(true);
          container.layout();
        }
        else{
          methodImpl.setVisible(false);
        }
      }
      
    };    
  }
  
  public SelectionAdapter toSelectionAdapterForAddDependency(final Group g, final Composite container){
    return  new SelectionAdapter() {

      public void widgetSelected(SelectionEvent e) {
        WizardDialog dialog = new WizardDialog(container.getShell(),
            new NewDependencyClassWizard());
        dialog.open();
      }

    };  
  }
  
  public void createTestMethodImplementationSection(final Group g,
      final Composite container) {
    
    {
      m_dependentClassNameText = SWTUtil.createFileBrowserText(g, container,
          "&Dependent Class Name:", new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              try {
                dependentClassNameTextChanged(m_dependentClassNameText.getText(), methods);
              } catch (ClassNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              }
            }
          });
    }
    
   
    
    Label methodsLabel = new Label(g, SWT.NULL);
    methodsLabel.setText("&Methods:");
    methods = new org.eclipse.swt.widgets.Combo(g, SWT.BORDER | SWT.SINGLE);
    GridData modifierName = new GridData(GridData.FILL_HORIZONTAL);
    methods.setLayoutData(modifierName);

    Label methodParamsLabel = new Label(g, SWT.NULL);
    methodParamsLabel.setText("&Method Params:");
    m_dependentMethodParamsText = new Text(g, SWT.BORDER | SWT.SINGLE);

    Label assignVariablesLabel = new Label(g, SWT.NULL);
    assignVariablesLabel.setText("&Assign variables:");

    Label assignVariablesTypeLabel = new Label(g, SWT.NULL);
    assignVariablesTypeLabel.setText("&Type:");
    assignVariableTypes = new org.eclipse.swt.widgets.Combo(g,
        SWT.BORDER | SWT.SINGLE);

    Label assignVariablesNameLabel = new Label(g, SWT.NULL);
    assignVariablesNameLabel.setText("&Name:");
    m_assignVariableNameText = new Text(g, SWT.BORDER | SWT.SINGLE);

    Label assertionsLabel = new Label(g, SWT.NULL);
    assertionsLabel.setText("&Add Assertions:");
    assertions = new org.eclipse.swt.widgets.Combo(g, SWT.BORDER | SWT.SINGLE);
    for (String assertion : ASSERTIONS) {
      assertions.add(assertion);
    }
    GridData assertionsGrid = new GridData(GridData.FILL_HORIZONTAL);
    assertions.setLayoutData(assertionsGrid);
    
    Button addMore = new Button(g, SWT.CHECK);
    addMore.setText("Add  More Dependency Invocation...");
    addMore.addSelectionListener(toSelectionAdapter(null, container));
    
  }
  
  public void dependentClassNameTextChanged(String className, Combo methods) throws ClassNotFoundException{
    String fullClassName = getPackageNameFromFullPath(className);
    Class dependentClass = Class.forName(fullClassName);
    Method[] classMethods = dependentClass.getDeclaredMethods();
    if(methods != null){
      for(Method m : classMethods){
        methods.add(m.getName());
      }
    }
  }

  private static String getPackageNameFromFullPath(String className) {
    int startIndex1 = className.lastIndexOf("src/main/");
    int startIndex2 = className.lastIndexOf("src/");
    int lastIndex = className.lastIndexOf(".");
    int startIndex = (startIndex1 == -1 ? startIndex2 + 4 : startIndex1 + 9);
    return className.substring(startIndex, lastIndex).replaceAll("/", ".");
  }
  
  public static void main(String[] args) {
    System.out.println(getPackageNameFromFullPath("L/Sample/src/main/com/tdd/demo/TDDDemo.java"));
  }
}