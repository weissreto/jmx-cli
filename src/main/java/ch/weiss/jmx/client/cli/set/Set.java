package ch.weiss.jmx.client.cli.set;

import ch.weiss.jmx.client.cli.AbstractCommand;
import picocli.CommandLine.Command;

@Command(name = "set", description="Sets values", subcommands = {
    SetAttribute.class})
public class Set extends AbstractCommand
{
  @Override
  public void run()
  {    
    new SetAttribute().run();
  }
}
