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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.testng.collections.CollectionUtils;
import org.testng.eclipse.util.JDTUtil;
import org.testng.eclipse.util.ResourceUtil;
import org.testng.eclipse.util.SWTUtil;
import org.testng.eclipse.util.StringUtils;
import org.testng.eclipse.util.Utils;
import org.testng.eclipse.util.Utils.JavaElement;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.eclipse.util.SWTUtil.getJavaClassNameFromFullPath;
import static org.testng.eclipse.util.SWTUtil.getJavaPackageNameFromFullPath;
import static org.testng.eclipse.util.SWTUtil.getPackageNameFromFullPath;
import static org.testng.eclipse.wizards.WizardConstants.*;

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
  private Combo m_returnTypeText;
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
      DISPLAY_ASSERT_EQUALS, DISPLAY_ASSERT_NON_EQUALS,
      DISPLAY_ASSERT_NULL, DISPLAY_ASSERT_NOTNULL,
      DISPLAY_ASSERT_TRUE, DISPLAY_ASSERT_FALSE
    };   

  private Map<String, Button> m_annotations = new HashMap<String, Button>();
  private List<JavaElement> m_elements;
  public static final String[] ANNOTATIONS = new String[] {
    "BeforeMethod", "AfterMethod", "DataProvider",
    "BeforeClass", "AfterClass", "",
    "BeforeTest",  "AfterTest", "",
    "BeforeSuite", "AfterSuite", ""
  };
  
  private AtomicInteger atomicInteger = new AtomicInteger(1);
//  private Map<Integer, Map<String, Control>> m_methodSignatureRowObjects = new HashMap<>();
  private Map<Integer, Map<String, Object>> m_methodSignature = new HashMap<>();
  private Map<String, Map<String, String>> m_methodImplementation = new HashMap<>();
  // Map<String, Object>
  // first map Object will be Map<String, Control>
  private Map<String, Control> m_methodSignatureRowObjects;  
  // second object will be list of maps of method implementation
  private Map<String, Control> m_methodImpl;
  List<Map<String, Control>> m_impl_lists;
  
//  private Map<Integer, Map<String, Map<String, Object>>> m_methodRowObjects = new HashMap<>();
  private  Map<Integer, Map<String, Object>> m_methodRowObjects = new ConcurrentHashMap<>(); 
