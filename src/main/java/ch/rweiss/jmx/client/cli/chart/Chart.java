package ch.rweiss.jmx.client.cli.chart;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import ch.rweiss.jmx.client.cli.AbstractJmxClientCommand;
import ch.rweiss.jmx.client.cli.chart.config.ChartConfig;
import ch.rweiss.jmx.client.cli.chart.config.ChartConfigLoader;
import ch.rweiss.jmx.client.cli.chart.config.Serie;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannel;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelFactory;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelScanner;
import ch.rweiss.jmx.client.cli.chart.data.channel.DataChannelSpecification;
import ch.rweiss.terminal.Color;
import ch.rweiss.terminal.Position;
import ch.rweiss.terminal.chart.XYChart;
import ch.rweiss.terminal.chart.serie.Axis;
import ch.rweiss.terminal.chart.serie.DataSerie;
import ch.rweiss.terminal.chart.serie.RollingTimeSerie;
import ch.rweiss.terminal.graphics.Point;
import ch.rweiss.terminal.graphics.Rectangle;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "chart", description="Draws a chart")
public class Chart extends AbstractJmxClientCommand
{
  @Option(names = {"-H", "--height"}, description = "Height of the chart")
  private int height = -1;
  
  @Option(names = {"-W", "--width"}, description = "Width of the chart")
  private int width = -1;
   
  @Option(names = {"-u", "--unit"}, description = "Unit of the values 'user' chart only")
  private String unit = "";
  
  @Option(names = {"-t", "--title"}, description = "Chart title ('user' chart only)")
  private String title = "";

  @Parameters(index="0", arity="1", paramLabel="CHART", 
      description="Name of the chart that should be displayed (use 'user' if you want to specify DATA-CHANNELS")
  private String chartName;

  @Parameters(index="1", arity="0..*", paramLabel="DATA-CHANNELS", 
      description= {"List of data channel specifications. The values of the data channel are displayed in the chart.",
                    "Format: MBeanName.AttributeName{->PostProcessFunction}.",
                    "MBeanName can contain wildcard *. Post processor functions can be DELTA or PERCENTAGE.",
                    "Examples:",
                    "-java.lang:type=Threading.ThreadCount",
                    "-java.lang:type=OperatingSystem.ProcessCpuLoad->PERCENTAGE ",
                    "-java.lang:type=GarbageCollector,name=*.CollectionTime->DELTA"})
  private List<String> dataChannelSpecifications = new ArrayList<>();

  private static final String USER_CHART = "user";

  private XYChart chart;
  private DataChannelScanner scanner = new DataChannelScanner();
  private List<DataChannelSerie> dataChannelSeries = new ArrayList<>();
  private ColorGenerator colorGenerator = new ColorGenerator();

  public Chart()
  {
    super("Chart");
    interval = 1;
  }
  
  @Override
  protected void printHeader()
  {
    // Do not print header
  }
  
  @Override
  protected void execute()
  {
    ensureChart();

    term.offScreen().on();
    try
    {
      scanner.scanNow();
      for (DataChannelSerie channel : dataChannelSeries)
      {
        channel.addDataPoint();
      }
      chart.paint(term.graphics());
      term.offScreen().syncToScreen();
    }
    finally
    {
      term.offScreen().off();
    }
  }
  
  private void ensureChart()
  {
    if (chart == null)
    {
      ChartConfig config = loadOrDefineChartConfig();
      DataChannelFactory factory = new DataChannelFactory(jmxClient);
      for (Serie serieConfig : config.getSeries())
      {        
        DataChannelSpecification specification = new DataChannelSpecification(serieConfig.getDataChannel());
        List<DataChannel> dataChannels = factory.createFor(specification, serieConfig.getName());
        for (DataChannel dataChannel : dataChannels)
        {
          Axis yAxis = new Axis(dataChannel.name(), serieConfig.getUnit());
          RollingTimeSerie serie = new RollingTimeSerie(yAxis, 60, TimeUnit.SECONDS, ensureColor(serieConfig.getColor()));
          dataChannelSeries.add(new DataChannelSerie(dataChannel, serie));
          scanner.add(dataChannel);
        }
      }      
      chart = new XYChart(config.getTitle(), getChartBounds(), 
          dataChannelSeries.stream().map(channel -> channel.serie).toArray(DataSerie[]::new));
    }
    else
    {
      chart.bounds(getChartBounds());
    }
  }

  private ChartConfig loadOrDefineChartConfig()
  {
    if (USER_CHART.equalsIgnoreCase(chartName))
    {
      ChartConfig config = new ChartConfig();
      config.setTitle(title);
      for (String dataChannelSpecification : dataChannelSpecifications)
      {        
        Serie serie = new Serie();
        serie.setDataChannel(dataChannelSpecification);
        if (StringUtils.isNotBlank(unit))
        {
          serie.setUnit(unit);
        }
        config.getSeries().add(serie);
      }
      return config;
    }
    return new ChartConfigLoader().load(chartName);
  }

  private Color ensureColor(Color color)
  {
    if (color == null)
    {
      return colorGenerator.nextColor();
    }
    return color;
  }

  private Rectangle getChartBounds()
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
  
  public static class DataChannelSerie
  {
    private final DataChannel dataChannel;
    public final RollingTimeSerie serie;

    public DataChannelSerie(DataChannel dataChannel, RollingTimeSerie serie)
    {
      this.dataChannel = dataChannel;
      this.serie = serie;
    }
    
    public void addDataPoint()
    {
      long value = toLong(dataChannel.value());
      serie.addDataPoint(value);
    }

    private static long toLong(Object value)
    {
      if (value instanceof Long)
      {
        return (long)value;
      }
      if (value instanceof Number)
      {
        return ((Number)value).longValue();
      }
      return -1;
    }
  }

}
