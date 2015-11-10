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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import static org.testng.eclipse.wizards.WizardConstants.*;
import static org.testng.eclipse.util.SWTUtil.getJavaClassNameFromFullPath;
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
  private Map<Integer, Map<String, Control>> m_methodSignatureRowObjects = new HashMap<>();  
  private Map<String, Control> m_methodSignatureRow;
  private AtomicInteger atomicInteger = new AtomicInteger(1);
  private AtomicInteger atomicIntegerForWritingJavaContent = new AtomicInteger(0);
  private Button b_static;
  private Button b_final;
  private Button b_throws;
  public static final String[] RETURN_TYPES = new String[] {
      "void", "Integer", "Double", "String", "Object", "Boolean"
    };
  private List<Control> mainMethodGoup = new CopyOnWriteArrayList<>();

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
  }


  private void createMethod(Composite parent) {
    {
      ScrolledComposite  container = null;
      Group methodSection = createMethodsGroupSection(parent, container);
      
    }
    
  }
  
  private Group createMethodsGroupSection(final Composite source, final ScrolledComposite parent){
    
    Group g = new Group(source, SWT.SHADOW_ETCHED_OUT);
    g.setText("Methods Signature");  
    g.setToolTipText(METHOD_SIGNATURE_GROUP);
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 19;
    //GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
    //gd.verticalSpan = 2;      
    g.setLayoutData(gd);
    
    GridLayout layout = new GridLayout();
    g.setLayout(layout);
    layout.numColumns = 19;     
    
    //set group
    mainMethodGoup.add(g);
    
    createMethodSignatureElements(g, source, parent);
    return g;
   
  }  
  
  private void createMethodSignatureElements(final Group g1, final Composite source, final ScrolledComposite parent){
    
    Group g = new Group(g1, SWT.SHADOW_ETCHED_OUT);
    g.setText("Method");  
    GridData gd = new GridData(GridData.FILL_HORIZONTAL);
    gd.horizontalSpan = 19;
    g.setLayoutData(gd);
    
    GridLayout layout = new GridLayout();
    g.setLayout(layout);
    layout.numColumns = 19;        
    
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
    
    b_throws = new Button(g, SWT.CHECK);
    b_throws.setText("throwsClause");    
    b_throws.addSelectionListener(new SelectionAdapter() {
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
    
    /*
    Label label2 = new Label(g, SWT.NULL);
    label2.setText("&ReturnType:");  
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
    */
    m_returnTypeText = SWTUtil.createFileBrowserCombo(g, source, "&ReturnType:", new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        if(m_returnTypeText.getText().contains("/") || m_returnTypeText.getText().contains(".")){
          m_returnTypeText.setText(getJavaClassNameFromFullPath(m_returnTypeText.getText()));
        }
        dialogChanged();
      }
    });
    m_returnTypeText.setToolTipText("Please select any of the below Method Return types. If not available, select any Java Type by clicking Browse");
    for(String returnType : RETURN_TYPES){
      m_returnTypeText.add(returnType);  
    }  
    // luk n feel
    GridData gd2 = (GridData)m_returnTypeText.getLayoutData();
    gd2.horizontalSpan = 12;
    m_returnTypeText.setLayoutData(gd2);    
    
    Label label3 = new Label(g, SWT.NULL);
    label3.setText("&MethodName:");  
    m_methodNameText = new Text(g, SWT.BORDER | SWT.SINGLE);
    GridData methodGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodGrid.horizontalSpan = 4; //3
    m_methodNameText.setLayoutData(methodGrid);
    m_methodNameText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        m_methodNameText.getText();
        dialogChanged();
      }
    });       
    
    Label label4 = new Label(g, SWT.NULL);
    label4.setText("&MethodParams:");   
    m_methodParamsText = new Text(g, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.V_SCROLL);
    GridData methodParamsGrid = new GridData(GridData.FILL_HORIZONTAL);
    methodParamsGrid.horizontalSpan = 12; //3
    m_methodParamsText.setLayoutData(methodParamsGrid);  
    m_methodParamsText.addModifyListener(new ModifyListener() {
      public void modifyText(ModifyEvent e) {
        dialogChanged();
      }
    });       
    
    
    final Button addMore = new Button(g, SWT.PUSH);
    addMore.setText("Add More...");
    addMore.addSelectionListener(new SelectionAdapter() {
      public void widgetSelected(SelectionEvent e) {
//        Group g = createMethodsGroupSection(source, parent);
//        parent.setExpandVertical(true);parent.setExpandHorizontal(true);parent.setRedraw(true);
//        parent.setContent(g);
        atomicInteger.addAndGet(1);
        createMethodSignatureElements(g1,source, parent);//g
        //parent.layout();
        source.layout();
        
        //set button add more invisible for pervious row
        Map<String, Control> prevRow = m_methodSignatureRowObjects.get(atomicInteger.get()-1);
        Button prevAddMore = (Button)prevRow.get(METHOD_ADD_MORE);
        prevAddMore.setVisible(false);
        
      }
      
    });
      m_methodSignatureRowObjects.put(atomicInteger.get(), new HashMap<String, Control> () {{
        put(METHOD_STATIC, b_static);
        put(METHOD_FINAL, b_final);
        put(METHOD_MODIFIER, modifierNames);
        put(METHOD_RETURN_TYPE, m_returnTypeText);
        put(METHOD_NAME, m_methodNameText);
        put(METHOD_PARAMS_TYPE, m_methodParamsText);
        put(METHOD_THROWS_CLAUSE, b_throws);
        put(METHOD_ADD_MORE, addMore);
      }});
       
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
    Group  g = (Group)mainMethodGoup.get(0);
    int hitsForAddMoreMethods = getAtomicInteger().get();
    if((b_static.getSelection() || b_final.getSelection() || b_throws.getSelection() || !StringUtils.isEmptyString(modifierNames.getText()) 
        || !StringUtils.isEmptyString(m_methodParamsText.getText()) || !StringUtils.isEmptyString(m_returnTypeText.getText()) 
        || !StringUtils.isEmptyString(m_methodNameText.getText())) && hitsForAddMoreMethods == 1) {
      if(!validateAndSetMethodSignature(b_static, b_final, b_throws, modifierNames, m_methodParamsText, m_returnTypeText, m_methodNameText)){
        g.setToolTipText(METHOD_SIGNATURE_GROUP);
        return;
      }else{
        //update tool tip for group
//        String toolTip = getMethodsSignature();
//        g.setToolTipText(toolTip);
      }
    }
    else{
      if(hitsForAddMoreMethods > 1){
        for(int i = 1 ; i <= getAtomicInteger().get(); i++){
          Map<String, Control> obj = m_methodSignatureRowObjects.get(i);
          if(obj != null){
            Button b_st = (Button)obj.get(METHOD_STATIC);
            Button b_fn = (Button)obj.get(METHOD_FINAL);
            Combo c_md = (Combo)obj.get(METHOD_MODIFIER);
            Combo c_rt = (Combo)obj.get(METHOD_RETURN_TYPE);
            Text t_mn = (Text)obj.get(METHOD_NAME);
            Text t_mp = (Text)obj.get(METHOD_PARAMS_TYPE);
            Button b_th = (Button)obj.get(METHOD_THROWS_CLAUSE);
            if(b_st.getSelection() || b_fn.getSelection() || b_th.getSelection() || !StringUtils.isEmptyString(c_md.getText()) 
                || !StringUtils.isEmptyString(t_mp.getText()) || !StringUtils.isEmptyString(c_rt.getText()) 
                || !StringUtils.isEmptyString(t_mn.getText())) {
              if(!validateAndSetMethodSignature(b_st, b_fn, b_th, c_md, t_mp, c_rt, t_mn)){
                g.setToolTipText(METHOD_SIGNATURE_GROUP);
                return;            
              }else{
                //update tool tip for group
//                String toolTip = getMethodsSignature();
//                g.setToolTipText(toolTip);
              }
            }
          }
        }
      }
    }
    updateStatus(null);
    //set tooltip in method group section
    setMethodSignaturesDuplicate();
  }
  
  private boolean validateAndSetMethodSignature(final Button b_static, final Button b_final, final Button b_throws, 
      final Combo modifierNames, final Text m_methodParamsText, final Combo m_returnTypeText, final Text m_methodNameText){
    if(StringUtils.isEmptyString(m_returnTypeText.getText())){
      updateStatus("Method Return Type cannot be empty");
      return false;        
    } 
    if(StringUtils.isEmptyString(m_methodNameText.getText())){
      updateStatus("Method Name cannot be empty");
      return false;        
    }
    
/*    m_methodSignature.put(atomicIntegerForWritingJavaContent.addAndGet(1), new HashMap<String, String> () {{
      put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
      put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
      put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
      put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
      put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
      put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
      put(METHOD_MODIFIER, StringUtils.isEmptyString(modifierNames.getText())?EMPTY:modifierNames.getText());
    }});    
    
    //set hover logic in the group -- get group object and set tool tip
    Group  g = (Group)mainMethodGoup.get(0);
    //update tool tip for group
    String toolTip = getMethodsSignature();
    g.setToolTipText(toolTip); 
    */   
    return true;
  }
  
  public void setMethodSignatures(){
    for(int i = 1; i <= m_methodSignatureRowObjects.size(); i++){
      Map<String, Control> methodSign = (Map<String, Control>) m_methodSignatureRowObjects.get(i); 

      final Button b_static = (Button)methodSign.get(METHOD_STATIC);
      final Button b_final = (Button)methodSign.get(METHOD_FINAL);
      final Combo modifierNames = (Combo)methodSign.get(METHOD_MODIFIER);
      final Combo m_returnTypeText = (Combo)methodSign.get(METHOD_RETURN_TYPE);
      final Text m_methodNameText = (Text)methodSign.get(METHOD_NAME);
      final Text m_methodParamsText = (Text)methodSign.get(METHOD_PARAMS_TYPE);
      final Button b_throws = (Button)methodSign.get(METHOD_THROWS_CLAUSE);   
      
      if(!StringUtils.isEmptyString(m_returnTypeText.getText()) && !StringUtils.isEmptyString(m_methodNameText.getText())) {
    
        m_methodSignature.put(atomicIntegerForWritingJavaContent.addAndGet(1), new HashMap<String, String> () {{
          put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
          put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
          put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
          put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
          put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
          put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
          put(METHOD_MODIFIER, StringUtils.isEmptyString(modifierNames.getText())?EMPTY:modifierNames.getText());
        }}); 
        
      }
    }
  }
  
  public void setMethodSignaturesDuplicate(){
    Map<Integer, Map<String, String>> m_methodSignatureD = new HashMap<>();
    AtomicInteger atomicIntegerForWritingJavaContentD = new AtomicInteger(0);
    
    for(int i = 1; i <= m_methodSignatureRowObjects.size(); i++){
      Map<String, Control> methodSign = (Map<String, Control>) m_methodSignatureRowObjects.get(i); 

      final Button b_static = (Button)methodSign.get(METHOD_STATIC);
      final Button b_final = (Button)methodSign.get(METHOD_FINAL);
      final Combo modifierNames = (Combo)methodSign.get(METHOD_MODIFIER);
      final Combo m_returnTypeText = (Combo)methodSign.get(METHOD_RETURN_TYPE);
      final Text m_methodNameText = (Text)methodSign.get(METHOD_NAME);
      final Text m_methodParamsText = (Text)methodSign.get(METHOD_PARAMS_TYPE);
      final Button b_throws = (Button)methodSign.get(METHOD_THROWS_CLAUSE);   
      
      if(!StringUtils.isEmptyString(m_returnTypeText.getText()) && !StringUtils.isEmptyString(m_methodNameText.getText())) {
    
        m_methodSignatureD.put(atomicIntegerForWritingJavaContentD.addAndGet(1), new HashMap<String, String> () {{
          put(METHOD_RETURN_TYPE, StringUtils.isEmptyString(m_returnTypeText.getText())?EMPTY:m_returnTypeText.getText());
          put(METHOD_NAME, StringUtils.isEmptyString(m_methodNameText.getText())?EMPTY:m_methodNameText.getText());
          put(METHOD_PARAMS_TYPE, StringUtils.isEmptyString(m_methodParamsText.getText())?EMPTY:m_methodParamsText.getText());
          put(METHOD_STATIC, b_static.getSelection()?STATIC:EMPTY);
          put(METHOD_FINAL, b_final.getSelection()?FINAL:EMPTY);
          put(METHOD_THROWS_CLAUSE, b_throws.getSelection()?THROWS+SPACE+EXCEPTION:EMPTY);
          put(METHOD_MODIFIER, StringUtils.isEmptyString(modifierNames.getText())?EMPTY:modifierNames.getText());
        }}); 
        
      }
    }    
    
    //set hover logic in the group -- get group object and set tool tip
    Group  g = (Group)mainMethodGoup.get(0);
    //update tool tip for group
    String toolTip = getMethodsSignature(m_methodSignatureD, atomicIntegerForWritingJavaContentD);
    g.setToolTipText(toolTip);    
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
  
  public Map<Integer, Map<String, Control>> getMethodSignatureRowObjects(){
    return m_methodSignatureRowObjects;
  }  
  
  public AtomicInteger getAtomicInteger(){
    return atomicInteger;
  }
  
  public String getSampleText(){
    String str = "\n      public String functionalMethod() {  \n       \n     }\n ";
    return str;
  } 
  
  public String getMethodsSignature(Map<Integer, Map<String, String>> map, AtomicInteger atomicIntegerForWritingJavaContentD){
    StringBuilder methods = new StringBuilder();
    for(int i = 1 ; i <= atomicIntegerForWritingJavaContentD.get(); i++){
      Map<String, String> obj = map.get(i);
      if(obj != null){
        methods.append("\n"
            + TAB + TAB
            + obj.get(METHOD_MODIFIER)+SPACE
            + (!StringUtils.isEmptyString(obj.get(METHOD_STATIC))?obj.get(METHOD_STATIC)+SPACE:EMPTY)
            + (!StringUtils.isEmptyString(obj.get(METHOD_FINAL))?obj.get(METHOD_FINAL)+SPACE:EMPTY)
            +obj.get(METHOD_RETURN_TYPE)+ SPACE
            + obj.get(METHOD_NAME)+SPACE
            + OPEN_BRACE  +SPACE
            + (!StringUtils.isEmptyString(obj.get(METHOD_PARAMS_TYPE)) ? obj.get(METHOD_PARAMS_TYPE)+SPACE : EMPTY)
            + CLOSE_BRACE +SPACE   
            + (!StringUtils.isEmptyString(obj.get(METHOD_THROWS_CLAUSE)) ? SPACE+obj.get(METHOD_THROWS_CLAUSE) : EMPTY)
            + TAB+"{\n \n"
            + TAB+TAB+"}\n");     
      }
    }
    return methods.toString();
  }
}