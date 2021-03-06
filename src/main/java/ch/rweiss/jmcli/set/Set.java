package ch.rweiss.jmcli.set;

import ch.rweiss.jmcli.AbstractCommand;
import picocli.CommandLine.Command;

@Command(name = "set", description="Sets values", subcommands = {
    SetAttribute.Cmd.class})
public class Set extends AbstractCommand
{
  @Override
  public void run()
  {    
    new SetAttribute.Cmd().run();
  }
}
