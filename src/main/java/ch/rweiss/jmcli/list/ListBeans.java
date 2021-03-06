package ch.rweiss.jmcli.list;

import ch.rweiss.jmcli.AbstractCommand;
import ch.rweiss.jmcli.IntervalOption;
import ch.rweiss.jmcli.JvmOption;
import ch.rweiss.jmcli.Styles;
import ch.rweiss.jmcli.executor.AbstractJmxExecutor;
import ch.rweiss.jmcli.ui.CommandUi;
import ch.rweiss.jmx.client.JmxClient;
import ch.rweiss.jmx.client.MBeanTreeNode;
import ch.rweiss.terminal.table.Table;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

public final class ListBeans extends AbstractJmxExecutor
{
  @Command(name = "beans", description="Lists all available management beans")
  public static final class Cmd extends AbstractCommand
  {
    @Mixin
    private IntervalOption intervalOption = new IntervalOption();
    
    @Mixin
    private JvmOption jvmOption = new JvmOption();

    @Override
    public void run()
    {
      new ListBeans(this).execute();
    }
  }
  
  private Table<MBeanTreeNode> table = declareTable();

  ListBeans(Cmd command)
  {
    super("Beans", command.intervalOption, command.jvmOption);
  }

  @Override
  protected void execute(CommandUi ui, JmxClient jmxClient)
  {
    table.clear();

    MBeanTreeNode beanTree = jmxClient.beanTree();    
    addBeans(beanTree);

    table.print();   
  }
  
  private static Table<MBeanTreeNode> declareTable()
  {
    Table<MBeanTreeNode> table = new Table<>();
    table.hideHeader();
    table.addColumn(
        table.createColumn("Name", 30, node -> simpleNameWithIndent(node))
          .withTitleStyle(Styles.NAME_TITLE)
          .withCellStyle(Styles.NAME)
          .withMinWidth(20)
          .toColumn());
    
    table.addColumn(
        table.createColumn("Full Qualified Name", 60, node -> node.name().fullQualifiedName())
          .withTitleStyle(Styles.NAME_TITLE)
          .withCellStyle(Styles.VALUE)
          .withMinWidth(10)
          .toColumn());
    return table;
  }

  private static String simpleNameWithIndent(MBeanTreeNode node)
  {
    StringBuilder builder = new StringBuilder();
    for (int indent = 0; indent < node.name().countParts()-1; indent++)
    {
      builder.append("  ");
    }
    builder.append(node.name().simpleName());
    return builder.toString();
  }

  private void addBeans(MBeanTreeNode node)
  {
    if (node.name().countParts() > 0)
    {
      table.addRow(node);
    }
    for (MBeanTreeNode child : node.children())
    {
      addBeans(child);
    }
  }
}
