package ch.rweiss.jmx.client.cli.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.rweiss.jmx.client.cli.AbstractJmxClientCommand;
import ch.rweiss.jmx.client.cli.chart.config.UnitConverter;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannel;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelFactory;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelScanner;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelSpecification;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelSpecification.Function;
import ch.rweiss.terminal.Position;
import ch.rweiss.terminal.chart.XYChart;
import ch.rweiss.terminal.chart.serie.Axis;
import ch.rweiss.terminal.chart.serie.DataSerie;
import ch.rweiss.terminal.chart.serie.RollingTimeSerie;
import ch.rweiss.terminal.chart.unit.Unit;
import ch.rweiss.terminal.graphics.Point;
import ch.rweiss.terminal.graphics.Rectangle;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name="user", description="User defined chart")
public class ChartUserDefined extends AbstractJmxClientCommand
{
  @Option(names = {"-d", "--delta"}, description = "Displays delta instead of absolute values")
  protected boolean delta;

  @Option(names = {"-H", "--height"}, description = "Height of the chart")
  private int height = -1;
  
  @Option(names = {"-w", "--width"}, description = "Width of the chart")
  private int width = -1;
  
  @Option(names = {"-u", "--unit"}, description = "Unit of the values")
  private String unit = "";
  
  @Option(names = {"-t", "--title"}, description = "Chart title")
  private String title = "";

  @Parameters(index="0", arity="0..*", paramLabel="VALUES", description="List of attribute names which values should be displayed")
  private List<String> beanAttributeNames = new ArrayList<>();

  private XYChart chart;
  private DataChannelScanner scanner = new DataChannelScanner();
  private List<DataChannelSerie> dataChannelSeries = new ArrayList<>();
  private ColorGenerator colorGenerator = new ColorGenerator();
  
  @Override
  protected void printTitle()
  {
    term.write(title);
  }
  
  @Override
  protected void execute()
  {
    ensureChart();

    term.clear().screen();
    
    scanner.scanNow();
    for (DataChannelSerie channel : dataChannelSeries)
    {
      channel.addDataPoint();
    }
    chart.paint();
  }

  private void ensureChart()
  {
    if (chart == null)
    {
      DataChannelFactory factory = new DataChannelFactory(jmxClient);
      for (String beanAttributeName : beanAttributeNames)
      {        
        if (delta)
        {
          beanAttributeName += "->"+Function.DELTA.name();
        }
        DataChannelSpecification specification = new DataChannelSpecification(beanAttributeName);
        List<DataChannel> dataChannels = factory.createFor(specification);
        for (DataChannel dataChannel : dataChannels)
        {
          Axis yAxis = new Axis(dataChannel.name(), getUnit());
          RollingTimeSerie serie = new RollingTimeSerie(yAxis, 60, TimeUnit.SECONDS, colorGenerator.nextColor());
          dataChannelSeries.add(new DataChannelSerie(dataChannel, serie));
          scanner.add(dataChannel);
        }
      }      
      chart = new XYChart(title, getChartWindow(), 
          dataChannelSeries.stream().map(channel -> channel.serie).toArray(DataSerie[]::new));
    }
    else
    {
      chart.setWindow(getChartWindow());
    }
  }

  private Rectangle getChartWindow()
  {
    int w = width;
    int h = height;
    
    if (width < 0 || height < 0)
    {
      Position maxPosition = term.cursor().maxPosition();
      if (width < 0)
      {
        w = maxPosition.column()-1;
      }
      if (height < 0)
      {
        h = maxPosition.line()-1;
      }        
    }
    return new Rectangle(Point.ORIGIN, w, h);
  }

  private Unit getUnit()
  {
    return new UnitConverter(unit).toUnit();
  }

  private static class DataChannelSerie
  {
    private final DataChannel dataChannel;
    private final RollingTimeSerie serie;

    private DataChannelSerie(DataChannel dataChannel, RollingTimeSerie serie)
    {
      this.dataChannel = dataChannel;
      this.serie = serie;
    }
    
    private void addDataPoint()
    {
      serie.addDataPoint(toLong(dataChannel.value()));
    }

    private static long toLong(Object value)
    {
      if (value instanceof Long)
      {
        return (long)value;
      }
      return -1;
    }
  }
}