//  private Map<String, Map<String, Object>> m_methodHolder = new HashMap<>(); 
  private Map<String, Object> m_methodSignImplContainer;
  
  private List<Map<String, Map<String, String>>> methodImplList = new ArrayList<>();
  
  private Button b_static;
  private Button b_final;
  private Button b_throws;
  
  public static final String[] RETURN_TYPES = new String[] {
      "void", "String", "Integer", "Long", "Double", "Object", "Boolean"
    }; 
  
  public static final String[] ASSIGN_TYPES = new String[] {
      "Object", "String", "Integer", "Long", "Double", "Boolean"
    };  
  
  private AtomicInteger atomicIntegerForWritingJavaContent = new AtomicInteger(0);
  private Text m_assignVariableValueText; 
  

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
    final Composite container = SWTUtil.createGridContainer(parent, 6);
    
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
      g.setText("Test Dependencies");
      g.setToolTipText("Add any new functional code required for Test Class by clicking the below button");
      GridData gd = new GridData(GridData.FILL_HORIZONTAL);
      g.setLayoutData(gd);

      GridLayout layout = new GridLayout();
      g.setLayout(layout);
      layout.numColumns = 1;
      
      Button b = new Button(g, SWT.PUSH);
      b.setText("Add Functional Class...");
      b.addSelectionListener(toSelectionAdapterForAddDependency(g, parent));
    }
    
    
  }

  private void createTestMethods(Composite parent) {
    //Test Methods
      createTestGroupSection(parent);
  }
  
  private Composite createTestGroupSection(final Composite child){
    
    Group g = new Group(child, SWT.SHADOW_ETCHED_OUT);
    g.setText("Test Method");
    g.setToolTipText("Hover ove this once you are ready with Test Implementation to check how the Test looks like!");
    //g.setToolTipText(getSampleText());
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    //GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    //gd.verticalSpan = 2; 
    gd.horizontalSpan = 20;
    g.setLayoutData(gd);
    
    //ScrolledComposite g = new ScrolledComposite(group, SWT.V_SCROLL | SWT.H_SCROLL);
    
    GridLayout layout = new GridLayout();
    g.setLayout(layout);
    layout.numColumns = 20; 
    
    //set group
//    testMethodGoupsForNegativeCase.add(g);
    
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
    label1.setText("&Modifier:");   
    modifierNames = new org.eclipse.swt.widgets.Combo(g,
        SWT.BORDER | SWT.SINGLE);
    for(String modifier : MODIFIERS){
      modifierNames.add(modifier);  
    }
    GridData modifierName = new GridData(GridData.FILL_HORIZONTAL);
    modifierName.horizontalSpan = 1;
    modifierNames.setLayoutData(modifierName);      
    modifierNames.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });     
    
    m_returnTypeText = SWTUtil.createFileBrowserCombo(g, child, "&ReturnType:", new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if ((m_returnTypeText.getText().contains("/")|| m_returnTypeText.getText().contains("."))){
          m_returnTypeText.setText(getJavaClassNameFromFullPath(m_returnTypeText.getText()));
        }
        dialogChanged();
      }
    });
    m_returnTypeText.setToolTipText("Select any of the below Method Return types. If not available, select any Java Type by clicking Browse");
    for(String returnType : RETURN_TYPES){
      m_returnTypeText.add(returnType);  
    }     
    
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
    m_methodParamsText = new Text(g, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    GridData methodParamsGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodParamsGrid.horizontalSpan = 3;
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
    
    
    final Button addMore = new Button(g, SWT.PUSH);
    addMore.setText("Add More...");
    addMore.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        atomicInteger.addAndGet(1);
        createTestMethods(child);
        child.layout();
        
        Map<String, Object> prevObj =  m_methodRowObjects.get(atomicInteger.get()-1);
        Map<String, Control> prevRow = (Map<String, Control>) prevObj.get(METHOD_ROW_SIGNATURE);
        
        Button prevAddMore = (Button)prevRow.get(METHOD_ADD_MORE);
        prevAddMore.setVisible(false);         
        
      }
      
    });
    
    Group methodImpl = new Group(g, SWT.SHADOW_ETCHED_OUT);
    methodImpl.setText("Method Implementation");
    GridData gd1 = new GridData(GridData.FILL_HORIZONTAL);
    methodImpl.setLayoutData(gd1);
    gd1.horizontalSpan = 20;
    GridLayout layout1 = new GridLayout();
    methodImpl.setLayout(layout1);
    layout1.numColumns = 20;
  
    
    {
      m_dependentClassNameText = SWTUtil.createFileBrowserText(methodImpl, child,
          "&Type Name:", new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              try {
                if ((m_dependentClassNameText.getText().contains("/")|| m_dependentClassNameText.getText().contains("."))){
                  dependentClassNameTextChanged((m_dependentClassNameText.getText()), methods);
                }
                dialogChanged();
              } catch (ClassNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              }
            }
          });
      GridData gd2 = (GridData)m_dependentClassNameText.getLayoutData();
      gd2.horizontalSpan = 7;//4
      m_dependentClassNameText.setLayoutData(gd2);
      m_dependentClassNameText.setToolTipText("Select any Dependent Class Name that needs to be used for method invocation. If not available, create new one using Add Dependency Class");
    }
    
    Label methodsLabel = new Label(methodImpl, SWT.NULL);
    methodsLabel.setText("&Methods:");
    methods = new org.eclipse.swt.widgets.Combo(methodImpl, SWT.BORDER | SWT.SINGLE);
    GridData modifierNameImpl = new GridData(GridData.FILL_HORIZONTAL);
    // luk n feel
    modifierNameImpl.horizontalSpan = 3;
    methods.setLayoutData(modifierNameImpl);
    methods.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });     

    Label methodParamsLabel = new Label(methodImpl, SWT.NULL);
    methodParamsLabel.setText("&Arguments:");
    m_dependentMethodParamsText = new Text(methodImpl, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    m_dependentMethodParamsText.setToolTipText("Enter Method Arguments as comma(,) separated values like int a, int b ");
    m_dependentMethodParamsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    }); 
    //added for look n feel
    GridData methodParamsImplGrid = new GridData(GridData.FILL_BOTH);
    methodParamsImplGrid.horizontalSpan = 6;  
    m_dependentMethodParamsText.setLayoutData(methodParamsImplGrid);

    // 11 : 9 is there change it to 20 : 20 
    // better to have below  as new line 
    
    Label assignVariablesLabel = new Label(methodImpl, SWT.NULL);
    assignVariablesLabel.setText("&Assign variables:");

    assignVariableTypes = SWTUtil.createFileBrowserCombo(methodImpl, child, "&Type:", new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if ((assignVariableTypes.getText().contains("/")|| assignVariableTypes.getText().contains("."))){
          assignVariableTypes.setText(getJavaClassNameFromFullPath(assignVariableTypes.getText()));
        }
        dialogChanged();
      }
    });
    assignVariableTypes.setToolTipText("Select any of the below Method Return types. If not available, select any Java Type by clicking Browse");
    for(String assignType : ASSIGN_TYPES){
      assignVariableTypes.add(assignType);  
    }     

    Label assignVariablesNameLabel = new Label(methodImpl, SWT.NULL);
    assignVariablesNameLabel.setText("&Name:");
    m_assignVariableNameText = new Text(methodImpl, SWT.BORDER | SWT.SINGLE);
    m_assignVariableNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    //added for look n feel
    GridData assignVariableGrid = new GridData(GridData.FILL_HORIZONTAL);
    assignVariableGrid.horizontalSpan = 4;  
    m_assignVariableNameText.setLayoutData(assignVariableGrid);    
    
    Label assertionsLabel = new Label(methodImpl, SWT.NULL);
    assertionsLabel.setText("&Assertions:");
    assertions = new org.eclipse.swt.widgets.Combo(methodImpl, SWT.BORDER | SWT.SINGLE);
    for (String assertion : ASSERTIONS) {
      assertions.add(assertion);
    }
    GridData assertionsGrid = new GridData(GridData.FILL_HORIZONTAL);
    assertions.setLayoutData(assertionsGrid);
    assertions.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });    
    
    Label assignVariablesValueLabel = new Label(methodImpl, SWT.NULL);
    assignVariablesValueLabel.setText("&Assert Value:");
    m_assignVariableValueText = new Text(methodImpl, SWT.BORDER | SWT.SINGLE);   
    m_assignVariableValueText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    //added for look n feel
    GridData assignVariableValueGrid = new GridData(GridData.FILL_HORIZONTAL);
    assignVariableValueGrid.horizontalSpan = 4;  
    m_assignVariableValueText.setLayoutData(assignVariableValueGrid);  
    
    Button display = new Button(methodImpl, SWT.PUSH);
    display.setText("Show");
    display.addSelectionListener(toSelectionAdapterForShow(child));
    display.setToolTipText("Click on this to check how the user selection inputs looks like...");
    display.setData(atomicInteger.get());    
    
    Button addMoreImpl = new Button(methodImpl, SWT.PUSH);
    addMoreImpl.setText("Add More...");
    addMoreImpl.addSelectionListener(toSelectionAdapterForDependencyInvocation(methodImpl, child));
    addMoreImpl.setData(atomicInteger.get());
    
    m_methodSignatureRowObjects = new HashMap<>();
    m_methodSignatureRowObjects.put(METHOD_STATIC, b_static);
    m_methodSignatureRowObjects.put(METHOD_FINAL, b_final);
    m_methodSignatureRowObjects.put(METHOD_MODIFIER, modifierNames);
    m_methodSignatureRowObjects.put(METHOD_RETURN_TYPE, m_returnTypeText);
    //set group obj inside method name
    m_methodNameText.setData(g);
    m_methodSignatureRowObjects.put(METHOD_NAME, m_methodNameText);
    m_methodSignatureRowObjects.put(METHOD_PARAMS_TYPE, m_methodParamsText);
    m_methodSignatureRowObjects.put(METHOD_THROWS_CLAUSE, b_throws);
    m_methodSignatureRowObjects.put(METHOD_ADD_MORE, addMore);
    
    m_methodImpl = new HashMap<>();
    m_methodImpl.put(DEPENDENT_CLASSNAME, m_dependentClassNameText);
    m_methodImpl.put(METHODS, methods);
    m_methodImpl.put(DEPENDENT_METHOD_PARAMS, m_dependentMethodParamsText);
    m_methodImpl.put(ASSIGN_VARIABLE_TYPES, assignVariableTypes);
    m_methodImpl.put(ASSIGN_VARIABLE_NAME, m_assignVariableNameText);
    m_methodImpl.put(ASSIGN_VARIABLE_VALUE, m_assignVariableValueText);
    m_methodImpl.put(ASSERTIONS_COMBO, assertions);
    m_methodImpl.put(METHOD_ADD_MORE_IMPL, addMoreImpl);
    
    m_impl_lists = new ArrayList<>();
    m_impl_lists.add(m_methodImpl);
    
    m_methodSignImplContainer = new HashMap<>();
    m_methodSignImplContainer.put(METHOD_ROW_SIGNATURE, m_methodSignatureRowObjects);
    m_methodSignImplContainer.put(METHOD_ROW_IMPL, m_impl_lists);
    
    m_methodRowObjects.put(atomicInteger.get(), m_methodSignImplContainer);
    
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
    //validations for method signature
    int hitsForAddMoreMethods = getAtomicInteger().get();
    if((b_static.getSelection() || b_final.getSelection() || b_throws.getSelection() || !StringUtils.isEmptyString(modifierNames.getText()) 
        || !StringUtils.isEmptyString(m_methodParamsText.getText()) || !StringUtils.isEmptyString(m_returnTypeText.getText()) 
        || !StringUtils.isEmptyString(m_methodNameText.getText())) && hitsForAddMoreMethods == 1)  {
      if(validateMethodAndSetSignature(b_static, b_final, b_throws, modifierNames, m_methodParamsText, m_returnTypeText, m_methodNameText)){
//        setMethodSignatureAndImpl();
        //Iterate for every  method impl inside method row
        Map<String, Object> obj =  m_methodRowObjects.get(atomicInteger.get());
        List<Map<String, Control>>  objImplList = (List<Map<String, Control>>) obj.get(METHOD_ROW_IMPL);
        for(Map<String, Control> map :objImplList){
          Text t_dc = (Text)map.get(DEPENDENT_CLASSNAME);
          Combo c_m = (Combo)map.get(METHODS);
          Text t_dmp = (Text)map.get(DEPENDENT_METHOD_PARAMS);
          Combo c_vt = (Combo)map.get(ASSIGN_VARIABLE_TYPES);
          Text t_vn = (Text)map.get(ASSIGN_VARIABLE_NAME);
          Combo c_a = (Combo)map.get(ASSERTIONS_COMBO);
          Text t_avv = (Text)map.get(ASSIGN_VARIABLE_VALUE);
          if(!StringUtils.isEmptyString(t_dc.getText()) || !StringUtils.isEmptyString(c_m.getText()) 
              || !StringUtils.isEmptyString(t_dmp.getText())  || !StringUtils.isEmptyString(c_vt.getText())  
              || !StringUtils.isEmptyString(t_vn.getText()) || !StringUtils.isEmptyString(c_a.getText()) || !StringUtils.isEmptyString(t_avv.getText())){
            
            if(!validateAndSetMethodImpl(t_dc, c_m, t_dmp, c_vt, t_vn, c_a, t_avv, m_methodNameText)){
              return;
            }else {
//              setMethodSignatureAndImpl();
              //update tool tip for group
              /*
              for(Group g : testMethodGoupsForPositiveCase){
//                String toolTip = getMethodsSignature();
                g.setToolTipText(""); 
              }
              */
            }
          }
        }
      }
      else{
        return;
      }
    }
    else{
      if(hitsForAddMoreMethods > 1){
        for(int i = 1 ; i <= getAtomicInteger().get(); i++){
          Map<String, Object> rowObj = m_methodRowObjects.get(i);
          Map<String, Control> methodSign = (Map<String, Control>) rowObj.get(METHOD_ROW_SIGNATURE);
          if(methodSign != null){
            Button b_st = (Button)methodSign.get(METHOD_STATIC);
            Button b_fn = (Button)methodSign.get(METHOD_FINAL);
            Combo c_md = (Combo)methodSign.get(METHOD_MODIFIER);
            Combo c_mrt = (Combo)methodSign.get(METHOD_RETURN_TYPE);
            Text t_mn = (Text)methodSign.get(METHOD_NAME);
            Text t_mp = (Text)methodSign.get(METHOD_PARAMS_TYPE);
            Button b_th = (Button)methodSign.get(METHOD_THROWS_CLAUSE);
            if(b_st.getSelection() || b_fn.getSelection() || b_th.getSelection() || !StringUtils.isEmptyString(c_md.getText()) 
                || !StringUtils.isEmptyString(t_mp.getText()) || !StringUtils.isEmptyString(c_mrt.getText()) 
                || !StringUtils.isEmptyString(t_mn.getText())) {
              if(validateMethodAndSetSignature(b_st, b_fn, b_th, c_md, t_mp, c_mrt, t_mn)){
//                setMethodSignatureAndImpl();
                //Iterate for every  method impl inside method row 
                Map<String, Object> obj =  m_methodRowObjects.get(atomicInteger.get());
                List<Map<String, Control>>  objImplList = (List<Map<String, Control>>) obj.get(METHOD_ROW_IMPL);
                for(Map<String, Control> map :objImplList){
                  Text t_dc = (Text)map.get(DEPENDENT_CLASSNAME);
                  Combo c_m = (Combo)map.get(METHODS);
                  Text t_dmp = (Text)map.get(DEPENDENT_METHOD_PARAMS);
                  Combo c_vt = (Combo)map.get(ASSIGN_VARIABLE_TYPES);
                  Text t_vn = (Text)map.get(ASSIGN_VARIABLE_NAME);
                  Combo c_a = (Combo)map.get(ASSERTIONS_COMBO);
                  Text t_avv = (Text)map.get(ASSIGN_VARIABLE_VALUE);
                  if(!StringUtils.isEmptyString(t_dc.getText()) || !StringUtils.isEmptyString(c_m.getText()) 
                      || !StringUtils.isEmptyString(t_dmp.getText())  || !StringUtils.isEmptyString(c_vt.getText())  
                      || !StringUtils.isEmptyString(t_vn.getText()) || !StringUtils.isEmptyString(c_a.getText()) || !StringUtils.isEmptyString(t_avv.getText())){
                    if(!validateAndSetMethodImpl(t_dc, c_m, t_mp, c_vt, t_vn, c_a, t_avv, t_mn)){
                      return;
                    }else {
//                      setMethodSignatureAndImpl();
                      //update tool tip for group
                      /*
                      for(Group g : testMethodGoupsForPositiveCase){
//                        String toolTip = getMethodsSignature();
                        g.setToolTipText("");
                      }
                      */
                    }
                  }
                }
              }
              else{
                return;
              }
            }
          }
        }
      }
    }
    updateStatus(null);
    //set hover logic in the group -- get group object and set tool tip
    setMethodSignatureAndImplDuplicate();
  }
  
  private boolean validateAndSetMethodImpl(final Text m_dependentClassNameText, final Combo methods, final Text dependentMethodParamsText,
      final Combo assignVariableTypes, final Text assignVariableNameText, final Combo assertions, final Text assignVariableValueText, final Text m_methodNameText) {
    Group g = (Group)m_methodNameText.getData();
    if(StringUtils.isEmptyString(m_dependentClassNameText.getText())){
      updateStatus("Class cannot be empty");
      g.setToolTipText(METHOD_SIGNATURE_GROUP);
      return false;        
    } 
    if(StringUtils.isEmptyString(methods.getText())){
      updateStatus("Methods cannot be empty");
      g.setToolTipText(METHOD_SIGNATURE_GROUP);
      return false;        
    }
    
    if (!StringUtils.isEmptyString(assignVariableTypes.getText())
        || !StringUtils.isEmptyString(assignVariableNameText.getText())) {
      if (StringUtils.isEmptyString(assignVariableNameText.getText())) {
        updateStatus("Assign Variable Name cannot be empty");
        g.setToolTipText(METHOD_SIGNATURE_GROUP);
        return false;
      } else if (StringUtils.isEmptyString(assignVariableTypes.getText())) {
        updateStatus("Assign Variable Type cannot be empty");
        g.setToolTipText(METHOD_SIGNATURE_GROUP);
        return false;
      }
    }
    
    if (!StringUtils.isEmptyString(assertions.getText())) {
      if (DISPLAY_ASSERT_EQUALS.equals(assertions.getText())
          || DISPLAY_ASSERT_NON_EQUALS.equals(assertions.getText())) {
        if (StringUtils.isEmptyString(assignVariableValueText.getText())) {
          updateStatus("Assertion Value(Assert Equals/Assert NonEquals) cannot be empty");
          g.setToolTipText(METHOD_SIGNATURE_GROUP);
          return false;
        }
      }
      if (DISPLAY_ASSERT_TRUE.equals(assertions.getText())
          || DISPLAY_ASSERT_FALSE.equals(assertions.getText())) {
        if(!StringUtils.isEmptyString(assignVariableNameText.getText()) && !StringUtils.isEmptyString(assignVariableTypes.getText())){
          if (!"Boolean".equals(assignVariableTypes.getText())) {
            updateStatus("Assign Type has to be a Boolean for Assert True or Assert False");
            g.setToolTipText(METHOD_SIGNATURE_GROUP);
            return false;
          }
        }
      }      
      
    }
 /*   
    m_methodImplementation.put(METHOD_IMPLEMENTATION, new HashMap<String, String> () {{
      put(DEPENDENT_CLASSNAME, StringUtils.isEmptyString(m_dependentClassNameText.getText())?EMPTY:m_dependentClassNameText.getText());
      put(METHODS, StringUtils.isEmptyString(methods.getText())?EMPTY:methods.getText());
      put(DEPENDENT_METHOD_PARAMS, StringUtils.isEmptyString(dependentMethodParamsText.getText())?EMPTY:dependentMethodParamsText.getText());
      put(ASSIGN_VARIABLE_TYPES, StringUtils.isEmptyString(assignVariableTypes.getText())?EMPTY:assignVariableTypes.getText());
      put(ASSIGN_VARIABLE_NAME, StringUtils.isEmptyString(assignVariableNameText.getText())?EMPTY:assignVariableNameText.getText());
      put(ASSIGN_VARIABLE_VALUE, StringUtils.isEmptyString(assignVariableValueText.getText())?EMPTY:assignVariableValueText.getText());
      put(ASSERTIONS_COMBO, StringUtils.isEmptyString(assertions.getText())?EMPTY:assertions.getText());
    }}); 
    methodImplList.add(m_methodImplementation);
    
    Map<String, Object> methodObj = m_methodSignature.get(atomicIntegerForWritingJavaContent.get());
    methodObj.put(METHOD_IMPLEMENTATION_LIST, methodImplList);
    */
    return true;    
  }

  private boolean validateMethodAndSetSignature(final Button b_static, final Button b_final, final Button b_throws, 
      final Combo modifierNames, final Text m_methodParamsText, final Combo m_returnTypeText, final Text m_methodNameText){
    Group g = (Group)m_methodNameText.getData();
    if(StringUtils.isEmptyString(m_returnTypeText.getText())){
      updateStatus("Method Return Type cannot be empty");
      g.setToolTipText(METHOD_SIGNATURE_GROUP);
      return false;        
    } 
    if(StringUtils.isEmptyString(m_methodNameText.getText())){
      updateStatus("Method Name cannot be empty");
      g.setToolTipText(METHOD_SIGNATURE_GROUP);
      return false;        
    }
    /*
    m_methodSignature.put(atomicIntegerForWritingJavaContent.addAndGet(1), new HashMap<String, Object> () {{
      put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
      put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
      put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
      put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
      put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
      put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
      put(METHOD_MODIFIER, StringUtils.isEmptyString(modifierNames.getText())?EMPTY:modifierNames.getText());
    }}); 
    */      
    return true;
  }  

  public void setMethodSignatureAndImpl(){
    
    for(int i = 1; i <= m_methodRowObjects.size(); i++){
      Map<String, Object> rowObj = m_methodRowObjects.get(i);
      Map<String, Control> methodSign = (Map<String, Control>) rowObj.get(METHOD_ROW_SIGNATURE); 

      final Button b_static = (Button)methodSign.get(METHOD_STATIC);
      final Button b_final = (Button)methodSign.get(METHOD_FINAL);
      final Combo modifierNames = (Combo)methodSign.get(METHOD_MODIFIER);
      final Combo m_returnTypeText = (Combo)methodSign.get(METHOD_RETURN_TYPE);
      final Text m_methodNameText = (Text)methodSign.get(METHOD_NAME);
      final Text m_methodParamsText = (Text)methodSign.get(METHOD_PARAMS_TYPE);
      final Button b_throws = (Button)methodSign.get(METHOD_THROWS_CLAUSE);   
      
      if(!StringUtils.isEmptyString(m_returnTypeText.getText()) && !StringUtils.isEmptyString(m_methodNameText.getText())) {
          //method signature for 1st method...
          m_methodSignature.put(atomicIntegerForWritingJavaContent.addAndGet(1), new HashMap<String, Object> () {{
            put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
            put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
            put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
            put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
            put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
            put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
            put(METHOD_MODIFIER, StringUtils.isEmptyString(modifierNames.getText())?EMPTY:modifierNames.getText());
          }}); 
          
          //Iterate for every  method impl inside method row 
          List<Map<String, Control>>  objImplList = (List<Map<String, Control>>) rowObj.get(METHOD_ROW_IMPL);
      
          for(Map<String, Control> map :objImplList){
            final Text m_dependentClassNameText = (Text)map.get(DEPENDENT_CLASSNAME);
            final Combo methods = (Combo)map.get(METHODS);
            final Text dependentMethodParamsText = (Text)map.get(DEPENDENT_METHOD_PARAMS);
            final Combo assignVariableTypes = (Combo)map.get(ASSIGN_VARIABLE_TYPES);
            final Text assignVariableNameText = (Text)map.get(ASSIGN_VARIABLE_NAME);
            final Combo assertions = (Combo)map.get(ASSERTIONS_COMBO);
            final Text assignVariableValueText = (Text)map.get(ASSIGN_VARIABLE_VALUE);
            
              if(!StringUtils.isEmptyString(m_dependentClassNameText.getText()) && !StringUtils.isEmptyString(methods.getText())) {
                //impl present for 1st method....
                m_methodImplementation.put(METHOD_IMPLEMENTATION, new HashMap<String, String> () {{
                  put(DEPENDENT_CLASSNAME, StringUtils.isEmptyString(m_dependentClassNameText.getText())?EMPTY:m_dependentClassNameText.getText());
                  put(METHODS, StringUtils.isEmptyString(methods.getText())?EMPTY:methods.getText());
                  put(DEPENDENT_METHOD_PARAMS, StringUtils.isEmptyString(dependentMethodParamsText.getText())?EMPTY:dependentMethodParamsText.getText());
                  put(ASSIGN_VARIABLE_TYPES, StringUtils.isEmptyString(assignVariableTypes.getText())?EMPTY:assignVariableTypes.getText());
                  put(ASSIGN_VARIABLE_NAME, StringUtils.isEmptyString(assignVariableNameText.getText())?EMPTY:assignVariableNameText.getText());
                  put(ASSIGN_VARIABLE_VALUE, StringUtils.isEmptyString(assignVariableValueText.getText())?EMPTY:assignVariableValueText.getText());
                  put(ASSERTIONS_COMBO, StringUtils.isEmptyString(assertions.getText())?EMPTY:assertions.getText());
                }}); 
                methodImplList.add(m_methodImplementation);
                
                Map<String, Object> methodObj = m_methodSignature.get(atomicIntegerForWritingJavaContent.get());
                methodObj.put(METHOD_IMPLEMENTATION_LIST, methodImplList);        
              }
          }      
      }
      
    }
   
  }
  
  
  public void setMethodSignatureAndImplDuplicate(){
    //iterate methodrow object pick up the user selected rows and iterate impls if available, then populate row and its impls one by one
    Map<Integer, Map<String, Object>> m_methodSignatureD =  new HashMap<>();
    Map<String, Map<String, String>> m_methodImplementationD =  new HashMap<>();
    List<Map<String, Map<String, String>>> methodImplListD = new ArrayList<>();
    AtomicInteger atomicIntegerForWritingJavaContentD = new AtomicInteger(0);
    
    for(int i = 1; i <= m_methodRowObjects.size(); i++){
      Map<String, Object> rowObj = m_methodRowObjects.get(i);
      Map<String, Control> methodSign = (Map<String, Control>) rowObj.get(METHOD_ROW_SIGNATURE); 

      final Button b_static = (Button)methodSign.get(METHOD_STATIC);
      final Button b_final = (Button)methodSign.get(METHOD_FINAL);
      final Combo modifierNames = (Combo)methodSign.get(METHOD_MODIFIER);
      final Combo m_returnTypeText = (Combo)methodSign.get(METHOD_RETURN_TYPE);
      final Text m_methodNameText = (Text)methodSign.get(METHOD_NAME);
      final Text m_methodParamsText = (Text)methodSign.get(METHOD_PARAMS_TYPE);
      final Button b_throws = (Button)methodSign.get(METHOD_THROWS_CLAUSE);   
      
      if(!StringUtils.isEmptyString(m_returnTypeText.getText()) && !StringUtils.isEmptyString(m_methodNameText.getText())) {
          //method signature for 1st method...
        m_methodSignatureD.put(atomicIntegerForWritingJavaContentD.addAndGet(1), new HashMap<String, Object> () {{
            put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
            put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
            put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
            put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
            put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
            put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
            put(METHOD_MODIFIER, StringUtils.isEmptyString(modifierNames.getText())?EMPTY:modifierNames.getText());
          }}); 
      
          //set tooltip for user input methods
          Group g = (Group)m_methodNameText.getData();
          //g.setToolTipText(getMethodText(m_methodSignatureD, atomicIntegerForWritingJavaContentD.get()));
          //testMethodGoupsForPositiveCase.add(g);
          
          
          //Iterate for every  method impl inside method row 
          List<Map<String, Control>>  objImplList = (List<Map<String, Control>>) rowObj.get(METHOD_ROW_IMPL);
      
          for(Map<String, Control> map :objImplList){
            final Text m_dependentClassNameText = (Text)map.get(DEPENDENT_CLASSNAME);
            final Combo methods = (Combo)map.get(METHODS);
            final Text dependentMethodParamsText = (Text)map.get(DEPENDENT_METHOD_PARAMS);
            final Combo assignVariableTypes = (Combo)map.get(ASSIGN_VARIABLE_TYPES);
            final Text assignVariableNameText = (Text)map.get(ASSIGN_VARIABLE_NAME);
            final Combo assertions = (Combo)map.get(ASSERTIONS_COMBO);
            final Text assignVariableValueText = (Text)map.get(ASSIGN_VARIABLE_VALUE);
            
              if(!StringUtils.isEmptyString(m_dependentClassNameText.getText()) && !StringUtils.isEmptyString(methods.getText())) {
                //impl present for 1st method....
                m_methodImplementationD.put(METHOD_IMPLEMENTATION, new HashMap<String, String> () {{
                  put(DEPENDENT_CLASSNAME, StringUtils.isEmptyString(m_dependentClassNameText.getText())?EMPTY:m_dependentClassNameText.getText());
                  put(METHODS, StringUtils.isEmptyString(methods.getText())?EMPTY:methods.getText());
                  put(DEPENDENT_METHOD_PARAMS, StringUtils.isEmptyString(dependentMethodParamsText.getText())?EMPTY:dependentMethodParamsText.getText());
                  put(ASSIGN_VARIABLE_TYPES, StringUtils.isEmptyString(assignVariableTypes.getText())?EMPTY:assignVariableTypes.getText());
                  put(ASSIGN_VARIABLE_NAME, StringUtils.isEmptyString(assignVariableNameText.getText())?EMPTY:assignVariableNameText.getText());
                  put(ASSIGN_VARIABLE_VALUE, StringUtils.isEmptyString(assignVariableValueText.getText())?EMPTY:assignVariableValueText.getText());
                  put(ASSERTIONS_COMBO, StringUtils.isEmptyString(assertions.getText())?EMPTY:assertions.getText());
                }}); 
                methodImplListD.add(m_methodImplementationD);
                
                Map<String, Object> methodObj = m_methodSignatureD.get(atomicIntegerForWritingJavaContentD.get());
                methodObj.put(METHOD_IMPLEMENTATION_LIST, methodImplListD);        
              }
          }      
          //set tooltip for user input methods -  override if merthod impl presents
          g.setToolTipText(getMethodText(m_methodSignatureD, atomicIntegerForWritingJavaContentD.get()));            
      }
      
    }
   
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
//      Label methodImplmn;
      public void widgetSelected(SelectionEvent e) {
        Button btn = (Button) e.getSource();
        if(btn.getSelection()){    
          if(methodImpl == null){
            
            methodImpl = new Group(g, SWT.SHADOW_ETCHED_OUT);
            methodImpl.setText("Method Implementation");
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            methodImpl.setLayoutData(gd);
//            gd.verticalSpan = 10;
            gd.horizontalSpan = 20;//16
            GridLayout layout = new GridLayout();
            methodImpl.setLayout(layout);
            layout.numColumns = 20;//16
            createTestMethodImplementationSection(methodImpl, container, atomicInteger.get()); 
          }
          methodImpl.setVisible(true);
          container.layout();
          g.layout();
        }
        else{
          methodImpl.setVisible(false);
          container.layout();
          methodImpl.layout();
          g.layout();
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
  
  public void createTestMethodImplementationSection(final Group methodImpl,
      final Composite child, int buttonValue) {
    {
      m_dependentClassNameText = SWTUtil.createFileBrowserText(methodImpl, child,
          "&ClassName:", new ModifyListener() {
            public void modifyText(ModifyEvent e) {
              try {
                if ((m_dependentClassNameText.getText().contains("/")|| m_dependentClassNameText.getText().contains("."))){
                  dependentClassNameTextChanged((m_dependentClassNameText.getText()), methods);
                }
                dialogChanged();
              } catch (ClassNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
              }
            }
          });
      GridData gd2 = (GridData)m_dependentClassNameText.getLayoutData();
      gd2.horizontalSpan = 7;//4
      m_dependentClassNameText.setLayoutData(gd2);
      m_dependentClassNameText.setToolTipText("Select any Dependent Class Name that needs to be used for method invocation. If not available, create new one using Add Dependency Class");      
    }
    
    Label methodsLabel = new Label(methodImpl, SWT.NULL);
    methodsLabel.setText("&Methods:");
    methods = new org.eclipse.swt.widgets.Combo(methodImpl, SWT.BORDER | SWT.SINGLE);
    GridData modifierNameImpl = new GridData(GridData.FILL_HORIZONTAL);
    // luk n feel
    modifierNameImpl.horizontalSpan = 3;
    methods.setLayoutData(modifierNameImpl);
    methods.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });      

    Label methodParamsLabel = new Label(methodImpl, SWT.NULL);
    methodParamsLabel.setText("&Params:");
    m_dependentMethodParamsText = new Text(methodImpl, SWT.BORDER | SWT.SINGLE);
    m_dependentMethodParamsText.setToolTipText("Enter Method Arguments if any as comma separated values");
    m_dependentMethodParamsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });  
    
    //added for look n feel
    GridData methodParamsImplGrid = new GridData(GridData.FILL_BOTH);
    methodParamsImplGrid.horizontalSpan = 6;  
    m_dependentMethodParamsText.setLayoutData(methodParamsImplGrid);  
    
    // 11 : 9 is there change it to 20 : 20 
    // better to have below  as new line     

    Label assignVariablesLabel = new Label(methodImpl, SWT.NULL);
    assignVariablesLabel.setText("&Assign variables:");

    assignVariableTypes = SWTUtil.createFileBrowserCombo(methodImpl, child, "&Type:", new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if ((assignVariableTypes.getText().contains("/")|| assignVariableTypes.getText().contains("."))){
          assignVariableTypes.setText(getJavaClassNameFromFullPath(assignVariableTypes.getText()));
        }
        dialogChanged();
      }
    });
    assignVariableTypes.setToolTipText("Select any of the below Method Return types. If not available, select any Java Type by clicking Browse");
    for(String returnType : RETURN_TYPES){
      assignVariableTypes.add(returnType);  
    }    

    Label assignVariablesNameLabel = new Label(methodImpl, SWT.NULL);
    assignVariablesNameLabel.setText("&Name:");
    m_assignVariableNameText = new Text(methodImpl, SWT.BORDER | SWT.SINGLE);
    m_assignVariableNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    }); 
    //added for look n feel
    GridData assignVariableGrid = new GridData(GridData.FILL_HORIZONTAL);
    assignVariableGrid.horizontalSpan = 4;  
    m_assignVariableNameText.setLayoutData(assignVariableGrid);    
    
    Label assertionsLabel = new Label(methodImpl, SWT.NULL);
    assertionsLabel.setText("&Assertions:");
    assertions = new org.eclipse.swt.widgets.Combo(methodImpl, SWT.BORDER | SWT.SINGLE);
    for (String assertion : ASSERTIONS) {
      assertions.add(assertion);
    }
    GridData assertionsGrid = new GridData(GridData.FILL_HORIZONTAL);
    assertions.setLayoutData(assertionsGrid);
    assertions.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });    
    
    Label assignVariablesValueLabel = new Label(methodImpl, SWT.NULL);
    assignVariablesValueLabel.setText("&Assert Value:");
    m_assignVariableValueText = new Text(methodImpl, SWT.BORDER | SWT.SINGLE);   
    m_assignVariableValueText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });
    //added for look n feel
    GridData assignVariableValueGrid = new GridData(GridData.FILL_HORIZONTAL);
    assignVariableValueGrid.horizontalSpan = 4;  
    m_assignVariableValueText.setLayoutData(assignVariableValueGrid);  
    
    Button display = new Button(methodImpl, SWT.PUSH);
    display.setText("Show");
    display.addSelectionListener(toSelectionAdapterForShow(child));
    display.setToolTipText("Click on this to check how the user selection inputs looks like...");
    display.setData(atomicInteger.get());    
    
    Button addMoreImpl = new Button(methodImpl, SWT.PUSH);
    addMoreImpl.setText("Add More...");
    addMoreImpl.addSelectionListener(toSelectionAdapterForDependencyInvocation(methodImpl, child));
    addMoreImpl.setData(atomicInteger.get());
    
    m_methodImpl = new HashMap<>();
    m_methodImpl.put(DEPENDENT_CLASSNAME, m_dependentClassNameText);
    m_methodImpl.put(METHODS, methods);
    m_methodImpl.put(DEPENDENT_METHOD_PARAMS, m_dependentMethodParamsText);
    m_methodImpl.put(ASSIGN_VARIABLE_TYPES, assignVariableTypes);
    m_methodImpl.put(ASSIGN_VARIABLE_NAME, m_assignVariableNameText);
    m_methodImpl.put(ASSIGN_VARIABLE_VALUE, m_assignVariableValueText);
    m_methodImpl.put(ASSERTIONS_COMBO, assertions);
    m_methodImpl.put(METHOD_ADD_MORE_IMPL, addMoreImpl);
    
    Map<String, Object> selObj = m_methodRowObjects.get(buttonValue);
    List<Map<String, Control>>  selObjImplList = (List<Map<String, Control>>) selObj.get(WizardConstants.METHOD_ROW_IMPL); 
    selObjImplList.add(m_methodImpl);
    
  }
  
  public SelectionAdapter toSelectionAdapterForDependencyInvocation(final Group g, final Composite container){
    return new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Button btn = (Button) e.getSource();
        int incrmValue = (Integer)btn.getData();
        
        createTestMethodImplementationSection(g, container, incrmValue); 
        g.layout();
        container.layout();
        
        Map<String, Object> prevObj =  m_methodRowObjects.get(incrmValue);
        List<Map<String, Control>>  prevImplList = (List<Map<String, Control>>) prevObj.get(METHOD_ROW_IMPL);        
        
        Map<String, Control> prevImplRow = prevImplList.get(prevImplList.size()-2);
        Button prevImplAddMore = (Button) prevImplRow.get(METHOD_ADD_MORE_IMPL);
        prevImplAddMore.setVisible(false);  
      }
    };    
  }
  
  public SelectionAdapter toSelectionAdapterForShow(final Composite container){
    return new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
        Button btn = (Button) e.getSource();
        int incrmValue = (Integer)btn.getData();
        
        Map<String, Object> prevObj =  m_methodRowObjects.get(incrmValue);
        List<Map<String, Control>>  prevImplList = (List<Map<String, Control>>) prevObj.get(METHOD_ROW_IMPL);        
        
        Map<String, Control> currImplRow = prevImplList.get(prevImplList.size()-1);
        String assignText = "";
        String methodParamsText = "";
        String assertText = "";
        boolean assertFlag = false;
        Text typeName = (Text)currImplRow.get(DEPENDENT_CLASSNAME);
        Combo methodName = (Combo)currImplRow.get(METHODS);
        Text methodParams = (Text)currImplRow.get(DEPENDENT_METHOD_PARAMS);
        Combo assignVarTypes = (Combo)currImplRow.get(ASSIGN_VARIABLE_TYPES);
        Text varName = (Text)currImplRow.get(ASSIGN_VARIABLE_NAME);
        Text varValue = (Text)currImplRow.get(ASSIGN_VARIABLE_VALUE);
        Combo assertType = (Combo)currImplRow.get(ASSERTIONS_COMBO);
        methodParamsText = " ("+methodParams.getText()+") ";
        String invocation = typeName.getText()+"."+methodName.getText()+(!StringUtils.isEmptyString(methodParams.getText())?methodParamsText:"()");
        if(!StringUtils.isEmptyString(assignVarTypes.getText()) && !StringUtils.isEmptyString(varName.getText())){
          assignText = assignVarTypes.getText()+" "+varName.getText()+" = ";
        }
        if(!StringUtils.isEmptyString(assertType.getText())){
//          assertText = assertType.getText()+"(";
          assertText = TESTNG_ASSERT_NOTNULL+"(";
          assertFlag = true;
        }        
        if(!StringUtils.isEmptyString(typeName.getText()) && !StringUtils.isEmptyString(methodName.getText())){
          if(assertFlag)
            btn.setToolTipText("   \n\n"+TAB+TAB+TAB+assertText+assignText + invocation+");"+TAB+TAB+TAB+"\n\n ");
          else
            btn.setToolTipText(assignText + invocation + ";"+"\n\n");
        }
        else{
          btn.setToolTipText("Click on this to check how the user selection inputs looks like...");
        }
      }
    };    
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
  
  public Map<String, Control> getMethodSignatureRowObjects(){
    return m_methodSignatureRowObjects;
  } 
  
  public Map<Integer, Map<String, Object>> getMethodSignature(){
    return m_methodSignature;
  } 
  
  public AtomicInteger getAtomicInteger(){
    return atomicInteger;
  }

  public AtomicInteger getAtomicIntegerForWritingJavaContent() {
    return atomicIntegerForWritingJavaContent;
  }

  public void setAtomicIntegerForWritingJavaContent(
      AtomicInteger atomicIntegerForWritingJavaContent) {
    this.atomicIntegerForWritingJavaContent = atomicIntegerForWritingJavaContent;
  }  
  
  public String getSampleText(){
    String str = "\n      public void tddTest() {\n       String result = functionalClass.functionalMethod();\n       assertNotNull(result);\n    }\n ";
    return str;
  }
  
  public String getMethodText(Map<Integer, Map<String, Object>> map, int i){
    StringBuilder methods = new StringBuilder();
    int assertCount = 0;
    //Test Methods
      Map<String, Object> obj = map.get(i);
      if(obj != null){
        List<Map<String, Map<String, String>>> methodImplList = (List<Map<String, Map<String, String>>>) obj.get(METHOD_IMPLEMENTATION_LIST);
        boolean hasElements = CollectionUtils.hasElements(methodImplList);
        String methodModifier = (String) obj.get(METHOD_MODIFIER);
        String methodStatic = (String) obj.get(METHOD_STATIC);
        String methodFinal = (String) obj.get(METHOD_FINAL);
        String methodReturnType = (String) obj.get(METHOD_RETURN_TYPE);
        String methodName = (String) obj.get(METHOD_NAME);
        String methodParamsType = (String) obj.get(METHOD_PARAMS_TYPE);
        String methodThrows = (String) obj.get(METHOD_THROWS_CLAUSE);        
        methods.append("\n"
            + TAB + TAB
            + ((!StringUtils.isEmptyString(methodModifier))?methodModifier+SPACE:EMPTY)
            + ((!StringUtils.isEmptyString(methodStatic))?methodStatic+SPACE:EMPTY)
            + ((!StringUtils.isEmptyString(methodFinal))?methodFinal+SPACE:EMPTY)
            + ((!StringUtils.isEmptyString(methodReturnType))?methodReturnType+SPACE:EMPTY)
            + ((!StringUtils.isEmptyString(methodName))?methodName+SPACE:EMPTY)
            + OPEN_BRACE  +SPACE
            + ((!StringUtils.isEmptyString(methodParamsType))?methodParamsType+SPACE:EMPTY)
            + CLOSE_BRACE +SPACE
            + ((!StringUtils.isEmptyString(methodThrows))?methodThrows+SPACE:EMPTY)
            );
        if(hasElements){
          methods.append(" {\n" );
          for(Map<String, Map<String, String>> lstEntry : methodImplList){
            Map<String, String> invocation = lstEntry.get(METHOD_IMPLEMENTATION);
            String depClassName = invocation.get(DEPENDENT_CLASSNAME);
            String method = invocation.get(METHODS);
            String methodParam = invocation.get(DEPENDENT_METHOD_PARAMS);
            String assignVarType = invocation.get(ASSIGN_VARIABLE_TYPES);
            String assignVarName = invocation.get(ASSIGN_VARIABLE_NAME);
            String assignVarValue = invocation.get(ASSIGN_VARIABLE_VALUE);
            String assertion = invocation.get(ASSERTIONS_COMBO);
            String javaClassName = null;
            String javaClassNameVariable = null;
            
            if("String".equals(assignVarType)){
              assignVarValue = "\""+assignVarValue+"\"";
            }
            
            if(depClassName.contains("/") || depClassName.contains(".")){
              javaClassName = getJavaClassNameFromFullPath(depClassName);
            }else{
              javaClassName = depClassName;
            }
            
            String firstChar = javaClassName.substring(0, 1).toLowerCase();
            String remainingChars = javaClassName.substring(1);
            javaClassNameVariable = firstChar+remainingChars;
            
            boolean isAssert = !StringUtils.isEmptyString(assertion);
            String javaClassInstantiate = TAB+TAB+javaClassName + SPACE + javaClassNameVariable + SPACE + EQUALS + SPACE + " new " 
                                          + javaClassName + OPEN_BRACE + CLOSE_BRACE + COLON;
            methods.append(javaClassInstantiate);
            
            String methodInvoke = javaClassNameVariable+DOT+method+OPEN_BRACE+(!StringUtils.isEmptyString(methodParam)?methodParam:EMPTY)+CLOSE_BRACE;
              
            if(!StringUtils.isEmptyString(assignVarName)){
              methods.append("\n");
              methods.append(TAB+TAB+assignVarType + SPACE + assignVarName + SPACE + EQUALS + SPACE +methodInvoke + COLON);
            }             
            boolean assign = !StringUtils.isEmptyString(assignVarName);
            boolean assignVal = !StringUtils.isEmptyString(assignVarValue);
            if(isAssert){
              assertCount++;
              switch (assertion) {
              case DISPLAY_ASSERT_EQUALS:
                methods.append("\n");
                methods.append(TAB+TAB+TESTNG_ASSERT_EQUALS+OPEN_BRACE+(assign ? assignVarName : methodInvoke)+COMMA+assignVarValue+CLOSE_BRACE +COLON);
                break;
                
              case DISPLAY_ASSERT_NON_EQUALS:
                methods.append("\n");
                methods.append(TAB+TAB+TESTNG_ASSERT_NON_EQUALS+OPEN_BRACE+(assign ? assignVarName : methodInvoke)+COMMA+assignVarValue+CLOSE_BRACE+COLON);
                break;
                
              case DISPLAY_ASSERT_NULL:
                methods.append("\n");
                methods.append(TAB+TAB+TESTNG_ASSERT_NULL+OPEN_BRACE+(assign ? assignVarName : methodInvoke)+CLOSE_BRACE+COLON);
                break;
                
              case DISPLAY_ASSERT_NOTNULL:
                methods.append("\n");
                methods.append(TAB+TAB+TESTNG_ASSERT_NOTNULL+OPEN_BRACE+(assign ? assignVarName : methodInvoke)+CLOSE_BRACE+COLON);
                break;  
                
              case DISPLAY_ASSERT_TRUE:
                methods.append("\n");
                methods.append(TAB+TAB+TESTNG_ASSERT_TRUE+OPEN_BRACE+(assign ? assignVarName : methodInvoke)+COMMA+(assignVal?"\""+assignVarValue+"\"":"\"Your Message\"")+CLOSE_BRACE+COLON);
                break;  
                
              case DISPLAY_ASSERT_FALSE:
                methods.append("\n");
                methods.append(TAB+TAB+TESTNG_ASSERT_FALSE+OPEN_BRACE+(assign ? assignVarName : methodInvoke)+COMMA+(assignVal?"\""+assignVarValue+"\"":"\"Your Message\"")+CLOSE_BRACE+COLON);
                break;                  
                
              }
            }  
            else{
              if(StringUtils.isEmptyString(assignVarName)){
                methods.append("\n");
                methods.append(TAB+TAB+methodInvoke + COLON);
              }
            }
          }
          methods.append(" \n  }\n\n" );
        }
        else{
          methods.append(
              TAB +"{\n"
                  + "\n"
                  +TAB+TAB+ "}\n");  
        }
      }
    return methods.toString();
  }

}