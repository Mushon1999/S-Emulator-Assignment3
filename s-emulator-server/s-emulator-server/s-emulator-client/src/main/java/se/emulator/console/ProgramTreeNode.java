package se.emulator.console;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TreeItem;
import se.emulator.engine.*;

/**
 * Tree node representing program execution hierarchy for TreeTableView
 * Supports different node types: Root, Function, Expansion, Command
 */
public class ProgramTreeNode {
    
    public enum NodeType {
        ROOT,           // Program root
        FUNCTION,       // Function definition
        EXPANSION,      // Function expansion/call
        COMMAND         // Individual command (leaf node)
    }
    
    private NodeType nodeType;
    private String displayName;
    private String fullDescription;
    private int depth;
    private int lineNumber;
    private int cycles;
    private String nodeIcon;
    private Object sourceData; // Original instruction, function, etc.
    
    // Tree structure
    private ProgramTreeNode parent;
    private ObservableList<ProgramTreeNode> children;
    
    public ProgramTreeNode(NodeType type, String displayName, String description) {
        this.nodeType = type;
        this.displayName = displayName;
        this.fullDescription = description;
        this.children = FXCollections.observableArrayList();
        this.depth = 0;
        this.lineNumber = -1;
        this.cycles = 0;
        this.nodeIcon = getDefaultIcon(type);
    }
    
    public ProgramTreeNode(NodeType type, String displayName, String description, int depth) {
        this(type, displayName, description);
        this.depth = depth;
    }
    
    /**
     * Create tree node from instruction
     */
    public static ProgramTreeNode fromInstruction(Instruction instruction, int lineNumber, int depth) {
        ProgramTreeNode node = new ProgramTreeNode(
            NodeType.COMMAND,
            formatInstructionDisplay(instruction, lineNumber),
            instruction.toString()
        );
        node.setLineNumber(lineNumber);
        node.setDepth(depth);
        node.setSourceData(instruction);
        return node;
    }
    
    /**
     * Create tree node for function expansion
     */
    public static ProgramTreeNode createFunctionExpansion(String functionName, int depth) {
        ProgramTreeNode node = new ProgramTreeNode(
            NodeType.EXPANSION,
            "▶ " + functionName + " (expansion)",
            "Function expansion: " + functionName
        );
        node.setDepth(depth);
        node.setNodeIcon("🔧");
        return node;
    }
    
    /**
     * Create tree node for function definition
     */
    public static ProgramTreeNode createFunctionDefinition(Function function) {
        ProgramTreeNode node = new ProgramTreeNode(
            NodeType.FUNCTION,
            "📁 " + function.getUserString(),
            "Function: " + function.getUserString()
        );
        node.setNodeIcon("📁");
        node.setSourceData(function);
        return node;
    }
    
    /**
     * Create root node for program
     */
    public static ProgramTreeNode createRootNode(String programName) {
        ProgramTreeNode node = new ProgramTreeNode(
            NodeType.ROOT,
            "🏠 " + programName,
            "Program root: " + programName
        );
        node.setNodeIcon("🏠");
        return node;
    }
    
    /**
     * Add child node
     */
    public void addChild(ProgramTreeNode child) {
        child.setParent(this);
        child.setDepth(this.depth + 1);
        children.add(child);
    }
    
    /**
     * Remove child node
     */
    public void removeChild(ProgramTreeNode child) {
        children.remove(child);
        child.setParent(null);
    }
    
    /**
     * Get TreeItem for JavaFX TreeTableView
     */
    public TreeItem<ProgramTreeNode> createTreeItem() {
        TreeItem<ProgramTreeNode> item = new TreeItem<>(this);
        
        // Add children recursively
        for (ProgramTreeNode child : children) {
            item.getChildren().add(child.createTreeItem());
        }
        
        // Expand by default for better visibility
        item.setExpanded(true);
        
        return item;
    }
    
    /**
     * Format instruction for display in tree
     */
    private static String formatInstructionDisplay(Instruction instruction, int lineNumber) {
        if (instruction instanceof SyntheticInstruction) {
            SyntheticInstruction syn = (SyntheticInstruction) instruction;
            return String.format("%d. S(%s)", lineNumber, syn.getOp().toString());
        } else if (instruction instanceof BasicInstruction) {
            BasicInstruction basic = (BasicInstruction) instruction;
            return String.format("%d. %s", lineNumber, basic.getOp().toString());
        }
        return String.format("%d. %s", lineNumber, instruction.toString());
    }
    
    /**
     * Get default icon for node type
     */
    private String getDefaultIcon(NodeType type) {
        switch (type) {
            case ROOT: return "🏠";
            case FUNCTION: return "📁";
            case EXPANSION: return "🔧";
            case COMMAND: return "⚡";
            default: return "📄";
        }
    }
    
    /**
     * Check if this node is a leaf (has no children)
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
    
    /**
     * Get path from root to this node
     */
    public String getNodePath() {
        if (parent == null) {
            return displayName;
        }
        return parent.getNodePath() + " → " + displayName;
    }
    
    /**
     * Count total nodes in subtree
     */
    public int getSubtreeSize() {
        int size = 1; // Count this node
        for (ProgramTreeNode child : children) {
            size += child.getSubtreeSize();
        }
        return size;
    }
    
    // Getters and Setters
    public NodeType getNodeType() { return nodeType; }
    public void setNodeType(NodeType nodeType) { this.nodeType = nodeType; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getFullDescription() { return fullDescription; }
    public void setFullDescription(String fullDescription) { this.fullDescription = fullDescription; }
    
    public int getDepth() { return depth; }
    public void setDepth(int depth) { this.depth = depth; }
    
    public int getLineNumber() { return lineNumber; }
    public void setLineNumber(int lineNumber) { this.lineNumber = lineNumber; }
    
    public int getCycles() { return cycles; }
    public void setCycles(int cycles) { this.cycles = cycles; }
    
    public String getNodeIcon() { return nodeIcon; }
    public void setNodeIcon(String nodeIcon) { this.nodeIcon = nodeIcon; }
    
    public Object getSourceData() { return sourceData; }
    public void setSourceData(Object sourceData) { this.sourceData = sourceData; }
    
    public ProgramTreeNode getParent() { return parent; }
    public void setParent(ProgramTreeNode parent) { this.parent = parent; }
    
    public ObservableList<ProgramTreeNode> getChildren() { return children; }
    public void setChildren(ObservableList<ProgramTreeNode> children) { this.children = children; }
    
    @Override
    public String toString() {
        return displayName;
    }
}