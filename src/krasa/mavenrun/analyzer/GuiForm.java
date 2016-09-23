package krasa.mavenrun.analyzer;

import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.application.ex.ApplicationInfoEx;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.components.JBList;
import com.intellij.util.text.VersionComparatorUtil;
import krasa.mavenrun.analyzer.action.LeftTreePopupHandler;
import krasa.mavenrun.analyzer.action.RightTreePopupHandler;
import krasa.mavenrun.model.SortableListDataModel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.model.MavenArtifact;
import org.jetbrains.idea.maven.model.MavenArtifactNode;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.server.MavenServerManager;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;

/**
 * @author Vojtech Krasa
 */
public class GuiForm {
    private static final Logger LOG = Logger.getInstance("#krasa.mavenrun.analyzer.GuiForm");

    public static final String WARNING = "Your settings indicates, that conflicts will not be visible, see IDEA-133331\n"
            + "If your project is Maven2 compatible, you could try one of the following:\n"
            + "-use IJ 2016.1+ and configure it to use external Maven 3.1.1+ (File | Settings | Build, Execution, Deployment | Build Tools | Maven | Maven home directory)\n"
            + "-press Apply Fix button to alter Maven VM options for importer (might cause trouble for IJ 2016.1+)\n"
            + "-turn off File | Settings | Build, Execution, Deployment | Build Tools | Maven | Importing | Use Maven3 to import project setting\n";
    protected static final Comparator<MavenArtifactNode> BY_ARTICATF_ID = new Comparator<MavenArtifactNode>() {
        @Override
        public int compare(MavenArtifactNode o1, MavenArtifactNode o2) {
            return o1.getArtifact().getArtifactId().compareTo(o2.getArtifact().getArtifactId());
        }
    };

    private final Project project;
    private final VirtualFile file;
    private MavenProject mavenProject;
    private JBList leftPanelList;
    private JTree rightTree;
    private JPanel rootPanel;

    private JRadioButton allDependenciesAsListRadioButton;
    private JRadioButton conflictsRadioButton;
    private JRadioButton allDependenciesAsTreeRadioButton;

    private JLabel noConflictsLabel;
    private JScrollPane noConflictsWarningLabelScrollPane;
    private JTextPane noConflictsWarningLabel;
    private JButton refreshButton;
    private JSplitPane splitPane;
    private SearchTextField searchField;
    private JButton applyMavenVmOptionsFixButton;
    private JPanel leftPanelWrapper;
    private JTree leftTree;
    private JCheckBox showGroupId;
    private JPanel buttonsPanel;
    protected SortableListDataModel listDataModel;
    protected Map<String, List<MavenArtifactNode>> allArtifactsMap;
    protected final DefaultTreeModel rightTreeModel;
    protected final DefaultTreeModel leftTreeModel;
    protected final DefaultMutableTreeNode rightTreeRoot;
    protected final DefaultMutableTreeNode leftTreeRoot;
    protected ListSpeedSearch myListSpeedSearch;
    protected List<MavenArtifactNode> dependencyTree;
    protected CardLayout leftPanelLayout;

