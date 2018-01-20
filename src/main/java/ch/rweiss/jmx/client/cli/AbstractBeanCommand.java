package ch.rweiss.jmx.client.cli;

import java.util.List;

import ch.rweiss.jmx.client.MBean;
import ch.rweiss.jmx.client.MBeanFilter;
import picocli.CommandLine.Parameters;

public abstract class AbstractBeanCommand extends AbstractJmxClientCommand
{
  @Parameters(index="0", arity="0..1", paramLabel="BEAN", description="Bean name or filter with wildcards. E.g *:*, java.lang:*, java.lang:type=Memory")
  private String beanNameOrFilter = "*:*";

  protected List<MBean> getBeans()
  {
    return getJmxClient().beansThatMatch(MBeanFilter.with(beanNameOrFilter));
  }
  
}