    public GuiForm(final Project project, VirtualFile file, final MavenProject mavenProject) {
        this.project = project;
        this.file = file;
        this.mavenProject = mavenProject;
        final ActionListener radioButtonListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateLeftPanel();
            }
        };
        conflictsRadioButton.addActionListener(radioButtonListener);
        allDependenciesAsListRadioButton.addActionListener(radioButtonListener);
        allDependenciesAsTreeRadioButton.addActionListener(radioButtonListener);

        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initializeModel();
                rootPanel.requestFocus();
            }
        });

        myListSpeedSearch = new ListSpeedSearch(leftPanelList);
        searchField.addDocumentListener(new DocumentAdapter() {
            @Override
            protected void textChanged(DocumentEvent documentEvent) {
                updateLeftPanel();
            }
        });
        try {
            Method searchField = this.searchField.getClass().getMethod("getTextEditor");
            JTextField invoke = (JTextField) searchField.invoke(this.searchField);
            invoke.addFocusListener(new FocusAdapter() {
                @Override
                public void focusLost(FocusEvent e) {
                    if (GuiForm.this.searchField.getText() != null) {
                        GuiForm.this.searchField.addCurrentTextToHistory();
                    }
                }
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        noConflictsWarningLabel.setBackground(null);
        applyMavenVmOptionsFixButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String mavenEmbedderVMOptions = MavenServerManager.getInstance().getMavenEmbedderVMOptions();
                int baselineVersion = ApplicationInfoEx.getInstanceEx().getBuild().getBaselineVersion();
                if (baselineVersion >= 140) {
                    mavenEmbedderVMOptions += " -Didea.maven3.use.compat.resolver";
                } else {
                    mavenEmbedderVMOptions += " -Dmaven3.use.compat.resolver";
                }
                MavenServerManager.getInstance().setMavenEmbedderVMOptions(mavenEmbedderVMOptions);
                final MavenProjectsManager projectsManager = MavenProjectsManager.getInstance(project);
                projectsManager.forceUpdateAllProjectsOrFindAllAvailablePomFiles();
                refreshButton.getActionListeners()[0].actionPerformed(e);
            }
        });
        noConflictsWarningLabel.setText(WARNING);
        leftPanelLayout = (CardLayout) leftPanelWrapper.getLayout();

        rightTreeRoot = new DefaultMutableTreeNode();
        rightTreeModel = new DefaultTreeModel(rightTreeRoot);
        rightTree.setModel(rightTreeModel);
        rightTree.setRootVisible(false);
        rightTree.setShowsRootHandles(true);
        rightTree.expandPath(new TreePath(rightTreeRoot.getPath()));
        rightTree.setCellRenderer(new TreeRenderer(showGroupId));
        rightTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        rightTree.addMouseListener(new RightTreePopupHandler(project, mavenProject, rightTree));

        leftTree.addTreeSelectionListener(new LeftTreeSelectionListener());
        leftTreeRoot = new DefaultMutableTreeNode();
        leftTreeModel = new DefaultTreeModel(leftTreeRoot);
        leftTree.setModel(leftTreeModel);
        leftTree.setRootVisible(false);
        leftTree.setShowsRootHandles(true);
        leftTree.expandPath(new TreePath(leftTreeRoot.getPath()));
        leftTree.setCellRenderer(new TreeRenderer(showGroupId));
        leftTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        leftTree.addMouseListener(new LeftTreePopupHandler(project, mavenProject, leftTree));

        showGroupId.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanelList.repaint();
                TreeUtils.nodesChanged(GuiForm.this.rightTreeModel);
                TreeUtils.nodesChanged(GuiForm.this.leftTreeModel);
            }
        });

        final DefaultTreeExpander treeExpander = new DefaultTreeExpander(leftTree);
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(CommonActionsManager.getInstance().createExpandAllAction(treeExpander, leftTree));
        actionGroup.add(CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, leftTree));
        ActionToolbar actionToolbar = ActionManagerEx.getInstance().createActionToolbar("krasa.MavenHelper.buttons",
                actionGroup, true);
        buttonsPanel.add(actionToolbar.getComponent(), "1");
    }

    private void createUIComponents() {
        listDataModel = new SortableListDataModel();
        leftPanelList = new JBList(listDataModel);
        leftPanelList.addListSelectionListener(new MyListSelectionListener());
        // no generics in IJ12
        leftPanelList.setCellRenderer(new ColoredListCellRenderer() {
            @Override
            protected void customizeCellRenderer(JList jList, Object o, int i, boolean b, boolean b2) {
                MyListNode value = (MyListNode) o;
                String maxVersion = value.getMaxVersion();
                final String[] split = value.key.split(":");
                if (showGroupId.isSelected()) {
                    append(split[0] + " : ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                }
                append(split[1], SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                append(" : " + maxVersion, SimpleTextAttributes.REGULAR_ATTRIBUTES);

            }
        });
        rightTree = new MyHighlightingTree();
        leftTree = new MyHighlightingTree();
    }

    public static String sortByVersion(List<MavenArtifactNode> value) {
        Collections.sort(value, new Comparator<MavenArtifactNode>() {
            @Override
            public int compare(MavenArtifactNode o1, MavenArtifactNode o2) {
                DefaultArtifactVersion version = new DefaultArtifactVersion(o1.getArtifact().getVersion());
                DefaultArtifactVersion version1 = new DefaultArtifactVersion(o2.getArtifact().getVersion());
                return version1.compareTo(version);
            }
        });
        return value.get(0).getArtifact().getVersion();
    }

    public static String sortByName(List<MavenArtifactNode> value) {
        Collections.sort(value, new Comparator<MavenArtifactNode>() {
            @Override
            public int compare(MavenArtifactNode o1, MavenArtifactNode o2) {
                return o1.getArtifact().getLibraryName().compareTo(o2.getArtifact().getLibraryName());
            }
        });
        return value.get(0).getArtifact().getVersion();
    };

    private class LeftTreeSelectionListener implements TreeSelectionListener {
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath selectionPath = e.getPath();
            if (selectionPath != null) {
                DefaultMutableTreeNode lastPathComponent = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                MyTreeUserObject userObject = (MyTreeUserObject) lastPathComponent.getUserObject();

                final String key = getArtifactKey(userObject.getArtifact());
                List<MavenArtifactNode> mavenArtifactNodes = allArtifactsMap.get(key);
                if (mavenArtifactNodes != null) {// can be null while refreshing
                    fillRightTree(mavenArtifactNodes, sortByVersion(mavenArtifactNodes));
                }
            }
        }
    }

    private class MyListSelectionListener implements ListSelectionListener {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            if (listDataModel.isEmpty() || leftPanelList.getSelectedValue() == null) {
                return;
            }

            final MyListNode myListNode = (MyListNode) leftPanelList.getSelectedValue();
            List<MavenArtifactNode> artifacts = myListNode.value;
            fillRightTree(artifacts, myListNode.getMaxVersion());
        }
    }

    private void fillRightTree(List<MavenArtifactNode> mavenArtifactNodes, String maxVersion) {
        rightTreeRoot.removeAllChildren();
        for (MavenArtifactNode mavenArtifactNode : mavenArtifactNodes) {
            MyTreeUserObject userObject = MyTreeUserObject.create(mavenArtifactNode, maxVersion);
            userObject.showOnlyVersion = true;
            final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(userObject);
            fillRightTree(mavenArtifactNode, newNode);
            rightTreeRoot.add(newNode);
        }
        rightTreeModel.nodeStructureChanged(rightTreeRoot);
        TreeUtils.expandAll(rightTree);
    }

    private void fillRightTree(MavenArtifactNode mavenArtifactNode, DefaultMutableTreeNode node) {
        final MavenArtifactNode parent = mavenArtifactNode.getParent();
        if (parent == null) {
            return;
        }
        final DefaultMutableTreeNode parentDependencyNode = new DefaultMutableTreeNode(new MyTreeUserObject(parent));
        node.add(parentDependencyNode);
        parentDependencyNode.setParent(node);
        fillRightTree(parent, parentDependencyNode);
    }

    private void initializeModel() {
        final Object selectedValue = leftPanelList.getSelectedValue();

        dependencyTree = mavenProject.getDependencyTree();
        allArtifactsMap = createAllArtifactsMap(dependencyTree);
        updateLeftPanel();

        rightTreeRoot.removeAllChildren();
        rightTreeModel.reload();
        leftPanelWrapper.revalidate();

        if (selectedValue != null) {
            leftPanelList.setSelectedValue(selectedValue, true);
        }
    }

    private void updateLeftPanel() {
        listDataModel.clear();
        leftTreeRoot.removeAllChildren();

        final String searchFieldText = searchField.getText();
        boolean conflictsWarning = false;
        boolean showNoConflictsLabel = false;
        if (conflictsRadioButton.isSelected()) {
            for (Map.Entry<String, List<MavenArtifactNode>> s : allArtifactsMap.entrySet()) {
                final List<MavenArtifactNode> nodes = s.getValue();
                if (nodes.size() > 1 && hasConflicts(nodes)) {
                    if (searchFieldText == null || s.getKey().contains(searchFieldText)) {
                        listDataModel.addElement(new MyListNode(s));
                    }
                }
            }
            showNoConflictsLabel = listDataModel.isEmpty();
            BuildNumber build = ApplicationInfoEx.getInstanceEx().getBuild();
            int baselineVersion = build.getBaselineVersion();
            if (showNoConflictsLabel && baselineVersion >= 139) {
                MavenServerManager server = MavenServerManager.getInstance();
                boolean useMaven2 = server.isUseMaven2();
                boolean contains139 = server.getMavenEmbedderVMOptions().contains("-Dmaven3.use.compat.resolver");
                boolean contains140 = server.getMavenEmbedderVMOptions().contains("-Didea.maven3.use.compat.resolver");
                boolean containsProperty = (baselineVersion == 139 && contains139)
                        || (baselineVersion >= 140 && contains140);
                conflictsWarning = !containsProperty && !useMaven2;

                if (conflictsWarning && VersionComparatorUtil.compare(build.asStringWithoutProductCode(), "145.258") >= 0) {
                    boolean oldMaven = VersionComparatorUtil.compare(MavenServerManager.getInstance().getCurrentMavenVersion(), "3.1.1") < 0;
                    conflictsWarning = conflictsWarning && oldMaven;
                }
            }
            leftPanelLayout.show(leftPanelWrapper, "list");
        } else if (allDependenciesAsListRadioButton.isSelected()) {
            for (Map.Entry<String, List<MavenArtifactNode>> s : allArtifactsMap.entrySet()) {
                if (searchFieldText == null || s.getKey().contains(searchFieldText)) {
                    listDataModel.addElement(new MyListNode(s));
                }
            }
            showNoConflictsLabel = false;
            leftPanelLayout.show(leftPanelWrapper, "list");
        } else { // tree
            fillLeftTree(leftTreeRoot, dependencyTree, searchFieldText);
            leftTreeModel.nodeStructureChanged(leftTreeRoot);
            TreeUtils.expandAll(leftTree);

            showNoConflictsLabel = false;
            leftPanelLayout.show(leftPanelWrapper, "allAsTree");
        }

        listDataModel.sort(new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                String first = ((MyListNode) o1).getKey();
                String second = ((MyListNode) o2).getKey();

                return first.split(":")[1].compareTo(second.split(":")[1]);
            }
        });

        if (conflictsWarning) {
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    noConflictsWarningLabelScrollPane.getVerticalScrollBar().setValue(0);
                }
            });
            leftPanelLayout.show(leftPanelWrapper, "noConflictsWarningLabel");
        }
        buttonsPanel.setVisible(allDependenciesAsTreeRadioButton.isSelected());
        noConflictsWarningLabelScrollPane.setVisible(conflictsWarning);
        applyMavenVmOptionsFixButton.setVisible(conflictsWarning);
        noConflictsLabel.setVisible(showNoConflictsLabel);
    }

    private boolean fillLeftTree(DefaultMutableTreeNode parent, List<MavenArtifactNode> dependencyTree,
                                 String searchFieldText) {
        boolean search = StringUtils.isNotBlank(searchFieldText);
        Collections.sort(dependencyTree, BY_ARTICATF_ID);
        boolean containsFilteredItem = false;

        for (MavenArtifactNode mavenArtifactNode : dependencyTree) {
            SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
            MyTreeUserObject treeUserObject = new MyTreeUserObject(mavenArtifactNode, attributes);
            if (search && contains(searchFieldText, mavenArtifactNode)) {
                containsFilteredItem = true;
                treeUserObject.highlight = true;
            }
            final DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(treeUserObject);
            containsFilteredItem |= fillLeftTree(newNode, mavenArtifactNode.getDependencies(), searchFieldText);

            if (parent == leftTreeRoot) {
                if (search && !containsFilteredItem) {
                    // do not add
                } else {
                    parent.add(newNode);
                }
                containsFilteredItem = false;
            } else {
                parent.add(newNode);
            }
        }

        return containsFilteredItem;
    }

    private boolean contains(String searchFieldText, MavenArtifactNode mavenArtifactNode) {
        MavenArtifact artifact = mavenArtifactNode.getArtifact();
        String displayStringSimple = artifact.getDisplayStringSimple();
        return displayStringSimple.contains(searchFieldText);
    }

    private boolean hasConflicts(List<MavenArtifactNode> nodes) {
        String version = null;
        for (MavenArtifactNode node : nodes) {
            if (version != null && !version.equals(node.getArtifact().getVersion())) {
                return true;
            }
            version = node.getArtifact().getVersion();
        }
        return false;
    }

    private Map<String, List<MavenArtifactNode>> createAllArtifactsMap(List<MavenArtifactNode> dependencyTree) {
        final Map<String, List<MavenArtifactNode>> map = new TreeMap<String, List<MavenArtifactNode>>();
        addAll(map, dependencyTree, 0);
        return map;
    }

    private void addAll(Map<String, List<MavenArtifactNode>> map, List<MavenArtifactNode> artifactNodes, int i) {
        if (map == null) {
            return;
        }
        if (i > 100) {
            final StringBuilder stringBuilder = new StringBuilder();
            for (MavenArtifactNode s : artifactNodes) {
                final String s1 = s.getArtifact().toString();
                stringBuilder.append(s1);
                stringBuilder.append(" ");
            }
            LOG.error("Recursion aborted, artifactNodes = [" + stringBuilder + "]");
            return;
        }
        for (MavenArtifactNode mavenArtifactNode : artifactNodes) {
            final MavenArtifact artifact = mavenArtifactNode.getArtifact();

            final String key = getArtifactKey(artifact);
            final List<MavenArtifactNode> mavenArtifactNodes = map.get(key);
            if (mavenArtifactNodes == null) {
                final ArrayList<MavenArtifactNode> value = new ArrayList<MavenArtifactNode>(1);
                value.add(mavenArtifactNode);
                map.put(key, value);
            } else {
                mavenArtifactNodes.add(mavenArtifactNode);
            }
            addAll(map, mavenArtifactNode.getDependencies(), i + 1);
        }
    }

    @NotNull
    private String getArtifactKey(MavenArtifact artifact) {
        return artifact.getGroupId() + " : " + artifact.getArtifactId();
    }

    public JComponent getRootComponent() {
        return rootPanel;
    }

    public JComponent getPreferredFocusedComponent() {
        return rootPanel;
    }

    public void selectNotify() {
        if (dependencyTree == null) {
            initializeModel();
            splitPane.setDividerLocation(0.5);
        }
    }

}
